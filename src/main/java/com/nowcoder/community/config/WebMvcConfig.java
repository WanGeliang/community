package com.nowcoder.community.config;

import com.nowcoder.community.interceptor.DataInterceptor;
import com.nowcoder.community.interceptor.LoginRequiredInterceptor;
import com.nowcoder.community.interceptor.LoginTicketInterceptor;
import com.nowcoder.community.interceptor.MessageInterceptor;
import org.elasticsearch.search.aggregations.bucket.histogram.DateIntervalConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
//    @Autowired
//    LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    MessageInterceptor messageInterceptor;

    @Autowired
    DataInterceptor dataInterceptor;
    //设置拦截器，拦截所有路径
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //过滤掉静态资源
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/.*jpg", "/**/*.jpeg");
//        registry.addInterceptor(loginRequiredInterceptor)
//                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/.*jpg", "/**/*.jpeg");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/.*jpg", "/**/*.jpeg");
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/.*jpg", "/**/*.jpeg");
    }
}
