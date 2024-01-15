package com.yid.agv;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;

@Configuration
public class DataSourceConfig {
    @Value("${jdbc.primary.url}")
    private String jdbcUrl;

    @Value("${jdbc.primary.username}")
    private String username;

    @Value("${jdbc.primary.password}")
    private String password;

    @Bean(name = "primaryDataSource")
    public DataSource dataSource() throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setMaxConnectionAge(3600);
        dataSource.setMaxIdleTimeExcessConnections(60);
        dataSource.setDebugUnreturnedConnectionStackTraces(true);
        return dataSource;
    }

    @Bean(name = "primaryJdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Value("${jdbc.WTools.url}")
    private String WToolsJdbcUrl;

    @Value("${jdbc.WTools.username}")
    private String WToolsUsername;

    @Value("${jdbc.WTools.password}")
    private String WToolsPassword;

    @Bean(name = "WToolsDataSource")
    public DataSource WToolsDataSource() throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(WToolsJdbcUrl);
        dataSource.setUser(WToolsUsername);
        dataSource.setPassword(WToolsPassword);
        dataSource.setMaxConnectionAge(3600);
        dataSource.setMaxIdleTimeExcessConnections(60);
        dataSource.setDebugUnreturnedConnectionStackTraces(true);
        return dataSource;
    }

    @Bean(name = "WToolsJdbcTemplate")
    public JdbcTemplate WToolsJdbcTemplate(@Qualifier("WToolsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
