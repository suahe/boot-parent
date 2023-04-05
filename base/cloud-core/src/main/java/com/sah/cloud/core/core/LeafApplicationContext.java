package com.sah.cloud.core.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description: TODO
 * @Author: yang.yonglian
 * @CreateDate: 2020/1/9 20:18
 * @Version: 1.0
 */
@Component
public class LeafApplicationContext implements ApplicationContextAware, EnvironmentAware {
    private static ApplicationContext applicationContext;
    private static Environment environment;

    /**
     * 发布事件
     * @param event
     */
    public static void publishEvent(ApplicationEvent event){
        applicationContext.publishEvent(event);
    }

    public static Environment getEnvironment(){
        return environment;
    }

    public static RootBeanDefinition getRootBeanDefinitionFromCache(String beanName){
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)applicationContext.getAutowireCapableBeanFactory();
        return (RootBeanDefinition) beanFactory.getMergedBeanDefinition(beanName);
    }

    public static BeanFactory getBeanFactory(){
        return applicationContext.getAutowireCapableBeanFactory();
    }

    public static <T> T getBean(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String beanName){
        return (T)applicationContext.getBean(beanName);
    }
    public static String[] getBeanNamesForType(Class clazz){
        return applicationContext.getBeanNamesForType(clazz);
    }
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz){
        return applicationContext.getBeansOfType(clazz);
    }

    @Override
    public void setEnvironment(Environment environment) {
        LeafApplicationContext.environment = environment;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        LeafApplicationContext.applicationContext = applicationContext;
    }




}
