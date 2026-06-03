package cc.langhai.service;

import cc.langhai.domain.Article;
import cc.langhai.utils.PageUtil;
import com.github.pagehelper.PageInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 文章分页服务测试类
 *
 * @author langhai
 * @date 2026-06-03
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ArticleServicePageTest {

    @Autowired
    private ArticleService articleService;

    /**
     * 测试 PageUtil 分页工具类的基本功能
     */
    @Test
    public void testPageUtilBasicFunction() {
        // 模拟数据查询
        Supplier<List<String>> supplier = () -> {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                list.add("Test Data " + i);
            }
            return list;
        };

        // 测试正常分页
        PageInfo<String> pageInfo = PageUtil.page(1, 10, supplier);
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(1, pageInfo.getPageNum());
        Assert.assertEquals(10, pageInfo.getPageSize());
        Assert.assertEquals(50, pageInfo.getTotal());
        Assert.assertEquals(5, pageInfo.getPages());
    }

    /**
     * 测试 PageUtil 分页工具类的参数校验功能
     */
    @Test
    public void testPageUtilParameterValidation() {
        Supplier<List<String>> supplier = () -> {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                list.add("Test Data " + i);
            }
            return list;
        };

        // 测试 null 参数
        PageInfo<String> pageInfo1 = PageUtil.page(null, null, supplier);
        Assert.assertNotNull(pageInfo1);
        Assert.assertEquals(1, pageInfo1.getPageNum());
        Assert.assertEquals(10, pageInfo1.getPageSize());

        // 测试负数参数
        PageInfo<String> pageInfo2 = PageUtil.page(-1, -5, supplier);
        Assert.assertNotNull(pageInfo2);
        Assert.assertEquals(1, pageInfo2.getPageNum());
        Assert.assertEquals(10, pageInfo2.getPageSize());
    }

    /**
     * 测试 ArticleService 的 search 分页方法
     */
    @Test
    public void testArticleServiceSearch() {
        // 调用 search 方法
        PageInfo<Article> pageInfo = articleService.search(1, 10, null, null);

        // 验证结果
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(1, pageInfo.getPageNum());
        Assert.assertEquals(10, pageInfo.getPageSize());
        Assert.assertNotNull(pageInfo.getList());
    }

    /**
     * 测试 ArticleService 的 getAllArticlePage 分页方法
     */
    @Test
    public void testArticleServiceGetAllArticlePage() {
        // 调用 getAllArticlePage 方法
        PageInfo<Article> pageInfo = articleService.getAllArticlePage(1, 10, null, null, "system");

        // 验证结果
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(1, pageInfo.getPageNum());
        Assert.assertEquals(10, pageInfo.getPageSize());
        Assert.assertNotNull(pageInfo.getList());
    }
}
