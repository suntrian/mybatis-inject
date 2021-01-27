package com.quantchi.sqlinject.parser.hive;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.parser.common.CaseChangingCharStream;
import com.quantchi.sqlinject.parser.common.SqlRewriter;
import com.quantchi.sqlinject.parser.hive.parser.HiveLexer;
import com.quantchi.sqlinject.parser.hive.parser.HiveParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HiveInject implements SqlRewriter {

    private static final Logger log = LoggerFactory.getLogger(HiveInject.class);

    public String injectFilters(String sql, Logic logic, Map<String, List<String>> tableFilters) {
        CharStream charStream = new CaseChangingCharStream(CharStreams.fromString(sql), true);
        HiveLexer hiveLexer = new HiveLexer(charStream);
        TokenStream tokenStream = new CommonTokenStream(hiveLexer);
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokenStream);
        HiveParser hiveParser = new HiveParser(tokenStream);
        hiveParser.removeErrorListeners();
        hiveParser.addParseListener(new HiveRewriteListener(rewriter, logic, tableFilters));
        try {
            hiveParser.statement();
        } catch (ParseCancellationException e) {
            log.error("SQL解析错误", e);
            return sql;
        }
        return rewriter.getText();
    }

}
