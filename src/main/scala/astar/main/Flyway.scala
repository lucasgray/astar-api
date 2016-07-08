package astar.main

import javax.sql.DataSource

import org.flywaydb.core.Flyway

trait HasFlyway {

  def datasource: DataSource = AStarConfig.pooledDataSource

  lazy val flyway = {

    val flyway = new Flyway()
    val basicMigrations = "db/migration"

    flyway.setLocations(basicMigrations)
    flyway.setBaselineOnMigrate(true)
    flyway.setValidateOnMigrate(true)
    flyway.setDataSource(datasource)
    flyway
  }
}

object Migrate extends HasFlyway {
  def main(args: Array[String]): Unit = {
    flyway.migrate()
  }
}

object Repair extends HasFlyway {
  def main(args: Array[String]): Unit = {
    flyway.repair()
  }
}


object Clean extends HasFlyway {
  def main(args: Array[String]): Unit = {
    flyway.clean()
  }
}

//object Seed extends ExecutableBase {
//  def main(args: Array[String]): Unit = {
//
//
//  }
//}