package com.community.controller;

import com.community.annotation.LoginRequired;
import com.community.entity.Comment;
import com.community.entity.DiscussPost;
import com.community.entity.Page;
import com.community.entity.User;
import com.community.service.*;
import com.community.util.*;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domainPath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    private String headerBucketUrl;

    @Autowired
    private AliyunUtil aliyunUtil;

    @Value("${aliyun.bucket.header.url}")
    private String headerUrl;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ReadingService readingService;

    @Autowired
    private CommentService commentService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {

        // 上传文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    // 更改密码
    @RequestMapping(path = "/changepsw", method = RequestMethod.POST)
    public String changepsw(Model model, String old_password,
                            String new_password, String confirm_password) {

        User user = hostHolder.getUser();
        Map<String, Object> map = userService.changepsw(old_password, new_password, confirm_password, user);

        if(map == null || map.isEmpty()) {
            // 成功
            model.addAttribute("msg", "修改成功！请重新登录。");
            model.addAttribute("target", "/index");
            return "/site/login";
        }

        // 修改失败
        model.addAttribute("old_passwordMsg", map.get("old_passwordMsg"));
        model.addAttribute("new_passwordMsg", map.get("new_passwordMsg"));
        model.addAttribute("confirm_passwordMsg", map.get("confirm_passwordMsg"));
        return "/site/setting";
    }

    // 不使用
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String updateHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确");
            return "/site/setting";
        }

        // filename = CommunityUtil.generateUUID() + suffix;
        // File file = new File(uploadPath + "/" + filename);
        //
        // try {
        //     headerImage.transferTo(file);
        // } catch (IOException e) {
        //     logger.error("上传文件错误", e.getMessage());
        //     throw new RuntimeException("上传文件失败，服务器异常", e);
        // }
        //
        // // 更新当前用户的头像的路径(web访问路径)
        // // http://localhost:8080/community/user/header/xxx.png
        //
        // User user = hostHolder.getUser();
        // String headerUrl = domainPath + contextPath + "/user/header/" + filename;


        filename = CommunityUtil.generateUUID() + suffix;

        try {
            aliyunUtil.upload2Aliyun(headerImage.getBytes(), filename);
        } catch (IOException e) {
            logger.error("上传文件错误", e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常", e);
        }

        String url = headerUrl + "/" + filename;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return "redirect:/index";
    }

    // 不使用
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        fileName = uploadPath + "/" + fileName;
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        response.setContentType("image/" + suffix);

        try (
                FileInputStream fis = new FileInputStream(fileName);
                ServletOutputStream os = response.getOutputStream()
        ) {

            byte[] buffer = new byte[1024];
            int b;
            while((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败", e.getMessage());
        }
    }

    // 个人主页
    // 个人信息
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        final User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);

        // 点赞数量
        final int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        final long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        // 粉丝数量
        final long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        // 是否关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    // 我的帖子
    @RequestMapping(path = "/myPost/{userId}", method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        int postCount = discussPostService.findDiscussPostRows(userId);
        page.setRows(postCount);
        // orderMode = 0 不使用缓存
        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(list != null) {
            for(DiscussPost post : list) {
                // 帖子
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);

                // 点赞
                long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                // 阅读量
                int readCount = readingService.findPostReadCount(post.getId());
                map.put("readCount", readCount);

                discussPosts.add(map);
            }
        }

        // 用户
        model.addAttribute("user", user);
        model.addAttribute("postCount", postCount);
        model.addAttribute("discussPosts", discussPosts);

        return "/site/my-post";
    }

    // 个人信息-我的回复
    @RequestMapping(path = "/myReply/{userId}", method = RequestMethod.GET)
    public String getMyReply(Model model, Page page, @PathVariable("userId") int userId) {
        // 只查询回复帖子，不查询回复评论
        User user = userService.findUserById(userId);
        int commentCount = commentService.findCommentRows(userId, ENTITY_TYPE_POST);
        page.setRows(commentCount);

        List<Comment> list = commentService.findCommentsByUserId(userId, ENTITY_TYPE_POST, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new ArrayList<>();

        if(list != null) {
            for(Comment com : list) {
                Map<String, Object> map = new HashMap<>();

                map.put("comment", com);

                // 获取评论的帖子
                DiscussPost post = discussPostService.findDiscussPostById(com.getEntityId());
                map.put("postTitle", post.getTitle());
                map.put("postId", post.getId());

                comments.add(map);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("commentCount", commentCount);
        model.addAttribute("comments", comments);

        return "/site/my-reply";
    }

}
