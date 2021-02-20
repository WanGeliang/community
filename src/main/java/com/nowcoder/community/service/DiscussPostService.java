package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Value("${caffeine.posts.max-size}")
    int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    long expireSeconds;

    @Autowired
    DiscussPostMapper discussPostMapper;

    @Autowired
    SensitiveFilter sensitiveFilter;

    //Caffeine核心接口：Cache, LoadingCache, AsyncLoadingCache

    //帖子列表的缓存
    public LoadingCache<String, List<DiscussPost>> postListCache;

    //帖子行数的缓存
    public LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //二级缓存：Redis->mysql
                        logger.info("load post from db");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        //
                        logger.info("load row from db");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        //热门帖子，加上用户没有登录时才进行缓存
//        if (userId == 0 && orderMode == 1) {
//            return postListCache.get(offset + ":" + limit);
//        }
        logger.debug("load post list form db");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        //缓存用户没有登录时的帖子总数
//        if (userId == 0) {
//            return postRowsCache.get(userId);
//        }
        logger.debug("load post rows form db");
        return discussPostMapper.selectDiscussPostRows(userId);
    }


    //添加一条帖子
    public int addDiscussPost(DiscussPost post) {
        System.out.println("正在发布帖子");
        if (post == null) {
            throw new IllegalArgumentException("输入的帖子为空，不合法");
        }
        //转义html标签
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //过滤敏感词汇
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        //调用mapper层进行调用
        return discussPostMapper.insertDiscussPost(post);
    }

    //查询一条帖子的详情
    public DiscussPost getDiscussPostDetails(int id) {
        return discussPostMapper.selectDiscussPostDetail(id);
    }

    //更新帖子数量
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    //跟新帖子类型
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    //跟新帖子状态
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    //跟新帖子状态
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
