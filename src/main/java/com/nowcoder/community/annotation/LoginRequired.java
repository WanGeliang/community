package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//设置自定义注解的作用：保证没有登录的用户不能跳转到可以通过url的方式来访问页面
// 如果使用了此方法，就使页面跳转到登录界面
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

}
