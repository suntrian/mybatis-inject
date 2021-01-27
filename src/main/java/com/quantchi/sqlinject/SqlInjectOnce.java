package com.quantchi.sqlinject;

import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.injector.SqlInjector;

public class SqlInjectOnce {

    private static final ThreadLocal<SqlInjectOnce> config = new ThreadLocal<>();

    private boolean enabled = true;

    private FailoverStrategy failoverStrategy;

    private SqlInjector sqlInjector;

    private boolean stay = false;

    private SqlInjectOnce() {}

    public static SqlInjectOnce enabled(boolean enabled) {
        if (config.get() == null) {
            config.set(new SqlInjectOnce());
        }
        SqlInjectOnce conf = config.get();
        conf.enabled = enabled;
        return conf;
    }

    public static SqlInjectOnce failoverStrategy(FailoverStrategy strategy) {
        if (config.get() == null) {
            config.set(new SqlInjectOnce());
        }
        SqlInjectOnce conf = config.get();
        conf.failoverStrategy = strategy;
        return conf;
    }

    public static SqlInjectOnce temporaryInject(SqlInjector sqlInjector) {
        if (config.get() == null) {
            config.set(new SqlInjectOnce());
        }
        SqlInjectOnce conf = config.get();
        conf.sqlInjector = sqlInjector;
        return conf;
    }

    public static boolean enabled() {
        return config.get() == null || config.get().enabled;
    }

    public static FailoverStrategy failoverStrategy() {
        return config.get() == null? null : config.get().failoverStrategy;
    }

    public static SqlInjectOnce stay() {
        if (config.get() == null) {
            config.set(new SqlInjectOnce());
        }
        SqlInjectOnce conf = config.get();
        conf.stay = true;
        return conf;
    }

    public static SqlInjector temporaryInject() {
        return config.get() == null?null: config.get().sqlInjector;
    }

    public static void clear() {
        if (config.get() != null) {
            if (!config.get().stay) {
                config.remove();
            } else {
                config.get().stay = false;
            }
        }
    }
}
