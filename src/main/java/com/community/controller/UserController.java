package com.community.controller;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.community.annotation.LoginRequired;
import com.community.entity.User;
import com.community.service.FollowService;
import com.community.service.LikeService;
import com.community.service.UserService;
import com.community.util.CommunityConstant;
import com.community.util.CommunityUtil;
import com.community.util.HostHolder;
import com.community.util.RedisKeyUtil;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

    /*@Value("${aliyun.AccessKeyID}")
    private String AccessKeyId;

    @Value("${aliyun.AccessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.bucket.header.name}")
    private String headerBucketName;

    @Value("${aliyun.bucket.header.endpoint}")
    private String endpoint;

    @Value("${aliyun.bucket.header.oss}")
    private String oss;

    @Value("${aliyun.roleArn}")
    private String roleArn;*/

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        /*// STS接入地址，例如sts.cn-hangzhou.aliyuncs.com。
        // 填写步骤1生成的访问密钥AccessKey ID和AccessKey Secret。
        // 填写步骤3获取的角色ARN。
        // 自定义角色会话名称，用来区分不同的令牌，例如可填写为SessionTest。
        String roleSessionName = "community";
        // 以下Policy用于限制仅允许使用临时访问凭证向目标存储空间examplebucket上传文件。
        // 临时访问凭证最后获得的权限是步骤4设置的角色权限和该Policy设置权限的交集，即仅允许将文件上传至目标存储空间examplebucket下的exampledir目录。
        String policy = "{\n" +
                "    \"Version\": \"1\", \n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Action\": [\n" +
                "                \"oss:PutObject\"\n" +
                "            ], \n" +
                "            \"Resource\": [\n" +
                "                \"acs:oss:*:*:"+headerBucketName+"/*\" \n" +
                "            ], \n" +
                "            \"Effect\": \"Allow\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        try {
            // regionId表示RAM的地域ID。以华东1（杭州）地域为例，regionID填写为cn-hangzhou。也可以保留默认值，默认值为空字符串（""）。
            String regionId = "cn-zhangjiakou";
            // 添加endpoint。
            DefaultProfile.addEndpoint(regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, AccessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);
            // 如果policy为空，则用户将获得该角色下所有权限。
            request.setPolicy(policy);
            // 设置临时访问凭证的有效时间为3600秒。
            request.setDurationSeconds(3600L);
            final AssumeRoleResponse response = client.getAcsResponse(request);
            model.addAttribute("Expiration", response.getCredentials().getExpiration());
            model.addAttribute("AccessKeyId", response.getCredentials().getAccessKeyId());
            model.addAttribute("AccessKeySecret", response.getCredentials().getAccessKeySecret());
            model.addAttribute("SecurityToken", response.getCredentials().getSecurityToken());
            model.addAttribute("RequestId", response.getRequestId());
            model.addAttribute("regionId", regionId);
            model.addAttribute("bucket", headerBucketName);

            // 上传文件名称
            String fileName = CommunityUtil.generateUUID();
            model.addAttribute("fileName", fileName);

            System.out.println("Expiration: " + response.getCredentials().getExpiration());
            System.out.println("Access Key Id: " + response.getCredentials().getAccessKeyId());
            System.out.println("Access Key Secret: " + response.getCredentials().getAccessKeySecret());
            System.out.println("Security Token: " + response.getCredentials().getSecurityToken());
            System.out.println("RequestId: " + response.getRequestId());

        } catch (ClientException e) {
            e.printStackTrace();
        }*/

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

        filename = CommunityUtil.generateUUID() + suffix;
        File file = new File(uploadPath + "/" + filename);

        try {
            headerImage.transferTo(file);
        } catch (IOException e) {
            logger.error("上传文件错误", e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png

        User user = hostHolder.getUser();
        String headerUrl = domainPath + contextPath + "/user/header/" + filename;

        userService.updateHeader(user.getId(), headerUrl);

        // System.out.println("test");
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

}
