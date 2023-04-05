package com.sah.system.api;

import com.sah.core.model.Result;
import com.sah.core.model.VerifyToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务化 system模块 对外接口请求类
 */
@RestController
@RequestMapping("/sys/api")
public class SystemApiController {

    /**
     * token校验
     *
     * @param token
     * @return
     */
    @GetMapping("/verifyToken")
    public Result verifyToken(@RequestParam("token") String token, @RequestParam("refreshToken") String refreshToken) {

        VerifyToken verifyToken = new VerifyToken();
        return Result.OK(verifyToken);
    }
}
