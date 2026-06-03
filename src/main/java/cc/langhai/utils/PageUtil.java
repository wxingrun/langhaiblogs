package cc.langhai.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;

public final class PageUtil {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageUtil() {
    }

    public static void startPage(Integer pageNum, Integer pageSize) {
        startPage(pageNum, pageSize, null);
    }

    public static void startPage(Integer pageNum, Integer pageSize, String orderBy) {
        PageHelper.startPage(resolvePageNum(pageNum), resolvePageSize(pageSize));
        if (StrUtil.isNotBlank(orderBy)) {
            PageHelper.orderBy(orderBy);
        }
    }

    public static <T> PageInfo<T> buildPageInfo(List<T> records) {
        return new PageInfo<>(records);
    }

    public static int resolvePageNum(Integer pageNum) {
        if (ObjectUtil.isNull(pageNum) || pageNum < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    public static int resolvePageSize(Integer pageSize) {
        if (ObjectUtil.isNull(pageSize) || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    public static int resolveOffset(Integer pageNum, Integer pageSize) {
        return (resolvePageNum(pageNum) - 1) * resolvePageSize(pageSize);
    }

    public static long resolvePages(long total, Integer pageSize) {
        int validPageSize = resolvePageSize(pageSize);
        return (total + validPageSize - 1) / validPageSize;
    }

    public static boolean matchTopArticleCondition(Integer pageNum, String searchArticleStr, Long labelId) {
        return resolvePageNum(pageNum) == DEFAULT_PAGE_NUM
                && StrUtil.isBlank(searchArticleStr)
                && (ObjectUtil.isNull(labelId) || Long.valueOf(0L).equals(labelId));
    }

    public static <T> List<T> limit(List<T> records, int maxSize) {
        if (ObjectUtil.isNull(records) || records.size() <= maxSize) {
            return records;
        }
        return records.subList(0, maxSize);
    }
}
