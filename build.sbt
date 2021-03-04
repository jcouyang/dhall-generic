val scala212 = "2.12.13"
val scala213 = "2.13.3"
val scala3 = "3.0.0-RC1"
lazy val supportedScalaVersions = List(scala213, scala212, scala3)

inScope(Scope.GlobalScope)(
  List(
    organization := "us.oyanglul",
    licenses := Seq(
      "Apache License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/jcouyang/dhall-generic")),
    developers := List(
      Developer(
        "jcouyang",
        "Jichao Ouyang",
        "oyanglulu+dhallgeneric@gmail.com",
        url("https://github.com/jcouyang")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/jcouyang/dhall-generic"),
        "scm:git@github.com:jcouyang/dhall-generic.git"
      )
    ),
    pgpPublicRing := file(".") / ".gnupg" / "pubring.asc",
    pgpSecretRing := file(".") / ".gnupg" / "secring.asc",
    releaseEarlyWith := SonatypePublisher,
    scalaVersion := scala213,
    version := "0.1.0-SNAPSHOT",
    organization := "us.oyanglul",
    organizationName := "blog.oyanglul.us"
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "Dhall Generic",
    crossScalaVersions := supportedScalaVersions,
    //    scalacOptions += "-Xlog-implicits",
    libraryDependencies ++= dhall.load.modules.map { case Module(o, n, v) =>
      o %% n % v
    }.map(_.withDottyCompat(scalaVersion.value)),
    libraryDependencies ++= (if (scalaVersion.value == scala3) Seq() else Seq("com.chuusai" %% "shapeless" % "2.4.0-M1")),
    libraryDependencies += ("org.scalameta" %% "munit" % "0.7.22" % Test).withDottyCompat(scalaVersion.value),
    testFrameworks += new TestFramework("munit.Framework")
  )
