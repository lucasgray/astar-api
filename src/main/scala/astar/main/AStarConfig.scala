package astar.main

import java.util.concurrent.ForkJoinPool

import com.mchange.v2.c3p0.{ComboPooledDataSource, PooledDataSource}
import com.typesafe.config.ConfigFactory
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext

object AStarConfig {

  implicit val db = Database.forDataSource(pooledDataSource)
  implicit val executor = ExecutionContext.fromExecutor(new ForkJoinPool(8))

  lazy val config = {
    println("loading config")

    ConfigFactory.load()
  }

  def pooledDataSource: PooledDataSource =  {

    cpdsFor(
      config.getString("db.driver"),
      config.getString("db.username"),
      config.getString("db.password"),
      config.getString("db.url")
    )
  }

  def cpdsFor(driver: String, user: String, password: String, jdbcUrl: String) = {
    val cpds = new ComboPooledDataSource()
    cpds.setDriverClass(driver)
    cpds.setJdbcUrl(jdbcUrl)
    cpds.setUser(user)
    cpds.setPassword(password)
    cpds.setMinPoolSize(1)
    cpds.setAcquireIncrement(1)
    cpds.setMaxPoolSize(20)

    //More robust connection pool
    //make sure connection is alive before query, retry for 30 secs in case db/network blip, etc.
    cpds.setTestConnectionOnCheckout(true)
    cpds.setMaxConnectionAge(3600)
    cpds.setAcquireRetryDelay(1000)
    cpds.setAcquireRetryAttempts(30)
    cpds.setBreakAfterAcquireFailure(false)

    cpds

  }

}
