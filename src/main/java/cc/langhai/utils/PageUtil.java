package cc.langhai.utils;

import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 通用分页工具类
 *
 * @author langhai
 * @date 2023-06-03
 */
public class PageUtil {

    private static final Integer DEFAULT_PAGE = 1;
    private static final Integer DEFAULT_SIZE = 10;
    private static final Integer MIN_PAGE = 1;
    private static final Integer MIN_SIZE = 1;
    private static final Integer MAX_SIZE = 100;

    /**
     * 校验并修正分页参数
     *
     * @param page 页码
     * @param size 每页条数
     * @return 修正后的分页参数数组 [page, size]
     */
    public static Integer[] validateAndFix(Integer page, Integer size) {
        Integer fixedPage = (ObjectUtil.isNull(page) || page < MIN_PAGE) ? DEFAULT_PAGE : page;
        Integer fixedSize = (ObjectUtil.isNull(size) || size < MIN_SIZE) ? DEFAULT_SIZE : size;
        if (fixedSize > MAX_SIZE) {
            fixedSize = MAX_SIZE;
        }
        return new Integer[]{fixedPage, fixedSize};
    }

    /**
     * 开启分页查询
     *
     * @param page 页码
     * @param size 每页条数
     */
    public static void startPage(Integer page, Integer size) {
        Integer[] params = validateAndFix(page, size);
        PageHelper.startPage(params[0], params[1]);
    }

    /**
     * 执行分页查询并返回 PageInfo
     *
     * @param page 页码
     * @param size 每页条数
     * @param dataList 查询结果列表
     * @param <T> 数据类型
     * @return PageInfo 分页对象
     */
    public static <T> PageInfo<T> createPageInfo(Integer page, Integer size, List<T> dataList) {
        return new PageInfo<>(dataList);
    }

    /**
     * 完整的分页查询流程：开启分页 -> 执行查询 -> 返回 PageInfo
     *
     * @param page 页码
     * @param size 每页条数
     * @param queryExecutor 查询执行器（Lambda）
     * @param <T> 数据类型
     * @return PageInfo 分页对象
     */
    public static <T> PageInfo<T> doPageQuery(Integer page, Integer size, QueryExecutor<T> queryExecutor) {
        startPage(page, size);
        List<T> dataList = queryExecutor.execute();
        return createPageInfo(page, size, dataList);
    }

    /**
     * 查询执行器函数式接口
     *
     * @param <T> 数据类型
     */
    @FunctionalInterface
    public interface QueryExecutor<T> {
        List<T> execute();
    }
}
