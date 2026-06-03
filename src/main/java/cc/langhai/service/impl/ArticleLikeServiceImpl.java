package cc.langhai.service.impl;

import cc.langhai.domain.ArticleLike;
import cc.langhai.exception.BusinessException;
import cc.langhai.mapper.ArticleLikeMapper;
import cc.langhai.response.ArticleLikeReturnCode;
import cc.langhai.service.IArticleLikeService;
import cc.langhai.utils.UserContext;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements IArticleLikeService {

    private static final String LIKE_COUNT_KEY_PREFIX = "article:like:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean like(Long articleId) {
        Long userId = UserContext.getUserId();
        if (ObjectUtil.isNull(userId)) {
            throw new BusinessException(ArticleLikeReturnCode.ARTICLE_LIKE_NOT_LOGIN_FAIL_00004);
        }

        ArticleLike exist = this.getOne(Wrappers.<ArticleLike>lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));

        if (ObjectUtil.isNotNull(exist)) {
            this.removeById(exist.getId());
            this.decrRedisLikeCount(articleId);
            return false;
        } else {
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticleId(articleId);
            articleLike.setUserId(userId);
            articleLike.setCreateTime(new Date());
            this.save(articleLike);
            this.incrRedisLikeCount(articleId);
            return true;
        }
    }

    @Override
    public Long getLikeCount(Long articleId) {
        String key = LIKE_COUNT_KEY_PREFIX + articleId;
        String count = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(count)) {
            return Long.parseLong(count);
        }
        Long dbCount = this.count(Wrappers.<ArticleLike>lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId));
        redisTemplate.opsForValue().set(key, dbCount.toString(), 1, TimeUnit.HOURS);
        return dbCount;
    }

    private void incrRedisLikeCount(Long articleId) {
        String key = LIKE_COUNT_KEY_PREFIX + articleId;
        String count = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(count)) {
            redisTemplate.opsForValue().increment(key, 1L);
        } else {
            Long dbCount = this.count(Wrappers.<ArticleLike>lambdaQuery()
                    .eq(ArticleLike::getArticleId, articleId));
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount), 1, TimeUnit.HOURS);
        }
    }

    private void decrRedisLikeCount(Long articleId) {
        String key = LIKE_COUNT_KEY_PREFIX + articleId;
        String count = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(count)) {
            Long val = Long.parseLong(count);
            if (val > 0) {
                redisTemplate.opsForValue().decrement(key, 1L);
            }
        }
    }
}