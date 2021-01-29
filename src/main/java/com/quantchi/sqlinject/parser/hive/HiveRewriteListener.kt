package com.quantchi.sqlinject.parser.hive

import com.quantchi.sqlinject.annotation.Logic
import com.quantchi.sqlinject.parser.hive.parser.HiveParser.*
import com.quantchi.sqlinject.parser.hive.parser.HiveParserBaseListener
import org.antlr.v4.runtime.TokenStreamRewriter

class HiveRewriteListener(private val rewriter: TokenStreamRewriter,
                          private val logic: Logic,
                          tableFilters: Map<String?, List<String>>
) : HiveParserBaseListener() {

    private val tableFilters: MutableMap<String?, List<String>>

    private val targetTable: Set<String?> = tableFilters.keys.filterNotNull().map { it.toUpperCase() }.toSet()

    private val hasNullTable = tableFilters.keys.any { it == null || "" == it }

    private var level = 0

    init {
        this.tableFilters = HashMap()
        tableFilters.forEach { (k, v) -> this.tableFilters[if (k==null||k=="") null else k.toUpperCase()] = v }
    }

    override fun enterFromClause(ctx: FromClauseContext?) { level ++ }

    override fun exitFromClause(ctx: FromClauseContext?) { level -- }

    override fun exitDeleteStatement(ctx: DeleteStatementContext) {
        val tableName = unwrapTableName(ctx.tableName().text)
        val filters = getFilters(tableName, null)
        if (filters.isNotEmpty()) {
            if (ctx.whereClause() != null) {
                rewriter.insertAfter(ctx.whereClause().KW_WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.whereClause().stop, " )")
            } else {
                rewriter.insertAfter(ctx.tableName().stop, " WHERE ${mergeFilters(filters)} ")
            }
        }
    }

    override fun exitUpdateStatement(ctx: UpdateStatementContext) {
        val tableName = unwrapTableName(ctx.tableName().text)
        val filters = getFilters(tableName, null)
        if (filters.isNotEmpty()) {
            if (ctx.whereClause() != null) {
                rewriter.insertAfter(ctx.whereClause().KW_WHERE().symbol, " ${mergeFilters(filters)} AND (")
                rewriter.insertAfter(ctx.whereClause().stop, " )")
            } else {
                rewriter.insertAfter(ctx.setColumnsClause().stop, " WHERE ${mergeFilters(filters)} ")
            }
        }
    }

    override fun exitSingleFromStatement(ctx: SingleFromStatementContext) {
        val filters = getFilters(ctx.fromClause().fromSource())
        if (filters.isNotEmpty()) {
            for (bodyContext in ctx.body()) {
                if (bodyContext.whereClause() != null) {
                    rewriter.insertAfter(bodyContext.whereClause().KW_WHERE().symbol, " ${mergeFilters(filters)} AND (")
                    rewriter.insertAfter(bodyContext.whereClause().stop, " )")
                } else {
                    if (bodyContext.lateralView() != null) {
                        rewriter.insertAfter(bodyContext.lateralView().stop, " WHERE ${mergeFilters(filters)} ")
                    } else {
                        rewriter.insertAfter(bodyContext.selectClause().stop, " WHERE ${mergeFilters(filters)} ")
                    }
                }
            }
        }
    }

    override fun exitAtomSelectStatement(ctx: AtomSelectStatementContext) {
        if (ctx.fromClause() != null) {
            val filters = getFilters(ctx.fromClause().fromSource())
            if (filters.isNotEmpty()) {
                if (ctx.whereClause() != null) {
                    rewriter.insertAfter(ctx.whereClause().KW_WHERE().symbol, " ${mergeFilters(filters)} AND (")
                    rewriter.insertAfter(ctx.whereClause().stop, " )")
                } else {
                    rewriter.insertAfter(ctx.fromClause().stop, " WHERE ${mergeFilters(filters)} ")
                }
            }
        } else if (hasNullTable) {
            val filters = this.tableFilters[null]?: this.tableFilters[""]?: emptyList()
            if (filters.isNotEmpty()) {
                if (ctx.whereClause() != null) {
                    rewriter.insertAfter(ctx.whereClause().KW_WHERE().symbol, " ${mergeFilters(filters)} AND (")
                    rewriter.insertAfter(ctx.whereClause().stop, " )")
                } else {
                    rewriter.insertAfter(ctx.selectClause().stop, " WHERE ${mergeFilters(filters)} ")
                }
            }
        }
    }

    /**
     * @return Set<Pair<tableName, tableAlias>>
     */
    private fun fromSource(ctx: FromSourceContext): Set<Pair<String?, String?>> {
        return if (ctx.joinSource() != null ) {
            joinSource(ctx.joinSource())
        } else {
            val tables = mutableSetOf<Pair<String?, String?>>()
            for (uniqueJoinSourceContext in ctx.uniqueJoinSource()) {
                val tableName = unwrapTableName(uniqueJoinSourceContext.uniqueJoinTableSource().tableName().text)
                val tableAlias = uniqueJoinSourceContext.uniqueJoinTableSource().identifier()?.text
                tables.add(Pair(tableName, tableAlias))
            }
            tables
        }
    }

    private fun joinSource(ctx: JoinSourceContext): Set<Pair<String?, String?>> {
        val tables = mutableSetOf<Pair<String?, String?>>()
        with(ctx.atomjoinSource()) {
            when {
                tableSource() != null -> tables.add(Pair(unwrapTableName(tableSource().tableName().text), tableSource().identifier()?.text))
                virtualTableSource() != null -> tables.add(Pair(null, virtualTableSource().tableAlias().text))
                subQuerySource() != null -> tables.add(Pair(null, subQuerySource().identifier().text))
                joinSource() != null -> tables.addAll(joinSource(joinSource()))
                else -> {}
            }
        }
        if (ctx.joinPart()?.isNotEmpty() == true) {
            for (joinPartContext in ctx.joinPart()) {
                with(joinPartContext.joinSourcePart()) {
                    when {
                        tableSource() != null -> tables.add(Pair(unwrapTableName(tableSource().tableName().text), tableSource().identifier()?.text))
                        virtualTableSource() != null -> tables.add(Pair(null, virtualTableSource().tableAlias().text))
                        subQuerySource() != null -> tables.add(Pair(null, subQuerySource().identifier().text))
                        partitionedTableFunction() != null -> {}
                        else -> {}
                    }
                }
            }
        }
        return tables
    }

    private fun mergeFilters(filters: List<String>) : String {
        return if (filters.size==1) filters[0] else filters.joinToString(separator = " ${logic.name} ", prefix = " ( ", postfix = " ) ")
    }

    private fun getFilters(ctx: FromSourceContext): List<String> {
        var tempFilter: MutableList<String> = mutableListOf()
        if (targetTable.isNotEmpty()) {
            val tables = fromSource(ctx)
            val matchTables = tables.filter { targetTable.contains(it.first?.toUpperCase()) || targetTable.contains(it.second?.toUpperCase()) }
            if (matchTables.isNotEmpty()) {
                for (tableFilter in this.tableFilters) {
                    if (tableFilter.key == null) continue
                    for (filter in tableFilter.value) {
                        val matchTableFilters = mutableListOf<String>()
                        for (matchTable in matchTables) {
                            if (tableFilter.key == matchTable.second?.toUpperCase() || tableFilter.key == matchTable.first?.toUpperCase()) {
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
                val filters = tableFilters[tableName.toUpperCase()]?:tableFilters[tableAlias?.toUpperCase()]?: emptyList()
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

    private fun unwrapTableName(tableName: String): String {
        return tableName.split(".").last()
            .trim('`', '"')
    }

}