package dev.hll.checker;

import dev.hll.ast.Node;
import dev.hll.ast.Node.*;

import java.util.*;

public class TypeChecker {

    private final Map<String, TypeExpr> typeAliases = new HashMap<>();
    private final Map<String, Node.WhereConstraint> typeConstraints = new HashMap<>();
    private final Map<String, StructDecl> structs = new HashMap<>();
    private final Map<String, FnDecl> functions = new HashMap<>();
    private final Map<String, ImportDecl> imports = new HashMap<>();
    private final Set<String> effectTypes = new HashSet<>();
    private final Map<String, Node.StateDecl> stateTypes = new HashMap<>();
    private final Map<String, String> varStates = new HashMap<>();
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
                case StructDecl sd -> {
                    if (sd.name().startsWith("__effect__")) {
                        effectTypes.add(sd.name().substring("__effect__".length()));
                    } else {
                        structs.put(sd.name(), sd);
                    }
                }
                case FnDecl fd -> functions.put(fd.name(), fd);
                case ImportDecl id -> imports.put(id.alias(), id);
                case StateDecl sd -> stateTypes.put(sd.name(), sd);
                case TestDecl td -> {}
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

    public void checkBlock(Block block, Map<String, TypeExpr> scope, String context) {
        for (var stmt : block.statements()) {
            checkStatement(stmt, scope, context);
        }
    }

    private void checkStatement(Statement stmt, Map<String, TypeExpr> scope, String context) {
        switch (stmt) {
            case LetStmt ls -> {
                var exprType = inferType(ls.value(), scope, context);
                var resolvedType = ls.type().orElse(exprType);
                if (resolvedType != null) {
                    scope.put(ls.name(), resolvedType);
                }
                // Track state type: if value is TypeName.new(), register as state variable
                if (ls.value() instanceof MethodCall mc && mc.method().equals("new") && mc.object() instanceof Identifier typeId) {
                    if (stateTypes.containsKey(typeId.name())) {
                        scope.put(ls.name(), new TypeExpr(typeId.name(), java.util.Optional.empty()));
                        varStates.put(ls.name(), stateTypes.get(typeId.name()).variants().get(0).stateName());
                    }
                }
                // Track state rebinding: let conn = conn.method() → new state
                if (ls.value() instanceof MethodCall mc2 && mc2.object() instanceof Identifier objId2) {
                    String varName2 = objId2.name();
                    var varType2 = scope.get(varName2);
                    if (varType2 != null && stateTypes.containsKey(varType2.name())) {
                        // The method call already updated varStates in inferType
                        // Copy the state to the new binding (same name or different)
                        String newState = varStates.get(varName2);
                        if (newState != null) {
                            scope.put(ls.name(), varType2);
                            varStates.put(ls.name(), newState);
                        }
                    }
                }
                // Check: effectful function assigned without handle
                if (ls.value() instanceof FnCall fc) {
                    var calledFn = functions.get(fc.name());
                    var callerFn = functions.get(context);
                    if (calledFn != null && !calledFn.effects().isEmpty()) {
                        boolean callerPropagates = callerFn != null && callerFn.effects().containsAll(calledFn.effects());
                        if (!callerPropagates) {
                            errors.add("Function '" + fc.name() + "' has effects " + calledFn.effects() + " that are not handled. Use 'handle' to manage effects. In: " + context);
                        }
                    }
                }
            }
            case ReturnStmt rs -> {
                if (rs.value().isPresent()) {
                    inferType(rs.value().get(), scope, context);
                }
            }
            case ExprStmt es -> {
                inferType(es.expr(), scope, context);
                if (es.expr() instanceof FnCall fc) {
                    var fn = functions.get(fc.name());
                    if (fn != null && !fn.effects().isEmpty()) {
                        errors.add("Function '" + fc.name() + "' has effects " + fn.effects() + " that are not handled. Use 'handle' to manage effects. In: " + context);
                    }
                }
            }
            case MatchStmt ms -> checkMatch(ms, scope, context);
            case AssertStmt as -> inferType(as.condition(), scope, context);
            case ExpectErrorStmt ee -> {}
            case IfStmt is -> {
                inferType(is.condition(), scope, context);
                checkBlock(is.thenBlock(), new HashMap<>(scope), context);
                is.elseBlock().ifPresent(b -> checkBlock(b, new HashMap<>(scope), context));
            }
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
    }

    private TypeExpr inferType(Expr expr, Map<String, TypeExpr> scope, String context) {
        return inferType(expr, scope, context, 0);
    }

    private TypeExpr inferType(Expr expr, Map<String, TypeExpr> scope, String context, int chainDepth) {
        if (expr == null) return null;
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
                // State checking: if object is a state variable, verify method is available
                if (mc.object() instanceof Identifier objId) {
                    String varName = objId.name();
                    var varType = scope.get(varName);
                    if (varType != null && stateTypes.containsKey(varType.name())) {
                        var stateDecl = stateTypes.get(varType.name());
                        String currentState = varStates.getOrDefault(varName, stateDecl.variants().get(0).stateName());
                        // Find current state variant
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
                                // Transition to target state
                                varStates.put(varName, method.targetState());
                            }
                        }
                    }
                }
                return null;
            }

            case FnCall fc -> {
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
