package cc.langhai.utils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 通用分页工具类
 *
 * @author langhai
 */
public class PageUtil {

    /**
     * 开启分页（包含参数校验）
     *
     * @param page 页码
     * @param size 条数
     */
    public static void startPage(Integer page, Integer size) {
        startPage(page, size, null);
    }

    /**
     * 开启分页（包含参数校验、排序）
     *
     * @param page    页码
     * @param size    条数
     * @param orderBy 排序规则
     */
    public static void startPage(Integer page, Integer size, String orderBy) {
        page = getValidPage(page);
        size = getValidSize(size);
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            PageHelper.startPage(page, size, orderBy);
        } else {
            PageHelper.startPage(page, size);
        }
    }

    /**
     * 获取分页对象
     *
     * @param list 集合
     * @param <T>  泛型
     * @return 分页对象
     */
    public static <T> PageInfo<T> getPageInfo(List<T> list) {
        return new PageInfo<>(list);
    }

    /**
     * 获取有效的页码
     *
     * @param page 页码
     * @return 有效的页码
     */
    public static int getValidPage(Integer page) {
        return (page == null || page <= 0) ? 1 : page;
    }

    /**
     * 获取有效的条数
     *
     * @param size 条数
     * @return 有效的条数
     */
    public static int getValidSize(Integer size) {
        return (size == null || size <= 0) ? 10 : size;
    }
}
