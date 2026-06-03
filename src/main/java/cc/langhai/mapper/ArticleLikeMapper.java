package cc.langhai.mapper;

import cc.langhai.domain.ArticleLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞Mapper接口
 *
 * @author langhai
 * @date 2024-01-01
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
}