package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.Dialect;
import com.quantchi.sqlinject.annotation.FilterMode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class SqlParseInjector implements ValueSqlInjector {

    private final String table;

    private final String filter;

    private final String field;

    private final boolean not;

    private final FilterMode mode;

    private final Object[] values;

    private final Dialect dialect;

    public SqlParseInjector(String table, String field, boolean not, FilterMode mode, String[] filters, Dialect dialect) {
        this.table = StringUtils.isEmpty(table)?null:table;
        this.field = field;
        this.not = not;
        this.mode = mode;
        this.values = filters == null? new Object[0]: Stream.of(filters).toArray();
        this.filter = null;
        this.dialect = dialect;
    }

    public SqlParseInjector(String table, String field, boolean not, FilterMode mode, String[] filters) {
        this(table, field, not, mode, filters, Dialect.MYSQL);
    }

    public SqlParseInjector(String filter, Dialect dialect) {
        this.filter = filter == null? "": filter;
        this.table = null;
        this.field = null;
        this.not = false;
        this.mode = FilterMode.CUSTOM;
        this.values = new String[0];
        this.dialect = dialect;
    }

    String evalFilter() {
        if (this.filter != null) {
            return this.filter;
        }
        StringBuilder filterBuilder = new StringBuilder();
        switch (mode) {
            case EQUAL:
                if (values().length != 1) {
                    throw new IllegalStateException("EQUAL模式仅能使用一个参数");
                }
                filterBuilder.append(wrapField());
                Object value1 = values()[0];
                if ("null".equalsIgnoreCase(Optional.ofNullable(value1).map(Object::toString).map(String::trim).orElse(""))) {
                    filterBuilder.append(not?" IS NOT NULL ": " IS NULL");
                    break;
                }
                filterBuilder.append(not?" !":" ").append("= ");
                filterBuilder.append(checkAndWrapValue(value1));
                break;
            case LIKE:
                if (values().length != 1) {
                    throw new IllegalStateException("LIKE模式仅能使用一个参数");
                }
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" LIKE");
                Object value2 = values()[0];
                checkAndWrapValue(value2);
                filterBuilder.append("'").append(value2).append("'");
                break;
            case IN:
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" IN (");
                if (values().length == 1) {
                    Object value3 = values()[0];
                    if (value3 instanceof Collection) {
                        for (Object v : (Collection<?>)value3) {
                            filterBuilder.append(checkAndWrapValue(v)).append(",");
                        }
                    } else {
                        filterBuilder.append(checkAndWrapValue(value3)).append(",");
                    }
                } else if (values().length > 1) {
                    for (Object value : values()) {
                        filterBuilder.append(checkAndWrapValue(value)).append(',');
                    }
                } else {
                    throw new IllegalStateException("IN模式过滤必须存在参数");
                }
                filterBuilder.setCharAt(filterBuilder.length()-1, ')');
                break;
            case BETWEEN:
                filterBuilder.append(wrapField()).append(not?" NOT":" ").append(" BETWEEN ");
                if (values().length != 2) {
                    throw new IllegalStateException("BETWEEN模式只支持两个参数");
                }
                Object value4 = values()[0];
                filterBuilder.append(checkAndWrapValue(value4)).append(" AND ");
                value4 = values()[1];
                filterBuilder.append(checkAndWrapValue(value4));
                break;
            case EXISTS:
                filterBuilder.append(not?" NOT ":"").append(" EXISTS (");
                if (values().length!= 1) {
                    throw new IllegalStateException("EXISTS模式仅能使用一个参数");
                }
                Object value5 = values()[0];
                checkAndWrapValue(value5);
                filterBuilder.append(value5).append(")");
                break;
            case CUSTOM:
                if (values().length!= 1) {
                    throw new IllegalStateException("CUSTOM模式仅能使用一个参数");
                }
                Object value6 = values()[0];
                checkAndWrapValue(value6);
                filterBuilder.append(value6);
        }
        return filterBuilder.toString();
    }

    private String wrapField() {
        return "/**PREFIX**/" + this.field;
    }

    private String checkAndWrapValue(Object value) {
        if (value == null) return  "NULL";
        if (value instanceof Number) {
            return "" + value;
        } else {
            return "'" + value +"'";
        }
    }

    private String parseSql(String sql) {
        String filter = evalFilter();
        if (StringUtils.isEmpty(filter)) {
            return sql;
        }
        return dialect.getSqlRewriter().injectFilter(sql, table, filter);
    }

    @Override
    public String inject(String sql) {
        return parseSql(sql);
    }

    public String getTable() {
        return table;
    }

    public String getField() {
        return field;
    }

    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public Object[] values() {
        return this.values;
    }
}
