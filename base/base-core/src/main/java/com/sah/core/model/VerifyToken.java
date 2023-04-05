package com.sah.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: token校验结果
 * @author: yang.yonglian
 * @create: 2021-07-19
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyToken {
    /**
     * 令牌对象
     */
    private Token token;
    /**
     * 是否刷新
     */
    private boolean refresh;
}
