package com.quantchi.sqlinject.mysql;

import com.quantchi.sqlinject.mysql.parser.CaseChangingCharStream;
import com.quantchi.sqlinject.mysql.parser.MySqlLexer;
import com.quantchi.sqlinject.mysql.parser.MySqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

public class MysqlInject {

    private static final Logger log = LoggerFactory.getLogger(MysqlInject.class);

    public String injectFilter(String sql, @Nullable String targetTable, String filter) {
        CharStream charStream = new CaseChangingCharStream(CharStreams.fromString(sql), true);
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);
        TokenStream tokenStream = new CommonTokenStream(mySqlLexer);
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokenStream);
        MySqlParser mySqlParser = new MySqlParser(tokenStream);
        mySqlParser.removeErrorListeners();
        mySqlParser.addParseListener(new MysqlRewriteListener(rewriter, targetTable, filter));
        try {
            mySqlParser.root();
        } catch (ParseCancellationException e) {
            log.error("SQL解析错误", e);
            return sql;
        }
        return rewriter.getText();
    }

}
