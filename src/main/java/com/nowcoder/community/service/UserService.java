package com.nowcoder.community.service;


import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.nowcoder.community.util.CommunityConstant.*;

@Service
public class UserService {

    //    @Autowired
//    private LoginTicketMapper loginTicketMapper;
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    MailClient mailClient;

    @Autowired
    TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    String domain;

    @Value("${server.servlet.context-path}")
    String path;

    /**
     * 通过用户id查询user
     *
     * @param id
     * @return
     */
    public User findUserById(int id) {
        User user = getCacheUser(id);
        if (user == null) {
            user = userMapper.selectById(id);
            initCacheUser(id);
        }
        return user;
    }


    public User findUserByName(String name) {
        return userMapper.selectByName(name);
    }

    /**
     * 进行注册
     *
     * @param user
     * @return 返回到集合的页面的信息
     */
    public Map<String, Object> register(User user) {
        HashMap<String, Object> map = new HashMap<>();
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "输入的账号为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "输入的密码为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "输入的邮箱为空");
        }
        //不为空时，验证用户名
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        //不为空时，验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册过!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        //像数据库中插入用户信息
        userMapper.insertUser(user);

        //进行激活邮件+
        //利用Theameleaf进行传入
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + path + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        //利用模板引擎将邮箱，和地址传入到前端
        String content = templateEngine.process("/mail/activation", context);
        //使用之前定义好的工具类jing'xing发送邮件
        mailClient.sendMail(user.getEmail(), "激活账号", content);


        return map;
    }

    /**
     * 根据用户id和code值判断激活的状态
     * 只能激活一次，不能重复激活
     *
     * @param userId
     * @param code
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            reloadCacheUser(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 根据用户id进行判断在登录界面username和password是否一致
     *
     * @param username
     * @param password
     * @param expiredTime
     * @return
     */
    public Map<String, Object> login(String username, String password, int expiredTime) {
        HashMap<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号的用户名为空");
            return map;
        }

        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "账号的密码为空");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "用户不存在");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号尚未被激活");
            return map;
        }
        if (!user.getPassword().equals(CommunityUtil.md5(password + user.getSalt()))) {
            map.put("passwordMsg", "账号的密码错误");
            return map;
        }
        //先从数据库中查询loginticket
        LoginTicket loginTicket = new LoginTicket();
//        loginTicket = loginTicketMapper.selectLoginTicketByUserId(user.getId());
//        if (loginTicket != null) {
//            loginTicketMapper.updateLoginTicketByGetUserId(loginTicket.getTicket(),loginTicket.getExpired(),loginTicket.getUserId());
//            loginTicketMapper.updateLoginTicket(loginTicket.getTicket(),0);
//        } else {
        //如果没有从数据库里面查到，就自己插入一条loginTicket数据
        loginTicket.setUserId(user.getId());
        loginTicket.setStatus(0);
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredTime * 1000));

//        loginTicketMapper.insertLoginTicket(loginTicket);
        //将登录凭证存入redis
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
//        }
        //ticket是用户的登录凭证
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    /**
     * 退出功能
     *
     * @param ticket
     */
    public void logout(String ticket) {
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
//        loginTicketMapper.updateLoginTicket(ticket, 1);
    }

    /**
     * 根据用户查询Loginticket
     *
     * @param ticket
     * @return
     */
    public LoginTicket getLoginTicket(String ticket) {
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
//        LoginTicket loginTicket = loginTicketMapper.selectLoginTicket(ticket);
//        return loginTicket;
    }

    /**
     * 根据用户id修改图片路径
     *
     * @param userId
     * @param headerUrl
     * @return
     */
    public int updateHeaderUrl(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        reloadCacheUser(userId);
        return rows;
    }

    //1.从redis缓存里面取值
    public User getCacheUser(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    //2.初始化在redis缓存
    public void initCacheUser(int userId) {
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
    }

    //3.清除,并更新redis缓存
    public void reloadCacheUser(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }


    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list=new ArrayList<>();

        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
              switch (user.getType()){
                  case 1:
                      return AUTHORITY_ADMIN;
                  case 2:
                      return AUTHORITY_MODERATOR;
                  default:
                      return AUTHORITY_USER;
              }
            }
        });

        return list;
    }
}
