// 14: Java compiles — deep chain access (Law of Demeter violation)
class DemeterViolation {
    static class City { String zip = "00100"; }
    static class Address { City city = new City(); }
    static class Customer { Address address = new Address(); }
    static class Order { Customer customer = new Customer(); }
    static String getZip(Order order) {
        return order.customer.address.city.zip; // 4 levels deep — Java allows
    }
    public static void main(String[] args) { System.out.println(getZip(new Order())); }
}
