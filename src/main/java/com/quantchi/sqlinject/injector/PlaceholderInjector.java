package com.quantchi.sqlinject.injector;

import java.util.regex.Pattern;

public class PlaceholderInjector implements ValueSqlInjector {

    private final Pattern placeholderPattern;

    private final String replacement;

    public PlaceholderInjector(String placeholder, String replacement) {
        this.placeholderPattern = Pattern.compile(placeholder, Pattern.LITERAL);
        this.replacement = replacement;
    }

    public String inject(String sql) {
        Object evalReplacement = values()[0];
        if (evalReplacement instanceof String) {
            return placeholderPattern.matcher(sql).replaceAll((String) evalReplacement);
        }
        return placeholderPattern.matcher(sql).replaceAll(replacement);
    }

    @Override
    public Object[] values() {
        return new Object[]{ this.replacement };
    }
}
