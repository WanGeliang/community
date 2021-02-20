package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
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

    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Autowired
    LikeService likeService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    FollowService followService;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @Value("${community.path.domain}")
    String domain;

    @Value("${community.path.upload}")
    String uploadPath;

    @Value("${qiniu.key.access}")
    String accessKey;

    @Value("${qiniu.key.secret}")
    String secretKey;

    @Value("${qiniu.bucket.header.name}")
    String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    String headerBucketUrl;

    @LoginRequired//登录时才有这些功能
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        //上传文件的名称,生成随机名
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy = new StringMap();
        //返回方式：异步的请求方式
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        //生成上传的凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

    //更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeaderUrl(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }

    /**
     * 上传照片
     *
     * @param headerImage
     * @param model
     * @return
     */
    //废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeaderUrl(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "你还没有选择需要上传的图片");
            return "/site/setting";
        }
        String filename = headerImage.getOriginalFilename();
        //获取图片的后缀名
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;
        //确定文件存放的路径
        File dest = new File(uploadPath + "/" + filename);

        try {
            //存储文件到该路径下
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常");
        }

        //更新当前用户的头像的路径（web访问的路径）
        //http://localhost:8080/community/user/header/xxx.png
        //上面这种格式
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;//给下面这个方法进行调用
        userService.updateHeaderUrl(user.getId(), headerUrl);
        //重定向到首页
        return "redirect:/index";
    }

    /**
     * 从web服务器上进行调用
     *
     * @param fileName
     * @param response
     */
    //废弃
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //服务器存放的路径
        fileName = uploadPath + "/" + fileName;//在本机上是系统盘里的路径，相当于还是从本机上取的
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ServletOutputStream os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像信息失败：" + e.getMessage());
        }

    }

    @LoginRequired
    @RequestMapping(path = "/updateCode", method = RequestMethod.POST)
    public String updateCode(String oldpassword, String newpassword, String renewpassword, Model model, HttpServletResponse response) {
        //空值在前端就会进行判断
        User user = hostHolder.getUser();
        String oldmd5 = CommunityUtil.md5(oldpassword + user.getSalt());//要先进行判断md5加密

        if (!user.getPassword().equals(oldmd5)) {
            model.addAttribute("oldCodeMsg", "输入的原始密码不正确，请重新输入！");
            return "/site/setting";
        }
        if (!newpassword.equals(renewpassword)) {
            model.addAttribute("newCodeMsg", "两次新输入的密码不一致，请重新输入！");
            return "/site/setting";
        }
        userMapper.updatePassword(user.getId(), CommunityUtil.md5(renewpassword + user.getSalt()));
        return "redirect:/index";
    }

    /**
     * 查询个人信息
     *
     * @param userId
     * @param model
     * @return
     */
    @RequestMapping(value = "/profile/{userId}", method = RequestMethod.GET)
    public String getUserProfile(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        //用户
        model.addAttribute("user", user);
        //点赞数量
        int likeCount = likeService.findUserLikeCounts(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }


}
