package c1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VirtualThreadTask {

    // Executor for virtual threads
    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // Synchronous method: runs task on virtual thread and waits for result
    public static <T> T runSync(Callable<T> task) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, VIRTUAL_EXECUTOR).get();
    }

    // Asynchronous method: returns CompletionStage for task on virtual thread
    public static <T> CompletionStage<T> runAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, VIRTUAL_EXECUTOR);
    }

    // Simulate a long-running task (e.g., sleep for duration ms)
    public static Callable<String> longRunningTask(long duration) {
        return () -> {
            Thread.sleep(duration);
            return "Completed after " + duration + " ms";
        };
    }

    // Define Callable interface (normally in java.util.concurrent, included here for completeness)
    public interface Callable<V> {
        V call() throws Exception;
    }
}
