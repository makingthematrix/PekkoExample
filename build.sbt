ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.2"

lazy val root = (project in file("."))
  .settings(
    name := "PekkoExample",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % "1.2.0",
      "ch.qos.logback" % "logback-classic" % "1.5.18"
    )
  )
