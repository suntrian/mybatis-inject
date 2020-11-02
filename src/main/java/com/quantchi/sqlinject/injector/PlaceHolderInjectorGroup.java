package com.quantchi.sqlinject.injector;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.annotation.PlaceholderInject;

import java.util.List;

public class PlaceHolderInjectorGroup implements SqlInjector{

    List<PlaceholderInjector> placeholderInjectors;

    public PlaceHolderInjectorGroup(List<PlaceholderInjector> placeholderInjectors) {
        this.placeholderInjectors = placeholderInjectors;
    }

    @Override
    public String inject(String sql) {
        for (PlaceholderInjector placeholderInjector : placeholderInjectors) {
            sql = placeholderInjector.inject(sql);
        }
        return sql;
    }

}
