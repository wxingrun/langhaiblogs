package cc.langhai.response;

/**
 * 文章点赞相关枚举类
 *
 * @author langhai
 * @date 2024-01-01
 */
public enum ArticleLikeReturnCode implements ReturnCode {
    ARTICLE_LIKE_OK_00001(200, "文章点赞成功。"),
    ARTICLE_LIKE_FAIL_00002(500, "文章点赞失败。"),
    ARTICLE_LIKE_ALREADY_00003(500, "您已经点赞过该文章。"),
    ARTICLE_UNLIKE_OK_00004(200, "取消点赞成功。"),
    ARTICLE_UNLIKE_FAIL_00005(500, "取消点赞失败。"),
    ARTICLE_LIKE_NOT_LOGIN_00006(500, "请先登录后再点赞。"),
    ;

    private Integer code;
    private String message;

    ArticleLikeReturnCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}