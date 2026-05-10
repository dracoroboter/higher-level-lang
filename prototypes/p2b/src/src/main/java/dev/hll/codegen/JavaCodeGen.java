package dev.hll.codegen;

import dev.hll.ast.Node;
import dev.hll.ast.Node.*;

import java.util.*;

public class JavaCodeGen {

    private final StringBuilder out = new StringBuilder();
    private int indent = 0;
    private final Set<String> typeAliases = new HashSet<>();
    private final Map<String, Node.WhereConstraint> typeConstraints = new HashMap<>();

    public String generate(Program program) {
        emit("import java.util.Optional;");
        emit("");
        emit("public class HllGenerated {");
        indent++;

        for (var decl : program.declarations()) {
            switch (decl) {
                case TypeDecl td -> generateTypeDecl(td);
                case StructDecl sd -> generateStruct(sd);
                case FnDecl fd -> generateFn(fd);
                case ImportDecl id -> generateImport(id);
                case TestDecl td -> {} // tests not generated in Java output
            }
        }

        indent--;
        emit("}");
        return out.toString();
    }

    private void generateTypeDecl(TypeDecl td) {
        typeAliases.add(td.name());
        td.constraint().ifPresent(c -> typeConstraints.put(td.name(), c));

        emit("public record " + td.name() + "(String value) {");
        indent++;

        // Generate validation in compact constructor if where constraint exists
        if (td.constraint().isPresent()) {
            var c = td.constraint().get();
            String validatorCall = resolveValidator(c);
            emit("public " + td.name() + " {");
            indent++;
            emit("if (!(" + validatorCall + ").test(value)) {");
            indent++;
            emit("throw new IllegalArgumentException(\"Invalid " + td.name() + ": \" + value);");
            indent--;
            emit("}");
            indent--;
            emit("}");
        }

        emit("@Override public String toString() { return value; }");
        indent--;
        emit("}");
        emit("");
    }

    private String resolveValidator(Node.WhereConstraint c) {
        // Map HLL library names to Java class calls
        String qualifier = c.qualifier();
        String method = c.method();
        String args = c.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("");

        if (qualifier.equals("validate") || qualifier.equals("hll.validation")) {
            return "dev.hll.stdlib.HllValidation." + method + "(" + args + ")";
        }
        // Custom function — assume it's a static method in the generated class
        if (qualifier.isEmpty()) {
            return method + "(" + args + ")";
        }
        return qualifier.replace(".", "_") + "." + method + "(" + args + ")";
    }

    private void generateStruct(StructDecl sd) {
        var fields = sd.fields().stream()
                .map(f -> javaType(f.type()) + " " + f.name())
                .toList();
        emit("public record " + sd.name() + "(" + String.join(", ", fields) + ") {}");
        emit("");
    }

    private void generateFn(FnDecl fd) {
        String returnType = fd.returnType().map(this::javaType).orElse("void");
        var params = fd.params().stream()
                .map(p -> javaType(p.type()) + " " + p.name())
                .toList();

        String modifier = fd.name().equals("main") ? "public static" : "public static";
        String sig = modifier + " " + returnType + " " + fd.name() + "(" + String.join(", ", params) + ")";
        emit(sig + " {");
        indent++;
        generateBlock(fd.body());
        indent--;
        emit("}");
        emit("");
    }

    private void generateImport(ImportDecl id) {
        emit("// Import: " + id.javaClass() + " as " + id.alias());
    }

    private void generateBlock(Block block) {
        for (var stmt : block.statements()) {
            generateStatement(stmt);
        }
    }

    private void generateStatement(Statement stmt) {
        switch (stmt) {
            case LetStmt ls -> {
                String type = ls.type().map(this::javaType).orElse("var");
                emit(type + " " + ls.name() + " = " + generateExpr(ls.value()) + ";");
            }
            case ReturnStmt rs -> {
                emit("return" + rs.value().map(v -> " " + generateExpr(v)).orElse("") + ";");
            }
            case ExprStmt es -> emit(generateExpr(es.expr()) + ";");
            case MatchStmt ms -> generateMatch(ms);
            case IfStmt is -> {
                emit("if (" + generateExpr(is.condition()) + ") {");
                indent++;
                generateBlock(is.thenBlock());
                indent--;
                if (is.elseBlock().isPresent()) {
                    emit("} else {");
                    indent++;
                    generateBlock(is.elseBlock().get());
                    indent--;
                }
                emit("}");
            }
            case AssertStmt as -> emit("assert " + generateExpr(as.condition()) + ";");
            case ExpectErrorStmt ee -> {} // not generated
        }
    }

    private void generateMatch(MatchStmt ms) {
        String subject = generateExpr(ms.subject());
        emit("if (" + subject + ".isPresent()) {");
        indent++;
        for (var arm : ms.arms()) {
            if (arm.pattern() instanceof SomePattern sp) {
                emit("var " + sp.binding() + " = " + subject + ".get();");
                emit(generateExpr(arm.body()) + ";");
            }
        }
        indent--;
        emit("} else {");
        indent++;
        for (var arm : ms.arms()) {
            if (arm.pattern() instanceof NonePattern) {
                emit(generateExpr(arm.body()) + ";");
            }
        }
        indent--;
        emit("}");
    }

    private String generateExpr(Expr expr) {
        return switch (expr) {
            case Identifier id -> id.name();
            case StringLit sl -> "\"" + sl.value() + "\"";
            case NumberLit nl -> String.valueOf(nl.value());
            case FloatLit fl -> String.valueOf(fl.value());
            case BoolLit bl -> String.valueOf(bl.value());
            case FieldAccess fa -> generateExpr(fa.object()) + "." + fa.field() + "()";
            case MethodCall mc -> generateExpr(mc.object()) + "." + mc.method() + "(" +
                    mc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("") + ")";
            case FnCall fc -> {
                if (typeAliases.contains(fc.name())) {
                    yield "new " + fc.name() + "(" + fc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("") + ")";
                }
                yield fc.name() + "(" + fc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("") + ")";
            }
            case OptionPropagate op -> {
                String inner = generateExpr(op.expr());
                yield inner + ".isPresent() ? " + inner + ".get() : null /* early return */";
            }
            case BinaryOp bo -> generateExpr(bo.left()) + " " + bo.op() + " " + generateExpr(bo.right());
            case UnaryOp uo -> uo.op() + generateExpr(uo.operand());
            case BlockExpr be -> "/* block */";
        };
    }

    private String javaType(TypeExpr type) {
        if (type.isOption()) {
            return "Optional<" + javaType(type.inner()) + ">";
        }
        return switch (type.name()) {
            case "String" -> "String";
            case "Int" -> "int";
            case "Float" -> "double";
            case "Bool" -> "boolean";
            default -> type.name();
        };
    }

    private void emit(String line) {
        out.append("    ".repeat(indent)).append(line).append("\n");
    }
}
