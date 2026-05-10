// 08: Java compiles — instantiating a service directly instead of injecting
class ServiceInstantiation {
    interface Logger { void log(String msg); }
    static class ConsoleLogger implements Logger {
        public void log(String msg) { System.out.println(msg); }
    }
    static class UserService {
        Logger log = new ConsoleLogger(); // DIRECT instantiation — not injected!
        // Cannot swap for testing, tightly coupled
    }
    public static void main(String[] args) {
        new UserService().log.log("hello"); // works but untestable
    }
}
