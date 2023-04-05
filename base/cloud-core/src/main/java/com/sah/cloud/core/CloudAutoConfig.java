package com.sah.cloud.core;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: yang.yonglian
 * @create: 2021-07-09
 **/
@Configuration
@ComponentScan("com.sah.cloud.core")
@EnableFeignClients("com.sah.cloud.core")
public class CloudAutoConfig {
}
