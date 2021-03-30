package com.uaepay.basis.compensation.event;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.uaepay.basis.compensation.event.dal.CompensationEventMapper;
import com.uaepay.basis.compensation.event.domain.CompensationProperties;

/**
 * 事件补偿自动配置
 * 
 * @author cc
 */
@ConditionalOnProperty(prefix = "compensation.event", name = "enabled", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(CompensationProperties.class)
@ComponentScan("com.uaepay.basis.compensation.event")
public class CompensationEventAutoConfiguration {

    @Autowired
    private CompensationProperties compensationProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public CompensationEventMapper compensationEventMapper() throws Exception {
        DataSource dataSource =
            applicationContext.getBean(compensationProperties.getDataSourceBeanName(), DataSource.class);

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBean.getObject();

        MapperFactoryBean<CompensationEventMapper> mapperFactoryBean = new MapperFactoryBean<>();
        mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory);
        mapperFactoryBean.setMapperInterface(CompensationEventMapper.class);
        mapperFactoryBean.afterPropertiesSet();
        return mapperFactoryBean.getObject();
    }

}
