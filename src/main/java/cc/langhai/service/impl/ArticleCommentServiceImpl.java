package cc.langhai.service.impl;

import cc.langhai.config.constant.ArticleConstant;
import cc.langhai.domain.Article;
import cc.langhai.domain.ArticleComment;
import cc.langhai.exception.BusinessException;
import cc.langhai.mapper.ArticleCommentMapper;
import cc.langhai.response.ArticleCommentReturnCode;
import cc.langhai.response.ArticleReturnCode;
import cc.langhai.service.IArticleCommentService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章评论服务实现类 service接口 实现类
 *
 * @author langhai
 * @date 2023-05-22 10:31
 */
@Service
public class ArticleCommentServiceImpl extends ServiceImpl<ArticleCommentMapper, ArticleComment> implements IArticleCommentService {

    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    @Override
    public List<ArticleComment> getCommentByArticleId(Long articleId) {
        List<ArticleComment> result = articleCommentMapper.getCommentByArticleId(articleId);
        return result;
    }

    @Override
    public List<Article> getArticleHeat(List<Article> articleList) {
        if(CollectionUtil.isNotEmpty(articleList)){
            for (Article article : articleList) {
                Integer count = articleCommentMapper.selectCount(Wrappers.<ArticleComment>lambdaQuery()
                        .eq(ArticleComment::getArticleId, article.getId()));
                article.setCommentCount(count);
            }
        }
        return articleList;
    }

    @Override
    public List<ArticleComment> getAllArticleComment(String content) {
        List<ArticleComment> result = articleCommentMapper.getAllArticleComment(content);
        return result;
    }

    @Override
    public void submitComment(Long articleId, String content, Long userId) {
        String commentContent = StrUtil.trim(content);
        if (ObjectUtil.isNull(articleId) || ObjectUtil.isNull(userId) || StrUtil.isBlank(commentContent)) {
            throw new BusinessException(ArticleReturnCode.ARTICLE_SUBMIT_COMMENT_PARAM_FAIL_00013);
        }
        List<ArticleComment> list = this.list(Wrappers.<ArticleComment>lambdaQuery()
                .eq(ArticleComment::getUserId, userId)
                .eq(ArticleComment::getArticleId, articleId));
        if (list.size() >= ArticleConstant.ARTICLE_COMMENT_USER_COUNT) {
            throw new BusinessException(ArticleReturnCode.ARTICLE_SUBMIT_COMMENT_COUNT_FAIL_00015);
        }
        ArticleComment articleComment = new ArticleComment();
        articleComment.setArticleId(articleId);
        articleComment.setContent(commentContent);
        articleComment.setUserId(userId);
        this.save(articleComment);
    }

    @Override
    public void deleteArticleComment(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(ArticleCommentReturnCode.ARTICLE_COMMENT_DELETE_FAIL_00002);
        }
        this.removeById(id);
    }

    @Override
    public void show(Long id, Boolean showFlag) {
        ArticleComment articleComment = this.getById(id);
        if (ObjectUtil.isNull(articleComment)) {
            throw new BusinessException(500, "修改评论审核状态失败。");
        }
        articleComment.setShowFlag(showFlag);
        this.updateById(articleComment);
    }
}
