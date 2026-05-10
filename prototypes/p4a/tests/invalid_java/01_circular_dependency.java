// 01: Java compiles — circular dependency between classes
class CircularA {
    CircularB b = new CircularB(); // depends on B
    String getStatus() { return "A"; }
}
class CircularB {
    CircularA a = new CircularA(); // depends on A — CYCLE! StackOverflow at runtime
    String getStatus() { return a.getStatus(); }
}
