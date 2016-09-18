package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHEventInfo;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;

public class Repository {
    private static final Logger log = LogManager.getLogger(Repository.class);

    private final GitHub github;

    public Repository() throws IOException {
        log.debug("Connect to GitHub");
        github = GitHub.connectAnonymously();
    }

    public List<GHEventInfo> getEvents(String user) throws IOException {
        return github.getUser(user).listEvents().asList();
    }

    public boolean checkRepo(String user){
        // TODO
        return false;
    }
}
