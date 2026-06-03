package cc.langhai.service.impl;

import cc.langhai.domain.Article;
import cc.langhai.domain.ArticleLike;
import cc.langhai.exception.BusinessException;
import cc.langhai.mapper.ArticleLikeMapper;
import cc.langhai.response.ArticleLikeReturnCode;
import cc.langhai.service.ArticleLikeService;
import cc.langhai.utils.UserContext;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文章点赞服务实现类
 *
 * @author langhai
 * @date 2024-01-01
 */
@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LIKE_KEY_PREFIX = "article:like:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId) {
        Long userId = UserContext.getUserId();
        if (ObjectUtil.isNull(userId)) {
            throw new BusinessException(ArticleLikeReturnCode.ARTICLE_LIKE_NOT_LOGIN_00006);
        }

        // 检查是否已经点赞过
        ArticleLike articleLike = articleLikeMapper.selectOne(
                Wrappers.<ArticleLike>lambdaQuery()
                        .eq(ArticleLike::getArticleId, articleId)
                        .eq(ArticleLike::getUserId, userId)
        );
        if (ObjectUtil.isNotNull(articleLike)) {
            throw new BusinessException(ArticleLikeReturnCode.ARTICLE_LIKE_ALREADY_00003);
        }

        // 保存点赞记录
        articleLike = new ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        articleLike.setCreateTime(new Date());
        articleLikeMapper.insert(articleLike);

        // 更新Redis缓存
        this.updateLikeCountRedis(articleId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId) {
        Long userId = UserContext.getUserId();
        if (ObjectUtil.isNull(userId)) {
            throw new BusinessException(ArticleLikeReturnCode.ARTICLE_LIKE_NOT_LOGIN_00006);
        }

        // 删除点赞记录
        int deleteCount = articleLikeMapper.delete(
                Wrappers.<ArticleLike>lambdaQuery()
                        .eq(ArticleLike::getArticleId, articleId)
                        .eq(ArticleLike::getUserId, userId)
        );
        if (deleteCount == 0) {
            throw new BusinessException(ArticleLikeReturnCode.ARTICLE_UNLIKE_FAIL_00005);
        }

        // 更新Redis缓存
        this.updateLikeCountRedis(articleId, -1);
    }

    @Override
    public Integer getLikeCount(Long articleId) {
        String key = LIKE_KEY_PREFIX + articleId;
        String countStr = redisTemplate.opsForValue().get(key);

        if (ObjectUtil.isNotNull(countStr)) {
            return Integer.parseInt(countStr);
        }

        // 从数据库查询
        Integer count = Math.toIntExact(articleLikeMapper.selectCount(
                Wrappers.<ArticleLike>lambdaQuery().eq(ArticleLike::getArticleId, articleId)
        ));

        // 写入Redis缓存
        redisTemplate.opsForValue().set(key, count.toString(), 1, TimeUnit.HOURS);

        return count;
    }

    @Override
    public Boolean isLiked(Long articleId) {
        Long userId = UserContext.getUserId();
        if (ObjectUtil.isNull(userId)) {
            return false;
        }

        ArticleLike articleLike = articleLikeMapper.selectOne(
                Wrappers.<ArticleLike>lambdaQuery()
                        .eq(ArticleLike::getArticleId, articleId)
                        .eq(ArticleLike::getUserId, userId)
        );
        return ObjectUtil.isNotNull(articleLike);
    }

    @Override
    public List<Article> getArticleLikeCount(List<Article> articleList) {
        if (CollectionUtil.isNotEmpty(articleList)) {
            for (Article article : articleList) {
                Integer count = this.getLikeCount(article.getId());
                article.setLikeCount(count);
            }
        }
        return articleList;
    }

    /**
     * 更新Redis中的点赞数
     *
     * @param articleId 文章id
     * @param delta 增量（正数表示增加，负数表示减少）
     */
    private void updateLikeCountRedis(Long articleId, int delta) {
        String key = LIKE_KEY_PREFIX + articleId;
        String countStr = redisTemplate.opsForValue().get(key);

        if (ObjectUtil.isNotNull(countStr)) {
            int newCount = Integer.parseInt(countStr) + delta;
            if (newCount < 0) {
                newCount = 0;
            }
            redisTemplate.opsForValue().set(key, String.valueOf(newCount), 1, TimeUnit.HOURS);
        } else {
            // 缓存不存在，从数据库查询后写入
            Integer count = this.getLikeCount(articleId);
            redisTemplate.opsForValue().set(key, count.toString(), 1, TimeUnit.HOURS);
        }
    }
}