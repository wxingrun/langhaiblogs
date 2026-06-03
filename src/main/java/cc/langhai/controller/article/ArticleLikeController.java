package cc.langhai.controller.article;

import cc.langhai.domain.User;
import cc.langhai.response.ResultResponse;
import cc.langhai.response.ReturnCode;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private cc.langhai.service.ArticleLike articleLikeService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/like")
    public ResultResponse<Void> like(Long articleId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResultResponse.fail(new ReturnCode() {
                @Override
                public Integer getCode() { return 401; }
                @Override
                public String getMessage() { return "未登录弹窗提示登录"; }
            });
        }
        
        String lockKey = "article:like:lock:" + articleId + ":" + user.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 2, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return ResultResponse.fail(new ReturnCode() {
                @Override
                public Integer getCode() { return 500; }
                @Override
                public String getMessage() { return "请勿重复提交"; }
            });
        }
        
        articleLikeService.like(articleId, user.getId());
        return ResultResponse.success(new ReturnCode() {
            @Override
            public Integer getCode() { return 200; }
            @Override
            public String getMessage() { return "点赞成功"; }
        });
    }

    @PostMapping("/unlike")
    public ResultResponse<Void> unlike(Long articleId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResultResponse.fail(new ReturnCode() {
                @Override
                public Integer getCode() { return 401; }
                @Override
                public String getMessage() { return "未登录弹窗提示登录"; }
            });
        }
        
        String lockKey = "article:unlike:lock:" + articleId + ":" + user.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 2, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return ResultResponse.fail(new ReturnCode() {
                @Override
                public Integer getCode() { return 500; }
                @Override
                public String getMessage() { return "请勿重复提交"; }
            });
        }
        
        articleLikeService.unlike(articleId, user.getId());

        return ResultResponse.success(new ReturnCode() {
            @Override
            public Integer getCode() { return 200; }
            @Override
            public String getMessage() { return "取消点赞成功"; }
        });
    }

    @GetMapping("/status")
    public ResultResponse<JSONObject> status(Long articleId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Long userId = user != null ? user.getId() : null;

        Long count = articleLikeService.getLikeCount(articleId);
        boolean liked = false;
        if (userId != null) {
            liked = articleLikeService.isLiked(articleId, userId);
        }

        JSONObject json = new JSONObject();
        json.put("count", count);
        json.put("liked", liked);

        return ResultResponse.success(new ReturnCode() {
            @Override
            public Integer getCode() { return 200; }
            @Override
            public String getMessage() { return "获取成功"; }
        }, json);
    }
}
