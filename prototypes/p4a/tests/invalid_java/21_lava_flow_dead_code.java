// 21: Java compiles — dead code (function never called)
class LavaFlow {
    static int usedFunction() { return 42; }
    static int neverCalled() { return 99; } // DEAD CODE — Java doesn't warn
    public static void main(String[] args) {
        System.out.println(usedFunction());
    }
}
