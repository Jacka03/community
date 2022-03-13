package com.community;

import com.community.entity.Comment;
import com.community.service.CommentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CommentTest {
    @Autowired
    private CommentService commentService;

    @Test
    public void testComment() {
        int rows = commentService.findCommentRows(159, 1);
        System.out.println(rows);

        List<Comment> res = commentService.findCommentsByUserId(159, 1, 0, 10);
        if(res != null) {
            for(Comment com : res) {
                System.out.println(com);
            }
        }

    }
}
