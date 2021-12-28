package com.community.service;

import com.community.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReadingService {

    private static final Logger logger = LoggerFactory.getLogger(ReadingService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    //初始化帖子阅读数量
    public void initReading(int postId) {
        String redisKey = RedisKeyUtil.getPostReadingKey(postId);
        redisTemplate.opsForValue().set(redisKey, 0);
    }

    // 用户浏览贴子，帖子阅读数量增加
    public void reading(int postId){
        String redisKey = RedisKeyUtil.getPostReadingKey(postId);
        Integer readCount = (Integer) redisTemplate.opsForValue().get(redisKey);
        if(readCount == null) {
            // logger.info("reading init");
            redisTemplate.opsForValue().set(redisKey, 0);
        }
        // logger.info("incr");
        redisTemplate.opsForValue().increment(redisKey);
    }

    // 获取帖子的阅读数量
    public int readCount(int postId) {
        String redisKey = RedisKeyUtil.getPostReadingKey(postId);
        Integer readCount = (Integer) redisTemplate.opsForValue().get(redisKey);
        if(readCount == null) {
            redisTemplate.opsForValue().set(redisKey, 0);
            readCount = 0;
        }
        return readCount.intValue();
    }
}
