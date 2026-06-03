package cc.langhai.domain;

import lombok.Data;
import java.util.Date;

/**
 * 文章点赞实体类
 *
 * @author langhai
 */
@Data
public class ArticleLike {

    private Long id;

    private Long articleId;

    private Long userId;

    private Date createTime;

}
