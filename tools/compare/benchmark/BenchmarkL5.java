/**
 * Benchmark L5 — Java reference showing iteration antipatterns.
 *
 * Antipatterns demonstrated:
 * 1. ConcurrentModificationException (modify collection while iterating)
 * 2. Off-by-one error (manual index loop)
 * 3. Impure filter/map (I/O inside stream operations)
 * 4. N+1 query in loop (DB call per iteration)
 * 5. Infinite loop (no termination guarantee)
 * 6. Side-effect in stream (mutation inside lambda)
 */

import java.util.*;
import java.util.stream.*;

public class BenchmarkL5 {

    static class Order {
        String id;
        int amount;
        String status;
        Order(String id, int amount, String status) {
            this.id = id; this.amount = amount; this.status = status;
        }
    }

    // --- ANTIPATTERN 1: ConcurrentModification ---
    static void concurrentModification(List<Order> orders) {
        // Modifying the list while iterating — throws at runtime
        for (Order o : orders) {
            if (o.amount < 50) {
                orders.remove(o); // ConcurrentModificationException!
            }
        }
    }

    // --- ANTIPATTERN 2: Off-by-one ---
    static void offByOne(List<Order> orders) {
        // Classic: <= instead of <, or starting at 1 instead of 0
        for (int i = 0; i <= orders.size(); i++) { // BUG: <= causes IndexOutOfBounds
            System.out.println(orders.get(i).id);
        }
    }

    // --- ANTIPATTERN 3: Impure filter/map ---
    static List<String> impureFilter(List<Order> orders) {
        // I/O inside filter — non-deterministic, hard to test, breaks parallelism
        return orders.stream()
                .filter(o -> {
                    System.out.println("Checking: " + o.id); // SIDE EFFECT in filter!
                    return o.amount > 100;
                })
                .map(o -> {
                    // Network call inside map — N+1 problem hidden in stream
                    String details = fetchFromDb(o.id); // I/O in map!
                    return details;
                })
                .collect(Collectors.toList());
    }

    // --- ANTIPATTERN 4: N+1 query ---
    static void nPlusOneQuery(List<Order> orders) {
        // One query per order — should batch
        for (Order o : orders) {
            String detail = fetchFromDb(o.id); // N calls instead of 1 batch
            System.out.println(detail);
        }
    }

    // --- ANTIPATTERN 5: Infinite loop ---
    static void infiniteLoop() {
        int i = 0;
        while (true) { // No termination condition!
            i++;
            if (i > 1000000) break; // "safety" break buried in code
        }
    }

    // --- ANTIPATTERN 6: Side-effect in stream ---
    static int sideEffectInStream(List<Order> orders) {
        // Mutating external state inside a stream — race condition if parallel
        final int[] total = {0};
        orders.stream().forEach(o -> {
            total[0] += o.amount; // MUTATION inside stream!
        });
        return total[0];
    }

    // --- Helper ---
    static String fetchFromDb(String id) {
        return "details_" + id; // simulates DB call
    }

    // --- Correct versions (what HLL enforces) ---
    static List<String> correctFilter(List<Order> orders) {
        // Pure filter + map, I/O separate
        List<Order> big = orders.stream()
                .filter(o -> o.amount > 100)
                .collect(Collectors.toList());
        // I/O in a separate, explicit loop
        List<String> results = new ArrayList<>();
        for (Order o : big) {
            results.add(fetchFromDb(o.id));
        }
        return results;
    }

    static int correctReduce(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.status.equals("confirmed"))
                .mapToInt(o -> o.amount)
                .sum();
    }

    public static void main(String[] args) {
        List<Order> orders = List.of(
                new Order("O1", 50, "pending"),
                new Order("O2", 150, "confirmed"),
                new Order("O3", 200, "confirmed"),
                new Order("O4", 30, "cancelled")
        );

        // Correct usage
        List<String> filtered = correctFilter(orders);
        System.out.println("Filtered: " + filtered);

        int total = correctReduce(orders);
        System.out.println("Total confirmed: " + total);

        // Antipatterns (would crash or misbehave)
        // concurrentModification(new ArrayList<>(orders)); // throws
        // offByOne(orders); // throws IndexOutOfBounds
        System.out.println("Impure: " + impureFilter(orders));
        nPlusOneQuery(orders);
        sideEffectInStream(orders);
    }
}
