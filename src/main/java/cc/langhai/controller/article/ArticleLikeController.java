package cc.langhai.controller.article;

import cc.langhai.response.ArticleLikeReturnCode;
import cc.langhai.response.ResultResponse;
import cc.langhai.service.IArticleLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private IArticleLikeService articleLikeService;

    @ResponseBody
    @PostMapping("/like")
    public ResultResponse<Map<String, Object>> like(Long articleId) {
        boolean liked = articleLikeService.like(articleId);
        Long count = articleLikeService.getLikeCount(articleId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", count);
        if (liked) {
            return ResultResponse.success(ArticleLikeReturnCode.ARTICLE_LIKE_OK_00001, result);
        } else {
            return ResultResponse.success(ArticleLikeReturnCode.ARTICLE_LIKE_CANCEL_OK_00002, result);
        }
    }

    @ResponseBody
    @GetMapping("/count")
    public ResultResponse<Map<String, Object>> count(Long articleId) {
        Long count = articleLikeService.getLikeCount(articleId);
        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", count);
        return ResultResponse.success(ArticleLikeReturnCode.ARTICLE_LIKE_COUNT_OK_00005, result);
    }
}