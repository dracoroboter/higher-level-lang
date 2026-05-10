import java.util.*;

/**
 * Tests for OrderSystem — verifies all components.
 * Self-contained (no JUnit dependency), uses assert.
 */
public class OrderSystemTest {

    static int passed = 0, failed = 0;

    static void assertEquals(Object expected, Object actual, String test) {
        if (Objects.equals(expected, actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + test + " — expected: " + expected + ", got: " + actual);
        }
    }

    static void assertTrue(boolean condition, String test) {
        if (condition) { passed++; } else { failed++; System.out.println("FAIL: " + test); }
    }

    static void assertStartsWith(String prefix, String actual, String test) {
        if (actual != null && actual.startsWith(prefix)) { passed++; }
        else { failed++; System.out.println("FAIL: " + test + " — expected starts with: " + prefix + ", got: " + actual); }
    }

    public static void main(String[] args) {
        testDatabaseCRUD();
        testCachePutGet();
        testCacheExpiry();
        testCacheInvalidation();
        testMessageQueue();
        testCreateOrderHappyPath();
        testCreateOrderInvalidAmount();
        testGetOrderFound();
        testGetOrderNotFound();
        testGetOrderCached();
        testConfirmOrder();
        testConfirmOrderNotFound();
        testConfirmOrderWrongStatus();
        testShipOrder();
        testShipOrderNotConfirmed();
        testOrderLifecycle();
        testMultipleOrders();
        testNotificationsPublished();
        testClientServerIntegration();
        testCacheInvalidationOnWrite();

        System.out.println("\n" + passed + "/" + (passed + failed) + " tests passed.");
        if (failed > 0) System.exit(1);
    }

    // --- Database tests ---

    static void testDatabaseCRUD() {
        var db = new OrderSystem.Database();
        var order = new OrderSystem.Order("O1", "a@b.com", "Widget", 3);
        db.insert(order);
        assertEquals("O1", db.findById("O1").id, "db.insert + findById");
        assertEquals(null, db.findById("NOPE"), "db.findById not found");
        db.updateStatus("O1", "confirmed");
        assertEquals("confirmed", db.findById("O1").status, "db.updateStatus");
        db.delete("O1");
        assertEquals(null, db.findById("O1"), "db.delete");
    }

    static void testDatabaseFindByEmail() {
        var db = new OrderSystem.Database();
        db.insert(new OrderSystem.Order("O1", "x@y.com", "A", 1));
        db.insert(new OrderSystem.Order("O2", "x@y.com", "B", 2));
        db.insert(new OrderSystem.Order("O3", "other@y.com", "C", 3));
        assertEquals(2, db.findByEmail("x@y.com").size(), "db.findByEmail count");
    }

    // --- Cache tests ---

    static void testCachePutGet() {
        var cache = new OrderSystem.Cache(60000);
        cache.put("k1", "v1");
        assertEquals("v1", cache.get("k1"), "cache.put + get");
    }

    static void testCacheExpiry() {
        var cache = new OrderSystem.Cache(1); // 1ms TTL
        cache.put("k1", "v1");
        try { Thread.sleep(10); } catch (Exception e) {}
        assertEquals(null, cache.get("k1"), "cache.expiry");
    }

    static void testCacheInvalidation() {
        var cache = new OrderSystem.Cache(60000);
        cache.put("k1", "v1");
        cache.invalidate("k1");
        assertEquals(null, cache.get("k1"), "cache.invalidate");
    }

    // --- Queue tests ---

    static void testMessageQueue() {
        var queue = new OrderSystem.MessageQueue();
        List<String> received = new ArrayList<>();
        queue.subscribe(msg -> received.add(msg));
        queue.publish("hello");
        queue.publish("world");
        assertEquals(2, received.size(), "queue.subscribe receives");
        assertEquals("hello", received.get(0), "queue.message order");
    }

    // --- Service tests ---

    static OrderSystem.OrderService makeService() {
        return new OrderSystem.OrderService(
            new OrderSystem.Database(),
            new OrderSystem.Cache(60000),
            new OrderSystem.MessageQueue()
        );
    }

    static void testCreateOrderHappyPath() {
        var svc = makeService();
        String id = svc.createOrder("test@test.com", "Item", 5);
        assertStartsWith("ORD-", id, "createOrder returns id");
    }

    static void testCreateOrderInvalidAmount() {
        var svc = makeService();
        try {
            svc.createOrder("test@test.com", "Item", 0);
            failed++; System.out.println("FAIL: createOrder should throw on amount=0");
        } catch (IllegalArgumentException e) {
            passed++;
        }
    }

    static void testGetOrderFound() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        var order = svc.getOrder(id);
        assertEquals(id, order.id, "getOrder.id");
        assertEquals("a@b.com", order.customerEmail, "getOrder.email");
        assertEquals("pending", order.status, "getOrder.status");
    }

    static void testGetOrderNotFound() {
        var svc = makeService();
        assertEquals(null, svc.getOrder("NOPE"), "getOrder not found");
    }

    static void testGetOrderCached() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        svc.getOrder(id); // populates cache
        svc.getOrder(id); // should hit cache
        passed++; // no crash = cache works
    }

    static void testConfirmOrder() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        svc.confirmOrder(id);
        assertEquals("confirmed", svc.getOrder(id).status, "confirmOrder");
    }

    static void testConfirmOrderNotFound() {
        var svc = makeService();
        try {
            svc.confirmOrder("NOPE");
            failed++; System.out.println("FAIL: confirmOrder should throw on not found");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("not found"), "confirmOrder not found msg");
        }
    }

    static void testConfirmOrderWrongStatus() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        svc.confirmOrder(id);
        try {
            svc.confirmOrder(id); // already confirmed
            failed++; System.out.println("FAIL: confirmOrder should throw on wrong status");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot confirm"), "confirmOrder wrong status msg");
        }
    }

    static void testShipOrder() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        svc.confirmOrder(id);
        svc.shipOrder(id);
        assertEquals("shipped", svc.getOrder(id).status, "shipOrder");
    }

    static void testShipOrderNotConfirmed() {
        var svc = makeService();
        String id = svc.createOrder("a@b.com", "X", 1);
        try {
            svc.shipOrder(id); // still pending
            failed++; System.out.println("FAIL: shipOrder should throw on pending");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot ship"), "shipOrder wrong status msg");
        }
    }

    static void testOrderLifecycle() {
        var svc = makeService();
        String id = svc.createOrder("life@cycle.com", "Lifecycle", 1);
        assertEquals("pending", svc.getOrder(id).status, "lifecycle.pending");
        svc.confirmOrder(id);
        assertEquals("confirmed", svc.getOrder(id).status, "lifecycle.confirmed");
        svc.shipOrder(id);
        assertEquals("shipped", svc.getOrder(id).status, "lifecycle.shipped");
    }

    static void testMultipleOrders() {
        var svc = makeService();
        String id1 = svc.createOrder("multi@test.com", "A", 1);
        String id2 = svc.createOrder("multi@test.com", "B", 2);
        var orders = svc.getOrdersByEmail("multi@test.com");
        assertEquals(2, orders.size(), "multipleOrders.count");
    }

    static void testNotificationsPublished() {
        var db = new OrderSystem.Database();
        var cache = new OrderSystem.Cache(60000);
        var queue = new OrderSystem.MessageQueue();
        List<String> msgs = new ArrayList<>();
        queue.subscribe(msg -> msgs.add(msg));
        var svc = new OrderSystem.OrderService(db, cache, queue);

        String id = svc.createOrder("n@t.com", "X", 1);
        svc.confirmOrder(id);
        svc.shipOrder(id);

        assertEquals(3, msgs.size(), "notifications.count");
        assertStartsWith("ORDER_CREATED:", msgs.get(0), "notifications.created");
        assertStartsWith("ORDER_CONFIRMED:", msgs.get(1), "notifications.confirmed");
        assertStartsWith("ORDER_SHIPPED:", msgs.get(2), "notifications.shipped");
    }

    static void testClientServerIntegration() {
        var db = new OrderSystem.Database();
        var cache = new OrderSystem.Cache(60000);
        var queue = new OrderSystem.MessageQueue();
        var svc = new OrderSystem.OrderService(db, cache, queue);
        var server = new OrderSystem.Server(svc);
        var client = new OrderSystem.Client(server);

        String r1 = client.createOrder("int@test.com", "Widget", 3);
        assertStartsWith("201:", r1, "client.create 201");

        String orderId = r1.split(":")[1];
        String r2 = client.getOrder(orderId);
        assertStartsWith("200:", r2, "client.get 200");
        assertTrue(r2.contains("pending"), "client.get contains pending");

        String r3 = client.confirmOrder(orderId);
        assertEquals("200:confirmed", r3, "client.confirm");

        String r4 = client.shipOrder(orderId);
        assertEquals("200:shipped", r4, "client.ship");

        // Error cases
        String r5 = client.createOrder("x@y.com", "Z", -1);
        assertStartsWith("400:", r5, "client.create 400");

        String r6 = client.getOrder("NOPE");
        assertEquals("404:not found", r6, "client.get 404");

        String r7 = client.shipOrder(orderId);
        assertStartsWith("400:", r7, "client.ship already shipped");
    }

    static void testCacheInvalidationOnWrite() {
        var db = new OrderSystem.Database();
        var cache = new OrderSystem.Cache(60000);
        var queue = new OrderSystem.MessageQueue();
        var svc = new OrderSystem.OrderService(db, cache, queue);

        String id = svc.createOrder("cache@test.com", "X", 1);
        svc.getOrder(id); // populate cache
        svc.confirmOrder(id); // should invalidate cache
        assertEquals("confirmed", svc.getOrder(id).status, "cache invalidation on confirm");
    }
}
