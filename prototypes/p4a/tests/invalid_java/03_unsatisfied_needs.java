// 03: Java compiles — dependency not provided (NPE at runtime)
class UnsatisfiedNeeds {
    interface Cache { String get(String key); }
    static class AppService {
        Cache cache; // not initialized! Java doesn't enforce injection
        void run() { cache.get("key"); } // NPE at runtime
    }
    public static void main(String[] args) {
        new AppService().run(); // crashes — cache is null
    }
}
