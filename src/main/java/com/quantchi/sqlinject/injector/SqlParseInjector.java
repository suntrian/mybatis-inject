package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.annotation.SqlParseInject;
import com.quantchi.sqlinject.exception.EmptyValueException;
import com.quantchi.sqlinject.mysql.MysqlInject;
import org.springframework.util.StringUtils;

import java.util.Collection;

public class SqlParseInjector implements SqlInjector {

    private final String table;

    private final String filter;

    private final String field;

    private final boolean not;

    private final SqlParseInject.MODE mode;

    private final String[] values;

    private final SpringELHandler springELHandler;

    private final MysqlInject inject = new MysqlInject();

    public SqlParseInjector(SpringELHandler springELHandler, String table, String field, boolean not, SqlParseInject.MODE mode, String[] filters) {
        this.springELHandler = springELHandler;
        this.table = table;
        this.field = field;
        this.not = not;
        this.mode = mode;
        this.values = filters == null? new String[0]: filters;
        this.filter = null;
    }

    public SqlParseInjector(String filter) {
        this.springELHandler = null;
        this.filter = filter == null? "": filter;
        this.table = null;
        this.field = null;
        this.not = false;
        this.mode = SqlParseInject.MODE.CUSTOM;
        this.values = new String[0];
    }

    private String evalFilter() {
        if (this.filter != null) {
            return this.filter;
        }
        if (springELHandler == null) {
            throw new IllegalStateException("不可能发生的");
        }
        StringBuilder filterBuilder = new StringBuilder();
        switch (mode) {
            case EQUAL:
                if (values.length > 1) {
                    throw new IllegalStateException("EQUAL模式不支持多个参数");
                }
                filterBuilder.append(wrapField()).append(not?" !":" ").append("= ");
                Object value1 = springELHandler.handle(values[0]);
                filterBuilder.append(checkAndWrapValue(value1));
                break;
            case LIKE:
                if (values.length > 1) {
                    throw new IllegalStateException("EQUAL模式不支持多个参数");
                }
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" LIKE");
                Object value2 = springELHandler.handle(values[0]);
                checkAndWrapValue(value2);
                filterBuilder.append("'").append(value2).append("'");
                break;
            case IN:
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" IN (");
                if (values.length == 1) {
                    Object value3 = springELHandler.handle(values[0]);
                    if (value3 instanceof Collection) {
                        for (Object v : (Collection<?>)value3) {
                            filterBuilder.append(checkAndWrapValue(v)).append(",");
                        }
                        filterBuilder.setCharAt(filterBuilder.length()-1, ')');
                    } else {
                        checkAndWrapValue(value3);
                        filterBuilder.append(value3).append(")");
                    }
                } else if (values.length > 1) {
                    for (String value : values) {
                        Object value3 = springELHandler.handle(value);
                        filterBuilder.append(checkAndWrapValue(value3)).append(',');
                    }
                    filterBuilder.setCharAt(filterBuilder.length()-1, ')');
                } else {
                    throw new IllegalStateException("IN模式过滤必须存在参数");
                }
                break;
            case BETWEEN:
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" BETWEEN ");
                if (values.length != 2) {
                    throw new IllegalStateException("BETWEEN模式只支持两个参数");
                }
                Object value4 = springELHandler.handle(values[0]);
                filterBuilder.append(checkAndWrapValue(value4)).append(" AND ");
                value4 = springELHandler.handle(values[1]);
                filterBuilder.append(checkAndWrapValue(value4));
                break;
            case EXISTS:
                filterBuilder.append(not?" NOT ":"").append(" EXISTS (");
                if (values.length!= 1) {
                    throw new IllegalStateException("EXISTS模式过滤必须存在一个参数");
                }
                Object value5 = springELHandler.handle(values[0]);
                checkAndWrapValue(value5);
                filterBuilder.append(value5).append(")");
                break;
            case CUSTOM:
                if (values.length!= 1) {
                    throw new IllegalStateException("CUSTOM模式过滤必须存在一个参数");
                }
                Object value6 = springELHandler.handle(values[0]);
                checkAndWrapValue(value6);
                filterBuilder.append(value6);
        }
        return filterBuilder.toString();
    }

    private String wrapField() {
        return "/**PREFIX**/" + this.field;
    }

    private String checkAndWrapValue(Object value) {
        if (value == null) throw new NullPointerException("注入参数计算值为空");
        if (value instanceof Number) {
            return "" + value;
        } else {
            return "'" + value +"'";
        }
    }

    private String parseSql(String sql) {
        String filter = null;
        try {
            filter = evalFilter();
            if (StringUtils.isEmpty(filter)) {
                return sql;
            }
        } catch (Exception e) {
            if (e instanceof EmptyValueException) {
                if (springELHandler != null && FailoverStrategy.REJECT.equals(springELHandler.getFailoverStrategy())) {
                    return inject.injectFilter(sql, null, "1 = 2");
                }
            }
            throw e;
        }
        return inject.injectFilter(sql, table, filter);
    }

    public String inject(String sql) {
        return parseSql(sql);
    }
}
