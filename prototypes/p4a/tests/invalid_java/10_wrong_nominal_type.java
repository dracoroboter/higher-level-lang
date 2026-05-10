// 10: Java compiles — wrong type passed, no compile error
class WrongNominalType {
    static void sendEmail(String email) { System.out.println("Sending to " + email); }
    public static void main(String[] args) {
        String notAnEmail = "not-an-email"; // just a String, no validation
        sendEmail(notAnEmail); // Java accepts this — no type safety
    }
}
