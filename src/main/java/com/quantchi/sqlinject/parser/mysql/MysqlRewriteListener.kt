package com.quantchi.sqlinject.parser.mysql

import com.quantchi.sqlinject.annotation.Logic
import com.quantchi.sqlinject.parser.mysql.parser.MySqlParser.*
import com.quantchi.sqlinject.parser.mysql.parser.MySqlParserBaseListener
import org.antlr.v4.runtime.TokenStreamRewriter

class MysqlRewriteListener(private val rewriter: TokenStreamRewriter,
                           private val logic: Logic,
                           tableFilters: Map<String?, List<String>>
) : MySqlParserBaseListener() {

    private val tableFilters: MutableMap<String?, List<String>>

    private val targetTable: Set<String?> = tableFilters.keys.filterNotNull().map { it.toLowerCase() }.toSet()

    private val hasNullTable = tableFilters.keys.any { it == null || "" == it }

    private var level = 0

    init {
        this.tableFilters = HashMap()
        tableFilters.forEach { (k, v) -> this.tableFilters.put(if (k==null||k=="") null else k.toLowerCase(), v) }
    }

    override fun enterTableSources(ctx: TableSourcesContext) { level ++ }
    override fun exitTableSources(ctx: TableSourcesContext) { level -- }

    override fun exitFromClause(ctx: FromClauseContext) {
        var filters = getFilters(ctx.tableSources())
        if (filters.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.tableSources().stop, " WHERE ${mergeFilters(filters)}")
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, " ${mergeFilters(filters)} AND ( " )
                rewriter.insertAfter(ctx.whereExpr.stop, " )")
            }
        }
    }

    override fun exitSingleUpdateStatement(ctx: SingleUpdateStatementContext) {
        val tableName = unwrapTableName(ctx.tableName().text)
        val tableAlias = ctx.uid()?.text
        val filters: List<String> = getFilters(tableName, tableAlias)
        if (filters.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.updatedElement().last().stop, " WHERE ${mergeFilters(filters)}")
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.expression().stop, " )")
            }
        }
    }

    override fun exitMultipleUpdateStatement(ctx: MultipleUpdateStatementContext) {
        var filters = getFilters(ctx.tableSources())
        if (filters.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.updatedElement().last().stop, " WHERE ${mergeFilters(filters)}")
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.expression().stop, " )")
            }
        }
    }

    override fun exitSingleDeleteStatement(ctx: SingleDeleteStatementContext) {
        val tableName = unwrapTableName(ctx.tableName().text)
        val filters = getFilters(tableName, null)
        if (filters.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                if (ctx.PARTITION() != null) {
                    rewriter.insertAfter(ctx.RR_BRACKET().symbol, " WHERE ${mergeFilters(filters)}")
                } else {
                    rewriter.insertAfter(ctx.tableName().stop, " WHERE ${mergeFilters(filters)}")
                }
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.expression().stop, " )")
            }
        }
    }

    override fun exitMultipleDeleteStatement(ctx: MultipleDeleteStatementContext) {
        var filters = getFilters(ctx.tableSources())
        if (filters.isNotEmpty()) {
            if (ctx.WHERE() == null) {
                rewriter.insertAfter(ctx.tableSources().stop, " WHERE ${mergeFilters(filters)}")
            } else {
                rewriter.insertAfter(ctx.WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.expression().stop, " )")
            }
        }
    }

    private fun mergeFilters(filters: List<String>) : String {
        return if (filters.size==1) filters[0] else filters.joinToString(separator = " ${logic.name} ", prefix = " ( ", postfix = " ) ")
    }

    private fun getFilters(ctx: TableSourcesContext): List<String> {
        var tempFilter: MutableList<String> = mutableListOf()
        if (targetTable.isNotEmpty()) {
            val tables = tableSources(ctx)
            val matchTables = tables.filter { targetTable.contains(it.first?.toLowerCase()) || targetTable.contains(it.second?.toLowerCase()) }
            if (matchTables.isNotEmpty()) {
                for (tableFilter in this.tableFilters) {
                    if (tableFilter.key == null) continue
                    for (filter in tableFilter.value) {
                        val matchTableFilters = mutableListOf<String>()
                        for (matchTable in matchTables) {
                            if (tableFilter.key == matchTable.second?.toLowerCase() || tableFilter.key == matchTable.first?.toLowerCase()) {
                                val prefix: String? = matchTable.second?:matchTable.first
                                matchTableFilters.add(filter.replace("/**PREFIX**/", "$prefix."))
                            }
                        }
                        if (matchTableFilters.isNotEmpty()) {
                            tempFilter.add( if (matchTableFilters.size==1) matchTableFilters[0] else matchTableFilters.joinToString(" AND ", prefix = "(", postfix = ")"))
                        }
                    }
                }
            }
        }
        if (hasNullTable && level == 0) {
            //判断是否根查询,未指定表名时，只在根查询上加过滤条件
            val filters = this.tableFilters[null] ?:this.tableFilters[""]?: emptyList()
            if (filters.isEmpty()) {
                return emptyList()
            }
            tempFilter.addAll(filters.map{ it.replace("/**PREFIX**/", "") })
        }
        return tempFilter
    }

    private fun getFilters(tableName: String, tableAlias: String?) : List<String> {
        val tempFilters: MutableList<String> = mutableListOf()
        if (targetTable.isNotEmpty()) {
            if (targetTable.any{it == tableName || it == tableAlias}) {
                //当一个tabled存在多个查询时，如select * from table1 a join table1 b ON a.x = b.y, 将同一个table的filter进行AND连接
                val prefix: String? = tableAlias?:tableName
                val filters = tableFilters[tableName.toLowerCase()]?:tableFilters[tableAlias?.toLowerCase()]?: emptyList()
                for (filter in filters) {
                    if (prefix != null) {
                        tempFilters.add(if (filters.size==1) filters[0].replace("/**PREFIX**/", "$prefix.")
                        else filters.joinToString(" AND ", prefix=" ( ", postfix = " ) "){ it.replace("/**PREFIX**/", "$prefix.") })
                    } else {
                        tempFilters.add(if (filters.size==1) filters[0].replace("/**PREFIX**/", "$prefix.")
                        else filters.joinToString(" AND ", prefix = " ( ", postfix = " ) "){ it.replace("/**PREFIX**/", "") })
                    }
                }
            }
        }
        if (hasNullTable && level == 0) {
            //判断是否根查询,未指定表名时，只在根查询上加过滤条件
            val filters = this.tableFilters[null] ?:this.tableFilters[""]?: emptyList()
            if (filters.isNotEmpty()) {
                tempFilters.addAll(filters.map{ it.replace("/**PREFIX**/", "") })
            }
        }
        return tempFilters
    }

    /**
     * @return Set<Pair<tableName, tableAlias>>
     */
    private fun tableSources(ctx: TableSourcesContext): Set<Pair<String?, String?>> {
        val tables = mutableSetOf<Pair<String?, String?>>()
        for (tableSourceContext in ctx.tableSource()) {
            tables.addAll(tableSource(tableSourceContext))
        }
        return tables
    }

    /**
     * @return Set<Pair<tableName, tableAlias>>
     */
    private fun tableSource(ctx: TableSourceContext?): Set<Pair<String?, String?>> {
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

    /**
     * @return Set<Pair<tableName, tableAlias>>
     */
    private fun tableSourceItem(ctx: TableSourceItemContext?): Set<Pair<String?, String?>> {
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

    private fun unwrapTableName(tableName: String): String {
        return tableName.split(".").last()
                .trim('`')
    }
}