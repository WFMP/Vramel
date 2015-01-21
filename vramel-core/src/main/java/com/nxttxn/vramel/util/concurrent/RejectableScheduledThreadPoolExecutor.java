package com.nxttxn.vramel.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Scheduled thread pool executor that creates {@link RejectableFutureTask} instead of
 * {@link java.util.concurrent.FutureTask} when registering new tasks for execution.
 * <p/>
 * Instances of {@link RejectableFutureTask} are required to handle {@link org.apache.camel.ThreadPoolRejectedPolicy#Discard}
 * and {@link org.apache.camel.ThreadPoolRejectedPolicy#DiscardOldest} policies correctly, e.g. notify
 * {@link java.util.concurrent.Callable} and {@link Runnable} tasks when they are rejected.
 * To be notified of rejection tasks have to implement {@link org.apache.camel.Rejectable} interface: <br/>
 * <code><pre>
 * public class RejectableTask implements Runnable, Rejectable {
 *     &#064;Override
 *     public void run() {
 *         // execute task
 *     }
 *     &#064;Override
 *     public void reject() {
 *         // do something useful on rejection
 *     }
 * }
 * </pre></code>
 * <p/>
 * If the task does not implement {@link org.apache.camel.Rejectable} interface the behavior is exactly the same as with
 * ordinary {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
 *
 * @see RejectableFutureTask
 * @see org.apache.camel.Rejectable
 * @see RejectableThreadPoolExecutor
 */
public class RejectableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public RejectableScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public RejectableScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public RejectableScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public RejectableScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new RejectableFutureTask<T>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new RejectableFutureTask<T>(callable);
    }

    @Override
    public String toString() {
        // the thread factory often have more precise details what the thread pool is used for
        if (getThreadFactory() instanceof CamelThreadFactory) {
            String name = ((CamelThreadFactory) getThreadFactory()).getName();
            return super.toString() + "[" + name + "]";
        } else {
            return super.toString();
        }
    }

}
