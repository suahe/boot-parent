package com.sah.gateway.fallback;

import com.sah.gateway.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 响应超时熔断处理器
 *
 * @author zyf
 */
@RestController
@Slf4j
public class FallbackController {

    /**
     * 全局熔断处理
     *
     * @return
     */
    @RequestMapping("/fallback")
    public Mono<Result> fallback(Exception e) {
        if (e != null) {
            log.error("服务调用异常：", e);
        }
        Result result = Result.error(500, "服务异常，请重试");
        return Mono.just(result);
    }
}
