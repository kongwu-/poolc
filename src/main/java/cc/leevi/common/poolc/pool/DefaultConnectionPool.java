package cc.leevi.common.poolc.pool;

import cc.leevi.common.poolc.DriverDataSource;
import cc.leevi.common.poolc.PoolConfig;
import cc.leevi.common.poolc.PoolcException;
import cc.leevi.common.poolc.ProxyPooledConnection;
import cc.leevi.common.poolc.utils.ConcurrentBag;
import cc.leevi.common.poolc.utils.ConcurrentBagEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.leevi.common.poolc.utils.ConcurrentBagEntry.*;

public class DefaultConnectionPool extends PoolConfig implements ConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionPool.class);

    private static final long HOUSE_KEEPING_PERIOD_MS = TimeUnit.SECONDS.toMillis(30);

    private int maximumPoolSize;

    private DriverDataSource driverDataSource;

    private ConcurrentBag concurrentBag;

    private ScheduledThreadPoolExecutor houseKeeperTaskExecutor;

    public DefaultConnectionPool(PoolConfig poolConfig) {
        this.maximumPoolSize = poolConfig.getMaximumPoolSize();
        this.driverDataSource = DriverDataSource.createDriverDataSource(poolConfig);
        this.concurrentBag = new ConcurrentBag();
        this.houseKeeperTaskExecutor = new ScheduledThreadPoolExecutor(1);
        initializeCorePool();
        houseKeeperTaskExecutor.scheduleWithFixedDelay(new HouseKeeper(),0,HOUSE_KEEPING_PERIOD_MS,TimeUnit.MILLISECONDS);
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        return getConnection(getMaxWait(), TimeUnit.MILLISECONDS);
    }

    public Connection getConnection(long hardTimeout, TimeUnit timeUnit) throws InterruptedException {

        final long startTime = System.currentTimeMillis();
        long timeout = hardTimeout;
        do {
            ConcurrentBagEntry entry = concurrentBag.borrow(hardTimeout, TimeUnit.MILLISECONDS);
            if (entry == null) {
                break;
            }
            //自旋等待一个可用的连接，直至超时
            if (!isConnectionAlive(entry.getConnection())) {
                timeout -= System.currentTimeMillis() - startTime;
                closeConnection(entry);
            } else {
                return createProxyConnection(entry);
            }
        } while (timeout > 0);

        throw new PoolcException("Get connection Timeout");
    }

    private void closeConnection(ConcurrentBagEntry entry) {
        concurrentBag.remove(entry);
        quietlyCloseConnection(entry.getConnection(), null);
    }

    private void quietlyCloseConnection(final Connection connection, final String closureReason) {
        if (connection != null) {
            try {
                logger.debug("Closing connection {}: {}", connection, closureReason);
                connection.close(); // continue with the close even if setNetworkTimeout() throws
            } catch (Exception e) {
                logger.debug("Closing connection {} failed", connection, e);
            }
        }
    }

    private ProxyPooledConnection createProxyConnection(ConcurrentBagEntry entry) {
        return ProxyPooledConnection.createProxyConnection(entry, this);
    }

    boolean isConnectionAlive(final Connection connection) {
        try {
            return connection.isValid(3);
        } catch (SQLException e) {
            logger.warn("Failed to validate connection {} ({}).", connection, e.getMessage());
        }
        return false;
    }

    @Override
    public void release(Connection connection) {
        if (connection instanceof ProxyPooledConnection) {
            concurrentBag.requite(((ProxyPooledConnection) connection).getPoolEntry());
            return;
        }
        logger.warn("The released connection must instance of ProxyPooledConnection!");
    }

    @Override
    public void dump() {
        logger.info("maximumPoolSize: {}, totalConnections: {}, borrowedCount: {}", maximumPoolSize, concurrentBag.size(), concurrentBag.values(ConcurrentBagEntry.STATE_IN_USE).size());
    }

    @Override
    public void shutdown() {
        //shutdown...
    }

    private Connection openConnection() throws SQLException {
        return driverDataSource.getConnection();
    }

    public void initializeCorePool() {
        try {
            fillPool();
            logger.info("{} connections added to the connection pool", maximumPoolSize);
        } catch (SQLException e) {
            logger.error("Open connection failed!", e);
        }
    }

    private void fillPool() throws SQLException {
        //为了简单实现，所以省略了hikari的一些设计，但这里有点问题……
        //如果minIdle和maxPoolSize一致，那么这里connectionsToAdd一定是poolConfig.getMaximumPoolSize() - getTotalConnections()
        //如果不一致，那么初始化时就只会创建minIdle个connection……


        //需要补充的连接数量
        //优先使用minIdle，若minIdle和maxPoolSize不相等，那么连接池拥有"扩容"的功能，如果相等，那么是一个一直充满的"固定大小"的连接池
        int connectionsToAdd = Math.min(getMaximumPoolSize() - getTotalConnections(), getMinimumIdle() - getIdleConnections());
        if (connectionsToAdd <= 0) {
            logger.debug("无需填充连接池，连接池目前很健康，空闲连接数大于minimumIdle[{}]", getMinimumIdle());
        }
        for (int i = 0; i < connectionsToAdd; i++) {
            ConcurrentBagEntry poolEntry = createPoolEntry();
            concurrentBag.add(poolEntry);
        }
    }

    public int getIdleConnections() {
        return concurrentBag.getCount(STATE_NOT_IN_USE);
    }

    public int getTotalConnections() {
        return concurrentBag.size();
    }


    private ConcurrentBagEntry createPoolEntry() throws SQLException {
        Connection connection = openConnection();
        ConcurrentBagEntry concurrentBagEntry = new ConcurrentBagEntry();
        concurrentBagEntry.setConnection(connection);
        return concurrentBagEntry;
    }

    private class HouseKeeper implements Runnable {

        @Override
        public void run() {
            final long idleTimeout = getIdleTimeout();
            try {
                List<ConcurrentBagEntry> idleConnections = concurrentBag.values(STATE_NOT_IN_USE);
                int toRemove = idleConnections.size() - getMinimumIdle();
                for (ConcurrentBagEntry idleConnection : idleConnections) {
                    //删除超时的链接
                    if (toRemove > 0 && System.currentTimeMillis() - idleConnection.getLastAccess() >= idleTimeout) {
                        logger.debug("close idle connection: {}",idleConnection.getConnection());
                        closeConnection(idleConnection);
                        toRemove --;
                    }
                }

                fillPool();
            }catch (Exception e){
                logger.error("Unexpected exception in housekeeping task", e);
            }
        }
    }
}
