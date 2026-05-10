// 06: Java compiles — use after close
class UseAfterClose {
    static class Connection {
        boolean open = false;
        void connect() { open = true; }
        void send(String data) { System.out.println("Sending: " + data); }
        void close() { open = false; }
    }
    public static void main(String[] args) {
        Connection conn = new Connection();
        conn.connect();
        conn.close();
        conn.send("data"); // USE AFTER CLOSE — Java compiles fine, bug at runtime
    }
}
