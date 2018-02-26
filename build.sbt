lazy val commonSettings = Seq(
  organization := "com.github.amazingdreams",
  scalaVersion := "2.12.4",
  version      := "0.1.0-SNAPSHOT"
)

lazy val root = (project in file("."))
  .settings(skip in publish := true)
  .aggregate(playScheduler)

lazy val playScheduler = (project in file("play-scheduler"))
  .settings(
    commonSettings,
    name := "play-scheduler",
    libraryDependencies ++= Seq(
      guice,

      "org.mockito" % "mockito-core" % "2.15.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    ),
    PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value,
    homepage := Some(url("https://github.com/AmazingDreams/play-scheduler")),
    scmInfo := Some(ScmInfo(url("https://github.com/AmazingDreams/play-scheduler"),
                                "git@github.com:AmazingDreams/play-scheduler.git")),
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    publishMavenStyle := true,

    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    )
  ).enablePlugins(
    PlayScala
  ).disablePlugins(
    PlayLayoutPlugin
  )

lazy val example = (project in file("play-scheduler-example"))
  .settings(
    commonSettings,
    name := "play-scheduler-example",
    libraryDependencies ++= Seq(
      guice
    ),
    PlayKeys.externalizeResources := false
  ).enablePlugins(
    PlayScala
  )
  .dependsOn(playScheduler)
