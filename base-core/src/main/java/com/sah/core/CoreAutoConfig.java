package com.sah.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.sah.core")
@MapperScan("com.sah.core.mapper")
public class CoreAutoConfig {


}
