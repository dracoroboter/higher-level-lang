/**
 * ANTIPATTERN: Callback Hell
 *
 * Problem: nested callbacks create pyramid of doom.
 * Error handling is scattered, flow is unreadable,
 * each level depends on the previous completing.
 *
 * Why impossible in HLL: no callbacks, no async lambdas.
 * HLL uses sequential flow with `fails` for error handling.
 * Equivalent HLL code would be:
 *   let user = getUser(id) | DBError(e) => fail AppError(e)
 *   let orders = getOrders(user) | DBError(e) => fail AppError(e)
 *   let total = calculateTotal(orders)
 */
interface Callback<T> { void onResult(T result, Exception error); }

class OrderService {
    void getUser(int id, Callback<String> cb) {
        // async operation
        cb.onResult("user_" + id, null);
    }

    void getOrders(String user, Callback<String[]> cb) {
        cb.onResult(new String[]{"order1"}, null);
    }

    void calculateTotal(String[] orders, Callback<Integer> cb) {
        cb.onResult(orders.length * 100, null);
    }

    void processOrder(int userId) {
        getUser(userId, (user, err1) -> {
            if (err1 != null) { System.err.println("Error"); return; }
            getOrders(user, (orders, err2) -> {
                if (err2 != null) { System.err.println("Error"); return; }
                calculateTotal(orders, (total, err3) -> {
                    if (err3 != null) { System.err.println("Error"); return; }
                    System.out.println("Total: " + total);
                    // 4 levels deep, each error handled differently
                });
            });
        });
    }
}
