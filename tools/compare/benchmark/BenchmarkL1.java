/**
 * Benchmark Java — Livello 1 (NPE + tipi nominali + Demeter)
 * 
 * Questo programma è scritto CON gli antipattern che HLL elimina:
 * - String usata per email, URL, currency (stringly-typed)
 * - null per campi opzionali (NPE latente)
 * - Catene di accesso profonde (Demeter)
 * - Nessuna validazione dei tipi semantici
 * 
 * Input: argomenti da riga di comando
 * Output: struttura serializzata o errore
 */
public class BenchmarkL1 {

    // Struttura nidificata — tutti String e null (antipattern)
    record City(String name, String zipCode) {}
    record Address(String street, City city, String country) {}
    record Customer(String name, String email, Address address) {}  // address può essere null
    record Money(String amount) {}  // "19.99" come stringa — nessun vincolo
    record Order(int id, Customer customer, Money total, String status) {}

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java BenchmarkL1 <name> <email> <amount> <hasAddress>");
            return;
        }

        String name = args[0];
        String email = args[1];       // nessuna validazione — antipattern
        String amount = args[2];      // nessuna validazione — antipattern
        boolean hasAddress = Boolean.parseBoolean(args[3]);

        // Costruzione — address può essere null (antipattern)
        Address address = null;
        if (hasAddress) {
            address = new Address("Via Roma 1", new City("Milano", "20100"), "IT");
        }

        Customer customer = new Customer(name, email, address);
        Order order = new Order(1, customer, new Money(amount), "pending");

        // Stampa — catena profonda (Demeter violation + NPE latente)
        System.out.println("Order #" + order.id());
        System.out.println("Customer: " + order.customer().name());
        System.out.println("Email: " + order.customer().email());
        System.out.println("Total: " + order.total().amount());

        // QUESTO CRASHA se hasAddress=false (NPE)
        System.out.println("City: " + order.customer().address().city().name());
        System.out.println("Zip: " + order.customer().address().city().zipCode());
    }
}
