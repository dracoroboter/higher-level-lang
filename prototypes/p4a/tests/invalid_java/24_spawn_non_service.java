// 24: Java compiles — can create a thread with any Runnable (no type safety)
class SpawnNonService {
    static class Config { String name = "prod"; }
    public static void main(String[] args) {
        // You can put anything in a thread — no compile-time check
        // that it's designed for concurrent access
        new Thread(() -> {
            Config c = new Config(); // not thread-safe by design
            System.out.println(c.name);
        }).start();
    }
}
