name := "astar"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.2.1",
  "com.typesafe.play" %% "play-json" % "2.5.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "commons-validator" % "commons-validator" % "1.4.0",
  "joda-time" % "joda-time" % "2.7",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "com.mchange" % "c3p0" % "0.9.5",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "junit" % "junit" % "4.12" % "test",
  "org.flywaydb" % "flyway-core" % "3.2.1",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)
