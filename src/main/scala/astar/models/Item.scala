package astar.models

import org.joda.time.DateTime
import slick.lifted.{TableQuery, Tag}
import slick.driver.PostgresDriver.api._
import com.github.tototoshi.slick.PostgresJodaSupport._

import scala.concurrent.{Future, ExecutionContext}

case class Item(
  id: Option[Long],
  name: String,
  lat: Double,
  lng: Double,
  found: Boolean,
  foundAt: Option[DateTime],
  createdAt: DateTime,
  updatedAt: DateTime) extends AStarModel[Item] {
  override def withId(id: Long) = { copy(id = Some(id)) }
}

class Items(tag: Tag) extends AStarTable[Item](tag, "items") {

  def name = column[String]("name")
  def lat = column[Double]("lat")
  def lng = column[Double]("lng")
  def found = column[Boolean]("found")
  def foundAt = column[Option[DateTime]]("found_at")

  def * = (id.?, name, lat, lng, found, foundAt, createdAt, updatedAt) <> (Item.tupled, Item.unapply)
}

object Items extends AStarQuery[Item,Items](new Items(_))