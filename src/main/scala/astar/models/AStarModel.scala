package astar.models

import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

trait AStarModel[T] {
  def id: Option[Long]
  def createdAt: DateTime
  def updatedAt: DateTime
  def withId(id: Long): T
}

abstract class AStarTable[T <: AStarModel[T]](tag: Tag, name: String) extends Table[T](tag, name) {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def createdAt = column[DateTime]("created_at")
  def updatedAt = column[DateTime]("updated_at")
}

class AStarQuery[T <: AStarModel[T], A <: AStarTable[T]](t : (Tag) => A) extends TableQuery[A](t) {

  def create(toCreate: T)(implicit db: Database, ec: ExecutionContext) : Future[T] =
    db.run(this returning this.map(_.id) into ((d, id) => d.withId(id = id)) += toCreate)

  def find(toFind: T)(implicit db: Database, ec: ExecutionContext) : Future[Option[T]] =
    db.run(this.filter(_.id === toFind.id).result.headOption)

  def findById(id: Long)(implicit db: Database, ec: ExecutionContext) : Future[Option[T]] =
    db.run(this.filter(_.id === id).result.headOption)

  def update(toUpdate: T)(implicit db: Database, ec: ExecutionContext): Future[T] =
    db.run(this.filter(_.id === toUpdate.id).update(toUpdate).map(_=>toUpdate))

  def delete(toDelete: T)(implicit db: Database, ec: ExecutionContext): Future[Unit] =
    db.run(this.filter(_.id === toDelete.id).delete).map(_=>())

  def save(toSave: T)(implicit db: Database, ec: ExecutionContext): Future[T] =
    find(toSave).flatMap { tOpt =>
      tOpt match {
        case Some(t) =>
          update(t)
        case None =>
          create(toSave)
      }
    }
}