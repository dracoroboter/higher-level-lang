// 18: Java with Mockito — incomplete mock silently returns null/0
// import static org.mockito.Mockito.*;
class IncompleteMock {
    interface UserRepo {
        String find(int id);
        void save(String data);
    }
    public static void main(String[] args) {
        // UserRepo mock = mock(UserRepo.class);
        // mock.find(1) returns null — no setup required!
        // mock.save("x") does nothing — silent incomplete mock
        // Java mocking frameworks don't enforce completeness
        System.out.println("Mockito allows incomplete mocks by default");
    }
}
