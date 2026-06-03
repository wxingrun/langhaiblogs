package cc.langhai.service;

import cc.langhai.domain.Article;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章分页查询测试
 *
 * @author langhai
 * @date 2026-06-03
 */
@SpringBootTest
public class ArticleServicePageTest {

    @Autowired
    private ArticleService articleService;

    /**
     * 测试 search 方法正常分页查询
     */
    @Test
    public void testSearchNormalPaging() {
        PageInfo<Article> result = articleService.search(1, 10, null, null);
        assertNotNull(result, "分页结果不应为空");
        assertTrue(result.getPageNum() >= 1, "返回页码应 >= 1");
        assertTrue(result.getSize() <= 10, "每页条数应 <= 10");
    }

    /**
     * 测试 search 方法参数为空时使用默认值
     */
    @Test
    public void testSearchNullParamDefaults() {
        PageInfo<Article> result = articleService.search(null, null, null, null);
        assertNotNull(result, "分页结果不应为空");
        assertEquals(1, result.getPageNum(), "默认页码应为 1");
        assertEquals(10, result.getSize(), "默认每页条数应为 10");
    }

    /**
     * 测试 getUserArticlePage 方法分页查询
     */
    @Test
    public void testGetUserArticlePageNormalPaging() {
        PageInfo<Article> result = articleService.getUserArticlePage(1, 5, null, null);
        assertNotNull(result, "分页结果不应为空");
        assertTrue(result.getPageNum() >= 1, "返回页码应 >= 1");
        assertTrue(result.getSize() <= 5, "每页条数应 <= 5");
    }
}