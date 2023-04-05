package com.sah.core.model;

import lombok.Data;

/**
 * @description:
 * @author: yang.yonglian
 * @create: 2021-07-16
 **/
@Data
public class Token {
    /**
     * 令牌
     */
    private String token;
    /**
     * 用于刷新token
     */
    private String refreshToken;
}
