package com.example.service;

import com.example.utils.AlgorithmUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class AlgorithmUtilTest {

    @Test
    void test(){
        AlgorithmUtil algorithmUtil = new AlgorithmUtil();

        List<String> list1 = Arrays.asList("java", "打球", "男");
        List<String> list2 = Arrays.asList("c++", "打蓝球", "男");
        List<String> list3 = Arrays.asList("java", "打球", "女");

        int score1 = algorithmUtil.minDistance(list1, list2);
        int score2 = algorithmUtil.minDistance(list1, list3);

        System.out.println(score1);
        System.out.println(score2);
    }
}
