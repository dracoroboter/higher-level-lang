// 25: Java compiles — shared mutable reference (actor aliasing equivalent)
class ActorAliasing {
    static class Worker { int state = 0; void doWork() { state++; } }
    public static void main(String[] args) {
        Worker w1 = new Worker();
        Worker w2 = w1; // ALIAS — both point to same mutable state
        w1.doWork();
        w2.doWork(); // modifies same object — race condition if threaded
        System.out.println(w1.state); // 2 — shared mutable state
    }
}
