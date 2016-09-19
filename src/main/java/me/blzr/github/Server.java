package me.blzr.github;

import org.apache.http.ExceptionLogger;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Server {
    private static final Logger log = LogManager.getLogger(App.class);

    private final int port;
    private final Database database;

    private final HttpServer server;

    public Server(int port, Database database) {
        this.port = port;
        this.database = database;

        final BasicAsyncRequestHandler requestHandler = new BasicAsyncRequestHandler((httpRequest, httpResponse, httpContext) -> {
            String result;
            try {
                result = String.format("<H1>Server stats</H1>" +
                                "<STYLE>" +
                                "table, th, td {" +
                                "  border: 1px solid black;" +
                                "  border-collapse: collapse" +
                                "}" +
                                "</STYLE>" +
                                "<DIV>Subscribe at <A href='http://telegram.me/git_hub_bot'>Telegram</A></DIV>" +
                                "<DIV>Sources at <A href='https://github.com/theaspect/github-bot'>GitHub</A></DIV>" +
                                "<BR/>" +
                                "<TABLE>" +
                                "  <TR><TH>Repository</TH><TH>Events</TH><TH>Subscribers</TH><TH>Latest</TH></TR>" +
                                "  %s" +
                                "</TABLE>",
                        database.getStats().stream().map(Database.Stat::toString).collect(Collectors.joining()));
                httpResponse.setStatusCode(HttpStatus.SC_OK);
            } catch (Exception e) {
                log.error("Cannot get stats", e);
                result = "Can't get stats";
                httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            httpResponse.setEntity(new NStringEntity(result, ContentType.TEXT_HTML));
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
