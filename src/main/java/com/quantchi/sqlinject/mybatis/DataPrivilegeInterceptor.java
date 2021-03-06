package com.quantchi.sqlinject.mybatis;

import com.quantchi.sqlinject.SqlInjectOnce;
import com.quantchi.sqlinject.annotation.*;
import com.quantchi.sqlinject.exception.EmptyValueException;
import com.quantchi.sqlinject.injector.*;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Intercepts(
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
)
public class DataPrivilegeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataPrivilegeInterceptor.class);

    private boolean interceptPageHelperCountMethod = true;

    private String pageHelperCountSuffix = "_COUNT";

    private static final Map<String, List<SqlInjector>> needInjectMap = new ConcurrentHashMap<>();

    private static final Map<Dialect, SqlParseInjector> REJECT_INJECTOR = new ConcurrentHashMap<>();

    private final SpringELHandler springELHandler;

    public DataPrivilegeInterceptor(SpringELHandler springELHandler) {
        this.springELHandler = springELHandler;
    }


    public Object intercept(Invocation invocation) throws Throwable {
        try {
            return innerIntercept(invocation);
        } finally {
            SqlInjectOnce.clear();
        }
    }

    public Object innerIntercept(Invocation invocation) throws Throwable {
        if (Proxy.isProxyClass(invocation.getTarget().getClass())) {
            return invocation.proceed();
        }
        if (!SqlInjectOnce.enabled()) {
            return invocation.proceed();
        }

        final StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        final MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        final MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
//        if ( SqlCommandType.SELECT != mappedStatement.getSqlCommandType()){
//            return invocation.proceed();
//        }
        final String namespace = mappedStatement.getId();
        boolean isPageHelperCountMethod = interceptPageHelperCountMethod && namespace.endsWith(pageHelperCountSuffix);
        if (isPageHelperCountMethod) {
            SqlInjectOnce.stay();
        }
        if (!needInjectMap.containsKey(namespace)) {
            synchronized (namespace.intern()) {
                if (!needInjectMap.containsKey(namespace)) {
                    String className = namespace.substring(0, namespace.lastIndexOf('.'));
                    String methodName = namespace.substring(className.length()+1);
                    if (isPageHelperCountMethod) {
                        methodName = methodName.substring(0, methodName.length()-pageHelperCountSuffix.length());
                    }
                    List<SqlInjector> sqlInjectors = extractSqlInjectors(className, methodName);
                    needInjectMap.put(namespace, sqlInjectors);
                }
            }
        }
        List<SqlInjector> sqlInjectors = needInjectMap.get(namespace);
        if (SqlInjectOnce.temporaryInject() != null) {
            sqlInjectors = new ArrayList<>(sqlInjectors);
            sqlInjectors.add(SqlInjectOnce.temporaryInject());
        }

        if (sqlInjectors.isEmpty()) {
            return invocation.proceed();
        } else {
            final BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
            String sql = boundSql.getSql();
            for (SqlInjector sqlInjector : sqlInjectors) {
                try {
                    sql = sqlInjector.inject(sql);
                } catch (EmptyValueException e) {
                    TreatBlankStrategy treatBlankStrategy = springELHandler.getSqlInjectProperties().getTreatBlankStrategy();
                    switch (treatBlankStrategy) {
                        case THROW:
                            throw e;
                        case REJECT:
                            sql = getRejectSqlInjector(guessDialect(sqlInjectors)).inject(sql);
                            break;
                        case IGNORE:
                            //?????????SQL???do nothing
                        case ACCEPT:
                        default:
                            // do nothing
                    }
                } catch (Exception e) {
                    FailoverStrategy failoverStrategy = springELHandler.getSqlInjectProperties().getFailoverStrategy();
                    if (FailoverStrategy.THROW == failoverStrategy) {
                        throw e;
                    } else if (FailoverStrategy.REJECT == failoverStrategy) {
                        sql = getRejectSqlInjector(guessDialect(sqlInjectors)).inject(sql);
                        break;
                    }
                }
            }
            metaObject.setValue("delegate.boundSql.sql", sql);
        }
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    public void setProperties(Properties properties) {

    }

    public void setInterceptPageHelperCountMethod(boolean interceptPageHelperCountMethod) {
        this.interceptPageHelperCountMethod = interceptPageHelperCountMethod;
    }

    public void setPageHelperCountSuffix(String pageHelperCountSuffix) {
        this.pageHelperCountSuffix = pageHelperCountSuffix;
    }

    private Dialect guessDialect(List<SqlInjector> sqlInjectors) {
        return sqlInjectors.stream()
                .map(x-> (x instanceof SpringELValueInjector)? ((SpringELValueInjector) x).getSqlInjector(): x)
                .filter(x->(x instanceof SqlParseInjector))
                .map(x->((SqlParseInjector) x).getDialect())
                .findFirst()
                .orElse(springELHandler.getSqlInjectProperties().getDialect());
    }

    private SqlParseInjector getRejectSqlInjector(Dialect dialect) {
        SqlParseInjector result;
        if ((result = REJECT_INJECTOR.get(dialect)) == null) {
            synchronized (REJECT_INJECTOR) {
                if ( (result = REJECT_INJECTOR.get(dialect)) == null) {
                    result = new SqlParseInjector(" 1 = 2 ", dialect);
                    REJECT_INJECTOR.put(dialect, result);
                }
            }
        }
        return result;
    }

    private List<SqlInjector> extractSqlInjectors(String className, String methodName) throws ClassNotFoundException {
        for (Method method : Class.forName(className).getMethods()) {
            if (methodName.equals(method.getName())) {
                List<SqlInjector> injectors = new ArrayList<>(6);

                PlaceholderInject placeholderInject = method.getAnnotation(PlaceholderInject.class);
                if (placeholderInject != null) {
                    PlaceholderInjector placeholderInjector = new PlaceholderInjector(placeholderInject.placeholder(), placeholderInject.replacement());
                    SqlInjector springElInjector = new SpringELValueInjector(this.springELHandler, placeholderInjector);
                    injectors.add(springElInjector);
                }

                SqlParseInject sqlParseInject = method.getAnnotation(SqlParseInject.class);
                if (sqlParseInject != null) {
                    SqlParseInjector sqlParseInjector =
                            new SqlParseInjector(sqlParseInject.table(), sqlParseInject.field(), sqlParseInject.not(), sqlParseInject.mode(), sqlParseInject.filter(), sqlParseInject.dialect());
                    SpringELValueInjector springELValueInjector = new SpringELValueInjector(this.springELHandler, sqlParseInjector);
                    injectors.add(springELValueInjector);
                }

                SqlInject sqlInject = method.getAnnotation(SqlInject.class);
                if (sqlInject != null) {
                    SqlInjector sqlParseInjector = new SqlParseInjector(sqlInject.filter(), sqlInject.dialect());
                    injectors.add(sqlParseInjector);
                }

                SqlInjectGroup sqlInjectGroup = method.getAnnotation(SqlInjectGroup.class);
                if (sqlInjectGroup != null) {
                    Logic logic = sqlInjectGroup.logic();
                    Set<Dialect> dialects = new HashSet<>(4, 1.00f);
                    List<SqlInjector> sqlParseInjectors = Stream.concat(Stream.of(sqlInjectGroup.value()), Stream.of(sqlInjectGroup.parseInject()))
                            .map(it->{
                                dialects.add(it.dialect());
                                SqlParseInjector sqlParseInjector = new SqlParseInjector(it.table(), it.field(), it.not(), it.mode(), it.filter(), it.dialect());
                                return new SpringELValueInjector(this.springELHandler, sqlParseInjector);
                            })
                            .collect(Collectors.toList());
                    if (dialects.size() >1 ) {
                        throw new IllegalStateException("?????????????????????????????????SQL??????");
                    }
                    List<SqlInjector> placeholderInjectors = Stream.of(sqlInjectGroup.placeholderInject())
                            .map(it-> {
                                PlaceholderInjector placeholderInjector = new PlaceholderInjector(it.placeholder(), it.replacement());
                                return new SpringELValueInjector(this.springELHandler, placeholderInjector);
                            })
                            .collect(Collectors.toList());
                    SqlInjectorGroup sqlInjectorGroup = new SqlInjectorGroup(logic, sqlParseInjectors, placeholderInjectors);
                    injectors.add(sqlInjectorGroup);
                }

                return injectors;
            }
        }
        return new ArrayList<>(1);
    }
}
