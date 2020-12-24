package cc.leevi.common.poolc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;

public class DriverDataSource implements DataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverDataSource.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final Driver driver;

    private final Properties info;

    public DriverDataSource(String driverClassName,String jdbcUrl, String username, String password)
    {
        this.driverClassName = driverClassName;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        try {
            info = new Properties();

            if (username != null) {
                info.put("user", username);
            }
            if (password != null) {
                info.put("password", password);
            }
            driver = (Driver) Class.forName(driverClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("initialize jdbc driver failed!", e);
        }
    }

    public static DriverDataSource createDriverDataSource(PoolConfig poolConfig){
        return new DriverDataSource(poolConfig.getDriverClassName(),poolConfig.getJdbcUrl(),poolConfig.getUsername(),poolConfig.getPassword());
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return driver.connect(jdbcUrl,info);
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException
    {
        Properties info = new Properties();

        if (username != null) {
            info.put("user", username);
        }
        if (password != null) {
            info.put("password", password);
        }
        return driver.connect(jdbcUrl,info);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return driver.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }
}
