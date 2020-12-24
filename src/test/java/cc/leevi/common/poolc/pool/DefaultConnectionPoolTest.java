package cc.leevi.common.poolc.pool;

import cc.leevi.common.poolc.PoolConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.*;

public class DefaultConnectionPoolTest {


    private ConnectionPool connectionPool;

    @Before
    public void setUp() throws Exception {
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaxWait(3000);
        poolConfig.setCorePoolSize(2);

        poolConfig.setDriverClassName("com.mysql.jdbc.Driver");
        poolConfig.setUsername("ipcis_nvhl");
        poolConfig.setPassword("U0x!lRs^kV1");
        poolConfig.setJdbcUrl("jdbc:mysql://10.0.3.10:3306/ipcis_nvhl_dcdb");

        connectionPool = new DefaultConnectionPool(poolConfig);

    }

    @Test
    public void acquire() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Connection connection = connectionPool.acquire();
            Assert.assertNotNull(connection);
            Assert.assertTrue(connection instanceof Connection);

            connectionPool.dump();
        }
    }

    @Test
    public void release() throws InterruptedException {
        Connection connection = connectionPool.acquire();
        connectionPool.release(connection);

        connectionPool.dump();
    }

    @Test
    public void initCorePool() {
    }
}