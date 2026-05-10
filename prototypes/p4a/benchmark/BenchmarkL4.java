/**
 * Benchmark L4 — Java reference showing module system antipatterns.
 *
 * Antipatterns demonstrated:
 * 1. Circular dependency (A depends on B, B depends on A)
 * 2. God class (one class does everything)
 * 3. Hidden dependency (uses global/static state)
 * 4. Service Locator (runtime lookup instead of injection)
 * 5. Tight coupling (depends on concrete class, not interface)
 * 6. Singleton abuse (global mutable state)
 */

// --- ANTIPATTERN 1: Circular dependency ---
// In Java, this compiles fine. In HLL p4a, the DAG check rejects it.

class OrderService {
    private PaymentService payment = new PaymentService(); // depends on Payment
    public void placeOrder(String item) {
        payment.charge(item, 100);
    }
    public String getOrderStatus() { return "placed"; }
}

class PaymentService {
    private OrderService orders = new OrderService(); // depends on Order → CYCLE!
    public void charge(String item, int amount) {
        orders.getOrderStatus(); // circular call
    }
}

// --- ANTIPATTERN 2: God class ---
// One class handles auth, db, email, logging — no separation of concerns.

class AppManager {
    private static AppManager instance; // also singleton
    private String dbUrl = "jdbc:...";
    private String smtpHost = "smtp.example.com";

    public static AppManager getInstance() {
        if (instance == null) instance = new AppManager();
        return instance;
    }

    public boolean authenticate(String user, String pass) {
        // direct DB access
        return user.equals("admin") && pass.equals("secret");
    }

    public void sendEmail(String to, String body) {
        // direct SMTP access
        System.out.println("Sending to " + to + ": " + body);
    }

    public void log(String msg) {
        System.out.println("[LOG] " + msg);
    }

    public String queryDb(String sql) {
        return "result";
    }
}

// --- ANTIPATTERN 3: Hidden dependency ---
// The function uses a global singleton — caller can't see or control the dependency.

class ReportGenerator {
    public String generate(String reportId) {
        // Hidden: depends on AppManager singleton
        AppManager.getInstance().log("Generating " + reportId);
        String data = AppManager.getInstance().queryDb("SELECT * FROM reports");
        return "Report: " + data;
    }
}

// --- ANTIPATTERN 4: Service Locator ---
// Runtime lookup — if the service isn't registered, you get a runtime error.

class ServiceLocator {
    private static java.util.Map<String, Object> services = new java.util.HashMap<>();

    public static void register(String name, Object service) {
        services.put(name, service);
    }

    public static Object lookup(String name) {
        Object svc = services.get(name);
        if (svc == null) throw new RuntimeException("Service not found: " + name);
        return svc;
    }
}

class NotificationService {
    public void notify(String userId, String msg) {
        // Runtime lookup — no compile-time guarantee
        Object emailSvc = ServiceLocator.lookup("emailService");
        System.out.println("Notifying " + userId + ": " + msg);
    }
}

// --- ANTIPATTERN 5: Tight coupling ---
// Depends on concrete PostgresRepo, not an interface.

class PostgresRepo {
    public String find(int id) { return "user_" + id; }
}

class UserController {
    private PostgresRepo repo = new PostgresRepo(); // tight coupling!

    public String getUser(int id) {
        return repo.find(id);
    }
    // Can't swap to MockRepo for testing without changing this class
}

// --- ANTIPATTERN 6: Singleton abuse ---
// Global mutable state accessible from anywhere.

class Config {
    private static Config instance;
    private java.util.Map<String, String> values = new java.util.HashMap<>();

    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    public void set(String key, String value) { values.put(key, value); }
    public String get(String key) { return values.getOrDefault(key, ""); }
}

// --- Main: demonstrates the problems ---

public class BenchmarkL4 {
    public static void main(String[] args) {
        // Setup singleton state (hidden, global)
        Config.getInstance().set("env", "production");
        ServiceLocator.register("emailService", "fake");

        // God class does everything
        AppManager app = AppManager.getInstance();
        app.authenticate("admin", "secret");
        app.sendEmail("user@test.com", "Hello");
        app.log("Started");

        // Hidden dependency — can't test without the singleton
        ReportGenerator rg = new ReportGenerator();
        System.out.println(rg.generate("R001"));

        // Service locator — fails at runtime if not registered
        NotificationService ns = new NotificationService();
        ns.notify("U001", "Welcome");

        // Tight coupling — can't mock
        UserController uc = new UserController();
        System.out.println(uc.getUser(42));

        // Circular dependency — stack overflow risk
        // OrderService os = new OrderService(); // would overflow!
        System.out.println("Circular dependency: would overflow if instantiated");
    }
}
