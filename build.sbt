version := "0.1"

scalaVersion := "2.13.12"

val akkaVersion      = "2.8.8"
val akkaHttpVersion  = "10.5.3"
val scalaTestVersion = "3.2.19"
val jwtVersion       = "9.4.4"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  // Akka HTTP
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,

  // Testing
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,

  // JWT (NEW coordinates)
  "com.github.jwt-scala" %% "jwt-spray-json" % jwtVersion
)
