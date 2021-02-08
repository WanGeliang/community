package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.nowcoder.community.util.CommunityConstant.*;

@Controller
public class LoginController {
    //注入quanluj
    @Value("server.servlet.context-path")
    private String contextPath;

    //引入日志文件
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    //引入kaptcha
    @Autowired
    Producer kaptchaProducer;
    //引入用户层service
    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;

    //跳转到注册界面
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String register() {
        return "/site/register";
    }

    /**
     * 跳转到登录界面
     *
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }


    /**
     * 判断从是否注册成功
     *
     * @param model
     * @param user
     * @return
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        //model的作用，就是将数据带到前端，也会同model一起的user对象的值放到model中，一起也带到前端界面
        Map<String, Object> map = userService.register(user);
        //map为空此时说明用户注册的信息有效
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";//注册成功，跳转到操作结果界面
        } else {
            //map不为空，用户的注册的信息有问题
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));

            return "/site/register";//注册失败，继续跳转到注册界面
        }

    }

    // http://localhost:8080/community/activation/101/code

    /**
     * 将激活码进行验证
     *
     * @param model
     * @param userId
     * @param code
     * @return
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            //激活成功
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /**
     * 通过kaptcha生成图片返回到前端
     *
     * @param response 利用redis进行存储验证码
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void producerKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码,这个时候是字符串
        String text = kaptchaProducer.createText();
        //将生成的验证码生成为图片
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
//        session.setAttribute("kaptcha", text);
        //将验证码存到cookie
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //再将验证码存入到redis
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey, text,60, TimeUnit.SECONDS);
        // 将图片输出给浏览器
        response.setContentType("image/png");//格式
        try {
            OutputStream os = response.getOutputStream();

            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }


    /**
     * 检查登录界面输入的数据是否一致
     *
     * @param username
     * @param password
     * @param code
     * @param rememberme 超时时间使用
     * @param model      存错误数据使用
//     * @param session    取验证码数据使用
     * @param response   重定向使用
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,//这些单独的数据不会封装到MVC中
                        Model model, /*HttpSession session, */HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        //检查验证码
//        String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha =null;

        if(StringUtils.isNoneBlank(kaptchaOwner)){
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确");
            return "site/login";
        }
        //检查账号密码
        int expireSeconds = rememberme ? REMEMBERME_TIME : DEFAULT_TIME;
        Map<String, Object> map = userService.login(username, password, expireSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expireSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }

}
