package astar.models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import com.github.tototoshi.slick.PostgresJodaSupport._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.{Future, ExecutionContext}

case class LatLng(
  id: Option[Long],
  routeId: Option[Long],
  lat: Double,
  lng: Double,
  createdAt: DateTime,
  updatedAt: DateTime) extends AStarModel[LatLng] {
  override def withId(id: Long) = { copy(id = Some(id)) }
}

class LatLngs(tag: Tag) extends AStarTable[LatLng](tag, "lat_lngs") {

  def routeId = column[Long]("route_id")
  def lat = column[Double]("lat")
  def lng = column[Double]("lng")

  def * = (id.?, routeId.?, lat, lng, createdAt, updatedAt) <> (LatLng.tupled, LatLng.unapply)
}

object LatLngs extends AStarQuery[LatLng, LatLngs](new LatLngs(_)) {
//
//  def create(toCreate: LatLng)(implicit db: Database, ec: ExecutionContext) : Future[LatLng] = {
//    db.run(this returning this.map(_.id) into ((d, id) => d.withId(id = id)) += toCreate)
//  }
//
//  def findById(id: Long)(implicit db: Database, ec: ExecutionContext) : Future[Option[LatLng]] = {
//    db.run(this.filter(_.id === id).result).map(_.headOption)
//  }
}