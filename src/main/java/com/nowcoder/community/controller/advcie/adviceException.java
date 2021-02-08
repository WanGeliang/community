package com.nowcoder.community.controller.advcie;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
//统一记录日常
@ControllerAdvice(annotations = Controller.class)
public class adviceException {
    //记录日志
    private static final Logger logger = LoggerFactory.getLogger(adviceException.class);

    @ExceptionHandler({Exception.class})
    public void getError(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常：" + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        //判断请求是什么请求
        //同步还是异步请求
        String header = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(header)){
            response.setContentType("application/plain;charset=utf-8");
            response.getWriter().write(CommunityUtil.getJSONString(1,"服务器发生异常"));
        }else {
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }

}
