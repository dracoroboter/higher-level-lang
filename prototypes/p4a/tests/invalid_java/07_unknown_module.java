// 07: Java catches missing imports at compile time (similar to HLL)
// But with reflection, you can load unknown classes at runtime:
class UnknownModule {
    public static void main(String[] args) throws Exception {
        Class<?> cls = Class.forName("phantom.DoesNotExist"); // compiles! fails at runtime
        Object obj = cls.getDeclaredConstructor().newInstance();
    }
}
