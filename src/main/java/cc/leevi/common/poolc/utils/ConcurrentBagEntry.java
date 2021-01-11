package cc.leevi.common.poolc.utils;

import cc.leevi.common.poolc.pool.ConnectionPool;
import cc.leevi.common.poolc.pool.DefaultConnectionPool;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentBagEntry
{
    /**
     * 空闲
     */
    public static final int STATE_NOT_IN_USE = 0;
    /**
     * 使用中
     */
    public static final int STATE_IN_USE = 1;
    /**
     * 已移除
     */
    public static final int STATE_REMOVED = -1;
    /**
     * 移除中（保留状态）
     */
    public static final int STATE_RESERVED = -2;

    private Connection connection;

    private volatile long lastAccess;

    private AtomicInteger state = new AtomicInteger(STATE_NOT_IN_USE);

    public boolean compareAndSet(int expectState, int newState){
        return state.compareAndSet(expectState,newState);
    }

    void setState(int newState){
        state.set(newState);
    }

    int getState(){
        return state.get();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
