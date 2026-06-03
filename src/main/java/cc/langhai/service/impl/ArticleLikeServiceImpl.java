package cc.langhai.service.impl;

import cc.langhai.domain.ArticleLike;
import cc.langhai.exception.BusinessException;
import cc.langhai.mapper.ArticleLikeMapper;
import cc.langhai.response.ReturnCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleLikeServiceImpl implements cc.langhai.service.ArticleLike {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_LIKE_COUNT_KEY = "article:like:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long articleId, Long userId) {
        if (isLiked(articleId, userId)) {
            throw new BusinessException(new ReturnCode() {
                @Override
                public Integer getCode() { return 500; }
                @Override
                public String getMessage() { return "同一用户仅可点赞一次"; }
            });
        }
        
        ArticleLike articleLike = new ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        articleLike.setCreateTime(new Date());
        
        articleLikeMapper.insertArticleLike(articleLike);
        
        // Update Redis cache (double update)
        String key = REDIS_LIKE_COUNT_KEY + articleId;
        Long count = getLikeCountFromDB(articleId);
        redisTemplate.opsForValue().set(key, count, 1, TimeUnit.HOURS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long articleId, Long userId) {
        if (!isLiked(articleId, userId)) {
            throw new BusinessException(new ReturnCode() {
                @Override
                public Integer getCode() { return 500; }
                @Override
                public String getMessage() { return "您还未点赞，无法取消"; }
            });
        }
        
        articleLikeMapper.deleteArticleLike(articleId, userId);
        
        // Update Redis cache
        String key = REDIS_LIKE_COUNT_KEY + articleId;
        Long count = getLikeCountFromDB(articleId);
        redisTemplate.opsForValue().set(key, count, 1, TimeUnit.HOURS);
    }

    @Override
    public Long getLikeCount(Long articleId) {
        String key = REDIS_LIKE_COUNT_KEY + articleId;
        Object countObj = redisTemplate.opsForValue().get(key);
        if (countObj != null) {
            return Long.valueOf(countObj.toString());
        }
        
        // Cache miss, read from DB
        Long count = getLikeCountFromDB(articleId);
        redisTemplate.opsForValue().set(key, count, 1, TimeUnit.HOURS);
        return count;
    }

    private Long getLikeCountFromDB(Long articleId) {
        Long count = articleLikeMapper.countByArticleId(articleId);
        return count == null ? 0L : count;
    }

    @Override
    public boolean isLiked(Long articleId, Long userId) {
        return articleLikeMapper.countByArticleIdAndUserId(articleId, userId) > 0;
    }
}
