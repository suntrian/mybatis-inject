package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.mysql.MysqlInject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SqlInjectorGroup implements SqlInjector {

    private final Logic logic;

    private final List<SqlInjector> sqlParseInjectors;

    private final List<SqlInjector> placeholderInjectors;

    private final MysqlInject inject = new MysqlInject();

    public SqlInjectorGroup(Logic logic, List<SqlInjector> sqlParseInjectors, List<SqlInjector> placeholderInjectors) {
        this.logic = logic;
        this.sqlParseInjectors = sqlParseInjectors;
        this.placeholderInjectors = placeholderInjectors;
    }

    @Override
    public String inject(String sql) {
        for (SqlInjector placeholderInjector : this.placeholderInjectors) {
            sql = placeholderInjector.inject(sql);
        }
        Map<String, List<String>> tableFilters = new HashMap<>();
        for (SqlInjector sqlInjector : sqlParseInjectors) {
            SqlParseInjector sqlParseInjector;
            if (sqlInjector instanceof SpringELValueInjector) {
                ((SpringELValueInjector) sqlInjector).evalValue();
                sqlParseInjector = (SqlParseInjector) ((SpringELValueInjector) sqlInjector).getSqlInjector();
            } else if (sqlInjector instanceof SqlParseInjector) {
                sqlParseInjector = (SqlParseInjector) sqlInjector;
            } else {
                throw new RuntimeException("will never happen");
            }
            String filter = sqlParseInjector.evalFilter();
            if (!tableFilters.containsKey(sqlParseInjector.getTable())){
                tableFilters.put(sqlParseInjector.getTable(), new LinkedList<>());
            }
            tableFilters.get(sqlParseInjector.getTable()).add(filter);
        }
        return inject.injectFilters(sql, logic, tableFilters);
    }
}
