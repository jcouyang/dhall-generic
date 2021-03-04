lazy val root = (project in file("."))
  .settings(
    name := "Dhall Generic SBT",
    libraryDependencies ++= Seq("us.oyanglul" %% "dhall-generic" % "0.3.15")
  )
