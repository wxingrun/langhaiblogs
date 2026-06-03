package cc.langhai.service;

import cc.langhai.domain.Article;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 文章点赞服务接口
 *
 * @author langhai
 * @date 2024-01-01
 */
public interface ArticleLikeService extends IService<cc.langhai.domain.ArticleLike> {

    /**
     * 点赞文章
     *
     * @param articleId 文章id
     */
    void likeArticle(Long articleId);

    /**
     * 取消点赞文章
     *
     * @param articleId 文章id
     */
    void unlikeArticle(Long articleId);

    /**
     * 获取文章点赞数（优先从Redis获取）
     *
     * @param articleId 文章id
     * @return 点赞数
     */
    Integer getLikeCount(Long articleId);

    /**
     * 判断当前用户是否已点赞该文章
     *
     * @param articleId 文章id
     * @return 是否已点赞
     */
    Boolean isLiked(Long articleId);

    /**
     * 为文章列表添加点赞数
     *
     * @param articleList 文章列表
     * @return 包含点赞数的文章列表
     */
    List<Article> getArticleLikeCount(List<Article> articleList);
}