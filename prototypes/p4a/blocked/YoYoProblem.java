/**
 * ANTIPATTERN: Yo-Yo Problem
 *
 * Problem: to understand what process() does, you must jump
 * up and down the hierarchy: Child → Parent → GrandParent → Parent → Child.
 * Each level overrides or calls super, creating a maze.
 *
 * Why impossible in HLL: no inheritance, no super, no override.
 * Each function is self-contained or delegates explicitly.
 */
class GrandParent {
    void process() {
        setup();
        doWork();
        cleanup();
    }
    void setup() { System.out.println("GP setup"); }
    void doWork() { System.out.println("GP work"); }
    void cleanup() { System.out.println("GP cleanup"); }
}

class Parent extends GrandParent {
    @Override void setup() {
        super.setup();
        System.out.println("P setup");
    }
    @Override void doWork() { System.out.println("P work"); }
}

class Child extends Parent {
    @Override void setup() {
        super.setup(); // calls Parent.setup which calls GP.setup
        System.out.println("C setup");
    }
    @Override void cleanup() {
        System.out.println("C cleanup");
        super.cleanup(); // jumps back to GP
    }
}
// Reading Child.process() requires bouncing between 3 files
