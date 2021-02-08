package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

import java.util.Date;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    //插入一条登录用户信息的数据
    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired}) "
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
//自动生成id
    int insertLoginTicket(LoginTicket loginTicket);

    //根据ticket查询用户的信息
    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectLoginTicket(String ticket);

    //根据用户id查询用户的信息
    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where user_id=#{userId}"
    })
    LoginTicket selectLoginTicketByUserId(int userId);

    //根据用户的ticket修改用户的登录状态
    @Update({
            "update login_ticket set ticket=#{ticket},expired=#{expired} where user_id=#{userId}"
    })
    int updateLoginTicketByGetUserId(String ticket, Date expired, int userId);

    //根据用户的ticket修改用户的登录状态
    @Update({
            "update login_ticket set status=#{status} where ticket=#{ticket}"
    })
    int updateLoginTicket(String ticket, int status);
}
