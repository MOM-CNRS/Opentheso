package fr.cnrs.opentheso.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hibernate {@code ddl-auto=validate} builds the SessionFactory before Liquibase
 * unless the entity manager explicitly depends on it. Without this, a new
 * changeset is never applied and startup fails on a missing table.
 */
@Configuration
public class JpaLiquibaseOrderConfig {

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibase() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")
                    || !beanFactory.containsBeanDefinition("liquibase")) {
                return;
            }
            BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
            List<String> depends = new ArrayList<>();
            if (emf.getDependsOn() != null) {
                depends.addAll(Arrays.asList(emf.getDependsOn()));
            }
            if (!depends.contains("liquibase")) {
                depends.add("liquibase");
                emf.setDependsOn(depends.toArray(String[]::new));
            }
        };
    }
}
