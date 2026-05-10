// 12: Java compiles — parameters swapped, same type (String)
class SwappedNominalParams {
    static void sendNotification(String email, String url) {
        System.out.println("To: " + email + " Link: " + url);
    }
    public static void main(String[] args) {
        String email = "alice@example.com";
        String url = "https://example.com";
        sendNotification(url, email); // SWAPPED! Java doesn't catch this.
    }
}
