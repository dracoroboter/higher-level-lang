// 23: Java compiles — interface with too many methods
interface DoEverything {
    void create();
    String read();
    void update();
    void delete();
    boolean validate();
    String transform();
    void notify_();
    void log();
    void cache();
    String export_();
    // 10 methods — Java doesn't warn about interface bloat
}
