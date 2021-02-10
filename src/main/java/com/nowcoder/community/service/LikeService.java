package com.nowcoder.community.service;


import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    RedisTemplate redisTemplate;

    //实现点赞

    /**
     * @param userId       这是点赞这个人的id（就是界面当前的id）
     * @param entityType
     * @param entityId
     * @param entityUserId 这是被点赞那个人的id（就是entity这个实体的id）
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
//        String redisKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        Boolean isMember = redisTemplate.opsForSet().isMember(redisKey, userId);
//        if (isMember) {
//            redisTemplate.opsForSet().remove(redisKey, userId);
//        } else {
//            redisTemplate.opsForSet().add(redisKey, userId);
//        }

        //使用redis事务
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                //开启事务
                operations.multi();
                if (isMember) {
                    operations.opsForSet().remove(entityLikeKey, userId);//记录的是被点赞数量
//                    operations.opsForSet().
                    operations.opsForValue().decrement(userLikeKey);//
                } else {
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }
                //提交事务
                return operations.exec();
            }
        });



    }

    //查询某人对某实体的点赞数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(key);
    }

    //查询某人对某实体的点赞状态
    //点了赞为1，没有点赞为0
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(key, userId) ? 1 : 0;
    }

    //查询某个用户获得的赞
    public int findUserLikeCounts(int userId) {
        String key = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count == null ? 0 : count.intValue();
    }

}
