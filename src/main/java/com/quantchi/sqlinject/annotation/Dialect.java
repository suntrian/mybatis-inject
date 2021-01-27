package com.quantchi.sqlinject.annotation;

import com.quantchi.sqlinject.parser._Dialect;
import com.quantchi.sqlinject.parser.common.SqlRewriter;

public enum Dialect {

    MYSQL(_Dialect.MYSQL),
    HIVE(_Dialect.HIVE);

    private final _Dialect dialect;

    Dialect(_Dialect dialect) {
        this.dialect = dialect;
    }

    public SqlRewriter getSqlRewriter() {
        return dialect.getSqlRewriter();
    }

}
