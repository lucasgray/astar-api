package astar.models

import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.{ExecutionContext, Future}

case class RouteWithLatLngs(route: Route, latlngs: List[LatLng])

case class Route(
  id: Option[Long],
  name: String,
  createdAt: DateTime,
  updatedAt: DateTime) extends AStarModel[Route] {
  override def withId(id: Long) = { copy(id = Some(id)) }
}

class Routes(tag: Tag) extends AStarTable[Route](tag, "routes") {

  def name = column[String]("name")

  def * = (id.?, name, createdAt, updatedAt) <> (Route.tupled, Route.unapply)
}

object Routes extends TableQuery(new Routes(_)) {

  def create(toCreate: Route)(implicit db: Database, ec: ExecutionContext) : Future[Route] = {
    db.run(this returning this.map(_.id) into ((d, id) => d.withId(id = id)) += toCreate)
  }

  def findById(id: Long)(implicit db: Database, ec: ExecutionContext) : Future[Option[Route]] = {
    db.run(this.filter(_.id === id).result).map(_.headOption)
  }
}