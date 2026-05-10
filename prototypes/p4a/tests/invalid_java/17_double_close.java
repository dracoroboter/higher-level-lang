// 17: Java compiles — double close
class DoubleClose {
    static class Connection {
        void connect() {}
        void close() { System.out.println("closing"); }
    }
    public static void main(String[] args) {
        Connection conn = new Connection();
        conn.connect();
        conn.close();
        conn.close(); // DOUBLE CLOSE — Java compiles, potential resource corruption
    }
}
