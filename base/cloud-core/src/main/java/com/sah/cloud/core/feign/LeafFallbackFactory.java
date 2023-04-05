package com.sah.cloud.core.feign;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: yang.yonglian
 * @create: 2021-05-11
 **/
@Slf4j
@Component
public class LeafFallbackFactory implements FallbackFactory<LeafFeignClient> {
    @Override
    public LeafFeignClient create(Throwable throwable) {
        return new LeafFeignClient(){
            @Override
            public String getSnowflakeId(String key) {
                log.error("获取主键失败",throwable);
                throw new RuntimeException("获取主键失败");
            }
        };
    }
}
