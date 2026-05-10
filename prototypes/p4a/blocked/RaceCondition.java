/**
 * ANTIPATTERN: Race Condition
 *
 * Problem: two threads modify shared state without synchronization.
 * The final value depends on timing — non-deterministic behavior.
 *
 * Why impossible in HLL: Actor model. Each actor has private state.
 * Communication only via immutable messages. `spawn` creates isolated
 * actors. Aliasing of actors is forbidden (compile-time check).
 * No shared mutable state exists in the language.
 */
class Counter {
    private int value = 0; // shared mutable state

    void increment() { value++; } // not atomic!

    int get() { return value; }
}

public class RaceCondition {
    public static void main(String[] args) throws Exception {
        Counter counter = new Counter(); // shared between threads

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) counter.increment();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) counter.increment();
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        // Expected: 20000. Actual: unpredictable (race condition)
        System.out.println(counter.get());
    }
}
