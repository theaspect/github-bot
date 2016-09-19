package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Database implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(Database.class);

    private Connection connection;

    public Database(String url, String user, String password) throws SQLException {
        migrate(url, user, password);
        log.debug("Connectiong to database");
        this.connection = DriverManager.getConnection(url, user, password);
    }

    void migrate(String url, String user, String password) {
        log.debug("Migrating database");
        Flyway flyway = new Flyway();
        flyway.setDataSource(url, user, password);
        flyway.migrate();
    }

    List<String> getSubscriptions(Long chatId) throws SQLException {
        return selectAll("SELECT org_id FROM SUB WHERE chat_id = ?", chatId).stream()
                .map(row -> (String) row.get("org_id"))
                .collect(Collectors.toList());
    }

    void removeAll(Long chatId) throws SQLException {
        execute("DELETE FROM SUB WHERE chat_id = ?", chatId);
    }

    public Collection<String> add(Long chatId, Collection<String> repos) throws SQLException {
        repos.removeAll(getSubscriptions(chatId));
        for (String repo : repos) {
            insert("INSERT INTO SUB (chat_id, org_id) VALUES (?,?)", chatId, repo);
        }
        return repos;
    }

    public Collection<String> remove(Long chatId, Collection<String> repos) throws SQLException {
        for (String repo : repos) {
            execute("DELETE FROM SUB WHERE chat_id = ? and org_id = ?", chatId, repo);
        }
        return repos;
    }

    public void updateTimestamp(String orgId) throws SQLException {
        execute("UPDATE SUB SET date = NOW() WHERE org_id = ?", orgId);
    }

    public void addEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor, boolean sent) throws SQLException {
        if (sent) {
            addSentEvent(eventId, orgId, repoId, event, date, actor);
        } else {
            addNewEvent(eventId, orgId, repoId, event, date, actor);
        }
    }

    private void addSentEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor) throws SQLException {
        insert("INSERT INTO EVENT (event_id, org_id, repo_id, event, date, actor, sent)" +
                        "VALUES(?,?,?,?,?,?,?)" +
                        "ON CONFLICT DO NOTHING",
                eventId, orgId, repoId, event, Timestamp.from(date), actor, true);
    }

    private void addNewEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor) throws SQLException {
        insert("INSERT INTO EVENT (event_id, org_id, repo_id, event, date, actor)" +
                        "VALUES(?,?,?,?,?,?)" +
                        "ON CONFLICT DO NOTHING",
                eventId, orgId, repoId, event, Timestamp.from(date), actor);
    }

    public Optional<Event> getOldestUnsent() throws SQLException {
        final Map<String, Object> row = selectOne("SELECT * FROM EVENT WHERE sent = FALSE ORDER BY date ASC");
        return Optional.ofNullable(row == null ? null : new Event(row));
    }

    public Optional<String> getOldestNotified() throws SQLException {
        return Optional.ofNullable((String) selectOne("SELECT org_id FROM SUB ORDER BY date ASC").get("org_id"));
    }

    public boolean hasEvents(String orgId) throws SQLException {
        return (boolean) selectOne("SELECT COUNT(event_id)>0 has_events FROM EVENT WHERE org_id = ?", orgId).get("has_events");
    }

    public List<Long> getSubscribers(String orgId) throws SQLException {
        return selectAll("SELECT chat_id FROM SUB WHERE org_id = ?", orgId).stream()
                .map(row -> (Long) row.get("chat_id"))
                .collect(Collectors.toList());
    }

    public void markSentEvent(Long eventId) throws SQLException {
        execute("UPDATE EVENT SET sent = TRUE WHERE event_id = ?", eventId);
    }

    private List<Map<String, Object>> selectAll(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            results = new ArrayList<>();
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                final int cc = rs.getMetaData().getColumnCount();
                for (int i = 0; i < cc; i++) {
                    row.put(rs.getMetaData().getColumnName(i + 1).toLowerCase(), rs.getObject(i + 1));
                }
                results.add(row);
            }
        }
        return results;
    }

    private Map<String, Object> selectOne(String sql, Object... params) throws SQLException {
        Map<String, Object> results = null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                results = new HashMap<>();
                final int cc = rs.getMetaData().getColumnCount();
                for (int i = 0; i < cc; i++) {
                    results.put(rs.getMetaData().getColumnName(i + 1).toLowerCase(), rs.getObject(i + 1));
                }
            }
        }
        return results;
    }

    private Long execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return (long) ps.executeUpdate();
        }
    }

    private boolean insert(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.execute();
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    static class Event {
        Long eventId;
        String orgId;
        String repoId;
        String event;
        Instant date;
        String actor;

        public Event(Map<String, Object> params) {
            this.eventId = (Long) params.get("event_id");
            this.orgId = (String) params.get("org_id");
            this.repoId = (String) params.get("repo_id");
            this.event = (String) params.get("event");
            this.date = ((Timestamp) params.get("date")).toInstant();
            this.actor = (String) params.get("actor");
        }

        @Override
        public String toString() {
            return String.format("%s %s to http://github.com/%s at %s", actor, event, repoId, date);
        }
    }
}
