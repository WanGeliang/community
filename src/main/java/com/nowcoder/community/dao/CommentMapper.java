package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    //分页查询帖子的数量
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    //新增帖子数量
    int insertComment(Comment comment);

    //根据id查询帖子
    Comment selectCommentById(int id);
}
