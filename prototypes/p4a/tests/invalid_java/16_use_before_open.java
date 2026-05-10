// 16: Java compiles — use before open
class UseBeforeOpen {
    static class Connection {
        boolean open = false;
        void connect() { open = true; }
        void send(String data) { System.out.println("Sending: " + data); }
    }
    public static void main(String[] args) {
        Connection conn = new Connection();
        conn.send("data"); // USE BEFORE OPEN — Java compiles, silent bug
    }
}
