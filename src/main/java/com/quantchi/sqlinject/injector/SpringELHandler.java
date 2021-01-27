package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.exception.EmptyValueException;
import com.quantchi.sqlinject.exception.ValueEvalException;
import com.quantchi.sqlinject.spring.SqlInjectProperties;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpringELHandler {

    private final BeanExpressionResolver beanExpressionResolver;
    private final BeanExpressionContext beanExpressionContext;
    private final SqlInjectProperties sqlInjectProperties;

    private Runnable preHandler = null;
    private Runnable postHandler = null;

    public SpringELHandler(@NonNull AbstractBeanFactory beanFactory, Scope scope, SqlInjectProperties sqlInjectProperties) {
        this.beanExpressionResolver = beanFactory.getBeanExpressionResolver();
        this.beanExpressionContext = new BeanExpressionContext(beanFactory, scope == null?new SimpleThreadScope(): scope);
        this.sqlInjectProperties = sqlInjectProperties;
    }

    private static final Pattern SPEL_PATTERN = Pattern.compile("(#\\{.*?\\})", Pattern.DOTALL);

    private Object parseExpression(String expression) {
        Matcher matcher = SPEL_PATTERN.matcher(expression);
        if (matcher.matches()) {
           return completeExpression(matcher.group(1));
        }
        while (matcher.find()){
            Object value = completeExpression(matcher.group(1));
            if (value instanceof CharSequence) {
                expression = matcher.replaceFirst(value.toString());
            } else if (value instanceof Collection) {
                expression = matcher.replaceFirst(((Collection<?>) value).stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }
        return expression;
    }

    private Object completeExpression(String expression) {
        try {
            if (this.preHandler != null) {
                this.preHandler.run();
            }
            Object evaluate = this.beanExpressionResolver.evaluate(expression, this.beanExpressionContext);
            if (sqlInjectProperties.getEmptyValueAsFail() && isEmptyValue(evaluate)) {
                throw new EmptyValueException(evaluate);
            }
            return evaluate;
        } catch (Exception e) {
            if (FailoverStrategy.IGNORE.equals(sqlInjectProperties.getFailoverStrategy())){
                return null;
            }
            throw new ValueEvalException(e);
        } finally {
            if (this.postHandler != null) {
                this.postHandler.run();
            }
        }
    }

    public Object handle(String expression) {
        return parseExpression(expression);
    }


    public void setPreHandler(Runnable preHandler) {
        this.preHandler = preHandler;
    }

    public void setPostHandler(Runnable postHandler) {
        this.postHandler = postHandler;
    }

    public SqlInjectProperties getSqlInjectProperties() {
        return sqlInjectProperties;
    }

    private boolean isEmptyValue(Object value) {
        if (value == null ) return true;
        if (value instanceof CharSequence && ((CharSequence) value).length()==0) return true;
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) return true;
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) return true;
        return false;
    }

}
