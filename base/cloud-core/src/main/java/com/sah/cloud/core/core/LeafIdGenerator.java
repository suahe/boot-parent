package com.sah.cloud.core.core;

import com.sah.cloud.core.feign.LeafFeignClient;

/**
 * @description: 分布式主键生成器
 * @author: yang.yonglian
 * @create: 2021-05-11
 **/

public class LeafIdGenerator {
    /**
     * 雪花模式获取分布式主键ID
     * @return
     */
    public static Long getId(){
        String id = getStringId();
        return Long.parseLong(id);
    }


    /**
     * 雪花模式获取分布式主键ID
     * @return
     */
    public static String getStringId(){
        return LeafApplicationContext.getBean(LeafFeignClient.class)
                .getSnowflakeId("key");
    }
}
