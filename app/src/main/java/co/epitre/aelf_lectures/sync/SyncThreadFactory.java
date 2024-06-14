package co.epitre.aelf_lectures.sync;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fork of Executors.DefaultThreadFactory supporting priorities. The goal is to lower the priority
 * of the sync job with respect to, say, the UI (and WebView) threads.
 */
class SyncThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final int priority;

    SyncThreadFactory(int priority) {
        this.priority = priority;
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "sync-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(
                group,
                runnable,
                namePrefix + threadNumber.getAndIncrement(),
                0
        );

        t.setDaemon(false);
        t.setPriority(priority);

        return t;
    }
}
