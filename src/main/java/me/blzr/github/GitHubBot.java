package me.blzr.github;

import org.json.JSONObject;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.updateshandlers.SentCallback;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class GitHubBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;
    private Database database;

    public GitHubBot(Database database, String botUsername, String botToken) {
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
    }

    private void onUnknown(Message m) {
        sendReply(m, "Unknown command, try /help");
    }

    private void onRemove(Message m) {
        Collection<String> repos = database.remove(tryParse(m.getText()));

        String reply = "You unsubscribed from " + repos.size() + " repos";
        sendReply(m, reply);
    }

    private void onAdd(Message m) {
        Collection<String> repos = database.add(tryParse(m.getText()));

        String reply = "You subscribed to " + repos.size() + " repos";
        sendReply(m, reply);
    }

    private void onSettings(Message m) {
        String reply = "You subscribed to the following repos:\n" +
                database.getSubscriptions(m.getChatId()).stream().collect(Collectors.joining("\n"));
        sendReply(m, reply);
    }

    private void onStop(Message m) {
        database.removeAll(m.getChatId());
        sendReply(m, "Removed all your subscriptions");
    }

    private void onHelp(Message m) {
        sendReply(m,
                "Bot understand following commands\n" +
                        "/help – shows this help\n" +
                        "/settings – lists all your subscriptions \n" +
                        "/add https://github.com/example [https://github.com/example ...] – add subscription on users\n" +
                        "/remove https://github.com/example [https://github.com/example ...] – remove subscription on users\n" +
                        "/stop – remove all subscriptions\n");
    }

    private void onStart(Message m) {
        sendReply(m,
                "This is GitHub bot\n" +
                        "which sends you notifications\n" +
                        "about github events\n" +
                        "Try /help to see available commands");
    }

    void sendReply(Message m, String message) {
        sendReply(m, message, () -> {
        });
    }

    void sendReply(Message m, String message, Runnable runnable) {
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
                }

                public void onException(BotApiMethod<Message> botApiMethod, Exception e) {
                }
            });
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    Set<String> tryParse(String text) {
        return new HashSet<String>(Arrays.asList("1", "2", "3"));
    }
}
