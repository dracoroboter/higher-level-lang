// 13: Java compiles — raw String used as SQL query
class SqlInjection {
    static void execute(String sql) { System.out.println("Executing: " + sql); }
    public static void main(String[] args) {
        String userInput = "Robert'; DROP TABLE users;--";
        execute(userInput); // SQL injection — Java doesn't distinguish Query from String
    }
}
