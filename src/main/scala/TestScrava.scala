import kiambogo.scrava.ScravaClient

object TestScrava {

  def main(args: Array[String]) {

    val client = new ScravaClient("b0cbcf2abad318a53a0e35e997d7af16a1a18c0b")
    val athlete = client.retrieveAthlete()

    println(athlete)

    val activity = client.listAthleteActivities(Some(1))(0)

    val streams = client.retrieveActivityStream(activity.id+"")

    streams.foreach { it =>
      println(it)
    }

  }

}
