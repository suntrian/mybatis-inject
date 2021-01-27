package com.quantchi.sqlinject.parser.mysql;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.parser.common.CaseChangingCharStream;
import com.quantchi.sqlinject.parser.common.SqlRewriter;
import com.quantchi.sqlinject.parser.mysql.parser.MySqlLexer;
import com.quantchi.sqlinject.parser.mysql.parser.MySqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MysqlInject implements SqlRewriter {

    private static final Logger log = LoggerFactory.getLogger(MysqlInject.class);

    public String injectFilters(String sql, Logic logic, Map<String, List<String>> tableFilters) {
        CharStream charStream = new CaseChangingCharStream(CharStreams.fromString(sql), true);
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);
        TokenStream tokenStream = new CommonTokenStream(mySqlLexer);
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokenStream);
        MySqlParser mySqlParser = new MySqlParser(tokenStream);
        mySqlParser.removeErrorListeners();
        mySqlParser.addParseListener(new MysqlRewriteListener(rewriter, logic, tableFilters));
        try {
            mySqlParser.root();
        } catch (ParseCancellationException e) {
            log.error("SQL解析错误", e);
            return sql;
        }
        return rewriter.getText();
    }

}
