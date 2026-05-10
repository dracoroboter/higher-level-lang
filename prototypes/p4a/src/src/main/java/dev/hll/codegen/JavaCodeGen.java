package dev.hll.codegen;

import dev.hll.ast.Node;
import dev.hll.ast.Node.*;

import java.util.*;

public class JavaCodeGen {

    private final StringBuilder out = new StringBuilder();
    private int indent = 0;
    private final Set<String> typeAliases = new HashSet<>();
    private final Set<String> structs = new HashSet<>();
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
                case TestDecl td -> {}
                case StateDecl sd -> {}
                case ServiceDecl sd -> {} // interfaces, not generated directly
                case ProvideDecl pd -> {} // implementations, not generated directly
                case ExportDecl ed -> {
                    // Generate the inner declaration
                    switch (ed.inner()) {
                        case TypeDecl td -> generateTypeDecl(td);
                        case StructDecl sd -> generateStruct(sd);
                        case FnDecl fd -> generateFn(fd);
                        case StateDecl sd2 -> {}
                        case ServiceDecl sd2 -> {}
                        case ProvideDecl pd2 -> {}
                        default -> {}
                    }
                }
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
        structs.add(sd.name());
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

        String modifier = "public static";
        // Fix main signature for Java
        String paramStr;
        if (fd.name().equals("main")) {
            paramStr = "String[] args";
        } else {
            paramStr = String.join(", ", params);
        }
        String sig = modifier + " " + returnType + " " + fd.name() + "(" + paramStr + ")";
        emit(sig + " {");
        indent++;
        generateBlock(fd.body());
        indent--;
        emit("}");
        emit("");
    }

    private void generateImport(ImportDecl id) {
        emit("// Import: " + id.path() + " as " + id.alias());
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
            case ExprStmt es -> {
                if (es.expr() instanceof MatchExpr me) {
                    // Match as statement — generate if/else
                    String subject = generateExpr(me.subject());
                    if (me.arms().size() >= 2) {
                        var arm0 = me.arms().get(0);
                        var arm1 = me.arms().get(1);
                        if (arm0.pattern() instanceof SomePattern sp) {
                            emit("if (" + subject + ".isPresent()) {");
                            indent++;
                            emit("var " + sp.binding() + " = " + subject + ".get();");
                            emit("return " + generateExpr(arm0.body()) + ";");
                            indent--;
                            emit("} else {");
                            indent++;
                            emit("return " + generateExpr(arm1.body()) + ";");
                            indent--;
                            emit("}");
                        } else {
                            emit("if (" + subject + ") {");
                            indent++;
                            emit(generateExpr(arm0.body()) + ";");
                            indent--;
                            emit("} else {");
                            indent++;
                            emit(generateExpr(arm1.body()) + ";");
                            indent--;
                            emit("}");
                        }
                    }
                } else {
                    emit(generateExpr(es.expr()) + ";");
                }
            }
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
            case WhileStmt ws -> {
                emit("while (" + generateExpr(ws.condition()) + ") {");
                indent++;
                generateBlock(ws.body());
                indent--;
                emit("}");
            }
            case AssignStmt as2 -> emit(as2.name() + " = " + generateExpr(as2.value()) + ";");
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
            case Identifier id -> {
                if (id.name().equals("None")) yield "Optional.empty()";
                else yield id.name();
            }
            case StringLit sl -> "\"" + sl.value() + "\"";
            case NumberLit nl -> String.valueOf(nl.value());
            case FloatLit fl -> String.valueOf(fl.value());
            case BoolLit bl -> String.valueOf(bl.value());
            case FieldAccess fa -> generateExpr(fa.object()) + "." + fa.field() + "()";
            case MethodCall mc -> {
                String obj = generateExpr(mc.object());
                String mArgs = mc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("");
                // args.get(N) → args[N] for main args
                if (mc.method().equals("get") && obj.equals("args")) {
                    yield "args[" + mArgs + "]";
                }
                yield obj + "." + mc.method() + "(" + mArgs + ")";
            }
            case FnCall fc -> {
                if (typeAliases.contains(fc.name()) || structs.contains(fc.name())) {
                    yield "new " + fc.name() + "(" + fc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("") + ")";
                }
                // Match expression placeholder — should not reach here with new MatchExpr node
                if (fc.name().startsWith("__match__")) {
                    yield "/* match placeholder */";
                }
                // Builtins
                String args = fc.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("");
                yield switch (fc.name()) {
                    case "printLn" -> "System.out.println(" + args + ")";
                    case "parseInt" -> "Integer.parseInt(" + args + ")";
                    case "Some" -> "Optional.of(" + args + ")";
                    case "None" -> "Optional.empty()";
                    default -> fc.name() + "(" + args + ")";
                };
            }
            case OptionPropagate op -> {
                String inner = generateExpr(op.expr());
                yield inner + ".isPresent() ? " + inner + ".get() : null /* early return */";
            }
            case BinaryOp bo -> {
                String left = generateExpr(bo.left());
                String right = generateExpr(bo.right());
                // String comparison: == generates .equals() if one side is a string literal
                if (bo.op().equals("==") && (right.startsWith("\"") || left.startsWith("\""))) {
                    yield left + ".equals(" + right + ")";
                }
                yield left + " " + bo.op() + " " + right;
            }
            case UnaryOp uo -> uo.op() + generateExpr(uo.operand());
            case BlockExpr be -> "/* block */";
            case MatchExpr me -> {
                // Generate inline if/else for Option match, ternary for boolean match
                String subject = generateExpr(me.subject());
                if (me.arms().size() == 2) {
                    var arm0 = me.arms().get(0);
                    var arm1 = me.arms().get(1);
                    if (arm0.pattern() instanceof SomePattern sp) {
                        // Option match: subject.isPresent() ? ... : ...
                        yield "(" + subject + ".isPresent() ? ((" + sp.binding() + " = " + subject + ".get()) != null ? " + generateExpr(arm0.body()) + " : null) : " + generateExpr(arm1.body()) + ")";
                    } else if (arm0.pattern() instanceof Node.WildcardPattern) {
                        // Boolean/value match: subject ? arm0 : arm1
                        yield "(" + subject + " ? " + generateExpr(arm0.body()) + " : " + generateExpr(arm1.body()) + ")";
                    }
                }
                yield "/* match */";
            }
            case FailExpr fe -> {
                String fArgs = fe.args().stream().map(this::generateExpr).reduce((a, b) -> a + ", " + b).orElse("");
                yield "throw new " + fe.errorType() + "(" + fArgs + ")";
            }
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
