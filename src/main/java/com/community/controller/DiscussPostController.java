package com.community.controller;

import com.community.dao.DiscussPostMapper;
import com.community.entity.DiscussPost;
import com.community.entity.User;
import com.community.service.DiscussPostService;
import com.community.util.CommunityUtil;
import com.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequestMapping("discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {

        User user = hostHolder.getUser();
        if(user == null) {
            return CommunityUtil.getJSONString(403, "请登录再发贴子");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());

        discussPostService.addDiscussPost(discussPost);
        return CommunityUtil.getJSONString(0, "发布成功");

    }


}
