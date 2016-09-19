package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.updateshandlers.SentCallback;

import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Collectors;

import static me.blzr.github.Util.tryParse;

public class Bot extends TelegramLongPollingBot {
    private static final Logger log = LogManager.getLogger(Bot.class);

    private String botUsername;
    private String botToken;
    private Database database;

    public Bot(Database database, String botUsername, String botToken) {
        log.debug("Connectiong to Telegram");
        this.database = database;
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public void onUpdateReceived(Update update) {
        Message m = update.getMessage();
        try {
            if (m == null || m.getText() == null) {
                // Do nothing
                return;
            } else if (m.getText().startsWith(Commands.START)) {
                onStart(m);
            } else if (m.getText().startsWith(Commands.STOP)) {
                onStop(m);
            } else if (m.getText().startsWith(Commands.HELP)) {
                onHelp(m);
            } else if (m.getText().startsWith(Commands.SETTINGS)) {
                onSettings(m);
            } else if (m.getText().startsWith(Commands.ADD)) {
                onAdd(m);
            } else if (m.getText().startsWith(Commands.REMOVE)) {
                onRemove(m);
            } else {
                onUnknown(m);
            }
        } catch (Exception e) {
            log.error("Cannot execute command " + m.getText(), e);
        }
    }

    private void onUnknown(Message m) {
        log.debug("Unknown request from {}: {}", m.getChatId(), m.getText());

        sendReply(m, "Unknown command, try /help");
    }

    private void onRemove(Message m) throws SQLException {
        log.debug("Remove request from {}: {}", m.getChatId(), m.getText());

        Collection<String> repos = database.remove(m.getChatId(), tryParse(m.getText()));

        String reply = "You unsubscribed from " + repos.size() + " repos";
        sendReply(m, reply);
    }

    private void onAdd(Message m) throws SQLException {
        log.debug("/add request from {}: {}", m.getChatId(), m.getText());

        Collection<String> repos = database.add(m.getChatId(), tryParse(m.getText()));

        String reply = "You subscribed to " + repos.size() + " repos";
        sendReply(m, reply);
    }

    private void onSettings(Message m) throws SQLException {
        log.debug("/settings request from {}: {}", m.getChatId(), m.getText());

        String reply = "You subscribed to the following repos:\n" +
                database.getSubscriptions(m.getChatId()).stream()
                        .map(repo -> "https://github.com/" + repo)
                        .collect(Collectors.joining("\n"));
        sendReply(m, reply);
    }

    private void onStop(Message m) throws SQLException {
        log.debug("/stop request from {}: {}", m.getChatId(), m.getText());
        database.removeAll(m.getChatId());
        sendReply(m, "Removed all your subscriptions");
    }

    private void onHelp(Message m) {
        log.debug("/help request from {}: {}", m.getChatId(), m.getText());
        sendReply(m,
                "Bot understand following commands\n" +
                        "/help – shows this help\n" +
                        "/settings – lists all your subscriptions \n" +
                        "/add https://github.com/example [https://github.com/example ...] – add subscription on users\n" +
                        "/remove https://github.com/example [https://github.com/example ...] – remove subscription on users\n" +
                        "/stop – remove all subscriptions\n");
    }

    private void onStart(Message m) {
        log.debug("/start request from {}: {}", m.getChatId(), m.getText());
        sendReply(m,
                "This is GitHub bot\n" +
                        "which sends you notifications\n" +
                        "about github events\n" +
                        "Try /help to see available commands");
    }

    private void sendReply(Message m, String message) {
        sendReply(m, message, () -> {
        });
    }

    private void sendReply(Message m, String message, Runnable runnable) {
        SendMessage reply = new SendMessage();
        reply.setChatId(m.getChatId().toString());
        reply.setText(message);
        try {
            sendMessageAsync(reply, new SentCallback<Message>() {
                public void onResult(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                    Message sentMessage = botApiMethod.deserializeResponse(jsonObject);
                    if (sentMessage != null) {
                        runnable.run();
                    }
                }

                public void onError(BotApiMethod<Message> botApiMethod, JSONObject jsonObject) {
                    log.error("Error in send reply {}", jsonObject);
                }

                public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                    log.error("Exception in send reply {}", e);
                }
            });
        } catch (TelegramApiException e) {
            log.debug("Telegram exception", e);
        }
    }
}
