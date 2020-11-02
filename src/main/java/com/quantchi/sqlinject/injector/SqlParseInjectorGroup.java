package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.mysql.MysqlInject;

import java.util.*;

public class SqlParseInjectorGroup implements SqlInjector {

    private final Logic logic;

    private final List<SqlParseInjector> sqlParseInjectors;

    private final MysqlInject inject = new MysqlInject();

    public SqlParseInjectorGroup(Logic logic, List<SqlParseInjector> sqlParseInjectors) {
        this.logic = logic;
        this.sqlParseInjectors = sqlParseInjectors;
    }

    @Override
    public String inject(String sql) {
        Map<String, List<String>> tableFilters = new HashMap<>();
        for (SqlParseInjector sqlParseInjector : sqlParseInjectors) {
            String filter = sqlParseInjector.evalFilter();
            if (!tableFilters.containsKey(sqlParseInjector.getTable())){
                tableFilters.put(sqlParseInjector.getTable(), new LinkedList<>());
            }
            tableFilters.get(sqlParseInjector.getTable()).add(filter);
        }
        return inject.injectFilters(sql, logic, tableFilters);
    }
}
