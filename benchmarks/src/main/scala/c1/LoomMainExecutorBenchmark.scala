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
class LoomMainExecutorBenchmark {

  private var runtime: Runtime.Scoped[Any] = _

  /** Set up the runtime with the Loom-based main executor once per trial. */
  @Setup(Level.Trial)
  def setup(): Unit = {
    Unsafe.unsafe { implicit unsafe =>
      runtime = Runtime.unsafe.fromLayer(Runtime.enableLoomBasedExecutor)
    }
  }

  /** Shut down the runtime to release resources after the trial. */
  @TearDown(Level.Trial)
  def tearDown(): Unit = {
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.shutdown()
    }
  }

  /** Helper to run ZIO effects synchronously. */
  private def unsafeRun[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(zio).getOrThrowFiberFailure()
    }

  @Param(Array("10", "50", "100"))
  var concurrency: Int = _

  private val duration = 100L

  /** ZIO effect using ZIO.attempt on the main executor. */
  private def syncMainZio(concurrency: Int): ZIO[Any, Throwable, Unit] = {
    val task = VirtualThreadTask.longRunningTask(duration)
    ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attempt(VirtualThreadTask.runSync(task))
    )
  }

  /** ZIO effect using CompletionStage for comparison. */
  private def asyncZio(concurrency: Int): ZIO[Any, Throwable, Unit] = {
    val task = VirtualThreadTask.longRunningTask(duration)
    ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.fromCompletionStage(VirtualThreadTask.runAsync(task))
    )
  }

  @Benchmark
  def syncOnLoomMain(): Unit = {
    unsafeRun(syncMainZio(concurrency))
  }

  @Benchmark
  def asyncWithDefault(): Unit = {
    unsafeRun(asyncZio(concurrency))
  }
}
