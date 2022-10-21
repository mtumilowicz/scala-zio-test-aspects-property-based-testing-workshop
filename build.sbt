ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "scala-zio-test-aspects-property-based-testing-workshop",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.2",
      "com.beachape" %% "enumeratum" % "1.7.0",
      "eu.timepit" %% "refined" % "0.10.1",
      "dev.zio" %% "zio-test-magnolia" % "2.0.2" % Test,
      "dev.zio" %% "zio-test" % "2.0.2" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
