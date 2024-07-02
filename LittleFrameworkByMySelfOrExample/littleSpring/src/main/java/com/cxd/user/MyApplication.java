package com.cxd.user;

import com.cxd.springframework.LittleApplicationContext;
import com.cxd.user.service.UserService;

// 测试类，用于测试自己写的框架，没啥其他作用
public class MyApplication {
    public static void main(String[] args) {
        LittleApplicationContext applicationContext = new LittleApplicationContext(AppConfig.class);
        UserService userService = (UserService) applicationContext.getBean("userService");
        System.out.println(userService.test());
    }
}
