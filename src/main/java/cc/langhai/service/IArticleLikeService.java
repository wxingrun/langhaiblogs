package cc.langhai.service;

import cc.langhai.domain.ArticleLike;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IArticleLikeService extends IService<ArticleLike> {

    boolean like(Long articleId);

    Long getLikeCount(Long articleId);
}