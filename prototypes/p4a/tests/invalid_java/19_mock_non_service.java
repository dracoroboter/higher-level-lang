// 19: Java with Mockito — can mock anything (even final classes with PowerMock)
class MockNonService {
    static class Config { String name = "prod"; }
    public static void main(String[] args) {
        // Config mock = mock(Config.class); // Mockito can mock concrete classes!
        // No distinction between "mockable" and "not mockable"
        System.out.println("Java allows mocking any class");
    }
}
