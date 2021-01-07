package cc.leevi.common.poolc.pool;

import cc.leevi.common.poolc.DriverDataSource;
import cc.leevi.common.poolc.PoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DefaultConnectionPool implements ConnectionPool{

    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionPool.class);

    private BlockingQueue<Connection> pooledQueue;

    private long maxWait;

    private int corePoolSize;

    private int connectionCount;

    private int maximumPoolSize;

    private PoolConfig poolConfig;

    private DriverDataSource driverDataSource;

    private int acquiredCount = 0;

    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;

    public DefaultConnectionPool(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        this.maxWait = poolConfig.getMaxWait();
        this.corePoolSize = poolConfig.getCorePoolSize();
        this.maximumPoolSize = poolConfig.getMaximumPoolSize();
        this.driverDataSource = DriverDataSource.createDriverDataSource(poolConfig);
        initCorePool();
    }

    @Override
    public synchronized Connection acquire() throws InterruptedException {
        Connection idle = pooledQueue.poll(maxWait, DEFAULT_TIMEUNIT);
        if(idle == null){
            throw new RuntimeException("acquire connection from pool timeout!");
        }
        acquiredCount++;
        return idle;
    }

    @Override
    public synchronized void release(Connection connection) {
        pooledQueue.offer(connection);
        acquiredCount--;
    }

    @Override
    public void dump() {
        logger.info("coreSize: {}, connectionCount: {}, acquiredCount: {}",pooledQueue.size(),connectionCount,acquiredCount);
    }

    private Connection openConnection() throws SQLException {
        if(acquiredCount >= connectionCount && acquiredCount <= maximumPoolSize){
            return addConnection();
        }
        return driverDataSource.getConnection();
    }

    private Connection addConnection() throws SQLException {
        acquiredCount++;
        return openConnection();
    }



    public void initCorePool(){
        try {
            pooledQueue = new ArrayBlockingQueue<>(maximumPoolSize);
            for (int i = 0; i < corePoolSize; i++) {
                Connection connection = openConnection();
                pooledQueue.add(connection);
            }
            connectionCount = corePoolSize;
            logger.info("{} connections added to the connection pool",corePoolSize);
        } catch (SQLException e) {
            logger.error("Open connection failed!",e);
        }
    }
}
