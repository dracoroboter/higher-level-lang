/**
 * ANTIPATTERN: Deep Inheritance
 * 
 * Problem: fragile base class, tight vertical coupling, 
 * changes in Animal break Dog which breaks GuideDog.
 * Override chains become unpredictable.
 *
 * Why impossible in HLL: no `class`, no `extends`, no inheritance.
 * HLL uses composition + service interfaces.
 */
class Animal {
    String name;
    void eat() { System.out.println(name + " eats"); }
}

class Mammal extends Animal {
    void breathe() { System.out.println(name + " breathes"); }
}

class Dog extends Mammal {
    void bark() { System.out.println(name + " barks"); }
}

class GuideDog extends Dog {
    void guide() { System.out.println(name + " guides"); }
}

class PoliceDog extends Dog {
    void patrol() { System.out.println(name + " patrols"); }
}
// 5 levels deep — any change to Animal cascades through all
