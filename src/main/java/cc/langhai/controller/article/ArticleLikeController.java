package cc.langhai.controller.article;

import cc.langhai.config.annotation.RequestAuthority;
import cc.langhai.domain.User;
import cc.langhai.exception.BusinessException;
import cc.langhai.response.ResultResponse;
import cc.langhai.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private cc.langhai.service.ArticleLike articleLikeService;

    @Autowired
    private RegisterService registerService;

    @ResponseBody
    @GetMapping("/status")
    public ResultResponse<Map<String, Object>> status(Long articleId, HttpServletRequest request, HttpSession session) {
        User user = this.getCurrentUser(request, session);
        Long userId = user == null ? null : user.getId();
        return new ResultResponse<>(200, "获取点赞状态成功。", articleLikeService.getLikeStatus(articleId, userId));
    }

    @ResponseBody
    @PostMapping("/like")
    @RequestAuthority(value = {"admin", "user", "vip"})
    public ResultResponse<Map<String, Object>> like(Long articleId, HttpServletRequest request, HttpSession session) {
        User user = this.requireUser(request, session);
        return new ResultResponse<>(200, "点赞成功。", articleLikeService.likeArticle(articleId, user.getId()));
    }

    @ResponseBody
    @PostMapping("/cancel")
    @RequestAuthority(value = {"admin", "user", "vip"})
    public ResultResponse<Map<String, Object>> cancel(Long articleId, HttpServletRequest request, HttpSession session) {
        User user = this.requireUser(request, session);
        return new ResultResponse<>(200, "取消点赞成功。", articleLikeService.cancelLike(articleId, user.getId()));
    }

    private User requireUser(HttpServletRequest request, HttpSession session) {
        User user = this.getCurrentUser(request, session);
        if (user == null) {
            throw new BusinessException(500, "请先登录后再点赞。");
        }
        return user;
    }

    private User getCurrentUser(HttpServletRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            registerService.remember(request, session);
            user = (User) session.getAttribute("user");
        }
        return user;
    }
}
