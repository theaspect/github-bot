package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Watcher {
    private static final Logger log = LogManager.getLogger(Watcher.class);

    private final Database eventsDb;
    private final Database gitHubDb;
    private final GitHub gitHub;

    public Watcher(Database eventsDb, Database gitHubDb, GitHub gitHub) {
        log.debug("Watcher started");
        this.eventsDb = eventsDb;
        this.gitHubDb = gitHubDb;
        this.gitHub = gitHub;

        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchRepos, 0, 60, TimeUnit.SECONDS);
        //new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(this::watchEvents, 0, 1, TimeUnit.SECONDS);
    }

    void watchRepos() {
        log.debug("Check subscriptions");
        try {
            gitHubDb.getOldestNotified().ifPresent(orgId -> {
                log.debug("Request {} events", orgId);
                try {
                    boolean firstRequest = !gitHubDb.hasEvents(orgId);

                    for (GitHub.GHEvent event : gitHub.getEvents(orgId)) {
                        gitHubDb.addEvent(event.getId(),
                                orgId,
                                event.repo.name,
                                event.type,
                                event.getCreated(),
                                event.actor.login,
                                firstRequest);
                    }

                    gitHubDb.updateTimestamp(orgId);
                } catch (IOException | SQLException e) {
                    log.error("Cannot get events", e);
                }
            });
        } catch (SQLException e) {
            log.error("Cannot get oldest notified", e);
        }
    }

    void watchEvents() {
        try {
            eventsDb.getOldestUnsent().ifPresent(e -> {
                //
            });
        } catch (SQLException e) {
            log.error("Cannot get unsent events", e);
        }
    }
}
