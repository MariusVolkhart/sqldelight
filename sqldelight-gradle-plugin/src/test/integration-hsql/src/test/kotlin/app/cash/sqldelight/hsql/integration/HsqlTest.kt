package app.cash.sqldelight.hsql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager

class HsqlTest {
  val conn = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb;shutdown=true")
  val driver = object : JdbcDriver() {
    override fun getConnection() = conn
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun notifyListeners(queryKeys: Array<String>) = Unit
  }
  val database = MyDatabase(driver)

  @Before fun before() {
    MyDatabase.Schema.create(driver)
  }

  @After fun after() {
    conn.close()
  }

  @Test fun simpleSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian", true)
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true
        )
      )
  }

  @Test fun types() {
    val int = BigDecimal("1234")
    val decimal = BigDecimal("1234.1234")
    database.typeQueries.insertTypes(decimal, decimal, decimal, decimal, decimal, decimal)
    assertThat(database.typeQueries.selectTypes().executeAsOne())
      .isEqualTo(
        Types(
          test1 = int,
          test2 = int,
          test3 = int,
          test4 = decimal,
          test5 = decimal,
          test6 = decimal,
        )
      )
  }
}
