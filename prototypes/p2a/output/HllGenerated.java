import java.util.Optional;

public class HllGenerated {
    public static int fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        var a = 0;
        var b = 1;
        var i = 2;
        while (i <= n) {
            var temp = b;
            b = a + b;
            a = temp;
            i = i + 1;
        }
        return b;
    }
    
    public static int factorial(int n) {
        var result = 1;
        var i = 1;
        while (i <= n) {
            result = result * i;
            i = i + 1;
        }
        return result;
    }
    
    public static boolean is_prime(int n) {
        if (n < 2) {
            return false;
        }
        var i = 2;
        while (i * i <= n) {
            if (n % i == 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
    
    public static int gcd(int a, int b) {
        var x = a;
        var y = b;
        while (y != 0) {
            var temp = y;
            y = x % y;
            x = temp;
        }
        return x;
    }
    
    public static void main(String[] args) {
        var fib_n = Integer.parseInt(args[0]);
        var fact_n = Integer.parseInt(args[1]);
        var prime_n = Integer.parseInt(args[2]);
        var gcd_a = Integer.parseInt(args[3]);
        var gcd_b = Integer.parseInt(args[4]);
        System.out.println("fib(" + fib_n + ") = " + fibonacci(fib_n));
        System.out.println("fact(" + fact_n + ") = " + factorial(fact_n));
        System.out.println("prime(" + prime_n + ") = " + is_prime(prime_n));
        System.out.println("gcd(" + gcd_a + ", " + gcd_b + ") = " + gcd(gcd_a, gcd_b));
    }
    
}

