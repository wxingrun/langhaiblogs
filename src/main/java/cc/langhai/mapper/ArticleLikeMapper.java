package cc.langhai.mapper;

import cc.langhai.domain.ArticleLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    @Select("select id, article_id as articleId, user_id as userId, create_time as createTime from article_like where article_id = #{articleId} and user_id = #{userId} limit 1")
    ArticleLike getByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("select count(1) from article_like where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Long articleId);
}
