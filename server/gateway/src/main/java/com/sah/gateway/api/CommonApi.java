package com.sah.gateway.api;

import com.sah.gateway.common.Result;
import com.sah.gateway.common.VerifyToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description:
 * @author: yang.yonglian
 * @create: 2021-06-01
 **/
@Component
@FeignClient( value = "system", fallbackFactory = CommonAPIFallbackFactory.class)
public interface CommonApi {
    /**
     * token校验
     * @param token
     * @return
     */
    @GetMapping("/sys/api/verifyToken")
    Result<VerifyToken> verifyToken(@RequestParam("token") String token, @RequestParam("refreshToken") String refreshToken);
}
