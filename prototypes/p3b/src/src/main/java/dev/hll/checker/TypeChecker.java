package dev.hll.checker;

import dev.hll.ast.Node;
import dev.hll.ast.Node.*;

import java.util.*;

public class TypeChecker {

    private final Map<String, TypeExpr> typeAliases = new HashMap<>();
    private final Map<String, Node.WhereConstraint> typeConstraints = new HashMap<>();
    private final Map<String, StructDecl> structs = new HashMap<>();
    private final Map<String, FnDecl> functions = new HashMap<>();
    private final Map<String, Node.StateDecl> stateTypes = new HashMap<>();
    private final Map<String, String> varStates = new HashMap<>();
    private final Map<String, ImportDecl> imports = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final int maxChainDepth;

    public TypeChecker(int maxChainDepth) {
        this.maxChainDepth = maxChainDepth;
    }

    public TypeChecker() {
        this(2);
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public void check(Program program) {
        // Pass 1: collect declarations
        for (var decl : program.declarations()) {
            switch (decl) {
                case TypeDecl td -> {
                    typeAliases.put(td.name(), new TypeExpr(td.baseType(), Optional.empty()));
                    td.constraint().ifPresent(c -> typeConstraints.put(td.name(), c));
                }
                case StructDecl sd -> structs.put(sd.name(), sd);
                case FnDecl fd -> functions.put(fd.name(), fd);
                case ImportDecl id -> imports.put(id.alias(), id);
                case StateDecl sd -> stateTypes.put(sd.name(), sd);
                case TestDecl td -> {} // tests handled separately in --test mode
            }
        }

        // Pass 2: check function bodies
        for (var decl : program.declarations()) {
            if (decl instanceof FnDecl fd) {
                checkFn(fd);
            }
        }
    }

    private void checkFn(FnDecl fn) {
        var scope = new HashMap<String, TypeExpr>();
        for (var param : fn.params()) {
            scope.put(param.name(), param.type());
        }
        varStates.clear();
        checkBlock(fn.body(), scope, fn.name());
    }

    public void checkBlock(Block block, java.util.Map<String, TypeExpr> scope, String context) {
        for (var stmt : block.statements()) {
            checkStatement(stmt, scope, context);
        }
    }

    private void checkStatement(Statement stmt, Map<String, TypeExpr> scope, String context) {
        switch (stmt) {
            case LetStmt ls -> {
                var exprType = inferType(ls.value(), scope, context);
                scope.put(ls.name(), ls.type().orElse(exprType));
            }
            case ReturnStmt rs -> {
                if (rs.value().isPresent()) {
                    inferType(rs.value().get(), scope, context);
                }
            }
            case ExprStmt es -> {
                var exprType = inferType(es.expr(), scope, context);
                // Check: Result must not be ignored
                if (exprType != null && exprType.name().equals("Result")) {
                    errors.add("Result of function call is not consumed. Must handle with match or assign to variable. In: " + context);
                }
            }
            case MatchStmt ms -> checkMatch(ms, scope, context);
            case IfStmt is -> {
                inferType(is.condition(), scope, context);
                checkBlock(is.thenBlock(), new HashMap<>(scope), context);
                is.elseBlock().ifPresent(b -> checkBlock(b, new HashMap<>(scope), context));
            }
            case AssertStmt as -> inferType(as.condition(), scope, context);
            case ExpectErrorStmt ee -> {}
            case WhileStmt ws -> {
                inferType(ws.condition(), scope, context);
                checkBlock(ws.body(), scope, context);
            }
            case AssignStmt as2 -> inferType(as2.value(), scope, context); // handled in test runner, not here
        }
    }

    private void checkMatch(MatchStmt ms, Map<String, TypeExpr> scope, String context) {
        var subjectType = inferType(ms.subject(), scope, context);
        if (subjectType != null && subjectType.isOption()) {
            boolean hasSome = false, hasNone = false;
            for (var arm : ms.arms()) {
                if (arm.pattern() instanceof SomePattern) hasSome = true;
                if (arm.pattern() instanceof NonePattern) hasNone = true;
                if (arm.pattern() instanceof WildcardPattern) { hasSome = true; hasNone = true; }
            }
            if (!hasSome || !hasNone) {
                errors.add("Non-exhaustive match on Option in '" + context + "': must handle both Some and None");
            }
        }
        // Check: match on Result must handle both Ok and Err
        if (subjectType != null && subjectType.name().equals("Result")) {
            boolean hasOk = false, hasErr = false;
            for (var arm : ms.arms()) {
                if (arm.pattern() instanceof SomePattern sp) {
                    if (sp.binding() != null) {
                        // Check pattern name from the text
                    }
                }
                if (arm.pattern() instanceof WildcardPattern) { hasOk = true; hasErr = true; }
            }
            // Simple heuristic: check if arms mention Ok and Err patterns
            String matchText = ms.arms().stream()
                    .map(a -> a.pattern().toString())
                    .reduce("", (a, b) -> a + " " + b);
            hasOk = matchText.contains("Ok") || matchText.contains("Wildcard");
            hasErr = matchText.contains("Err") || matchText.contains("Wildcard");
            if (!hasOk || !hasErr) {
                errors.add("Non-exhaustive match on Result in '" + context + "': must handle both Ok and Err");
            }
        }
    }

    private TypeExpr inferType(Expr expr, Map<String, TypeExpr> scope, String context) {
        return inferType(expr, scope, context, 0);
    }

    private TypeExpr inferType(Expr expr, Map<String, TypeExpr> scope, String context, int chainDepth) {
        switch (expr) {
            case Identifier id -> {
                if (id.name().equals("null")) {
                    errors.add("'null' does not exist in HLL. Use Option<T> for absent values. In: " + context);
                    return null;
                }
                var type = scope.get(id.name());
                if (type == null && !imports.containsKey(id.name()) && !functions.containsKey(id.name())) {
                    // Could be a type constructor or unknown — skip for prototype
                }
                return type;
            }
            case StringLit sl -> { return new TypeExpr("String", Optional.empty()); }
            case NumberLit nl -> { return new TypeExpr("Int", Optional.empty()); }
            case FloatLit fl -> { return new TypeExpr("Float", Optional.empty()); }
            case BoolLit bl -> { return new TypeExpr("Bool", Optional.empty()); }

            case FieldAccess fa -> {
                int newDepth = chainDepth + 1;
                if (newDepth > maxChainDepth) {
                    warnings.add("Law of Demeter: chain depth " + newDepth + " exceeds max " + maxChainDepth + " in '" + context + "'");
                }
                var objType = inferType(fa.object(), scope, context, newDepth);
                if (objType != null && objType.isOption()) {
                    errors.add("Cannot access field '" + fa.field() + "' on Option<" + objType.inner().name() + "> without matching. Use 'match' or '?' operator. In: " + context);
                    return null;
                }
                return resolveFieldType(objType, fa.field());
            }

            case MethodCall mc -> {
                int newDepth = chainDepth + 1;
                if (newDepth > maxChainDepth) {
                    warnings.add("Law of Demeter: chain depth " + newDepth + " exceeds max " + maxChainDepth + " in '" + context + "'");
                }
                inferType(mc.object(), scope, context, newDepth);
                if (mc.object() instanceof Identifier objId) {
                    String varName = objId.name();
                    var varType = scope.get(varName);
                    if (varType != null && stateTypes.containsKey(varType.name())) {
                        var stateDecl = stateTypes.get(varType.name());
                        String currentState = varStates.getOrDefault(varName, stateDecl.variants().get(0).stateName());
                        var currentVariant = stateDecl.variants().stream()
                                .filter(v -> v.stateName().equals(currentState))
                                .findFirst().orElse(null);
                        if (currentVariant != null) {
                            var method = currentVariant.methods().stream()
                                    .filter(m -> m.name().equals(mc.method()))
                                    .findFirst().orElse(null);
                            if (method == null) {
                                errors.add("Method '" + mc.method() + "' is not available in state '" + currentState + "' of " + varType.name() + ". In: " + context);
                            } else {
                                varStates.put(varName, method.targetState());
                            }
                        }
                    }
                }
                return null;
            }

            case MatchExpr me -> {
                var subjectType = inferType(me.subject(), scope, context);
                // Check exhaustiveness
                if (subjectType != null && subjectType.name().equals("Result") && me.arms().size() < 2) {
                    errors.add("Non-exhaustive match on Result in '" + context + "': must handle both Ok and Err");
                }
                if (subjectType != null && subjectType.isOption() && me.arms().size() < 2) {
                    errors.add("Non-exhaustive match on Option in '" + context + "': must handle both Some and None");
                }
                return null;
            }

            case FnCall fc -> {
                // Check match exhaustiveness on Result
                if (fc.name().startsWith("__match__")) {
                    int armCount = Integer.parseInt(fc.name().substring("__match__".length()));
                    if (!fc.args().isEmpty()) {
                        var subjectType = inferType(fc.args().get(0), scope, context);
                        if (subjectType != null && subjectType.name().equals("Result") && armCount < 2) {
                            errors.add("Non-exhaustive match on Result in '" + context + "': must handle both Ok and Err");
                        }
                        if (subjectType != null && subjectType.isOption() && armCount < 2) {
                            errors.add("Non-exhaustive match on Option in '" + context + "': must handle both Some and None");
                        }
                    }
                    return null;
                }
                // Builtins
                if (fc.name().equals("printLn") || fc.name().equals("parseInt")) {
                    return null; // builtins, no type check needed
                }
                // Check nominal type constraints
                var fn = functions.get(fc.name());
                if (fn != null && fc.args().size() == fn.params().size()) {
                    for (int i = 0; i < fc.args().size(); i++) {
                        var argType = inferType(fc.args().get(i), scope, context);
                        var paramType = fn.params().get(i).type();
                        checkNominalType(argType, paramType, fc.name(), fn.params().get(i).name(), context);
                    }
                }
                if (fn != null) return fn.returnType().orElse(null);
                // Could be a type constructor
                if (typeAliases.containsKey(fc.name()) || structs.containsKey(fc.name())) {
                    return new TypeExpr(fc.name(), Optional.empty());
                }
                return null;
            }

            case OptionPropagate op -> {
                var innerType = inferType(op.expr(), scope, context);
                if (innerType != null && !innerType.isOption() && !innerType.name().equals("Result")) {
                    errors.add("'?' operator used on non-Option/Result type '" + innerType.name() + "' in '" + context + "'");
                }
                return innerType != null && innerType.isOption() ? innerType.inner() : innerType;
            }

            case BinaryOp bo -> {
                inferType(bo.left(), scope, context);
                inferType(bo.right(), scope, context);
                if (Set.of("==", "!=", "<", ">", "<=", ">=", "&&", "||").contains(bo.op())) {
                    return new TypeExpr("Bool", Optional.empty());
                }
                return inferType(bo.left(), scope, context);
            }

            case UnaryOp uo -> { return inferType(uo.operand(), scope, context); }
            case BlockExpr be -> { return null; }
            default -> { return null; }
        }
    }

    private void checkNominalType(TypeExpr argType, TypeExpr paramType, String fnName, String paramName, String context) {
        if (argType == null || paramType == null) return;
        String paramTypeName = paramType.name();
        String argTypeName = argType.name();
        // If param expects a nominal type
        if (typeAliases.containsKey(paramTypeName)) {
            // Reject raw primitives
            if (argTypeName.equals("String") || argTypeName.equals("Int") || argTypeName.equals("Float") || argTypeName.equals("Bool")) {
                errors.add("Type mismatch: cannot pass " + argTypeName + " as " + paramTypeName + " (parameter '" + paramName + "' of '" + fnName + "'). Use " + paramTypeName + "(...) constructor. In: " + context);
            }
            // Reject different nominal types
            else if (typeAliases.containsKey(argTypeName) && !argTypeName.equals(paramTypeName)) {
                errors.add("Type mismatch: cannot pass " + argTypeName + " as " + paramTypeName + " (parameter '" + paramName + "' of '" + fnName + "'). These are distinct nominal types. In: " + context);
            }
        }
    }

    private TypeExpr resolveFieldType(TypeExpr objType, String field) {
        if (objType == null) return null;
        var struct = structs.get(objType.name());
        if (struct == null) return null;
        for (var f : struct.fields()) {
            if (f.name().equals(field)) return f.type();
        }
        return null;
    }
}
