package fr.cnrs.opentheso.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hibernate {@code ddl-auto=validate} builds the SessionFactory before Liquibase
 * unless the entity manager explicitly depends on it.
 */
@Component
public class JpaLiquibaseOrderConfig implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
            return;
        }
        String liquibaseBean = liquibaseBeanName(beanFactory);
        if (liquibaseBean == null) {
            return;
        }
        addDependsOn(beanFactory.getBeanDefinition("entityManagerFactory"), liquibaseBean);
    }

    private static String liquibaseBeanName(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory.containsBeanDefinition("liquibase")) {
            return "liquibase";
        }
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition def = beanFactory.getBeanDefinition(name);
            String type = def.getBeanClassName();
            if (type != null && type.endsWith("SpringLiquibase")) {
                return name;
            }
        }
        return null;
    }

    private static void addDependsOn(BeanDefinition emf, String liquibaseBean) {
        List<String> depends = new ArrayList<>();
        if (emf.getDependsOn() != null) {
            depends.addAll(Arrays.asList(emf.getDependsOn()));
        }
        if (!depends.contains(liquibaseBean)) {
            depends.add(liquibaseBean);
            emf.setDependsOn(depends.toArray(String[]::new));
        }
    }
}
