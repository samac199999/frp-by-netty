package com.samac.frpclient.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping("")
    public String sayHello() {
        return "Hello frp client!";
    }
}
