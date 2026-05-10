// 02: Java — package-private is the default, but public is easy to add accidentally
// In Java, any class can access public members of any other class
class VisibilityViolation {
    // In a real project: someone makes a helper public "just to test it"
    // and now everyone depends on an internal implementation detail
    public static int internalHelper() { return 42; } // should be private
    public static void main(String[] args) {
        System.out.println(internalHelper()); // accessible from anywhere
    }
}
