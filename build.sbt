name := """auth-test"""
organization := "com.serli"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

libraryDependencies += guice
libraryDependencies += jdbc
libraryDependencies += ws
libraryDependencies += caffeine
libraryDependencies += evolutions

libraryDependencies += "com.h2database" % "h2" % "1.4.197"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.4"


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.serli.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.serli.binders._"
