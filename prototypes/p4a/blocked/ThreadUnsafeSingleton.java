/**
 * ANTIPATTERN: Thread Unsafe Singleton
 *
 * Problem: lazy initialization without synchronization.
 * Two threads can create two instances, violating the singleton
 * guarantee. Global mutable state is accessible from anywhere.
 *
 * Why impossible in HLL: no static fields, no global state,
 * no mutable top-level variables. Services are injected by the
 * module system — their lifecycle is managed by the compiler,
 * not by manual getInstance() patterns.
 */
class DatabaseConnection {
    private static DatabaseConnection instance; // global mutable state
    private String url;

    private DatabaseConnection(String url) { this.url = url; }

    // Thread unsafe: two threads can enter simultaneously
    public static DatabaseConnection getInstance() {
        if (instance == null) { // race condition here
            instance = new DatabaseConnection("jdbc:postgres://localhost/db");
        }
        return instance;
    }

    public String query(String sql) { return "result"; }
}

class UserService {
    public String getUser(int id) {
        // Hidden dependency on global state
        return DatabaseConnection.getInstance().query("SELECT * FROM users WHERE id=" + id);
    }
}
