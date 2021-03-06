package me.blzr.github;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Database {
    private static final Logger log = LogManager.getLogger(Database.class);

    private DataSource dataSource;

    public Database(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        migrate(dataSource);
    }

    private void migrate(DataSource dataSource) {
        log.debug("Migrating database");
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    List<String> getSubscriptions(Long chatId) throws SQLException {
        return selectAll("SELECT org_id FROM SUB WHERE chat_id = ? ORDER BY org_id ASC", chatId).stream()
                .map(row -> (String) row.get("org_id"))
                .collect(Collectors.toList());
    }

    void removeAll(Long chatId) throws SQLException {
        execute("DELETE FROM SUB WHERE chat_id = ?", chatId);
    }

    Collection<String> add(Long chatId, Collection<String> repos) throws SQLException {
        repos.removeAll(getSubscriptions(chatId));
        for (String repo : repos) {
            insert("INSERT INTO SUB (chat_id, org_id) VALUES (?,?)", chatId, repo);
        }
        return repos;
    }

    Collection<String> remove(Long chatId, Collection<String> repos) throws SQLException {
        for (String repo : repos) {
            execute("DELETE FROM SUB WHERE chat_id = ? and org_id = ?", chatId, repo);
        }
        return repos;
    }

    void updateTimestamp(String orgId) throws SQLException {
        execute("UPDATE SUB SET date = NOW() WHERE org_id = ?", orgId);
    }

    int addEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor, boolean sent) throws SQLException {
        if (sent) {
            return addSentEvent(eventId, orgId, repoId, event, date, actor);
        } else {
            return addNewEvent(eventId, orgId, repoId, event, date, actor);
        }
    }

    private int addSentEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor) throws SQLException {
        // Compatible UPSERT
        return insert("INSERT INTO EVENT (event_id, org_id, repo_id, event, date, actor, sent) " +
                        "SELECT ?,?,?,?,?,?,? " +
                        "WHERE NOT EXISTS (SELECT * FROM EVENT WHERE event_id = ?)",
                eventId, orgId, repoId, event, Timestamp.from(date), actor, true, eventId);
    }

    private int addNewEvent(Long eventId, String orgId, String repoId, String event, Instant date, String actor) throws SQLException {
        // Compatible UPSERT
        return insert("INSERT INTO EVENT (event_id, org_id, repo_id, event, date, actor) " +
                        "SELECT ?,?,?,?,?,? " +
                        "WHERE NOT EXISTS (SELECT * FROM EVENT WHERE event_id = ?)",
                eventId, orgId, repoId, event, Timestamp.from(date), actor, eventId);
    }

    Optional<Event> getOldestUnsent() throws SQLException {
        final Map<String, Object> row = selectOne("SELECT * FROM EVENT WHERE sent = FALSE ORDER BY date ASC");
        return Optional.ofNullable(row).map(Event::new);
    }

    Optional<String> getOldestNotified() throws SQLException {
        return Optional.ofNullable((String) selectOne("SELECT org_id FROM SUB ORDER BY date ASC").get("org_id"));
    }

    boolean hasEvents(String orgId) throws SQLException {
        return (boolean) selectOne("SELECT COUNT(event_id)>0 has_events FROM EVENT WHERE org_id = ?", orgId).get("has_events");
    }

    List<Long> getSubscribers(String orgId) throws SQLException {
        return selectAll("SELECT chat_id FROM SUB WHERE org_id = ?", orgId).stream()
                .map(row -> (Long) row.get("chat_id"))
                .collect(Collectors.toList());
    }

    void markSentEvent(Long eventId) throws SQLException {
        execute("UPDATE EVENT SET sent = TRUE WHERE event_id = ?", eventId);
    }

    List<Stat> getStats() throws SQLException {
        return selectAll("SELECT s.org_id org_id, COUNT(e.event_id) event_cnt, COUNT(DISTINCT s.chat_id) chat_cnt, MAX(e.date) latest " +
                "FROM sub s LEFT JOIN event e on e.org_id = s.org_id " +
                "GROUP BY 1 " +
                "ORDER BY org_id ASC")
                .stream()
                .map(Stat::new)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> selectAll(String sql, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            List<Map<String, Object>> results;
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
            return results;
        }
    }

    private Map<String, Object> selectOne(String sql, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Map<String, Object> results = null;
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
            return results;
        }
    }

    private int execute(String sql, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();

        }
    }

    private int insert(String sql, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    static class Stat {
        String orgId;
        Long subscribers;
        Long events;
        Instant latest;

        public Stat(Map<String, Object> params) {
            this.orgId = (String) params.get("org_id");
            this.subscribers = (Long) params.get("chat_cnt");
            this.events = (Long) params.get("event_cnt");
            Timestamp ts = (Timestamp) params.get("latest");
            this.latest = (ts == null) ? null : ts.toInstant();
        }

        @Override
        public String toString() {
            return String.format("<TR><TD><A href='https://github.com/%s'>%s</A></TD><TD>%s</TD><TD>%s</TD><TD>%s</TD></TR>",
                    orgId, orgId, events, subscribers, (latest == null) ? "" : latest.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
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
            Timestamp date = (Timestamp) params.get("date");
            this.date = (date == null) ? null : date.toInstant();
            this.actor = (String) params.get("actor");
        }

        @Override
        public String toString() {
            return String.format("%s %s to http://github.com/%s at %s", actor, event, repoId,
                    (date == null) ? "" : date.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}
