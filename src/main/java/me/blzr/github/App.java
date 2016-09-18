package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.TelegramBotsApi;

import java.util.Properties;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        log.debug("Reading properties");
        Properties prop = new Properties();
        prop.load(App.class.getResourceAsStream("/config.properties"));

        // TODO Connection pool
        Database telegramDb = new Database(prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));
        Database eventsDb = new Database(prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));
        Database gitHubDb = new Database(prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));

        new TelegramBotsApi().registerBot(new Bot(telegramDb, prop.getProperty("username"), prop.getProperty("token")));

        GitHub gitHub = new GitHub();
        new Watcher(eventsDb, gitHubDb, gitHub);
        // TODO join to threads
    }
}
