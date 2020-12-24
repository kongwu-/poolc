package cc.leevi.common.poolc;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class DriverDataSourceTest {

    @org.junit.Test
    public void createDriverDataSource() throws SQLException {
        PoolConfig poolConfig = new PoolConfig();

        poolConfig.setDriverClassName("com.mysql.jdbc.Driver");
        poolConfig.setUsername("ipcis_nvhl");
        poolConfig.setPassword("U0x!lRs^kV1");
        poolConfig.setJdbcUrl("jdbc:mysql://10.0.3.10:3306/ipcis_nvhl_dcdb");

        DriverDataSource driverDataSource = DriverDataSource.createDriverDataSource(poolConfig);
        Connection connection = driverDataSource.getConnection();
        connection.close();
    }

    @org.junit.Test
    public void getConnection() {
    }
}