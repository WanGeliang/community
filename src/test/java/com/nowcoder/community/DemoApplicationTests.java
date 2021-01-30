package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class DemoApplicationTests {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Test
    void selectDiscussPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149, 0, 10);
        for(DiscussPost l:list){
            System.out.println(l);
        }
        int resutl = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(resutl);
    }

    @Test
    void test1(){
           Logger logger = LoggerFactory.getLogger(DemoApplicationTests.class);
            System.out.println(logger.getName());

            //测试
            logger.debug("debug log");
            logger.info("info log");
            logger.warn("warn log");
            logger.error("error log");

    }

}
