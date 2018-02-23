lazy val commonSettings = Seq(
  organization := "com.github.amazingdreams",
  scalaVersion := "2.12.4",
  version      := "0.1.0-SNAPSHOT"
)

lazy val root = (project in file("."))
  .aggregate(playScheduler, example)

lazy val playScheduler = (project in file("play-scheduler"))
  .settings(
    commonSettings,
    name := "play-scheduler",
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "4.1.0",

      "org.mockito" % "mockito-core" % "2.15.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    ),
    PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value
  ).enablePlugins(
    PlayScala
  ).disablePlugins(
    PlayLayoutPlugin
  )

lazy val example = (project in file("example"))
  .settings(
    commonSettings,
    name := "play-scheduler-example"
  ).enablePlugins(
    PlayScala
  )
  .dependsOn(playScheduler)
