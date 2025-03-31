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
class LoomBlockingExecutorBenchmark {

  private var runtime: Runtime.Scoped[Any] = _

  /** Set up the runtime with the Loom-based blocking executor once per trial. */
  @Setup(Level.Trial)
  def setup(): Unit = {
    Unsafe.unsafe { implicit unsafe =>
      runtime = Runtime.unsafe.fromLayer(Runtime.enableLoomBasedBlockingExecutor)
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

  /** ZIO effect using ZIO.attemptBlocking on the blocking executor. */
  private def syncBlockingZio(concurrency: Int): ZIO[Any, Throwable, Unit] = {
    val task = VirtualThreadTask.longRunningTask(duration)
    ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attemptBlocking(VirtualThreadTask.runSync(task))
    )
  }

  /** ZIO effect combining CompletionStage and ZIO.attemptBlocking. */
  private def asyncWithBlockingZio(concurrency: Int): ZIO[Any, Throwable, Unit] = {
    val task = VirtualThreadTask.longRunningTask(duration)
    ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.fromCompletionStage(VirtualThreadTask.runAsync(task)) *>
        ZIO.attemptBlocking { Thread.sleep(50) }
    )
  }

  @Benchmark
  def syncOnLoomBlocking(): Unit = {
    unsafeRun(syncBlockingZio(concurrency))
  }

  @Benchmark
  def asyncWithLoomBlocking(): Unit = {
    unsafeRun(asyncWithBlockingZio(concurrency))
  }
}
