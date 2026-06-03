package cc.langhai.service;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface ArticleLike extends IService<cc.langhai.domain.ArticleLike> {

    Map<String, Object> getLikeStatus(Long articleId, Long userId);

    Map<String, Object> likeArticle(Long articleId, Long userId);

    Map<String, Object> cancelLike(Long articleId, Long userId);
}
