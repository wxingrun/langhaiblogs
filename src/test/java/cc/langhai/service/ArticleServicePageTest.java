package cc.langhai.service;

import cc.langhai.utils.PageUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * PageUtil 分页工具类测试用例
 *
 * @author langhai
 * @date 2023-06-03
 */
public class ArticleServicePageTest {

    /**
     * 测试正常分页参数
     * 验证：合法的 page 和 size 参数不被修改
     */
    @Test
    public void testValidateAndFixWithValidParams() {
        Integer[] result = PageUtil.validateAndFix(2, 20);
        
        Assert.assertEquals("页码应保持为 2", Integer.valueOf(2), result[0]);
        Assert.assertEquals("每页条数应保持为 20", Integer.valueOf(20), result[1]);
    }

    /**
     * 测试分页参数为 null 的情况
     * 验证：null 参数被修正为默认值 page=1, size=10
     */
    @Test
    public void testValidateAndFixWithNullParams() {
        Integer[] result = PageUtil.validateAndFix(null, null);
        
        Assert.assertEquals("null 页码应修正为 1", Integer.valueOf(1), result[0]);
        Assert.assertEquals("null 每页条数应修正为 10", Integer.valueOf(10), result[1]);
    }

    /**
     * 测试分页参数为负数或超过最大值的情况
     * 验证：负数页码修正为 1，超过 100 的 size 修正为 100
     */
    @Test
    public void testValidateAndFixWithInvalidParams() {
        Integer[] result1 = PageUtil.validateAndFix(-1, -5);
        Assert.assertEquals("负数页码应修正为 1", Integer.valueOf(1), result1[0]);
        Assert.assertEquals("负数每页条数应修正为 10", Integer.valueOf(10), result1[1]);

        Integer[] result2 = PageUtil.validateAndFix(1, 200);
        Assert.assertEquals("超过 100 的每页条数应修正为 100", Integer.valueOf(100), result2[1]);
    }
}
