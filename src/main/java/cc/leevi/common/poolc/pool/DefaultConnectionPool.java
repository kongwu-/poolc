package cc.leevi.common.poolc.pool;

import cc.leevi.common.poolc.DriverDataSource;
import cc.leevi.common.poolc.PoolConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DefaultConnectionPool {
    private BlockingQueue<Connection> pooledQueue;

    private long maxWait;

    private int corePoolSize;

    private PoolConfig poolConfig;

    private DriverDataSource driverDataSource;

    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;


    public DefaultConnectionPool(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        this.maxWait = poolConfig.getMaxWait();
        this.corePoolSize = poolConfig.getCorePoolSize();
        this.driverDataSource = DriverDataSource.createDriverDataSource(poolConfig);


    }

    public Connection acquire(long maxWait) throws InterruptedException {
        Connection idle = pooledQueue.poll(maxWait, DEFAULT_TIMEUNIT);
        if(idle == null){
            throw new RuntimeException("acquire connection from pool timeout!");
        }
    }

    public Connection release(long maxWait) throws InterruptedException {
        return pooledQueue.take(maxWait, DEFAULT_TIMEUNIT);
    }

    private Connection openConnection() throws SQLException {
        return driverDataSource.getConnection();
    }




}
