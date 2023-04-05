package com.sah.gateway.api;

import com.sah.gateway.common.Result;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommonAPIFallbackFactory implements FallbackFactory<CommonApi> {

    @Override
    public CommonApi create(Throwable throwable) {
        CommonApi fallback = new CommonApi(){
            @Override
            public Result verifyToken(String token, String refreshToken) {
                log.error("verifyToken:"+token+" error",throwable);
                return Result.error("无效的token");
            }
        };
        return fallback;
    }
}