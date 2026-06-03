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

import java.util.Arrays;
import java.util.List;

/**
 * ArticleService 分页重构测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ArticleServicePageTest {

    @Autowired
    private ArticleService articleService;

    @Test
    public void testGetAllArticlePage() {
        // 测试新增的分页方法 getAllArticle
        PageInfo<Article> pageInfo = articleService.getAllArticle(1, 5, null, null, "system");
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(1, pageInfo.getPageNum());
        Assert.assertEquals(5, pageInfo.getPageSize());
    }

    @Test
    public void testSearchPage() {
        // 测试重构后的 search 方法
        PageInfo<Article> pageInfo = articleService.search(1, 3, null, null);
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(1, pageInfo.getPageNum());
        Assert.assertEquals(3, pageInfo.getPageSize());
    }

    @Test
    public void testPageUtil() {
        // 测试 PageUtil 参数校验
        int validPage = PageUtil.getValidPage(-1);
        Assert.assertEquals(1, validPage);
        
        int validSize = PageUtil.getValidSize(0);
        Assert.assertEquals(10, validSize);
        
        List<String> list = Arrays.asList("A", "B", "C");
        PageInfo<String> pageInfo = PageUtil.getPageInfo(list);
        Assert.assertNotNull(pageInfo);
        Assert.assertEquals(3, pageInfo.getList().size());
    }
}
