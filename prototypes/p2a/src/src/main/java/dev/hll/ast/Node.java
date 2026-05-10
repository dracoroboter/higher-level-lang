package dev.hll.ast;

import java.util.List;
import java.util.Optional;

public sealed interface Node {

    record Program(List<Declaration> declarations) implements Node {}

    sealed interface Declaration extends Node {}

    record TypeDecl(String name, String baseType, Optional<WhereConstraint> constraint) implements Declaration {}

    record WhereConstraint(String qualifier, String method, List<Expr> args) implements Node {}

    record StructDecl(String name, List<Field> fields) implements Declaration {}

    record FnDecl(String name, List<Param> params, Optional<TypeExpr> returnType, Block body) implements Declaration {}

    record ImportDecl(String javaClass, String alias, List<JavaMapping> mappings) implements Declaration {}

    record JavaMapping(String name, List<Param> params, TypeExpr returnType) implements Node {}

    record Field(String name, TypeExpr type) implements Node {}

    record Param(String name, TypeExpr type) implements Node {}

    record TypeExpr(String name, Optional<TypeExpr> typeArg) implements Node {
        public boolean isOption() { return "Option".equals(name); }
        public TypeExpr inner() { return typeArg.orElseThrow(); }
    }

    record Block(List<Statement> statements) implements Node {}

    sealed interface Statement extends Node {}

    record LetStmt(String name, Optional<TypeExpr> type, Expr value) implements Statement {}

    record ReturnStmt(Optional<Expr> value) implements Statement {}

    record ExprStmt(Expr expr) implements Statement {}

    record MatchStmt(Expr subject, List<MatchArm> arms) implements Statement {}

    record IfStmt(Expr condition, Block thenBlock, Optional<Block> elseBlock) implements Statement {}

    record MatchArm(Pattern pattern, Expr body) implements Node {}

    sealed interface Pattern extends Node {}

    record SomePattern(String binding) implements Pattern {}

    record NonePattern() implements Pattern {}

    record WildcardPattern() implements Pattern {}

    sealed interface Expr extends Node {}

    record Identifier(String name) implements Expr {}

    record StringLit(String value) implements Expr {}

    record NumberLit(int value) implements Expr {}

    record FloatLit(double value) implements Expr {}

    record BoolLit(boolean value) implements Expr {}

    record FieldAccess(Expr object, String field) implements Expr {}

    record MethodCall(Expr object, String method, List<Expr> args) implements Expr {}

    record FnCall(String name, List<Expr> args) implements Expr {}

    record OptionPropagate(Expr expr) implements Expr {}

    record BinaryOp(Expr left, String op, Expr right) implements Expr {}

    record UnaryOp(String op, Expr operand) implements Expr {}

    record BlockExpr(Block block) implements Expr {}
}
