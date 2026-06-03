package cc.langhai.response;

public enum ArticleLikeReturnCode implements ReturnCode {

    ARTICLE_LIKE_OK_00001(200, "点赞成功。"),

    ARTICLE_LIKE_CANCEL_OK_00002(200, "取消点赞成功。"),

    ARTICLE_LIKE_REPEAT_FAIL_00003(500, "已经点赞过了。"),

    ARTICLE_LIKE_NOT_LOGIN_FAIL_00004(500, "请先登录再进行点赞。"),

    ARTICLE_LIKE_COUNT_OK_00005(200, "点赞数量查询成功。"),
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