package me.blzr.github;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.TelegramBotsApi;

import java.io.InputStream;
import java.util.Properties;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        log.debug("Reading properties");
        Properties prop = new Properties();
        InputStream is = App.class.getResourceAsStream("/config.propertiess");
        if (is != null) {
            prop.load(is);
        }

        final String url = prop.getProperty("url", System.getenv("URL"));
        final String user = prop.getProperty("user", System.getenv("USER"));
        final String password = prop.getProperty("password", System.getenv("PASSWORD"));
        final String username = prop.getProperty("username", System.getenv("USERNAME"));
        final String token = prop.getProperty("token", System.getenv("TOKEN"));

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
