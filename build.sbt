ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "us.oyanglul"
ThisBuild / organizationName := "blog.oyanglul.us"

lazy val root = (project in file("."))
  .settings(
    name := "Dhall Scala Generic",
    libraryDependencies ++= Seq(
      "org.dhallj" %% "dhall-scala" % "0.4.0",
      "org.dhallj" %% "dhall-scala-codec" % "0.4.0",
      "com.chuusai" %% "shapeless" % "2.4.0-M1"
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.14" % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
