package cc.leevi.common.poolc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class HikaricpTest {
    public static void main(String[] args) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://10.0.3.10:3306/ipcis_nvhl_dcdb?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&useSSL=false");
        hikariConfig.setUsername("ipcis_nvhl");
        hikariConfig.setPassword("U0x!lRs^kV1");
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Connection connection = hikariDataSource.getConnection();
        System.out.println(connection);
//        connection.close();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection connection1 = hikariDataSource.getConnection();
                    System.out.println(connection1);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }).start();

    }
}
