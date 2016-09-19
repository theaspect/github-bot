package me.blzr.github;

import com.google.common.base.Preconditions;
import com.mchange.v2.c3p0.DataSources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.TelegramBotsApi;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    private final String url;
    private final String user;
    private final String password;
    private final String username;
    private final String token;
    private final String port;

    public static void main(String[] args) {
        try {
            new App().start();
        } catch (Exception e) {
            log.error("Cannot start app", e);
        }
    }

    public App() throws IOException {
        Properties prop = getProperties();

        this.url = prop.getProperty("url", System.getenv("URL"));
        this.user = prop.getProperty("user", System.getenv("USER"));
        this.password = prop.getProperty("password", System.getenv("PASSWORD"));
        this.username = prop.getProperty("username", System.getenv("USERNAME"));
        this.token = prop.getProperty("token", System.getenv("TOKEN"));
        this.port = prop.getProperty("port", System.getenv("PORT"));

        Preconditions.checkNotNull(url, "URL should not be null");
        Preconditions.checkNotNull(user, "USER should not be null");
        Preconditions.checkNotNull(password, "PASSWORD should not be null");
        Preconditions.checkNotNull(username, "USERNAME should not be null");
        Preconditions.checkNotNull(token, "TOKEN should not be null");
        Preconditions.checkNotNull(port, "PORT should not be null");
    }

    private Properties getProperties() throws IOException {
        log.debug("Reading properties");
        Properties prop = new Properties();
        InputStream is = App.class.getResourceAsStream("/config.properties");
        if (is != null) {
            prop.load(is);
        }
        return prop;
    }

    private void start() throws Exception {
        DataSource pool = DataSources.pooledDataSource(DataSources.unpooledDataSource(url, user, password));
        Database database = new Database(pool);

        new Server(Integer.valueOf(port), database).start();
        new TelegramBotsApi().registerBot(new Bot(database, username, token));
        new Watcher(database, new GitHub()).start();
    }
}
