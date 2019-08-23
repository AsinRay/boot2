package net.bittx.conf;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Shut down embedded servlet container gracefully
 * <p>
 * visit https://github.com/spring-projects/spring-boot/issues/4657
 * or https://github.com/spring-projects/spring-boot/issues/14074
 * for more details.
 */
@Configuration
public class BeanConf {

    @Bean
    public GracefulUndertowShutdown GracefulUndertowShutdown() {
        return new GracefulUndertowShutdown();
    }

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer(GracefulUndertowShutdown gracefulUndertowShutdown) {
        return (factory) -> {
            factory.addDeploymentInfoCustomizers((builder) -> {
                builder.addInitialHandlerChainWrapper(gracefulUndertowShutdown);
            });
        };
    }

    private static class GracefulUndertowShutdown
            implements ApplicationListener<ContextClosedEvent>, HandlerWrapper {

        private volatile GracefulShutdownHandler handler;

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            this.handler = new GracefulShutdownHandler(handler);
            return this.handler;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            try {
                this.handler.shutdown();
                this.handler.awaitShutdown(30000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
