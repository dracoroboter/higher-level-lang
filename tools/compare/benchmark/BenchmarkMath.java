/**
 * BenchmarkMath.java — Reference oracle per computazione pura imperativa.
 * Output di questo programma = output atteso dai prototipi HLL.
 *
 * Usage: java BenchmarkMath <fib_n> <fact_n> <prime_n> <gcd_a> <gcd_b>
 */
public class BenchmarkMath {

    static int fibonacci(int n) {
        if (n <= 1) return n;
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = b;
            b = a + b;
            a = temp;
        }
        return b;
    }

    static int factorial(int n) {
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result = result * i;
        }
        return result;
    }

    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java BenchmarkMath <fib_n> <fact_n> <prime_n> <gcd_a> <gcd_b>");
            return;
        }
        int fibN = Integer.parseInt(args[0]);
        int factN = Integer.parseInt(args[1]);
        int primeN = Integer.parseInt(args[2]);
        int gcdA = Integer.parseInt(args[3]);
        int gcdB = Integer.parseInt(args[4]);

        System.out.println("fib(" + fibN + ") = " + fibonacci(fibN));
        System.out.println("fact(" + factN + ") = " + factorial(factN));
        System.out.println("prime(" + primeN + ") = " + isPrime(primeN));
        System.out.println("gcd(" + gcdA + ", " + gcdB + ") = " + gcd(gcdA, gcdB));
    }
}
