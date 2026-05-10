import java.util.Optional;
import java.util.regex.Pattern;

/**
 * BenchmarkL2Exec.java — Java reference for L2 simplified benchmark.
 * Same behavior as benchmark_l2_exec.hll.
 * Usage: java BenchmarkL2Exec <name> <email> <hasAddress>
 */
public class BenchmarkL2Exec {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    // Nominal type: Email (validated)
    record Email(String value) {
        Email {
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("Invalid Email: " + value);
            }
        }
        @Override public String toString() { return value; }
    }

    record CityName(String value) {
        @Override public String toString() { return value; }
    }

    record Address(CityName city) {}

    record Customer(String name, Email email, Optional<Address> address) {}

    static String customerCity(Customer c) {
        if (c.address().isPresent()) {
            return c.address().get().city().toString();
        } else {
            return "Unknown";
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java BenchmarkL2Exec <name> <email> <hasAddress>");
            return;
        }

        String name = args[0];
        Email email = new Email(args[1]);
        boolean hasAddr = Boolean.parseBoolean(args[2]);

        Optional<Address> address;
        if (hasAddr) {
            address = Optional.of(new Address(new CityName("Milano")));
        } else {
            address = Optional.empty();
        }

        Customer customer = new Customer(name, email, address);
        System.out.println("Name: " + customer.name());
        System.out.println("Email: " + customer.email());
        System.out.println("City: " + customerCity(customer));
    }
}
