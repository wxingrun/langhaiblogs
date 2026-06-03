package cc.langhai.mapper;

import cc.langhai.domain.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;

/**
 * 文章点赞 Mapper 接口
 */
@Mapper
public interface ArticleLikeMapper {

    @Insert("insert into article_like (article_id, user_id, create_time) values (#{articleId}, #{userId}, #{createTime})")
    int insertArticleLike(ArticleLike articleLike);

    @Delete("delete from article_like where article_id = #{articleId} and user_id = #{userId}")
    int deleteArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("select count(*) from article_like where article_id = #{articleId}")
    Long countByArticleId(@Param("articleId") Long articleId);

    @Select("select count(*) from article_like where article_id = #{articleId} and user_id = #{userId}")
    int countByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
