package com.community.controller;

import com.community.entity.Comment;
import com.community.entity.DiscussPost;
import com.community.entity.Event;
import com.community.event.EventProducer;
import com.community.service.CommentService;
import com.community.service.DiscussPostService;
import com.community.util.CommunityConstant;
import com.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;


    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {

        // 获取用户id
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());

        commentService.addComment(comment);

        // 出发评论事件（通知）
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);

        // 评论的可能是帖子，也可能是评论
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            // 给帖子做评论
            final DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if(comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            // 给评论做评论
            final Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
