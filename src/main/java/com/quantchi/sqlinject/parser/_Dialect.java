package com.quantchi.sqlinject.parser;

import com.quantchi.sqlinject.parser.common.SqlRewriter;
import com.quantchi.sqlinject.parser.hive.HiveInject;
import com.quantchi.sqlinject.parser.mysql.MysqlInject;

public enum _Dialect {

    MYSQL(new MysqlInject()),
    HIVE(new HiveInject());

    private final SqlRewriter sqlRewriter;

    _Dialect(SqlRewriter sqlRewriter) {
        this.sqlRewriter = sqlRewriter;
    }

    public SqlRewriter getSqlRewriter() {
        return sqlRewriter;
    }
}
