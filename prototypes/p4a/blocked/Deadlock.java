/**
 * ANTIPATTERN: Deadlock
 *
 * Problem: two threads each hold a lock the other needs.
 * Both wait forever — the program hangs.
 *
 * Why impossible in HLL: no locks, no mutex, no synchronized.
 * Actors communicate via messages (non-blocking send).
 * The DAG constraint on module dependencies prevents circular
 * communication patterns that could cause message-based deadlock.
 */
public class Deadlock {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                try { Thread.sleep(100); } catch (Exception e) {}
                synchronized (lockB) { // waits for t2 to release lockB
                    System.out.println("t1 done");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                try { Thread.sleep(100); } catch (Exception e) {}
                synchronized (lockA) { // waits for t1 to release lockA
                    System.out.println("t2 done");
                }
            }
        });

        t1.start(); t2.start();
        // Program hangs forever — deadlock
    }
}
