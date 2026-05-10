import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * OrderSystem — A complete Java application with:
 * - REST-like server (simulated with method calls)
 * - Client that calls the server
 * - In-memory database (HashMap)
 * - Cache with TTL and invalidation
 * - Message queue for async events (notifications)
 * - Full error handling
 */
public class OrderSystem {

    // ===== Domain Types (just Strings in Java — no nominal safety) =====

    static class Order {
        String id;
        String customerEmail;
        String product;
        int amount;
        String status; // "pending", "confirmed", "shipped"

        Order(String id, String customerEmail, String product, int amount) {
            this.id = id;
            this.customerEmail = customerEmail;
            this.product = product;
            this.amount = amount;
            this.status = "pending";
        }
    }

    // ===== Database (in-memory) =====

    static class Database {
        private final Map<String, Order> orders = new HashMap<>();

        void insert(Order order) {
            orders.put(order.id, order);
        }

        Order findById(String id) {
            return orders.get(id); // returns null if not found!
        }

        List<Order> findByEmail(String email) {
            List<Order> result = new ArrayList<>();
            for (Order o : orders.values()) {
                if (o.customerEmail.equals(email)) result.add(o);
            }
            return result;
        }

        void updateStatus(String id, String status) {
            Order o = orders.get(id);
            if (o != null) o.status = status;
            // silently does nothing if not found
        }

        void delete(String id) {
            orders.remove(id);
        }
    }

    // ===== Cache =====

    static class Cache {
        private final Map<String, Object> store = new HashMap<>();
        private final Map<String, Long> expiry = new HashMap<>();
        private final long ttlMs;

        Cache(long ttlMs) { this.ttlMs = ttlMs; }

        void put(String key, Object value) {
            store.put(key, value);
            expiry.put(key, System.currentTimeMillis() + ttlMs);
        }

        Object get(String key) {
            Long exp = expiry.get(key);
            if (exp == null || System.currentTimeMillis() > exp) {
                store.remove(key);
                expiry.remove(key);
                return null;
            }
            return store.get(key);
        }

        void invalidate(String key) {
            store.remove(key);
            expiry.remove(key);
        }
    }

    // ===== Message Queue =====

    static class MessageQueue {
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private final List<Consumer<String>> listeners = new ArrayList<>();

        void publish(String message) {
            queue.offer(message);
            // Notify listeners synchronously (simplified)
            for (Consumer<String> listener : listeners) {
                listener.accept(message);
            }
        }

        void subscribe(Consumer<String> listener) {
            listeners.add(listener);
        }

        String poll() {
            return queue.poll();
        }
    }

    // ===== Service (business logic) =====

    static class OrderService {
        private final Database db;
        private final Cache cache;
        private final MessageQueue queue;
        private int nextId = 1;

        OrderService(Database db, Cache cache, MessageQueue queue) {
            this.db = db;
            this.cache = cache;
            this.queue = queue;
        }

        String createOrder(String email, String product, int amount) {
            // No validation on email! Could be anything.
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
            String id = "ORD-" + (nextId++);
            Order order = new Order(id, email, product, amount);
            db.insert(order);
            cache.invalidate("orders:" + email);
            queue.publish("ORDER_CREATED:" + id);
            return id;
        }

        Order getOrder(String id) {
            // Check cache first
            Object cached = cache.get("order:" + id);
            if (cached != null) return (Order) cached;
            // DB lookup
            Order order = db.findById(id);
            if (order != null) cache.put("order:" + id, order);
            return order; // may return null!
        }

        List<Order> getOrdersByEmail(String email) {
            Object cached = cache.get("orders:" + email);
            if (cached != null) return (List<Order>) cached;
            List<Order> orders = db.findByEmail(email);
            cache.put("orders:" + email, orders);
            return orders;
        }

        void confirmOrder(String id) {
            Order order = db.findById(id);
            if (order == null) throw new RuntimeException("Order not found: " + id);
            if (!order.status.equals("pending"))
                throw new RuntimeException("Cannot confirm order in status: " + order.status);
            db.updateStatus(id, "confirmed");
            cache.invalidate("order:" + id);
            queue.publish("ORDER_CONFIRMED:" + id);
        }

        void shipOrder(String id) {
            Order order = db.findById(id);
            if (order == null) throw new RuntimeException("Order not found: " + id);
            if (!order.status.equals("confirmed"))
                throw new RuntimeException("Cannot ship order in status: " + order.status);
            db.updateStatus(id, "shipped");
            cache.invalidate("order:" + id);
            queue.publish("ORDER_SHIPPED:" + id);
        }
    }

    // ===== Server (REST-like endpoint simulation) =====

    static class Server {
        private final OrderService service;

        Server(OrderService service) { this.service = service; }

        String handleCreateOrder(String email, String product, int amount) {
            try {
                String id = service.createOrder(email, product, amount);
                return "201:" + id;
            } catch (IllegalArgumentException e) {
                return "400:" + e.getMessage();
            } catch (Exception e) {
                return "500:" + e.getMessage();
            }
        }

        String handleGetOrder(String id) {
            Order order = service.getOrder(id);
            if (order == null) return "404:not found";
            return "200:" + order.id + "," + order.customerEmail + "," + order.product + "," + order.amount + "," + order.status;
        }

        String handleConfirmOrder(String id) {
            try {
                service.confirmOrder(id);
                return "200:confirmed";
            } catch (RuntimeException e) {
                return "400:" + e.getMessage();
            }
        }

        String handleShipOrder(String id) {
            try {
                service.shipOrder(id);
                return "200:shipped";
            } catch (RuntimeException e) {
                return "400:" + e.getMessage();
            }
        }
    }

    // ===== Client =====

    static class Client {
        private final Server server;

        Client(Server server) { this.server = server; }

        String createOrder(String email, String product, int amount) {
            return server.handleCreateOrder(email, product, amount);
        }

        String getOrder(String id) {
            return server.handleGetOrder(id);
        }

        String confirmOrder(String id) {
            return server.handleConfirmOrder(id);
        }

        String shipOrder(String id) {
            return server.handleShipOrder(id);
        }
    }

    // ===== Main =====

    public static void main(String[] args) {
        Database db = new Database();
        Cache cache = new Cache(60000);
        MessageQueue queue = new MessageQueue();
        List<String> notifications = new ArrayList<>();
        queue.subscribe(msg -> notifications.add(msg));

        OrderService service = new OrderService(db, cache, queue);
        Server server = new Server(service);
        Client client = new Client(server);

        // Happy path
        String r1 = client.createOrder("alice@example.com", "Laptop", 1);
        System.out.println(r1);
        String orderId = r1.split(":")[1];

        String r2 = client.getOrder(orderId);
        System.out.println(r2);

        String r3 = client.confirmOrder(orderId);
        System.out.println(r3);

        String r4 = client.shipOrder(orderId);
        System.out.println(r4);

        // Error cases
        String r5 = client.createOrder("bob@test.com", "Phone", -1);
        System.out.println(r5);

        String r6 = client.getOrder("NONEXISTENT");
        System.out.println(r6);

        String r7 = client.shipOrder(orderId); // already shipped
        System.out.println(r7);

        // Notifications
        System.out.println("Notifications: " + notifications.size());
        for (String n : notifications) System.out.println("  " + n);
    }
}
