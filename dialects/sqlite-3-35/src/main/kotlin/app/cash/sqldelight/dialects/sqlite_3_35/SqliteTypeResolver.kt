package app.cash.sqldelight.dialects.sqlite_3_35

import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteDeleteStmtLimited
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteInsertStmt
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.psi.util.PsiTreeUtil
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteTypeResolver as Sqlite324TypeResolver

class SqliteTypeResolver(private val parentResolver: TypeResolver) : Sqlite324TypeResolver(parentResolver) {
  override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? {
    fun List<QueryElement.QueryColumn>.flattenCompounded(): List<QueryElement.QueryColumn> {
      return map { column ->
        if (column.compounded.none { it.element != column.element || it.nullable != column.nullable }) {
          column.copy(compounded = emptyList())
        } else {
          column
        }
      }
    }

    sqlStmt.insertStmt?.let { insert ->
      check(insert is SqliteInsertStmt)
      insert.returningClause?.let {
        return object : QueryWithResults {
          override var statement: SqlAnnotatedElement = insert
          override val select = it
          override val pureTable by lazy {
            val pureColumns = select.queryExposed().singleOrNull()?.columns?.flattenCompounded()
            val resolvedTable = insert.tableName.reference?.resolve()
            val table = PsiTreeUtil.getParentOfType(resolvedTable, Queryable::class.java)?.tableExposed()
              ?: return@lazy null
            val requestedColumnsAreIdenticalToTable = table.query.columns.flattenCompounded() == pureColumns
            if (requestedColumnsAreIdenticalToTable) {
              insert.tableName
            } else {
              null
            }
          }
        }
      }
    }
    sqlStmt.updateStmtLimited?.let { update ->
      check(update is SqliteUpdateStmtLimited)
      update.returningClause?.let {
        return object : QueryWithResults {
          override var statement: SqlAnnotatedElement = update
          override val select = it
          override val pureTable = update.qualifiedTableName.tableName
        }
      }
    }
    sqlStmt.deleteStmtLimited?.let { delete ->
      check(delete is SqliteDeleteStmtLimited)
      delete.returningClause?.let {
        return object : QueryWithResults {
          override var statement: SqlAnnotatedElement = delete
          override val select = it
          override val pureTable = delete.qualifiedTableName?.tableName
        }
      }
    }
    return parentResolver.queryWithResults(sqlStmt)
  }
}
