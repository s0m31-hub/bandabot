package org.nwolfhub.bandabot;

import com.pengrad.telegrambot.TelegramBot;
import org.nwolfhub.bandabot.wolvesville.ClanData;
import org.nwolfhub.bandabot.wolvesville.WereWorker;
import org.nwolfhub.utils.Configurator;
import org.nwolfhub.utils.Utils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@org.springframework.context.annotation.Configuration
@EnableTransactionManagement
@EnableJpaRepositories("org.nwolfhub.bandabot.database")
@ComponentScan(basePackages = {"org.nwolfhub.bandabot.database", "org.nwolfhub.bandabot.telegram"})
public class Configuration {
    private final Configurator configurator = new Configurator(new File("banda.cfg"));

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("org.nwolfhub.bandabot.database");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(Utils.buildConnectionString(configurator.getValue("db_ip"), Integer.valueOf(configurator.getValue("db_port")), configurator.getValue("db_name")));
        dataSource.setUsername(configurator.getValue("db_username"));
        dataSource.setPassword(configurator.getValue("db_password"));
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
        return new PersistenceExceptionTranslationPostProcessor();
    }
    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(configurator.getValue("telegram_token"));
    }
    @Bean
    public List<Long> adminsList() {
        return new ArrayList<>(Arrays.stream(configurator.getValue("admins").split(",")).map(Long::valueOf).toList());
    }
    @Bean
    public ClanData clanData() {
        return new ClanData().setWereclan(configurator.getValue("wereclan")).setWeretoken(configurator.getValue("weretoken"));
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        return properties;
    }

}
