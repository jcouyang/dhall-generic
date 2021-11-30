lazy val root = (project in file("."))
  .settings(
    name := "Dhall Generic SBT",
    libraryDependencies ++= Seq("us.oyanglul" %% "dhall-generic" % "398f3fe3")
  )
