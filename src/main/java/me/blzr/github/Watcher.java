package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Watcher {
    private static final Logger log = LogManager.getLogger(Watcher.class);

    private final Database database;
    private final GitHub gitHub;
    private final Bot bot;

    public Watcher(Database database, GitHub gitHub, Bot bot) {
        log.debug("Watcher started");
        this.database = database;
        this.gitHub = gitHub;
        this.bot = bot;
    }

    void start() {
        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchRepos, 0, 65, TimeUnit.SECONDS);
        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchEvents, 0, 5, TimeUnit.SECONDS);
    }

    private void watchRepos() {
        try {
            database.getOldestNotified().ifPresent(orgId -> {
                log.debug("Request {} events", orgId);
                try {
                    boolean firstRequest = !database.hasEvents(orgId);

                    final List<GitHub.GHEvent> events = gitHub.getEvents(orgId);
                    log.debug("Found {} events", events.size());
                    int cnt = 0;
                    for (GitHub.GHEvent event : events) {
                        cnt += database.addEvent(event.getId(),
                                orgId,
                                event.repo.name,
                                event.type,
                                event.getCreated(),
                                event.actor.login,
                                firstRequest);
                    }
                    database.updateTimestamp(orgId);
                    log.debug("Inserted {} new events", cnt);
                } catch (IOException | SQLException e) {
                    log.error("Cannot get events from " + orgId, e);
                }
            });
        } catch (SQLException e) {
            log.error("Cannot get oldest notified", e);
        }
    }

    private void watchEvents() {
        try {
            database.getOldestUnsent().ifPresent(e -> {
                log.debug("Found unsent event {}", e);
                try {
                    final List<Long> subscribers = database.getSubscribers(e.orgId);
                    log.debug("Sending event from {} to {} subscribers", e.orgId, subscribers.size());
                    for (Long chatId : subscribers) {
                        bot.sendReply(chatId, e.toString());
                        sleep(1000);
                    }
                    database.markSentEvent(e.eventId);
                } catch (SQLException e1) {
                    log.error("Cannot get subscribers " + e.orgId, e);
                }
            });
        } catch (SQLException e) {
            log.error("Cannot get unsent events", e);
        }
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e1) {
            log.error("Sleep interrupted");
        }
    }
}
