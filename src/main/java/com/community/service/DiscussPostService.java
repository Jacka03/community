package com.community.service;

import com.community.dao.DiscussPostMapper;
import com.community.entity.DiscussPost;
import com.community.util.SensitiveFilter;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {

                        if(key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String[] split = key.split(":");
                        if(split == null || split.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.parseInt(split[0]);
                        int limit = Integer.parseInt(split[1]);

                        // 二级缓存：使用redis，先查询redis，没有在查询数据库

                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });

        // 初始化帖子总数
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {

                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {

        if(userId == 0 && orderMode == 1) {
            // 只缓存首页的，热门的帖子
            return postListCache.get(offset + ":" + limit);

        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if(userId == 0) {
            // 首页查询, 永远以0 作为key
            return postRowsCache.get(userId);
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost) {
        if(discussPost == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 转义HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentId){
        return discussPostMapper.updateCommentCount(id, commentId);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
