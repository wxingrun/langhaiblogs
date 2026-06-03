package cc.langhai.utils;

import cn.hutool.core.util.ObjectUtil;
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

    /**
     * 执行分页查询
     *
     * @param page 页码
     * @param size 每页大小
     * @param querySupplier 查询方法
     * @param <T> 实体类型
     * @return 分页信息
     */
    public static <T> PageInfo<T> page(Integer page, Integer size, Supplier<List<T>> querySupplier) {
        // 参数校验
        page = ObjectUtil.defaultIfNull(page, 1);
        size = ObjectUtil.defaultIfNull(size, 10);
        
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        
        // 开启分页
        PageHelper.startPage(page, size);
        
        // 执行查询
        List<T> list = querySupplier.get();
        
        // 返回分页信息
        return new PageInfo<>(list);
    }

    /**
     * 执行分页查询，带默认参数
     *
     * @param page 页码
     * @param size 每页大小
     * @param defaultPage 默认页码
     * @param defaultSize 默认每页大小
     * @param querySupplier 查询方法
     * @param <T> 实体类型
     * @return 分页信息
     */
    public static <T> PageInfo<T> page(Integer page, Integer size, 
                                         Integer defaultPage, Integer defaultSize, 
                                         Supplier<List<T>> querySupplier) {
        // 参数校验
        page = ObjectUtil.defaultIfNull(page, defaultPage);
        size = ObjectUtil.defaultIfNull(size, defaultSize);
        
        if (page < 1) {
            page = defaultPage;
        }
        if (size < 1) {
            size = defaultSize;
        }
        
        // 开启分页
        PageHelper.startPage(page, size);
        
        // 执行查询
        List<T> list = querySupplier.get();
        
        // 返回分页信息
        return new PageInfo<>(list);
    }
}
