package com.community.controller;

import com.community.entity.DiscussPost;
import com.community.entity.Page;
import com.community.service.ElasticsearchService;
import com.community.service.LikeService;
import com.community.service.UserService;
import com.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticSearchService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        Map<String, Object> searchMap = elasticSearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        List<DiscussPost> discussPostList = (List<DiscussPost>) searchMap.get("discussPosts");
        final Number num = (Number) searchMap.get("totalHits");
        int totalHits = num.intValue();

        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(discussPostList != null) {
            for (DiscussPost discussPost : discussPostList) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", discussPost);

                // 作者
                map.put("user", userService.findUserById(discussPost.getUserId()));

                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);
        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(discussPostList == null ? 0 : totalHits);

        return "/site/search";

    }


}
