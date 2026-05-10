/**
 * BenchmarkL3.java — Reference oracle for temporal coupling antipatterns.
 * 
 * Antipatterns present:
 * - Use before open (send before connect)
 * - Use after close (send after disconnect)
 * - Double close
 * - Resource leak (open without close)
 * - No compile-time protection — all errors are runtime
 */
public class BenchmarkL3 {

    // Simulated connection with state — no compile-time enforcement
    static class Connection {
        enum State { DISCONNECTED, CONNECTED, CLOSED }
        State state = State.DISCONNECTED;

        void connect() {
            // No check — antipattern: can connect when already connected
            state = State.CONNECTED;
            System.out.println("connected");
        }

        void send(String data) {
            // No check — antipattern: can send when disconnected or closed
            if (state != State.CONNECTED) {
                throw new IllegalStateException("Cannot send in state " + state);
            }
            System.out.println("sent: " + data);
        }

        void disconnect() {
            // No check — antipattern: can disconnect when already closed
            state = State.CLOSED;
            System.out.println("disconnected");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java BenchmarkL3 <scenario>");
            return;
        }

        Connection conn = new Connection();
        String scenario = args[0];

        switch (scenario) {
            case "happy" -> {
                conn.connect();
                conn.send("hello");
                conn.send("world");
                conn.disconnect();
                System.out.println("done");
            }
            case "use_before_open" -> {
                // BUG: send before connect — runtime error
                conn.send("hello");
            }
            case "use_after_close" -> {
                conn.connect();
                conn.disconnect();
                // BUG: send after disconnect — runtime error
                conn.send("hello");
            }
            case "double_close" -> {
                conn.connect();
                conn.disconnect();
                // BUG: disconnect again — no error in Java (silent bug)
                conn.disconnect();
                System.out.println("double close: no error in Java");
            }
            case "leak" -> {
                conn.connect();
                conn.send("hello");
                // BUG: never disconnected — resource leak, no error
                System.out.println("leaked: no error in Java");
            }
        }
    }
}
