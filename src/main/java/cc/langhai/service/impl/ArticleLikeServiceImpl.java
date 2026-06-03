package cc.langhai.service.impl;

import cc.langhai.domain.ArticleLike;
import cc.langhai.mapper.ArticleLikeMapper;
import cc.langhai.service.IArticleLikeService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements IArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> toggle(Long articleId, Long userId) {
        ArticleLike existing = articleLikeMapper.selectOne(Wrappers.<ArticleLike>lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));

        boolean liked;
        if (existing != null) {
            articleLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticleId(articleId);
            articleLike.setUserId(userId);
            articleLike.setCreateTime(new Date());
            articleLikeMapper.insert(articleLike);
            liked = true;
        }

        String redisKey = "article:like:" + articleId;
        long count = getCountFromDB(articleId);
        redisTemplate.opsForValue().set(redisKey, count, 1, TimeUnit.HOURS);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    @Override
    public long getLikeCount(Long articleId) {
        String redisKey = "article:like:" + articleId;
        Object value = redisTemplate.opsForValue().get(redisKey);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        long count = getCountFromDB(articleId);
        redisTemplate.opsForValue().set(redisKey, count, 1, TimeUnit.HOURS);
        return count;
    }

    @Override
    public boolean hasLiked(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        Integer count = articleLikeMapper.selectCount(Wrappers.<ArticleLike>lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        return count != null && count > 0;
    }

    private long getCountFromDB(Long articleId) {
        Integer count = articleLikeMapper.selectCount(Wrappers.<ArticleLike>lambdaQuery()
                .eq(ArticleLike::getArticleId, articleId));
        return count != null ? count : 0;
    }
}
