// 05: Java compiles — unchecked exception not handled
class UnhandledFails {
    static String readFile(String path) {
        throw new RuntimeException("file not found"); // unchecked — caller not forced to handle
    }
    public static void main(String[] args) {
        String content = readFile("/tmp/x"); // no try/catch required — crashes at runtime
        System.out.println(content);
    }
}
