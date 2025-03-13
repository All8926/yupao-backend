package com.example.service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.example.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testAddUser(){
        User user = new User();
        user.setUsername("zhangsan");
        user.setUserAccount("123");
        user.setAvatarUrl("https://img1.baidu.com/it/u=1309598079,2692816358&fm=253&fmt=auto&app=120&f=JPEG?w=190&h=190");
        user.setGender(0);
        user.setUserPassword("123");
        user.setPhone("110");
        user.setEmail("110@qq.com");



        boolean save = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(save);
    }


    @Test
    void userRegister() {
        String userAccount = "1234";
        String userPassword = "";
        String checkPassword = "1234";
        String planetCode = "1";

        // 字段是否为空
        long res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,res);

        // 账号不能小于4位
         userAccount = "123";
         res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
         Assertions.assertEquals(-1,res);

        // 密码不能小于6位
        userAccount = "1234";
        userPassword = "1234";
        checkPassword = "1234";
        res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,res);

        // 账号不能包含特殊字符
        userAccount = "1234@#";
        userPassword = "123456";
        checkPassword = "123456";
        res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,res);

        // 密码和校验密码是否相同
        userAccount = "1234";
        checkPassword = "1234567";
        res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,res);


        checkPassword = "123456";
        res = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertTrue(res > 0);
    }

    @Test
    void searchUserByTags(){
        List<String> tagNameList = Arrays.asList("java");

        List<User> userList = userService.searchUserByTags(tagNameList);
        Assert.assertNotNull(userList);
    };
}