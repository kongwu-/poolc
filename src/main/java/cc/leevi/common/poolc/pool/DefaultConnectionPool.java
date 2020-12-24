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
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultConnectionPool implements ConnectionPool{

    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionPool.class);

    private BlockingQueue<Connection> pooledQueue;

    private long maxWait;

    private int corePoolSize;

    private PoolConfig poolConfig;

    private DriverDataSource driverDataSource;

    private AtomicInteger acquiredCount = new AtomicInteger();

    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;

    public DefaultConnectionPool(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        this.maxWait = poolConfig.getMaxWait();
        this.corePoolSize = poolConfig.getCorePoolSize();
        this.driverDataSource = DriverDataSource.createDriverDataSource(poolConfig);
        initCorePool();
    }

    @Override
    public Connection acquire() throws InterruptedException {
        Connection idle = pooledQueue.poll(maxWait, DEFAULT_TIMEUNIT);
        if(idle == null){
            throw new RuntimeException("acquire connection from pool timeout!");
        }
        acquiredCount.incrementAndGet();
        return idle;
    }

    @Override
    public void release(Connection connection) {
        pooledQueue.offer(connection);
        acquiredCount.decrementAndGet();
    }

    @Override
    public void dump() {
        logger.info("coreSize: {}, acquiredCount: {}",pooledQueue.size(),acquiredCount.get());
    }

    private Connection openConnection() throws SQLException {
        return driverDataSource.getConnection();
    }

    public void initCorePool(){
        try {
            pooledQueue = new ArrayBlockingQueue<>(poolConfig.getMaximumPoolSize());
            for (int i = 0; i < corePoolSize; i++) {
                Connection connection = openConnection();
                pooledQueue.add(connection);
            }
            logger.info("{} connections added to the connection pool",corePoolSize);
        } catch (SQLException e) {
            logger.error("Open connection failed!",e);
        }
    }
}
