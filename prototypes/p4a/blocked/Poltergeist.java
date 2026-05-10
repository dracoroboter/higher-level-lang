/**
 * ANTIPATTERN: Poltergeist (Gypsy Wagon)
 *
 * Problem: a class that exists only to invoke methods on another class.
 * Has no state, no real logic — just forwarding. Adds indirection
 * without value.
 *
 * Why impossible in HLL: you cannot create a stateless class that
 * just forwards. HLL has struct (must have data) and service (must
 * declare an interface with a provide). A provide with needs is
 * explicit dependency injection, not pointless forwarding.
 * A function that just calls another function is dead code (caught
 * by lava flow detection).
 */
class OrderManager {
    // No state, no logic — just forwards to the real service
    private OrderRepository repo;

    OrderManager(OrderRepository repo) { this.repo = repo; }

    void createOrder(String item) { repo.save(item); }
    void deleteOrder(int id) { repo.delete(id); }
    String getOrder(int id) { return repo.find(id); }
    // Why does this class exist? Just use OrderRepository directly.
}

class OrderRepository {
    void save(String item) { /* actual DB logic */ }
    void delete(int id) { /* actual DB logic */ }
    String find(int id) { return "order_" + id; }
}
