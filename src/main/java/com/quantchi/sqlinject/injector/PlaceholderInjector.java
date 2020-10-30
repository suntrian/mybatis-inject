package com.quantchi.sqlinject.injector;

import java.util.regex.Pattern;

public class PlaceholderInjector implements SqlInjector {

    private final String placeholder;
    private final Pattern placeholderPattern;

    private final String replacement;

    private final SpringELHandler springELHandler;

    public PlaceholderInjector(SpringELHandler springELHandler, String placeholder, String replacement) {
        this.springELHandler = springELHandler;
        this.placeholder = placeholder;
        this.placeholderPattern = Pattern.compile(placeholder, Pattern.LITERAL);
        this.replacement = replacement;
    }

    public String inject(String sql) {
        Object evalReplacement = springELHandler.handle(replacement);
        if (evalReplacement instanceof String) {
            return placeholderPattern.matcher(sql).replaceAll((String) evalReplacement);
        }
        return sql.replaceAll(placeholder, replacement);
    }
}
