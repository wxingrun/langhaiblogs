package cc.langhai.controller.article;

import cc.langhai.response.ArticleLikeReturnCode;
import cc.langhai.response.ResultResponse;
import cc.langhai.service.ArticleLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章点赞控制器
 *
 * @author langhai
 * @date 2024-01-01
 */
@Controller
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private ArticleLikeService articleLikeService;

    /**
     * 点赞文章
     *
     * @param articleId 文章id
     * @return 点赞结果
     */
    @ResponseBody
    @PostMapping("/like")
    public ResultResponse<Void> likeArticle(Long articleId) {
        articleLikeService.likeArticle(articleId);
        return ResultResponse.success(ArticleLikeReturnCode.ARTICLE_LIKE_OK_00001);
    }

    /**
     * 取消点赞文章
     *
     * @param articleId 文章id
     * @return 取消点赞结果
     */
    @ResponseBody
    @PostMapping("/unlike")
    public ResultResponse<Void> unlikeArticle(Long articleId) {
        articleLikeService.unlikeArticle(articleId);
        return ResultResponse.success(ArticleLikeReturnCode.ARTICLE_UNLIKE_OK_00004);
    }

    /**
     * 获取文章点赞信息
     *
     * @param articleId 文章id
     * @return 点赞信息（包含点赞数和当前用户是否已点赞）
     */
    @ResponseBody
    @GetMapping("/info")
    public ResultResponse<Map<String, Object>> getLikeInfo(Long articleId) {
        Map<String, Object> info = new HashMap<>();
        info.put("likeCount", articleLikeService.getLikeCount(articleId));
        info.put("isLiked", articleLikeService.isLiked(articleId));
        return ResultResponse.success(info);
    }
}