package com.quantchi.sqlinject.injector;

public class SpringELValueInjector implements SqlInjector {

    private final SpringELHandler springELHandler;

    private final ValueSqlInjector sqlInjector;

    public SpringELValueInjector(SpringELHandler springELHandler, ValueSqlInjector sqlInjector) {
        this.springELHandler = springELHandler;
        this.sqlInjector = sqlInjector;
    }

    @Override
    public String inject(String sql) {
        evalValue();
        return sqlInjector.inject(sql);
    }

    public void evalValue() {
        Object[] values = sqlInjector.values();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof String) {
                Object evalValue  = springELHandler.handle((String) value);
                values[i] = evalValue;
            }
        }
    }

    public ValueSqlInjector getSqlInjector() {
        return sqlInjector;
    }

}
