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

    private final int port;
    private final Database database;

    private final HttpServer server;

    public Server(int port, Database database) {
        this.port = port;
        this.database = database;

        final BasicAsyncRequestHandler requestHandler = new BasicAsyncRequestHandler((httpRequest, httpResponse, httpContext) -> {
            httpResponse.setStatusCode(HttpStatus.SC_OK);
            // TODO add stats
            httpResponse.setEntity(new NStringEntity("It Works!"));
        });

        server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("GitHubBot/1.0")
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .registerHandler("*", requestHandler)
                .create();
    }

    public void start() throws IOException {
        log.debug("Starting server on {}", port);

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdown(5, TimeUnit.SECONDS)));
    }
}
