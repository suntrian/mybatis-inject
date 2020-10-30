package com.quantchi.sqlinject.mysql

import com.quantchi.sqlinject.mysql.parser.MySqlParser.*
import com.quantchi.sqlinject.mysql.parser.MySqlParserBaseListener
import org.antlr.v4.runtime.TokenStreamRewriter

class MysqlRewriteListener(private val rewriter: TokenStreamRewriter, targetTable: String?, private val filter: String) : MySqlParserBaseListener() {

    private val targetTable: String? = targetTable?.toLowerCase()

    private var level = 0;

    override fun enterTableSources(ctx: TableSourcesContext) { level ++ }
    override fun exitTableSources(ctx: TableSourcesContext) { level -- }

    override fun exitFromClause(ctx: FromClauseContext) {
        var tempFilter = getFilters(ctx.tableSources())
        if (tempFilter.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.tableSources().stop, tempFilter.joinToString(separator = " AND ", prefix = " WHERE "))
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, tempFilter.joinToString(" AND ", " ( ", " ) AND ") )
            }
        }
    }

    override fun exitSingleUpdateStatement(ctx: SingleUpdateStatementContext) {
        val tempFilter = this.filter.replace("/**PREFIX**/", "")
        if (this.targetTable != null) {
            val tableName = ctx.uid()?.text?:unwrapTableName(ctx.tableName().text)
            if (this.targetTable != tableName.toLowerCase()) {
                return
            }
        } else if (level > 1) {
            return
        }
        if (ctx.WHERE() == null) {
            rewriter.insertAfter(ctx.updatedElement().last().stop, " WHERE $tempFilter ")
        } else {
            rewriter.insertAfter(ctx.WHERE().symbol, " ($tempFilter) AND ")
        }
    }

    override fun exitMultipleUpdateStatement(ctx: MultipleUpdateStatementContext) {
        var tempFilter = getFilters(ctx.tableSources())
        if (tempFilter.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.updatedElement().last().stop, tempFilter.joinToString(separator = " AND ", prefix = " WHERE "))
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, tempFilter.joinToString(" AND ", " ( ", " ) AND "))
            }
        }
    }

    override fun exitSingleDeleteStatement(ctx: SingleDeleteStatementContext) {
        val tempFilter = this.filter.replace("/**PREFIX**/", "")
        if (this.targetTable != null) {
            val tableName = unwrapTableName(ctx.tableName().text)
            if (this.targetTable != tableName.toLowerCase()) {
                return
            }
        } else if (level > 1) {
            return
        }
        if (ctx.WHERE() == null) {
            if (ctx.PARTITION()!=null) {
                rewriter.insertAfter(ctx.RR_BRACKET().symbol, " WHERE $tempFilter ")
            } else {
                rewriter.insertAfter(ctx.tableName().stop, " WHERE $tempFilter ")
            }
        } else {
            rewriter.insertAfter(ctx.WHERE().symbol, " ($filter) AND ")
        }
    }

    override fun exitMultipleDeleteStatement(ctx: MultipleDeleteStatementContext) {
        var tempFilter = getFilters(ctx.tableSources())
        if (tempFilter.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.tableSources().stop, tempFilter.joinToString(separator = " AND ", prefix = " WHERE "))
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, tempFilter.joinToString(" AND ", " ( ", " ) AND "))
            }
        }
    }

    private fun getFilters(ctx: TableSourcesContext): List<String> {
        var tempFilter: MutableList<String> = mutableListOf(this.filter)
        if (targetTable != null) {
            val tables = tableSources(ctx)
            val matchTables = tables.filter { targetTable == it.first?.toLowerCase() || targetTable == it.second?.toLowerCase() }
            if (tables.isEmpty()) {
                return emptyList()
            } else {
                tempFilter = mutableListOf();
                for (matchTable in matchTables) {
                    val prefix: String? = matchTable.second?:matchTable.first
                    if (prefix != null) {
                        tempFilter.add(this.filter.replace("/**PREFIX**/", "$prefix."))
                    } else {
                        tempFilter.add(this.filter)
                    }
                }
            }
        } else if (level > 0) {
            //判断是否根查询,未指定表名时，只在根查询上加过滤条件
            return emptyList()
        }
        return tempFilter;
    }

    protected fun tableSources(ctx: TableSourcesContext): Set<Pair<String?, String?>> {
        val tables = mutableSetOf<Pair<String?, String?>>()
        for (tableSourceContext in ctx.tableSource()) {
            tables.addAll(tableSource(tableSourceContext))
        }
        return tables
    }

    protected fun tableSource(ctx: TableSourceContext?): Set<Pair<String?, String?>> {
        val tableSourceItem: TableSourceItemContext
        val joinPart: List<JoinPartContext>?
        when (ctx) {
            is TableSourceBaseContext -> {
                tableSourceItem = ctx.tableSourceItem()
                joinPart = ctx.joinPart()
            }
            is TableSourceNestedContext -> {
                tableSourceItem = ctx.tableSourceItem()
                joinPart = ctx.joinPart()
            }
            else -> return emptySet()
        }
        val tableSet = tableSourceItem(tableSourceItem).toMutableSet();
        for (joinPartContext in joinPart?: emptyList()) {
            when (joinPartContext) {
                is InnerJoinContext -> tableSet.addAll(tableSourceItem(joinPartContext.tableSourceItem()))
                is StraightJoinContext -> tableSet.addAll(tableSourceItem(joinPartContext.tableSourceItem()))
                is OuterJoinContext -> tableSet.addAll(tableSourceItem(joinPartContext.tableSourceItem()))
                is NaturalJoinContext -> tableSet.addAll(tableSourceItem(joinPartContext.tableSourceItem()))
            }
        }
        return tableSet
    }

    protected fun tableSourceItem(ctx: TableSourceItemContext?): Set<Pair<String?, String?>> {
        return when(ctx) {
            is AtomTableItemContext -> setOf(Pair(unwrapTableName(ctx.tableName().text), ctx.alias?.text))
            is SubqueryTableItemContext -> if (ctx.alias == null) emptySet() else setOf(Pair(null, ctx.alias.text))
            is TableSourcesItemContext -> {
                val tableSet: MutableSet<Pair<String?, String?>> = mutableSetOf();
                for (tableSourceContext in ctx.tableSources().tableSource()) {
                    tableSet.addAll(tableSource(tableSourceContext))
                }
                tableSet
            }
            else -> emptySet()
        }
    }

    protected fun unwrapTableName(tableName: String): String {
        return tableName.split(".").last()
                .trim('`')
    }
}