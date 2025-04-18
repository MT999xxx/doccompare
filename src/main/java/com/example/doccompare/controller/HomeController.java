package com.example.doccompare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        System.out.println("访问了hello页面");
        return "Hello World!"; // 直接返回字符串，不使用模板
    }

    @GetMapping("/test")
    public String test() {
        System.out.println("访问了test页面");
        return "index"; // 测试是否能找到模板
    }
}