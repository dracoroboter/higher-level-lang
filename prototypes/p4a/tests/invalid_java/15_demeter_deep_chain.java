// 15: Same as 14 — Java allows arbitrarily deep chains
class DemeterDeepChain {
    static class Zip { String code = "00100"; }
    static class City { Zip zip = new Zip(); }
    static class Address { City city = new City(); }
    static class Customer { Address address = new Address(); }
    static class Order { Customer customer = new Customer(); }
    public static void main(String[] args) {
        Order o = new Order();
        System.out.println(o.customer.address.city.zip.code); // 5 levels!
    }
}
