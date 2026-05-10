// 09: Java compiles fine — NPE at runtime
class DirectOptionAccess {
    static class Address { String city = "Rome"; }
    static class Customer { Address address = null; } // nullable!
    static class Order { Customer customer = new Customer(); }

    static String badCity(Order order) {
        return order.customer.address.city; // NPE if address is null
    }

    public static void main(String[] args) {
        System.out.println(badCity(new Order())); // throws NPE
    }
}
