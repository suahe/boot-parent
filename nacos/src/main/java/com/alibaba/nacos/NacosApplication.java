package com.alibaba.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @description:
 * @author: yangyonglian
 * @time: 2021/9/29 16:01
 */
@SpringBootApplication(scanBasePackages = "com.alibaba.nacos")
@ServletComponentScan
@EnableScheduling
public class NacosApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.standalone", "true");
        System.setProperty("nacos.core.auth.enabled", "false");
        SpringApplication.run(NacosApplication.class, args);
    }
}
