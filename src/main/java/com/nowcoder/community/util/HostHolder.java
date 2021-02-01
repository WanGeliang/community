package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 使用ThreadLocal来当单独线程，来代替session
 */
@Component
public class HostHolder {
    ThreadLocal<User> threadLocal = new ThreadLocal();

    //将用户信息设置在threadLocal里面
    public void setUser(User user) {
        threadLocal.set(user);
    }

    //将用户信息从local里面取出来
    public User getUser() {
        return threadLocal.get();
    }

    //从local里面移除信息，以免local里面的内容超出
    public void removeUser() {
        threadLocal.remove();
    }
}
