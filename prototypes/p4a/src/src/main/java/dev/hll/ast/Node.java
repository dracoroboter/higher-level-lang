package dev.hll.ast;

import java.util.List;
import java.util.Optional;

public sealed interface Node {

    record Program(Optional<ModuleDecl> module, List<Declaration> declarations) implements Node {}

    record ModuleDecl(String name) implements Node {}

    sealed interface Declaration extends Node {}

    record TypeDecl(String name, String baseType, Optional<WhereConstraint> constraint) implements Declaration {}

    record WhereConstraint(String qualifier, String method, List<Expr> args) implements Node {}

    record StructDecl(String name, List<Field> fields) implements Declaration {}

    record StateDecl(String name, List<StateVariant> variants) implements Declaration {}

    record StateVariant(String stateName, List<StateFn> methods) implements Node {}

    record StateFn(String name, List<Param> params, String targetState) implements Node {}

    record ServiceDecl(String name, List<ServiceFn> methods) implements Declaration {}

    record ServiceFn(String name, List<Param> params, TypeExpr returnType, List<String> fails) implements Node {}

    record ProvideDecl(String serviceName, List<NeedsDecl> needs, List<FnDecl> methods) implements Declaration {}

    record NeedsDecl(String serviceType, String name) implements Node {}

    record ExportDecl(Declaration inner) implements Declaration {}

    record FnDecl(String name, List<Param> params, Optional<TypeExpr> returnType, List<String> fails, List<String> effects, Block body) implements Declaration {}

    record ImportDecl(String path, String alias, List<JavaMapping> mappings, List<String> symbols) implements Declaration {}

    record TestDecl(String description, Block body) implements Declaration {}

    record JavaMapping(String name, List<Param> params, TypeExpr returnType) implements Node {}

    record Field(String name, TypeExpr type) implements Node {}

    record Param(String name, TypeExpr type) implements Node {}

    record TypeExpr(String name, Optional<TypeExpr> typeArg) implements Node {
        public boolean isOption() { return "Option".equals(name); }
        public TypeExpr inner() { return typeArg.orElseThrow(); }
    }

    record Block(List<Statement> statements) implements Node {}

    sealed interface Statement extends Node {}

    record LetStmt(String name, Optional<TypeExpr> type, Expr value, boolean hasErrorHandler, List<ErrorHandler> errorHandlers) implements Statement {}

    record ErrorHandler(String errorType, String binding, Expr body) implements Node {}

    record ReturnStmt(Optional<Expr> value) implements Statement {}

    record ExprStmt(Expr expr) implements Statement {}

    record MatchStmt(Expr subject, List<MatchArm> arms) implements Statement {}

    record IfStmt(Expr condition, Block thenBlock, Optional<Block> elseBlock) implements Statement {}

    record WhileStmt(Expr condition, Block body) implements Statement {}

    record AssignStmt(String name, Expr value) implements Statement {}

    record AssertStmt(Expr condition) implements Statement {}

    record ExpectErrorStmt(Block body) implements Statement {}

    record MockStmt(String serviceName, List<FnDecl> methods) implements Statement {}

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

    record MatchExpr(Expr subject, List<MatchArm> arms) implements Expr {}

    record FailExpr(String errorType, List<Expr> args) implements Expr {}
}
