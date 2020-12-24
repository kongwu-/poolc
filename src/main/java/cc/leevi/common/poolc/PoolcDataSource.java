package cc.leevi.common.poolc;

import cc.leevi.common.poolc.pool.ConnectionPool;
import cc.leevi.common.poolc.pool.DefaultConnectionPool;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientException;
import java.util.logging.Logger;

public class PoolcDataSource extends PoolConfig implements DataSource {

    private ConnectionPool connectionPool;

    public PoolcDataSource() {
        this.connectionPool = new DefaultConnectionPool(this);
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return ProxyPooledConnection.createProxyConnection(connectionPool.acquire(),connectionPool);
        } catch (InterruptedException e) {
            throw new SQLNonTransientException(e);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Wrapped DataSource is not an instance of " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLException("Wrapped DataSource is not an instance of " + iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
