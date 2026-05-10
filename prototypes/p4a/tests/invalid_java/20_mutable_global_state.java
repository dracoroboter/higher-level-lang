// 20: Java compiles — mutable global state
class MutableGlobalState {
    static int counter = 0; // global mutable state — accessible from anywhere
    static void increment() { counter++; }
    public static void main(String[] args) {
        increment();
        increment();
        System.out.println(counter); // hidden dependency on global state
    }
}
