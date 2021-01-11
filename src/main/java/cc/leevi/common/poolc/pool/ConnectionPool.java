package cc.leevi.common.poolc.pool;

import java.sql.Connection;

public interface ConnectionPool {

    Connection getConnection() throws InterruptedException;

    void release(Connection connection);

    void dump();

    void shutdown();
}
