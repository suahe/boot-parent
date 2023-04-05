package com.sah.system.controller;

import com.sah.core.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/sys/test")
@RestController
public class TestController {

    @GetMapping("/forward")
    public Result<String> forward() {
        return Result.OK();
    }
}
