package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PgJsonTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `postgres json columns are specialized`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  col1 JSON NOT NULL,
            |  col2 JSONB NOT NULL
            |);
            |
            |insert:
            |INSERT INTO data
            |VALUES (:col1, :col2);
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
            |/**
            | * @return The number of rows updated.
            | */
            |public fun insert(col1: kotlin.String, col2: kotlin.String): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
            |  val result = driver.execute(${insert.id.withUnderscores}, ""${'"'}
            |      |INSERT INTO data
            |      |VALUES (?, ?)
            |      ""${'"'}.trimMargin(), 2) {
            |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
            |        bindObject(0, org.postgresql.util.PGobject().apply {
            |          type = "json"
            |          value = col1
            |        })
            |        bindObject(1, org.postgresql.util.PGobject().apply {
            |          type = "json"
            |          value = col2
            |        })
            |      }
            |  notifyQueries(${insert.id.withUnderscores}) { emit ->
            |    emit("data")
            |  }
            |  return result
            |}
            |
      """.trimMargin(),
    )
  }

  @Test
  fun `postgres json array columns are specialized`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  col1 JSON[] NOT NULL,
            |  col2 JSONB[] NOT NULL
            |);
            |
            |insert:
            |INSERT INTO data
            |VALUES (:col1, :col2);
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
            |/**
            | * @return The number of rows updated.
            | */
            |public fun insert(col1: kotlin.Array<kotlin.String>, col2: kotlin.Array<kotlin.String>): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
            |  val result = driver.execute(${insert.id.withUnderscores}, ""${'"'}
            |      |INSERT INTO data
            |      |VALUES (?, ?)
            |      ""${'"'}.trimMargin(), 2) {
            |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
            |        bindObject(0, col1.map {
            |          org.postgresql.util.PGobject().apply {
            |            type = "json"
            |            value = it
            |          }
            |        })
            |        bindObject(1, col2.map {
            |          org.postgresql.util.PGobject().apply {
            |            type = "json"
            |            value = it
            |          }
            |        })
            |      }
            |  notifyQueries(${insert.id.withUnderscores}) { emit ->
            |    emit("data")
            |  }
            |  return result
            |}
            |
      """.trimMargin(),
    )
  }
}
