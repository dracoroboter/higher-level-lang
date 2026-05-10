/**
 * ANTIPATTERN: Busy Waiting / Spin Lock
 *
 * Problem: a thread loops continuously checking a shared flag,
 * wasting CPU cycles while waiting for another thread to change it.
 *
 * Why impossible in HLL: no shared mutable state between actors.
 * An actor waits for messages via its mailbox (managed by runtime).
 * You cannot write `while(!sharedFlag)` because there are no
 * shared variables. The `await` keyword suspends without spinning.
 */
public class BusyWaiting {
    private static volatile boolean ready = false; // shared flag

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            try { Thread.sleep(1000); } catch (Exception e) {}
            ready = true; // signal
        });

        Thread consumer = new Thread(() -> {
            while (!ready) {
                // BUSY WAIT — burns CPU doing nothing useful
                // Could run millions of iterations before ready=true
            }
            System.out.println("Ready!");
        });

        producer.start();
        consumer.start();
    }
}
