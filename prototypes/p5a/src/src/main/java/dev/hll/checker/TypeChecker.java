package dev.hll.checker;

import dev.hll.ast.Node;
import dev.hll.ast.Node.*;

import java.util.*;
import java.util.stream.Collectors;

public class TypeChecker {

    private final Map<String, TypeExpr> typeAliases = new HashMap<>();
    private final Map<String, Node.WhereConstraint> typeConstraints = new HashMap<>();
    private final Map<String, StructDecl> structs = new HashMap<>();
    private final Map<String, FnDecl> functions = new HashMap<>();
    private final Map<String, Node.StateDecl> stateTypes = new HashMap<>();
    private final Map<String, String> varStates = new HashMap<>();
    private final Map<String, ImportDecl> imports = new HashMap<>();
    private final Map<String, ServiceDecl> services = new HashMap<>();
    private final Map<String, ProvideDecl> provides = new HashMap<>();
    private final Set<String> exportedSymbols = new HashSet<>();
    private final Map<String, Set<String>> moduleExports = new HashMap<>();
    private final Set<String> importedSymbols = new HashSet<>();
    private final Set<String> importedModules = new HashSet<>();
    private String currentModule = "";
    // Multi-file context
    private Map<String, Set<String>> allModuleExports = new HashMap<>();
    private Map<String, Program> allModules = new HashMap<>();
    private final Set<String> calledFunctions = new HashSet<>();
    private final Set<String> spawnedVars = new HashSet<>();
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

    public void setModuleContext(String moduleName, Map<String, Set<String>> allExports, Map<String, Program> modules) {
        this.currentModule = moduleName;
        this.allModuleExports = allExports;
        this.allModules = modules;
    }

    public void check(Program program) {
        // Module declaration
        program.module().ifPresent(m -> currentModule = m.name());

        // Pass 1: collect declarations
        for (var decl : program.declarations()) {
            collectDeclaration(decl, false);
        }

        // Pass 2: check module-level constraints
        checkModuleConstraints(program);

        // Pass 3: check function bodies
        for (var decl : program.declarations()) {
            checkDeclaration(decl);
        }

        // Pass 4: dead code detection (functions declared but never called)
        for (var entry : functions.entrySet()) {
            String fnName = entry.getKey();
            if (fnName.equals("main")) continue; // main is entry point
            if (exportedSymbols.contains(fnName)) continue; // exported = public API
            if (!calledFunctions.contains(fnName)) {
                warnings.add("Dead code: function '" + fnName + "' is declared but never called");
            }
        }
    }

    private void collectDeclaration(Declaration decl, boolean exported) {
        switch (decl) {
            case TypeDecl td -> {
                typeAliases.put(td.name(), new TypeExpr(td.baseType(), Optional.empty()));
                td.constraint().ifPresent(c -> typeConstraints.put(td.name(), c));
                if (exported) exportedSymbols.add(td.name());
            }
            case StructDecl sd -> {
                structs.put(sd.name(), sd);
                if (exported) exportedSymbols.add(sd.name());
            }
            case FnDecl fd -> {
                functions.put(fd.name(), fd);
                if (exported) exportedSymbols.add(fd.name());
            }
            case ImportDecl id -> {
                if (!id.alias().isEmpty()) imports.put(id.alias(), id);
                importedModules.add(id.path());
                for (String sym : id.symbols()) {
                    importedSymbols.add(sym);
                    importedModules.add(id.path());
                }
            }
            case StateDecl sd -> {
                stateTypes.put(sd.name(), sd);
                if (exported) exportedSymbols.add(sd.name());
            }
            case ServiceDecl sd -> {
                services.put(sd.name(), sd);
                if (exported) exportedSymbols.add(sd.name());
            }
            case ProvideDecl pd -> {
                provides.put(pd.serviceName(), pd);
                if (exported) exportedSymbols.add("__provide__" + pd.serviceName());
            }
            case ExportDecl ed -> collectDeclaration(ed.inner(), true);
            case TestDecl td -> {
                // Check mock statements inside test bodies
                checkBlock(td.body(), new HashMap<>(), "test:" + td.description(), List.of());
            }
        }
    }

    private void checkModuleConstraints(Program program) {
        // Check: circular dependencies (within single file, multi-module)
        // Collect all module declarations in this file
        Map<String, Set<String>> moduleDeps = new HashMap<>();
        String lastModule = "";
        for (var decl : program.declarations()) {
            if (decl instanceof ImportDecl id) {
                moduleDeps.computeIfAbsent(currentModule, k -> new HashSet<>()).add(id.path());
            }
        }
        // Check for self-import
        for (var entry : moduleDeps.entrySet()) {
            if (entry.getValue().contains(entry.getKey())) {
                errors.add("Circular dependency: module '" + entry.getKey() + "' imports itself");
            }
        }
        // Check for A→B→A cycles (simple 2-hop check for single-file multi-module)
        // In a real compiler this would be a full DAG check across files

        // Check: God Interface — service with too many methods
        for (var entry : services.entrySet()) {
            if (entry.getValue().methods().size() > 7) {
                warnings.add("God Interface: service '" + entry.getKey() + "' has " + entry.getValue().methods().size() + " methods (max recommended: 7)");
            }
        }

        // Check: provide completeness
        for (var entry : provides.entrySet()) {
            String svcName = entry.getKey();
            var provide = entry.getValue();
            var service = services.get(svcName);
            if (service == null) {
                // Service might be imported — skip check for now
                continue;
            }
            // Check all service methods are implemented
            Set<String> providedMethods = provide.methods().stream()
                    .map(FnDecl::name).collect(Collectors.toSet());
            for (var svcFn : service.methods()) {
                if (!providedMethods.contains(svcFn.name())) {
                    errors.add("Incomplete provide for '" + svcName + "': missing method '" + svcFn.name() + "'");
                }
            }
        }

        // Check: needs satisfaction
        for (var entry : provides.entrySet()) {
            var provide = entry.getValue();
            for (var need : provide.needs()) {
                // The needed service must have a provide (declared or imported)
                if (!provides.containsKey(need.serviceType()) && !importedSymbols.contains(need.serviceType())) {
                    errors.add("Unsatisfied dependency: '" + entry.getKey() + "' needs '" + need.serviceType() + "' but no provide exists in the module graph");
                }
            }
        }

        // Check: visibility — imported symbols must be exported by their source module
        for (var decl : program.declarations()) {
            if (decl instanceof ImportDecl id && id.mappings().isEmpty()) {
                String importPath = id.path();
                // Skip stdlib imports (hll.*)
                if (importPath.startsWith("hll.")) continue;
                // Extract module and symbol: "auth.AuthService" → module="auth", symbol="AuthService"
                String importModule = importPath.contains(".") ? importPath.substring(0, importPath.lastIndexOf('.')) : importPath;
                String symbol = importPath.contains(".") ? importPath.substring(importPath.lastIndexOf('.') + 1) : "";

                for (String sym : id.symbols().isEmpty() && !symbol.isEmpty() ? List.of(symbol) : id.symbols()) {
                    // Check if the module exists
                    if (!allModuleExports.isEmpty()) {
                        if (!allModuleExports.containsKey(importModule) && !importModule.equals(currentModule)) {
                            errors.add("Cannot resolve module '" + importModule + "': module not found");
                            continue;
                        }
                        // Check if the symbol is exported
                        Set<String> exports = allModuleExports.getOrDefault(importModule, Set.of());
                        if (!exports.isEmpty() && !exports.contains(sym)) {
                            errors.add("Cannot import '" + sym + "' from module '" + importModule + "': symbol is not exported");
                        }
                    } else {
                        // Single-file mode: check if symbol exists locally
                        if (!services.containsKey(sym) && !structs.containsKey(sym) &&
                            !functions.containsKey(sym) && !typeAliases.containsKey(sym) &&
                            !stateTypes.containsKey(sym)) {
                            errors.add("Cannot resolve import '" + sym + "' from module '" + importModule + "': symbol not found or not exported");
                        }
                    }
                }
            }
        }
    }

    private void checkDeclaration(Declaration decl) {
        switch (decl) {
            case FnDecl fd -> checkFn(fd);
            case ExportDecl ed -> checkDeclaration(ed.inner());
            case ProvideDecl pd -> {
                for (var fn : pd.methods()) {
                    checkFn(fn);
                }
            }
            default -> {}
        }
    }

    private void checkFn(FnDecl fn) {
        var scope = new HashMap<String, TypeExpr>();
        for (var param : fn.params()) {
            scope.put(param.name(), param.type());
        }
        varStates.clear();
        checkBlock(fn.body(), scope, fn.name(), fn.fails());

        // Check: if function declares a return type, all paths must return
        if (fn.returnType().isPresent() && !fn.body().statements().isEmpty()) {
            if (!allPathsReturn(fn.body())) {
                errors.add("Not all paths return a value in function '" + fn.name() + "'. Declared return type: " + fn.returnType().get().name());
            }
        }
    }

    private boolean allPathsReturn(Block block) {
        if (block.statements().isEmpty()) return false;
        var last = block.statements().get(block.statements().size() - 1);
        return statementReturns(last);
    }

    private boolean statementReturns(Statement stmt) {
        return switch (stmt) {
            case ReturnStmt rs -> true;
            case ExprStmt es -> true; // expression as last statement = implicit return
            case IfStmt is -> is.elseBlock().isPresent()
                    && allPathsReturn(is.thenBlock())
                    && allPathsReturn(is.elseBlock().get());
            case LetStmt ls -> false;
            default -> false;
        };
    }

    public void checkBlock(Block block, Map<String, TypeExpr> scope, String context, List<String> currentFails) {
        for (var stmt : block.statements()) {
            checkStatement(stmt, scope, context, currentFails);
        }
    }

    private void checkStatement(Statement stmt, Map<String, TypeExpr> scope, String context, List<String> currentFails) {
        switch (stmt) {
            case LetStmt ls -> {
                var exprType = inferType(ls.value(), scope, context);
                scope.put(ls.name(), ls.type().orElse(exprType));
                // Track spawned actors
                if (ls.value() instanceof SpawnExpr) {
                    spawnedVars.add(ls.name());
                }
                // Check: no aliasing of spawned actors
                if (ls.value() instanceof Identifier id && spawnedVars.contains(id.name())) {
                    errors.add("Cannot alias actor '" + id.name() + "': actors cannot be shared. Each actor must have a single owner. In: " + context);
                }
                // Check: if calling a function that fails, must handle or propagate
                if (!ls.hasErrorHandler() && ls.value() instanceof FnCall fc) {
                    var calledFn = functions.get(fc.name());
                    if (calledFn != null && !calledFn.fails().isEmpty()) {
                        // Check if current function declares the same fails (propagation)
                        for (String err : calledFn.fails()) {
                            if (!currentFails.contains(err) && !ls.hasErrorHandler()) {
                                errors.add("Unhandled error '" + err + "' from '" + fc.name() + "' in '" + context + "'. Must handle with | handler or propagate with 'fails " + err + "'");
                            }
                        }
                    }
                }
                // Check: service instantiation
                if (ls.value() instanceof FnCall fc2 && services.containsKey(fc2.name())) {
                    errors.add("Cannot instantiate service '" + fc2.name() + "' directly. Services must be injected via 'needs'. In: " + context);
                }
            }
            case ReturnStmt rs -> {
                if (rs.value().isPresent()) {
                    inferType(rs.value().get(), scope, context);
                }
            }
            case ExprStmt es -> {
                var exprType = inferType(es.expr(), scope, context);
                if (exprType != null && exprType.name().equals("Result")) {
                    errors.add("Result of function call is not consumed. Must handle with match or assign to variable. In: " + context);
                }
                // Check fails on expr statements too
                if (es.expr() instanceof FnCall fc) {
                    var calledFn = functions.get(fc.name());
                    if (calledFn != null && !calledFn.fails().isEmpty()) {
                        for (String err : calledFn.fails()) {
                            if (!currentFails.contains(err)) {
                                errors.add("Unhandled error '" + err + "' from '" + fc.name() + "' in '" + context + "'. Must handle with | handler or propagate with 'fails " + err + "'");
                            }
                        }
                    }
                }
                // Check method calls on state types
                if (es.expr() instanceof MethodCall mc && mc.object() instanceof Identifier objId) {
                    // state tracking handled in inferType
                }
            }
            case MatchStmt ms -> checkMatch(ms, scope, context);
            case IfStmt is -> {
                inferType(is.condition(), scope, context);
                checkBlock(is.thenBlock(), new HashMap<>(scope), context, currentFails);
                is.elseBlock().ifPresent(b -> checkBlock(b, new HashMap<>(scope), context, currentFails));
            }
            case AssertStmt as -> inferType(as.condition(), scope, context);
            case ExpectErrorStmt ee -> {}
            case ExpectFailStmt ef -> {
                // Verify the error type exists (is a declared struct)
                if (!structs.containsKey(ef.errorType())) {
                    errors.add("Unknown error type '" + ef.errorType() + "' in expectFail. In: " + context);
                }
                // Check the block — it should contain a call that fails with this error
                checkBlock(ef.body(), new HashMap<>(scope), context, List.of());
            }
            case MockStmt ms -> {
                // Check: mock target must be a declared service
                if (!services.containsKey(ms.serviceName())) {
                    errors.add("Cannot mock '" + ms.serviceName() + "': not a declared service. In: " + context);
                } else {
                    // Check: mock must implement all methods of the service
                    var service = services.get(ms.serviceName());
                    var mockMethods = ms.methods().stream().map(FnDecl::name).collect(java.util.stream.Collectors.toSet());
                    for (var svcFn : service.methods()) {
                        if (!mockMethods.contains(svcFn.name())) {
                            errors.add("Incomplete mock for '" + ms.serviceName() + "': missing method '" + svcFn.name() + "'. In: " + context);
                        }
                    }
                }
            }
            case WhileStmt ws -> {
                inferType(ws.condition(), scope, context);
                checkBlock(ws.body(), scope, context, currentFails);
            }
            case ForInStmt fi -> {
                inferType(fi.iterable(), scope, context);
                var innerScope = new HashMap<>(scope);
                innerScope.put(fi.varName(), null);
                checkBlock(fi.body(), innerScope, context, currentFails);
            }
            case AssignStmt as2 -> inferType(as2.value(), scope, context);
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
        if (subjectType != null && subjectType.name().equals("Result")) {
            boolean hasOk = false, hasErr = false;
            for (var arm : ms.arms()) {
                if (arm.pattern() instanceof WildcardPattern) { hasOk = true; hasErr = true; }
            }
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
        if (expr == null) return null;
        switch (expr) {
            case Identifier id -> {
                if (id.name().equals("null")) {
                    errors.add("'null' does not exist in HLL. Use Option<T> for absent values. In: " + context);
                    return null;
                }
                var type = scope.get(id.name());
                if (type == null && services.containsKey(id.name())) {
                    // Service used as type reference (for injection)
                    return new TypeExpr(id.name(), Optional.empty());
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
                // State type constructor: StateType.new()
                if (mc.method().equals("new") && mc.object() instanceof Identifier objId && stateTypes.containsKey(objId.name())) {
                    var stateDecl = stateTypes.get(objId.name());
                    return new TypeExpr(objId.name(), Optional.empty());
                }
                // State tracking on variables
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
                if (subjectType != null && subjectType.name().equals("Result") && me.arms().size() < 2) {
                    errors.add("Non-exhaustive match on Result in '" + context + "': must handle both Ok and Err");
                }
                if (subjectType != null && subjectType.isOption() && me.arms().size() < 2) {
                    errors.add("Non-exhaustive match on Option in '" + context + "': must handle both Some and None");
                }
                return null;
            }

            case FnCall fc -> {
                calledFunctions.add(fc.name());
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
                if (fc.name().equals("println") || fc.name().equals("parseInt")) {
                    return null;
                }
                // Check service instantiation
                if (services.containsKey(fc.name())) {
                    errors.add("Cannot instantiate service '" + fc.name() + "' directly. Services must be injected via 'needs'. In: " + context);
                    return null;
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
                if (typeAliases.containsKey(fc.name()) || structs.containsKey(fc.name())) {
                    return new TypeExpr(fc.name(), Optional.empty());
                }
                return null;
            }

            case FailExpr fe -> {
                return null;
            }

            case SpawnExpr se -> {
                // spawn only works on services
                if (!services.containsKey(se.serviceName())) {
                    errors.add("Cannot spawn '" + se.serviceName() + "': not a declared service. Only services can be spawned as actors. In: " + context);
                }
                return new TypeExpr(se.serviceName(), Optional.empty());
            }

            case AwaitExpr ae -> {
                return inferType(ae.expr(), scope, context, chainDepth);
            }

            case LambdaExpr le -> {
                // Lambda is a value — type check the body with param in scope
                var lambdaScope = new HashMap<>(scope);
                lambdaScope.put(le.param(), null);
                inferType(le.body(), lambdaScope, context);
                return null; // lambda type
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
        if (typeAliases.containsKey(paramTypeName)) {
            if (argTypeName.equals("String") || argTypeName.equals("Int") || argTypeName.equals("Float") || argTypeName.equals("Bool")) {
                errors.add("Type mismatch: cannot pass " + argTypeName + " as " + paramTypeName + " (parameter '" + paramName + "' of '" + fnName + "'). Use " + paramTypeName + "(...) constructor. In: " + context);
            } else if (typeAliases.containsKey(argTypeName) && !argTypeName.equals(paramTypeName)) {
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
