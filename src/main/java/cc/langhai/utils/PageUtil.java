package cc.langhai.utils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Supplier;

/**
 * 通用分页工具类
 *
 * @author langhai
 * @date 2026-06-03
 */
public class PageUtil {

    public static final int DEFAULT_PAGE_NUM = 1;

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 执行分页查询
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param supplier 数据查询逻辑
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageInfo<T> page(Integer pageNum, Integer pageSize, Supplier<List<T>> supplier) {
        int page = resolvePageNum(pageNum);
        int size = resolvePageSize(pageSize);
        PageHelper.startPage(page, size);
        List<T> list = supplier.get();
        return new PageInfo<>(list);
    }

    /**
     * 执行分页查询（带排序）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param orderBy  排序条件
     * @param supplier 数据查询逻辑
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageInfo<T> page(Integer pageNum, Integer pageSize, String orderBy,
                                        Supplier<List<T>> supplier) {
        int page = resolvePageNum(pageNum);
        int size = resolvePageSize(pageSize);
        PageHelper.startPage(page, size, orderBy);
        List<T> list = supplier.get();
        return new PageInfo<>(list);
    }

    private static int resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private static int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return pageSize;
    }
}