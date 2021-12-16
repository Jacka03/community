package com.community;

import com.community.dao.DiscussPostMapper;
import com.community.dao.LoginTicketMapper;
import com.community.dao.UserMapper;
import com.community.entity.DiscussPost;
import com.community.entity.LoginTicket;
import com.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzTransactionManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testSelectDiscussPost(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 2, 10, 0);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
    }

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Test
    public void testInsertLoginTicket() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(111);
        loginTicket.setTicket("qwer");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        loginTicketMapper.insertLoginTicket(loginTicket);

    }

    @Test
    public void testSelectLoginTicket() {
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("qwer");
        System.out.println(loginTicket);

        // loginTicket.setStatus(1);
        loginTicketMapper.updateStatus("qwer", 1);
    }

    @Test
    public void test() {
        int[] arr = new int[0];
        System.out.println(0 >> 1);
    }
}
