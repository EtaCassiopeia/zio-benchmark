package c1

import org.openjdk.jmh.annotations.{Scope as JScope, *}
import zio.{Runtime, Unsafe, ZIO}

import java.util.concurrent.TimeUnit

@State(JScope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(1)
class BlockingBenchmark {

  // Define the unsafeRun utility
  private def unsafeRun[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(zio).getOrThrowFiberFailure()
    }

  var blockingMainZio: ZIO[Any, Throwable, Unit] = _
  var blockingSeparateZio: ZIO[Any, Throwable, Unit] = _

  @Param(Array("10", "50", "100"))
  var concurrency: Int = _

  private val duration = 100L // Duration of each blocking operation in milliseconds

  @Setup(Level.Trial)
  def setup(): Unit = {
    // Blocking operation on the main thread pool
    blockingMainZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attempt { Thread.sleep(duration) }
    )

    // Blocking operation on the separate blocking thread pool
    blockingSeparateZio = ZIO.foreachParDiscard(1 to concurrency)(_ =>
      ZIO.attemptBlocking { Thread.sleep(duration) }
    )
  }

  @Benchmark
  def blockingOnMain(): Unit = {
    val _ = unsafeRun(blockingMainZio)
  }

  @Benchmark
  def blockingOnSeparate(): Unit = {
    val _ = unsafeRun(blockingSeparateZio)
  }
}