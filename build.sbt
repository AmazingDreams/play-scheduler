lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "play-scheduler",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "4.1.0",

      "org.mockito" % "mockito-core" % "2.15.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    )
  ).enablePlugins(
    PlayScala
  ).disablePlugins(
    PlayLayoutPlugin
  )

PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value