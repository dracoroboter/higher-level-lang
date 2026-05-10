// 22: Java DOES catch this (won't compile without return on all paths)
// But with Object return type and null, it compiles:
class IncompleteReturn {
    static String classify(int x) {
        if (x > 0) return "positive";
        return null; // Java allows null as "no value" — HLL doesn't have null
    }
    public static void main(String[] args) {
        System.out.println(classify(-1)); // prints null — silent bug
    }
}
