package cc.leevi.common.poolc.pool;

import java.sql.Connection;

public interface ConnectionPool {

    Connection acquire() throws InterruptedException;

    void release(Connection connection);

    void dump();
}
