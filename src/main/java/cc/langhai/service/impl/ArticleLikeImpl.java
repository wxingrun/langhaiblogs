package cc.langhai.service.impl;

import cc.langhai.exception.BusinessException;
import cc.langhai.mapper.ArticleLikeMapper;
import cc.langhai.service.ArticleService;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleLikeImpl extends ServiceImpl<ArticleLikeMapper, cc.langhai.domain.ArticleLike> implements cc.langhai.service.ArticleLike {

    private static final String ARTICLE_LIKE_KEY_PREFIX = "article:like:";

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Map<String, Object> getLikeStatus(Long articleId, Long userId) {
        this.validateArticle(articleId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", this.hasLiked(articleId, userId));
        result.put("likeCount", this.getLikeCount(articleId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> likeArticle(Long articleId, Long userId) {
        this.validateArticle(articleId);
        this.validateUser(userId);
        if (this.hasLiked(articleId, userId)) {
            throw new BusinessException(500, "您已经点赞过该文章了。");
        }
        cc.langhai.domain.ArticleLike articleLike = new cc.langhai.domain.ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        articleLike.setCreateTime(new Date());
        try {
            this.save(articleLike);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(500, "您已经点赞过该文章了。");
        }
        return this.buildOperateResult(articleId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelLike(Long articleId, Long userId) {
        this.validateArticle(articleId);
        this.validateUser(userId);
        cc.langhai.domain.ArticleLike articleLike = articleLikeMapper.getByArticleIdAndUserId(articleId, userId);
        if (ObjectUtil.isNull(articleLike)) {
            throw new BusinessException(500, "您还没有点赞该文章。");
        }
        this.removeById(articleLike.getId());
        return this.buildOperateResult(articleId, false);
    }

    private Map<String, Object> buildOperateResult(Long articleId, boolean liked) {
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", this.refreshLikeCount(articleId));
        return result;
    }

    private boolean hasLiked(Long articleId, Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return false;
        }
        return ObjectUtil.isNotNull(articleLikeMapper.getByArticleIdAndUserId(articleId, userId));
    }

    private Long getLikeCount(Long articleId) {
        String likeCount = redisTemplate.opsForValue().get(this.buildLikeKey(articleId));
        if (StrUtil.isNotBlank(likeCount)) {
            return Long.parseLong(likeCount);
        }
        return this.refreshLikeCount(articleId);
    }

    private Long refreshLikeCount(Long articleId) {
        Integer likeCount = articleLikeMapper.countByArticleId(articleId);
        Long likeCountValue = ObjectUtil.isNull(likeCount) ? 0L : likeCount.longValue();
        redisTemplate.opsForValue().set(this.buildLikeKey(articleId), String.valueOf(likeCountValue), 1, TimeUnit.HOURS);
        return likeCountValue;
    }

    private String buildLikeKey(Long articleId) {
        return ARTICLE_LIKE_KEY_PREFIX + articleId;
    }

    private void validateArticle(Long articleId) {
        if (ObjectUtil.isNull(articleId) || ObjectUtil.isNull(articleService.getById(articleId))) {
            throw new BusinessException(500, "文章不存在。");
        }
    }

    private void validateUser(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            throw new BusinessException(500, "请先登录后再点赞。");
        }
    }
}
