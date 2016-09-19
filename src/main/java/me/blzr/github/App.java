package me.blzr.github;

import com.google.common.base.Preconditions;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.TelegramBotsApi;

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
    private final String driver;

    public static void main(String[] args) {
        try {
            new App().start();
        } catch (Exception e) {
            log.error("Cannot start app", e);
        }
    }

    public App() throws IOException {
        Properties prop = getProperties();
        //DB
        this.driver = prop.getProperty("driver", System.getenv("DRIVER"));
        this.url = prop.getProperty("url", System.getenv("URL"));
        this.user = prop.getProperty("user", System.getenv("USER"));
        this.password = prop.getProperty("password", System.getenv("PASSWORD"));
        // Telegram
        this.username = prop.getProperty("username", System.getenv("USERNAME"));
        this.token = prop.getProperty("token", System.getenv("TOKEN"));
        // Server
        this.port = prop.getProperty("port", System.getenv("PORT"));

        Preconditions.checkNotNull(driver, "DRIVER should not be null");
        Preconditions.checkNotNull(url, "URL should not be null");
        Preconditions.checkNotNull(user, "USER should not be null");
        Preconditions.checkNotNull(password, "PASSWORD should not be null");
        Preconditions.checkNotNull(username, "USERNAME should not be null");
        Preconditions.checkNotNull(token, "TOKEN should not be null");
        Preconditions.checkNotNull(port, "PORT should not be null");

        log.debug("Will connect to {}@{} using", user, url, driver);
        log.debug("Will listen {}", port);
    }

    private Properties getProperties() throws IOException {
        log.debug("Reading properties");

        // TRACE c3p0
        //Properties p = new Properties(System.getProperties());
        //p.put("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
        //p.put("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "ALL"); // Off or any other level
        //System.setProperties(p);

        Properties prop = new Properties();
        InputStream is = App.class.getResourceAsStream("/config.properties");
        if (is != null) {
            prop.load(is);
        }
        return prop;
    }

    private void start() throws Exception {
        final ComboPooledDataSource pool = new ComboPooledDataSource();
        pool.setDriverClass(driver);
        pool.setJdbcUrl(url);
        pool.setUser(user);
        pool.setPassword(password);

        final Database database = new Database(pool);
        final Bot bot = new Bot(database, username, token);
        final GitHub gitHub = new GitHub();

        new TelegramBotsApi().registerBot(bot);
        new Server(Integer.valueOf(port), database).start();
        new Watcher(database, gitHub, bot).start();
    }
}
