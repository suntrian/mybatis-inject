package com.quantchi.sqlinject.parser.common;

import com.quantchi.sqlinject.annotation.Logic;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface SqlRewriter {

    /**
     *
     * @param sql 原SQL
     * @param logic 同表下多个filter过滤条件的组合逻辑
     * @param tableFilters key: tableName(tableAlias), value: key表下的多个过滤条件
     * @return 过滤条件注入后的SQL
     */
    String injectFilters(String sql, Logic logic, Map<String, List<String>> tableFilters);

    default String injectFilter(String sql, @Nullable String targetTable, String filter) {
        return injectFilters(sql, Logic.AND, Collections.singletonMap(targetTable, Collections.singletonList(filter)));
    }

}
