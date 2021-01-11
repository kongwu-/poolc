package cc.leevi.common.poolc.pool;

import cc.leevi.common.poolc.PoolConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DefaultConnectionPoolTest {

    private static Logger logger = LoggerFactory.getLogger(DefaultConnectionPoolTest.class);

    private ConnectionPool connectionPool;

    @Before
    public void setUp() throws Exception {
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaxWait(3000);
        poolConfig.setCorePoolSize(2);
        poolConfig.setMaximumPoolSize(16);

        poolConfig.setDriverClassName("com.mysql.jdbc.Driver");
        poolConfig.setUsername("ipcis_nvhl");
        poolConfig.setPassword("U0x!lRs^kV1");
        poolConfig.setJdbcUrl("jdbc:mysql://10.0.3.10:3306/ipcis_nvhl_dcdb");

        connectionPool = new DefaultConnectionPool(poolConfig);

    }

    @Test
    public void acquire() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Connection connection = connectionPool.getConnection();
            Assert.assertNotNull(connection);
            Assert.assertTrue(connection instanceof Connection);

            connectionPool.dump();
        }
    }

    @Test
    public void release() throws InterruptedException, SQLException {
        List<Connection> connectionList = new ArrayList<>();
        Thread.sleep(6000);

        logger.info("release...");
        for (Connection connection : connectionList) {
            connection.close();
            connectionPool.dump();
        }

        connectionList.clear();
        logger.info("release 后的 get ...");
        for (int i = 0; i < 5; i++) {
            Connection connection = connectionPool.getConnection();
            connectionList.add(connection);
            connectionPool.dump();
        }
        logger.info("release...");
        for (Connection connection : connectionList) {
            connection.close();
            connectionPool.dump();
        }

        connectionList.clear();
    }

    @Test
    public void initCorePool() {
    }
}