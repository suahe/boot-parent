package com.sah.cloud.core.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @description:
 * @author: yang.yonglian
 * @create: 2021-05-11
 **/
@FeignClient(value = "leaf-server",fallbackFactory = LeafFallbackFactory.class)
public interface LeafFeignClient {
    /**
     * 获取ID
     * @param key
     * @return
     */
    @GetMapping(value = "/api/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);
}
