package com.quantchi.sqlinject;

public class SqlInjectConf {

    private static final ThreadLocal<SqlInjectConf> config = new ThreadLocal<>();

    private boolean enabled = true;

    private SqlInjectConf() {}

    public static SqlInjectConf setEnabled(boolean enabled) {
        if (config.get() == null) {
            config.set(new SqlInjectConf());
        }
        SqlInjectConf conf = config.get();
        conf.enabled = enabled;
        return conf;
    }

    public static boolean isEnabled() {
        return config.get() == null || config.get().enabled;
    }

    public static void clear() {
        config.remove();
    }
}
