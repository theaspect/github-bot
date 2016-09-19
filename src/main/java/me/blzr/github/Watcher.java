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

    public Watcher(Database database, GitHub gitHub) {
        log.debug("Watcher started");
        this.database = database;
        this.gitHub = gitHub;
    }

    public void start() {
        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchRepos, 0, 65, TimeUnit.SECONDS);
        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchEvents, 0, 5, TimeUnit.SECONDS);
    }

    void watchRepos() {
        try {
            database.getOldestNotified().ifPresent(orgId -> {
                log.debug("Request {} events", orgId);
                try {
                    boolean firstRequest = !database.hasEvents(orgId);

                    final List<GitHub.GHEvent> events = gitHub.getEvents(orgId);
                    log.debug("Found {} events", events.size());
                    for (GitHub.GHEvent event : events) {
                        database.addEvent(event.getId(),
                                orgId,
                                event.repo.name,
                                event.type,
                                event.getCreated(),
                                event.actor.login,
                                firstRequest);
                    }

                    database.updateTimestamp(orgId);
                } catch (IOException | SQLException e) {
                    log.error("Cannot get events from " + orgId, e);
                }
            });
        } catch (SQLException e) {
            log.error("Cannot get oldest notified", e);
        }
    }

    void watchEvents() {
        try {
            database.getOldestUnsent().ifPresent(e -> {
                //
            });
        } catch (SQLException e) {
            log.error("Cannot get unsent events", e);
        }
    }
}
