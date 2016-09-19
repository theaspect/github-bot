package me.blzr.github;

import org.apache.http.ExceptionLogger;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final Logger log = LogManager.getLogger(App.class);

    public Server(int port) throws IOException {
        log.debug("Starting server on {}", port);

        final BasicAsyncRequestHandler requestHandler = new BasicAsyncRequestHandler((httpRequest, httpResponse, httpContext) -> {
            httpResponse.setStatusCode(HttpStatus.SC_OK);
            httpResponse.setEntity(new NStringEntity("It Works!"));
        });

        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("GitHubBot/1.0")
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .registerHandler("*", requestHandler)
                .create();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdown(5, TimeUnit.SECONDS)));
    }
}
