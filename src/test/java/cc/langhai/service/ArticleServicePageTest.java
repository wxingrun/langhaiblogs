package cc.langhai.service;

import cc.langhai.domain.Article;
import cc.langhai.mapper.ArticleMapper;
import cc.langhai.service.impl.ArticleServiceImpl;
import com.github.pagehelper.PageInfo;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArticleServicePageTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private RestHighLevelClient restHighLevelClient;

    private ArticleServiceImpl articleService;

    @Before
    public void setUp() throws Exception {
        articleService = new ArticleServiceImpl();
        injectField("articleMapper", articleMapper);
        injectField("restHighLevelClient", restHighLevelClient);
    }

    @Test
    public void searchShouldKeepMapperQueryResult() {
        List<Article> articles = Arrays.asList(buildArticle(1L), buildArticle(2L));
        when(articleMapper.getAllArticlePublicShow("spring", 8L)).thenReturn(articles);

        PageInfo<Article> pageInfo = articleService.search(1, 10, "spring", 8L);

        assertNotNull(pageInfo);
        assertEquals(articles, pageInfo.getList());
        assertEquals(2, pageInfo.getList().size());
        verify(articleMapper).getAllArticlePublicShow("spring", 8L);
    }

    @Test
    public void searchESShouldNormalizeInvalidPagingParams() throws IOException {
        SearchResponse response = org.mockito.Mockito.mock(SearchResponse.class);
        SearchHits searchHits = org.mockito.Mockito.mock(SearchHits.class);
        when(response.getHits()).thenReturn(searchHits);
        when(searchHits.getTotalHits()).thenReturn(new TotalHits(21L, TotalHits.Relation.EQUAL_TO));
        when(searchHits.getHits()).thenReturn(new SearchHit[0]);
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        when(restHighLevelClient.search(requestCaptor.capture(), eq(RequestOptions.DEFAULT))).thenReturn(response);

        HashMap<String, Object> result = articleService.searchES(0, 0, "java");

        SearchRequest request = requestCaptor.getValue();
        assertEquals(0, request.source().from());
        assertEquals(10, request.source().size());
        assertTrue(((List<?>) result.get("list")).isEmpty());
        assertEquals(3L, result.get("pages"));
    }

    @Test
    public void topArticleShouldReturnFirstThreeOnlyWhenTopConditionMatches() {
        when(articleMapper.topArticle()).thenReturn(Arrays.asList(buildArticle(1L), buildArticle(2L), buildArticle(3L), buildArticle(4L)));

        List<Article> topArticles = articleService.topArticle(1, "", null);
        List<Article> skippedArticles = articleService.topArticle(2, "", null);

        assertEquals(Arrays.asList(1L, 2L, 3L), topArticles.stream().map(Article::getId).collect(Collectors.toList()));
        assertNull(skippedArticles);
        verify(articleMapper).topArticle();
    }

    private Article buildArticle(Long id) {
        Article article = new Article();
        article.setId(id);
        return article;
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = ArticleServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(articleService, value);
    }
}
