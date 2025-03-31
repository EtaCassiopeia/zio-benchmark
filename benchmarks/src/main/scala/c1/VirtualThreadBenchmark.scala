package c1

import org.openjdk.jmh.annotations.{Scope => JScope, *}
import zio.{ZIO, Runtime, Unsafe}
import java.util.concurrent.TimeUnit

@State(JScope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(1)
class VirtualThreadBenchmark {

  // Define the unsafeRun utility
  private def unsafeRun[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(zio).getOrThrowFiberFailure()
    }

  var syncMainZio: ZIO[Any, Throwable, Unit] = _
  var syncBlockingZio: ZIO[Any, Throwable, Unit] = _
  var asyncZio: ZIO[Any, Throwable, Unit] = _
  var asyncWithBlockingZio: ZIO[Any, Throwable, Unit] = _

  @Param(Array("10", "50", "100"))
  var concurrency: Int = _

  private val duration = 100L // Duration of each task in milliseconds

  @Setup(Level.Trial)
  def setup(): Unit = {
    val task = VirtualThreadTask.longRunningTask(duration)

    // Synchronous call with ZIO.attempt (main thread pool)
    syncMainZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attempt(VirtualThreadTask.runSync(task))
    )

    // Synchronous call with ZIO.attemptBlocking (blocking thread pool)
    syncBlockingZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attemptBlocking(VirtualThreadTask.runSync(task))
    )

    // Asynchronous call with ZIO.fromCompletionStage
    asyncZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.fromCompletionStage(VirtualThreadTask.runAsync(task))
    )

    // Asynchronous call combined with blocking operation
    asyncWithBlockingZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.fromCompletionStage(VirtualThreadTask.runAsync(task)) *>
        ZIO.attemptBlocking { Thread.sleep(50) } // Additional 50ms blocking operation
    )
  }

  @Benchmark
  def syncOnMain(): Unit = {
    val _ = unsafeRun(syncMainZio)
  }

  @Benchmark
  def syncOnBlocking(): Unit = {
    val _ = unsafeRun(syncBlockingZio)
  }

  @Benchmark
  def async(): Unit = {
    val _ = unsafeRun(asyncZio)
  }

  @Benchmark
  def asyncWithBlocking(): Unit = {
    val _ = unsafeRun(asyncWithBlockingZio)
  }
}
