package cc.leevi.common.poolc.pool;

import java.sql.Connection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class DefaultConnectionPool {
    private Queue<Connection> pooledQueue;

    private long maxWait;

    private int corePoolSize;

    private int maximumPoolSize;

    public DefaultConnectionPool(long maxWait,int corePoolSize,int maximumPoolSize) {
        this.pooledQueue = new ArrayBlockingQueue<>(corePoolSize);
        this.maxWait = maxWait;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
    }






}
