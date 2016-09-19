package me.blzr.github;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.TelegramBotsApi;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        log.debug("Reading properties");
        Properties prop = new Properties();
        InputStream is = App.class.getResourceAsStream("/config.properties");
        if (is != null) {
            prop.load(is);
        }

        final String url = prop.getProperty("url", System.getenv("URL"));
        final String user = prop.getProperty("user", System.getenv("USER"));
        final String password = prop.getProperty("password", System.getenv("PASSWORD"));
        final String username = prop.getProperty("username", System.getenv("USERNAME"));
        final String token = prop.getProperty("token", System.getenv("TOKEN"));
        final String port = prop.getProperty("port", System.getenv("PORT"));

        if (port != null) {
            new Thread(() -> {
                log.debug("Binding to {}", port);
                try {
                    ServerSocket srv = new ServerSocket(Integer.valueOf(port));
                    Socket s = srv.accept();

                    /*BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    while (true) {
                        String cominginText = "";
                        try {
                            cominginText = in.readLine();
                            log.debug("Received on socket: {}", cominginText);
                        } catch (IOException e) {
                            log.error("Connection to server lost!", e);
                            System.exit(1);
                        }
                    }*/
                } catch (IOException e) {
                    log.error("Cannot bind to Socket " + port, e);
                }
            }).start();
        }


        Preconditions.checkNotNull(url, "URL should not be null");
        Preconditions.checkNotNull(user, "USER should not be null");
        Preconditions.checkNotNull(password, "PASSWORD should not be null");
        Preconditions.checkNotNull(username, "USERNAME should not be null");
        Preconditions.checkNotNull(token, "TOKEN should not be null");

        // TODO Connection pool
        Database telegramDb = new Database(url, user, password);
        Database eventsDb = new Database(url, user, password);
        Database gitHubDb = new Database(url, user, password);

        new TelegramBotsApi().registerBot(new Bot(telegramDb, username, token));

        GitHub gitHub = new GitHub();
        new Watcher(eventsDb, gitHubDb, gitHub);
        // TODO join to threads
    }
}
