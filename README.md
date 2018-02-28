# Play Scheduler

Safely schedule tasks in Play Framework.

**USE THE LIBRARY ON YOUR OWN RISK**

## Usage
Currently only a snapshot version is available:

```sbtshell
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.github.amazingdreams" %% "play-scheduler" % "0.1.0-SNAPSHOT"
```

Enable the module in `application.conf` and add some tasks:

```
play.modules.enabled += com.github.amazingdreams.play.scheduler.module.PlaySchedulerModule

play.scheduler {
  tasks = [
    {
      task = tasks.LongRunningTask
      interval = 2 minutes
    }
  ]
}
```

This is an example of a task

```scala
class LongRunningTask @Inject()()(implicit ec: ExecutionContext)
  extends SchedulerTask {

  override def run(): Future[String] = Future {
    Thread.sleep(1000 * 5 * 60)
    "OK"
  }
}
```

## Persistence

The Play Scheduler uses an in memory persistence provider.
It's possible to provide your own persistence class using the `PlaySchedulerPersistence` trait.

For instance:

```scala
package tasks.persistence

class MyCustomPersistence extends PlaySchedulerPersistence {
  // Custom persistence logic
}
```

Then set the configuration property

```
play.scheduler.persistence = tasks.persistence.MyCustomPersistence
```


## Clustering

It's possible to use the scheduler as an akka cluster singleton.

```
play.scheduler.cluster = true
```
