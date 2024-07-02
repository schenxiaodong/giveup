package com.cxd.user.service;

import com.cxd.springframework.Autowired;
import com.cxd.springframework.Component;
import com.cxd.springframework.Transactional;


@Component
@Transactional
public class UserService {
    @Autowired
    private OrderService orderService;

    public OrderService test() {
        System.out.println("叫我干嘛");
        return orderService;
    }
}
