package me.blzr.github;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;

import java.io.IOException;
import java.util.Properties;

public class App {
    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        prop.load(App.class.getResourceAsStream("/config.properties"));


        Database database = new Database();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new GitHubBot(database,
                    prop.getProperty("username"),
                    prop.getProperty("token")));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
