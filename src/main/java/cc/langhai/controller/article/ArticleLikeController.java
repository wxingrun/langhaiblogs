package cc.langhai.controller.article;

import cc.langhai.domain.User;
import cc.langhai.response.ResultResponse;
import cc.langhai.service.IArticleLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private IArticleLikeService articleLikeService;

    @ResponseBody
    @PostMapping("/toggle")
    public ResultResponse<Map<String, Object>> toggle(Long articleId, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return new ResultResponse<>(401, "请先登录后再点赞", null);
        }
        if (articleId == null) {
            return new ResultResponse<>(500, "参数错误", null);
        }
        Map<String, Object> result = articleLikeService.toggle(articleId, user.getId());
        boolean liked = (boolean) result.get("liked");
        return new ResultResponse<>(200, liked ? "点赞成功" : "取消点赞成功", result);
    }

    @ResponseBody
    @GetMapping("/count")
    public ResultResponse<Map<String, Object>> count(Long articleId) {
        if (articleId == null) {
            return new ResultResponse<>(500, "参数错误", null);
        }
        long count = articleLikeService.getLikeCount(articleId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return new ResultResponse<>(200, "响应成功", data);
    }

    @ResponseBody
    @GetMapping("/status")
    public ResultResponse<Map<String, Object>> status(Long articleId, HttpServletRequest request) {
        if (articleId == null) {
            return new ResultResponse<>(500, "参数错误", null);
        }
        User user = (User) request.getSession().getAttribute("user");
        Long userId = user != null ? user.getId() : null;
        boolean liked = articleLikeService.hasLiked(articleId, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        return new ResultResponse<>(200, "响应成功", data);
    }
}
