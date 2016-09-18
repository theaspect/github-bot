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

        Database database = new Database(
                prop.getProperty("url"),
                prop.getProperty("user"),
                prop.getProperty("password"));

        Repository repository = new Repository();

        new TelegramBotsApi().registerBot(
                new Bot(database,
                    prop.getProperty("username"),
                    prop.getProperty("token")));
    }
}
