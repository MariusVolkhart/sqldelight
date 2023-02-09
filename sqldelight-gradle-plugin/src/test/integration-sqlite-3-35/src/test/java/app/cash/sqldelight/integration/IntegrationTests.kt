package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var personQueries: PersonQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    val queryWrapper = QueryWrapper(database)
    personQueries = queryWrapper.personQueries
  }

  @Test fun insertReturning1() {
    assertThat(personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne())
      .isEqualTo("Alec")
  }

  @Test fun insertReturningMany() {
    assertThat(personQueries.insertAndReturnMany(1, "Alec", "Strong").executeAsOne())
      .isEqualTo(
        InsertAndReturnMany(1, "Alec"),
      )
  }

  @Test fun insertReturningAll() {
    assertThat(personQueries.insertAndReturnAll(1, "Alec", "Strong").executeAsOne())
      .isEqualTo(
        Person(1, "Alec", "Strong"),
      )
  }
}
