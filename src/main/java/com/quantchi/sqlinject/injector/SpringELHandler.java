package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.exception.EmptyValueException;
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

    private Boolean emptyValueAsFail = true;
    private FailoverStrategy failoverStrategy = FailoverStrategy.IGNORE;
    private Runnable preHandler = null;
    private Runnable postHandler = null;

    public SpringELHandler(@NonNull AbstractBeanFactory beanFactory, Scope scope) {
        this.beanExpressionResolver = beanFactory.getBeanExpressionResolver();
        this.beanExpressionContext = new BeanExpressionContext(beanFactory, scope == null?new SimpleThreadScope(): scope);
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
            if (evaluate == null ) {
                if (emptyValueAsFail) {
                    throw new EmptyValueException(evaluate);
                }
            } else if (evaluate instanceof Collection<?>) {
                if (((Collection<?>) evaluate).isEmpty() && emptyValueAsFail) {
                    throw new EmptyValueException(evaluate);
                }
            } else if (evaluate instanceof Map) {
                if (((Map<?, ?>) evaluate).isEmpty() && emptyValueAsFail) {
                    throw new EmptyValueException(evaluate);
                }
            } else if (evaluate instanceof CharSequence) {
                if ("".equals(evaluate) && emptyValueAsFail) {
                    throw new EmptyValueException(evaluate);
                }
            }
            return evaluate;
        } catch (Exception e) {
            if (FailoverStrategy.IGNORE.equals(failoverStrategy)){
                return null;
            }
            throw e;
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

    public void setFailoverStrategy(FailoverStrategy failoverStrategy) {
        this.failoverStrategy = failoverStrategy;
    }

    public void setEmptyValueAsFail(Boolean emptyValueAsFail) {
        this.emptyValueAsFail = emptyValueAsFail;
    }

    public Boolean getEmptyValueAsFail() {
        return emptyValueAsFail;
    }

    public FailoverStrategy getFailoverStrategy() {
        return failoverStrategy;
    }
}
