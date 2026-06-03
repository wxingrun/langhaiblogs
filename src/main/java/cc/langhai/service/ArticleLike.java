package cc.langhai.service;

/**
 * 文章点赞 Service 接口
 */
public interface ArticleLike {

    /**
     * 点赞
     */
    void like(Long articleId, Long userId);

    /**
     * 取消点赞
     */
    void unlike(Long articleId, Long userId);

    /**
     * 获取点赞数
     */
    Long getLikeCount(Long articleId);

    /**
     * 是否点赞
     */
    boolean isLiked(Long articleId, Long userId);
}
