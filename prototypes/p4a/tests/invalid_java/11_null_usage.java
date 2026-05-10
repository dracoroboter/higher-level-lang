// 11: Java compiles — null is valid
class NullUsage {
    static class Customer { String name; }
    public static void main(String[] args) {
        Customer c = null; // Java allows this
        System.out.println(c.name); // NPE at runtime
    }
}
