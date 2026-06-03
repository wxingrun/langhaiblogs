package cc.langhai.service;

import cc.langhai.domain.ArticleLike;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface IArticleLikeService extends IService<ArticleLike> {

    Map<String, Object> toggle(Long articleId, Long userId);

    long getLikeCount(Long articleId);

    boolean hasLiked(Long articleId, Long userId);
}
