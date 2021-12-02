package com.community.service;

import com.community.dao.LoginTicketMapper;
import com.community.dao.UserMapper;
import com.community.entity.LoginTicket;
import com.community.entity.User;
import com.community.util.CommunityConstant;
import com.community.util.CommunityUtil;
import com.community.util.MailClient;
import com.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Jacka
 */
@Service
public class UserService implements CommunityConstant {

    // @Autowired
    // private LoginTicketMapper loginTicketMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id) {
        // return userMapper.selectById(id);

        // 使用redis
        // 首先从cache查询
        User user = getCache(id);
        if(user == null) {
            user = initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 检查用户是否正确
        if(user == null) {
            throw new IllegalArgumentException("账号为空");
        }

        if(StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "用户名为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱为空");
            return map;
        }

        // 验证账号是否存在
        User user1 = userMapper.selectByName(user.getUsername());
        if(user1 != null) {
            map.put("usernameMsg", "用户已经存在");
            return map;
        }

        user1 = userMapper.selectByEmail(user.getEmail());
        if(user1 != null) {
            map.put("emailMsg", "邮箱已经存在");
            // System.out.println("邮箱已存在");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());

        userMapper.insertUser(user);

        // 激活邮箱
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/userId/activateCode

        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);

        String process = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", process);

        return map;
    }

    /**
     * 检查激活码
     * @param userId
     * @param code
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if(user.getActivationCode().equals(code)) {

            userMapper.updateStatus(userId, 1);

            // 清理缓存
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }


    //----登录功能

    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        //检查账号密码
        if(StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不存在！");
            return map;
        }

        if(StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不存在");
        }

        User user = userMapper.selectByName(username);
        if(user == null) {
            map.put("usernameMsg", "用户不存在！");
            return map;
        }

        if(user.getStatus() == 0) {
            map.put("usernameMsg", "账号未激活！");
            return map;
        }

        String md5 = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(md5)) {
            map.put("passwordMsg", "密码错误！");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L));

        // loginTicketMapper.insertLoginTicket(loginTicket);
        // 存在redis中
        final String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);


        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket) {
        // loginTicketMapper.updateStatus(ticket, 1);

        // 存在redis中
        final String redisKey = RedisKeyUtil.getTicketKey(ticket);
        final LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);

    }

    public LoginTicket findLoginTicket(String ticket) {
        // return loginTicketMapper.selectByTicket(ticket);
        final String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId, String headerUrl) {
        // return userMapper.updateHeader(userId, headerUrl);

        final int i = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return i;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    // 1.优先从缓存中取数据
    private User getCache(int userId) {
        final String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    // 2.取不到数据就初始化缓存
    private User initCache(int userId) {
        final User user = userMapper.selectById(userId);
        final String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 数据变更时，清除缓存
    private void clearCache(int userId) {
        final String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

}
