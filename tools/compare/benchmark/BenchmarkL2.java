/**
 * Benchmark Java — Livello 2 (NPE + tipi nominali + Demeter + Eccezioni)
 * 
 * Estende il livello 1 con:
 * - Parsing dell'input che può fallire
 * - Validazione che può fallire
 * - Catena di operazioni fallibili
 * - Catch generico (antipattern)
 * - Return null su errore (antipattern)
 * 
 * Input: argomenti da riga di comando
 * Output: struttura serializzata o errore
 */
public class BenchmarkL2 {

    // Struttura nidificata (stessa di L1)
    record City(String name, String zipCode) {}
    record Address(String street, City city, String country) {}
    record Customer(String name, String email, Address address) {}
    record Money(String amount) {}
    record Order(int id, Customer customer, Money total, String status) {}

    // "Validazione" — ma torna null su errore (antipattern)
    static String validateEmail(String input) {
        if (input != null && input.contains("@")) return input;
        return null;  // antipattern: null come errore
    }

    static String validateMoney(String input) {
        try {
            double d = Double.parseDouble(input);
            if (input.contains(".") && input.split("\\.")[1].length() > 2) return null;
            return input;
        } catch (Exception e) {  // antipattern: catch generico
            return null;
        }
    }

    static Integer validateEven(String input) {
        try {
            int n = Integer.parseInt(input);
            if (n % 2 != 0) return null;
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    static Integer validatePrime(String input) {
        try {
            int n = Integer.parseInt(input);
            if (n < 2) return null;
            for (int i = 2; i <= Math.sqrt(n); i++) {
                if (n % i == 0) return null;
            }
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java BenchmarkL2 <name> <email> <amount> <hasAddress> <evenNumber>");
            return;
        }

        String name = args[0];
        String email = validateEmail(args[1]);
        String amount = validateMoney(args[2]);
        boolean hasAddress = Boolean.parseBoolean(args[3]);
        Integer evenNum = validateEven(args[4]);

        // Nessun check su null — antipattern (NPE latente)
        if (email == null) {
            System.out.println("ERROR: invalid email");
            return;
        }
        if (amount == null) {
            System.out.println("ERROR: invalid amount");
            return;
        }
        if (evenNum == null) {
            System.out.println("ERROR: not an even number");
            return;
        }

        Address address = null;
        if (hasAddress) {
            address = new Address("Via Roma 1", new City("Milano", "20100"), "IT");
        }

        Customer customer = new Customer(name, email, address);
        Order order = new Order(1, customer, new Money(amount), "pending");

        System.out.println("Order #" + order.id());
        System.out.println("Customer: " + order.customer().name());
        System.out.println("Email: " + order.customer().email());
        System.out.println("Total: " + order.total().amount());
        System.out.println("Even: " + evenNum);

        // Demeter + NPE
        if (order.customer().address() != null) {
            System.out.println("City: " + order.customer().address().city().name());
            System.out.println("Zip: " + order.customer().address().city().zipCode());
        } else {
            System.out.println("City: Unknown");
            System.out.println("Zip: Unknown");
        }
    }
}
