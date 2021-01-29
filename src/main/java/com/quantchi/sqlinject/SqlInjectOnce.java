package com.quantchi.sqlinject;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.annotation.TreatBlankStrategy;
import com.quantchi.sqlinject.injector.SqlInjector;

import java.util.concurrent.atomic.AtomicInteger;

public class SqlInjectOnce {

    private static final ThreadLocal<SqlInjectOnce> config = new ThreadLocal<>();

    private boolean enabled = true;

    private FailoverStrategy failoverStrategy;

    private TreatBlankStrategy treatBlankStrategy;

    private SqlInjector sqlInjector;

    private final AtomicInteger stay = new AtomicInteger(0);

    private SqlInjectOnce() {}

    public static SqlInjectOnce enabled(boolean enabled) {
        SqlInjectOnce conf = getSqlInjectConf();
        conf.enabled = enabled;
        return conf;
    }

    public static SqlInjectOnce failoverStrategy(FailoverStrategy strategy) {
        SqlInjectOnce conf = getSqlInjectConf();
        conf.failoverStrategy = strategy;
        return conf;
    }

    public static SqlInjectOnce temporaryInject(SqlInjector sqlInjector) {
        SqlInjectOnce conf = getSqlInjectConf();
        conf.sqlInjector = sqlInjector;
        return conf;
    }

    public static boolean enabled() {
        return config.get() == null || config.get().enabled;
    }

    public static FailoverStrategy failoverStrategy() {
        return config.get() == null? null : config.get().failoverStrategy;
    }

    public static TreatBlankStrategy treatBlankStrategy() {
        return config.get() == null? null : config.get().treatBlankStrategy;
    }

    public static SqlInjectOnce treatBlankStrategy(TreatBlankStrategy treatBlankStrategy) {
        SqlInjectOnce conf = getSqlInjectConf();
        conf.treatBlankStrategy = treatBlankStrategy;
        return conf;
    }

    public static SqlInjectOnce stay() {
        return stay(1);
    }

    /**
     * 保留配置多少次SQL执行本执行配置不清理，谨慎使用，在线程池场景下，单次执行配置不清理可能导致数据泄漏到一下次无关的SQL执行
     * @param times 当前线程中，不执行清理的SQL执行次数
     * @return SqlInjectOnce
     */
    public static SqlInjectOnce stay(int times) {
        SqlInjectOnce conf = getSqlInjectConf();
        conf.stay.addAndGet(times);
        return conf;
    }

    public static SqlInjector temporaryInject() {
        return config.get() == null?null: config.get().sqlInjector;
    }

    public static void clear() {
        if (config.get() != null) {
            if (config.get().stay.decrementAndGet() < 0) {
                config.remove();
            }
        }
    }

    private static SqlInjectOnce getSqlInjectConf() {
        if (config.get() == null) {
            config.set(new SqlInjectOnce());
        }
        return config.get();
    }
}
