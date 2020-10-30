package com.quantchi.sqlinject.mybatis;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.annotation.PlaceholderInject;
import com.quantchi.sqlinject.annotation.SqlInject;
import com.quantchi.sqlinject.annotation.SqlParseInject;
import com.quantchi.sqlinject.injector.PlaceholderInjector;
import com.quantchi.sqlinject.injector.SpringELHandler;
import com.quantchi.sqlinject.injector.SqlInjector;
import com.quantchi.sqlinject.injector.SqlParseInjector;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
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


@Intercepts(
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
)
public class DataPrivilegeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataPrivilegeInterceptor.class);

    private boolean interceptPageHelperCountMethod = true;

    private String pageHelperCountSuffix = "_COUNT";

    private static final Map<String, List<SqlInjector>> needInjectMap = new ConcurrentHashMap<String, List<SqlInjector>>();

    private final SpringELHandler springELHandler;

    public DataPrivilegeInterceptor(SpringELHandler springELHandler) {
        this.springELHandler = springELHandler;
    }


    public Object intercept(Invocation invocation) throws Throwable {
        if (Proxy.isProxyClass(invocation.getTarget().getClass())) {
            return invocation.proceed();
        }
        final StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        final MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        final MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
//        if ( SqlCommandType.SELECT != mappedStatement.getSqlCommandType()){
//            return invocation.proceed();
//        }
        final String namespace = mappedStatement.getId();
        if (!needInjectMap.containsKey(namespace)) {
            String className = namespace.substring(0, namespace.lastIndexOf('.'));
            String methodName = namespace.substring(className.length()+1);
            if (interceptPageHelperCountMethod && methodName.endsWith(pageHelperCountSuffix)) {
                methodName = methodName.substring(0, methodName.length()-pageHelperCountSuffix.length());
            }
            for (Method method : Class.forName(className).getMethods()) {
                if (methodName.equals(method.getName())) {
                    PlaceholderInject placeholderInject = method.getAnnotation(PlaceholderInject.class);
                    List<SqlInjector> injectors = new ArrayList<>(3);
                    if (placeholderInject != null) {
                        SqlInjector placeholderInjector = new PlaceholderInjector(this.springELHandler, placeholderInject.placeholder(), placeholderInject.replacement());
                        injectors.add(placeholderInjector);
                    }
                    SqlParseInject sqlParseInject = method.getAnnotation(SqlParseInject.class);
                    if (sqlParseInject != null) {
                        SqlInjector sqlParseInjector = new SqlParseInjector(this.springELHandler, sqlParseInject.table(), sqlParseInject.field(), sqlParseInject.not(), sqlParseInject.mode(), sqlParseInject.filter());
                        injectors.add(sqlParseInjector);
                    }
                    SqlInject sqlInject = method.getAnnotation(SqlInject.class);
                    if (sqlInject != null) {
                        SqlInjector sqlParseInjector = new SqlParseInjector(sqlInject.filter());
                        injectors.add(sqlParseInjector);
                    }
                    needInjectMap.put(namespace, injectors);
                }
            }
        }
        List<SqlInjector> sqlInjectors;
        if ( (sqlInjectors = needInjectMap.getOrDefault(namespace, Collections.emptyList())).isEmpty()) {
            return invocation.proceed();
        } else {
            final BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
            String sql = boundSql.getSql();
            for (SqlInjector sqlInjector : sqlInjectors) {
                sql = sqlInjector.inject(sql);
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
}
