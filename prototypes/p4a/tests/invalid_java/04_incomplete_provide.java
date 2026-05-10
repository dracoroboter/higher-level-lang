// 04: Java catches this (abstract method not implemented = compile error)
// But with default methods or partial implementation, it compiles:
interface Repo {
    String find(int id);
    default void save(String data) {} // default = silent no-op
}
class IncompleteProvide implements Repo {
    public String find(int id) { return "found"; }
    // save() silently does nothing — incomplete implementation
}
