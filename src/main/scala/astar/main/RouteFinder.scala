package astar.main

import astar.models._
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.xml.XML
import scalaj.http.Http
import scala.concurrent._


/**
 * Crawl mapmyrun to get a bunch of latest routes and massage the coords into our routes database
 */
object RouteFinder {

  val cities = Seq(City("madison+wisconsin", "43.0730517", "-89.40123019999999"))

  implicit val db = AStarConfig.db
  implicit val ec = AStarConfig.executor




  def main(args: Array[String])  {

    val routes = cities.flatMap { city =>
      val strFeed = Http("http://www.mapmyrun.com/vxproxy/v7.1/route/?search_radius=50000&minimum_distance=4828.041263659334&maximum_distance=3218688&text_search=&order_by=-date_created&close_to_location=%s&limit=200&offset=0".format(city.lat + "%2C" + city.long))
        .header("Referer", "http://www.mapmyrun.com/routes/?location=%s".format(city.loc))
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36")
        .header("Host", "www.mapmyrun.com")
        .asString.body

      val json = Json.parse(strFeed)

      val routes = json \\ "routes"

      val kmlLink = routes.flatMap{ it =>
        val alternate = it \\ "alternate"
        val name = it \\ "name"
        alternate.flatMap { alt =>

          val kmlAlt = alt.as[List[Map[String,String]]].find { map =>
            map.toSeq.exists(_._2.equals("kml"))
          }

          val href = kmlAlt.flatMap(_.toSeq.find(_._1 == "href"))
          href.map(_._2)
        }
      }

      kmlLink.flatMap { link =>

        val strFeed = Http("http://www.mapmyrun.com/vxproxy%s".format(link))
          .header("Referer", "http://www.mapmyrun.com/routes/")
          .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36")
          .header("Host", "www.mapmyrun.com")
          .asString.body

        val xml = XML.loadString(strFeed)

        ((xml \\ "Placemark" \ "name").headOption.map(_.text), (xml \\ "LineString" \ "coordinates").headOption.map(_.text)) match {
          case (Some(name), Some(coords)) =>
            try {

              val parsedCoords = coords.trim.split(" ").map(_.split(","))

              val asLongs = parsedCoords.flatMap { it =>

                try {
                  Some(LatLng(None, None, it(0).toDouble, it(1).toDouble, new DateTime(), new DateTime()))
                } catch {case e: Exception => println("Problem! %s on input %s,%s".format(e.getMessage, it(0), it(1)));None}
              }
              Some(RouteWithLatLngs(Route(None, name, new DateTime(), new DateTime()), asLongs.toList))
            } catch {case e: Exception => None}

          case _ => None
        }
      }
    }

    val futures = Future.sequence(
      routes.map{ route =>
        Routes.create(route.route).flatMap { created =>

          val latlngsWithRouteId = route.latlngs.map(_.copy(routeId = Some(created.id.getOrElse(-1))))

          Future.sequence(
            latlngsWithRouteId.map { it=>
              LatLngs.create(it)
            }
          )
        }
      }
    )


    Await.ready(futures, 5 minutes)
  }

}

/**
 * Grab some random markers from our routes database and some random items from our items database
 * and seed the area
 */
object ItemAdder {
  val cities = Seq(City("madison+wisconsin", "43.0730517", "-89.40123019999999"))

  val items = Seq("machine gun ammo", "robot arm", "transistors", "supply cache")

  implicit val db = AStarConfig.db
  implicit val ec = AStarConfig.executor

  val maxItemsInArea = 500

  //how many items are unfound and still in the area?
  //how many items do we need to add?
  //make a bag of that many items

  def main(args: Array[String]) {

    println("begin!")

    val itemsFut = getItemBag()

    Await.ready(itemsFut, 1 minute)

    val andThen = itemsFut.flatMap{ items =>
      println("creating!")
      Future.sequence(items.map(Items.create(_)))
    }

    andThen.map{seq =>
      println("items! "+seq)
    }

    andThen.onSuccess{ case s:Seq[Item] =>
      println("success! %s".format(s))
    }
    andThen.onFailure { case t: Throwable =>
       t.printStackTrace()
    }

    Await.ready(andThen, 1 minute)
    println("end!")
  }


  def getItemBag() : Future[Seq[Item]] = {

    db.run(Items.filterNot(_.found).result).map(_.size).flatMap { count =>

      val amountToAdd = maxItemsInArea - count

      println("adding %s".format(amountToAdd))

      val rand = SimpleFunction.nullary[Double]("random")
      val rslt = db.run(Routes.sortBy(x=>rand).take(amountToAdd).result).flatMap {routes =>

        val seq = routes.map { route =>

          db.run(LatLngs.filter(_.routeId === route.id).sortBy(x=>rand).result.headOption).map { latLngOpt =>
            latLngOpt.map { latLng =>

              val randName = Random.nextInt(items.size-1)

              val item = Item(None, items(randName), latLng.lat, latLng.lng, false, None, new DateTime(), new DateTime())
              println("made item %s".format(item))
              item
            }
          }
        }

        Future.sequence(seq)
      }
      rslt.map( seq => seq.flatten)
    }
  }
}

object JsonMaker {

//  {
//    "type": "Feature",
//    "geometry": {
//      "type": "Point",
//      "coordinates": [-89.40123019999999, 43.0730517]
//    },
//    "properties": {
//      "title": "Loot Crate",
//      "marker-symbol": "park"
//    }
//  }

  implicit val db = AStarConfig.db
  implicit val ec = AStarConfig.executor

  def main(args: Array[String]): Unit = {

    val rslt = db.run(Items.to[Seq].result).map { items =>

      items.map { item =>

        Json.obj(
          "type" -> "Feature",
          "geometry" -> Json.obj(
            "type" -> "Point",
            "coordinates" -> Json.arr(item.lat, item.lng)
          ),
          "properties" -> Json.obj(
            "title" -> item.name,
            "marker-symbol" -> "park"
          )
        )
      }
    }

    val andThen = rslt.map { jsseq =>
      jsseq.map(Json.prettyPrint(_)).mkString(",\n")
    }

    andThen.onSuccess{ case s:String =>
      println("success! %s".format(s))
    }
    andThen.onFailure { case t: Throwable =>
      t.printStackTrace()
    }



    Await.ready(andThen, 1 minute)
  }

}
