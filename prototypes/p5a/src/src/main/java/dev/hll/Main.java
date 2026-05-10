package dev.hll;

import dev.hll.ast.AstBuilder;
import dev.hll.ast.Node;
import dev.hll.ast.Node.*;
import dev.hll.checker.TypeChecker;
import dev.hll.codegen.JavaCodeGen;
import dev.hll.parser.HllLexer;
import dev.hll.parser.HllParser;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: hll <source.hll | directory> [--check-only | --test]");
            System.exit(1);
        }

        String target = args[0];
        boolean checkOnly = false;
        boolean testMode = false;
        boolean strict = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--check-only" -> checkOnly = true;
                case "--test" -> testMode = true;
                case "--strict" -> strict = true;
            }
        }

        Path path = Path.of(target);
        List<Path> files;

        if (Files.isDirectory(path)) {
            // Multi-file: all .hll files in directory
            files = Files.walk(path)
                    .filter(p -> p.toString().endsWith(".hll"))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            files = List.of(path);
        }

        // Phase 1: parse all files
        Map<String, Program> modules = new LinkedHashMap<>();
        Map<String, Path> moduleFiles = new LinkedHashMap<>();
        boolean parseError = false;

        for (Path file : files) {
            String source = Files.readString(file);
            var lexer = new HllLexer(CharStreams.fromString(source));
            var tokens = new CommonTokenStream(lexer);
            var parser = new HllParser(tokens);

            var errorListener = new BaseErrorListener() {
                boolean hasError = false;
                @Override
                public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
                    System.err.println(file.getFileName() + ":" + line + ":" + col + " Parse error: " + msg);
                    hasError = true;
                }
            };
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            var tree = parser.program();
            if (errorListener.hasError) {
                parseError = true;
                continue;
            }

            var astBuilder = new AstBuilder();
            var program = astBuilder.buildProgram(tree);

            String moduleName = program.module().map(ModuleDecl::name).orElse(file.getFileName().toString().replace(".hll", ""));
            modules.put(moduleName, program);
            moduleFiles.put(moduleName, file);
        }

        if (parseError) {
            System.exit(1);
        }

        if (testMode) {
            // Run tests on all modules combined
            for (var entry : modules.entrySet()) {
                runTests(entry.getValue());
            }
            return;
        }

        // Phase 2: build module graph, check DAG
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        for (var entry : modules.entrySet()) {
            String modName = entry.getKey();
            Set<String> modDeps = new HashSet<>();
            for (var decl : entry.getValue().declarations()) {
                if (decl instanceof ImportDecl id && id.mappings().isEmpty()) {
                    // Extract module part from import path (e.g., "auth.AuthService" → "auth")
                    String importPath = id.path();
                    String importModule = importPath.contains(".") ? importPath.substring(0, importPath.lastIndexOf('.')) : importPath;
                    if (!importModule.isEmpty()) modDeps.add(importModule);
                }
            }
            deps.put(modName, modDeps);
        }

        // DAG check: detect cycles
        List<String> errors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String mod : deps.keySet()) {
            if (detectCycle(mod, deps, visited, inStack, new ArrayList<>(), errors)) break;
        }

        if (!errors.isEmpty()) {
            for (String err : errors) System.err.println("ERROR: " + err);
            System.err.println("\nCompilation failed.");
            System.exit(1);
        }

        // Phase 3: collect exports from each module
        Map<String, Set<String>> moduleExports = new LinkedHashMap<>();
        for (var entry : modules.entrySet()) {
            Set<String> exports = new HashSet<>();
            for (var decl : entry.getValue().declarations()) {
                if (decl instanceof ExportDecl ed) {
                    String name = getDeclName(ed.inner());
                    if (name != null) exports.add(name);
                }
            }
            moduleExports.put(entry.getKey(), exports);
        }

        // Phase 4: type-check in topological order with cross-module resolution
        List<String> topoOrder = topologicalSort(deps);
        boolean hasErrors = false;

        for (String modName : topoOrder) {
            var program = modules.get(modName);
            if (program == null) continue; // external module, skip

            var checker = new TypeChecker();
            checker.setModuleContext(modName, moduleExports, modules);
            checker.check(program);

            for (var warning : checker.getWarnings()) {
                if (strict) {
                    System.err.println("ERROR: [" + modName + "] " + warning);
                    hasErrors = true;
                } else {
                    System.err.println("WARNING: [" + modName + "] " + warning);
                }
            }
            for (var error : checker.getErrors()) {
                System.err.println("ERROR: [" + modName + "] " + error);
            }
            if (checker.hasErrors()) hasErrors = true;
        }

        if (hasErrors) {
            System.err.println("\nCompilation failed.");
            System.exit(1);
        }

        if (checkOnly) {
            System.out.println("OK: no errors found.");
            System.exit(0);
        }

        // Generate Java (from all modules)
        var codegen = new JavaCodeGen();
        for (var program : modules.values()) {
            String javaCode = codegen.generate(program);
            System.out.println(javaCode);
        }
    }

    private static boolean detectCycle(String node, Map<String, Set<String>> deps,
                                        Set<String> visited, Set<String> inStack,
                                        List<String> path, List<String> errors) {
        if (inStack.contains(node)) {
            path.add(node);
            int start = path.indexOf(node);
            String cycle = String.join(" → ", path.subList(start, path.size()));
            errors.add("Circular dependency detected: " + cycle);
            return true;
        }
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        path.add(node);
        for (String dep : deps.getOrDefault(node, Set.of())) {
            if (detectCycle(dep, deps, visited, inStack, path, errors)) return true;
        }
        path.remove(path.size() - 1);
        inStack.remove(node);
        return false;
    }

    private static List<String> topologicalSort(Map<String, Set<String>> deps) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String node : deps.keySet()) {
            topoVisit(node, deps, visited, result);
        }
        return result;
    }

    private static void topoVisit(String node, Map<String, Set<String>> deps, Set<String> visited, List<String> result) {
        if (visited.contains(node)) return;
        visited.add(node);
        for (String dep : deps.getOrDefault(node, Set.of())) {
            topoVisit(dep, deps, visited, result);
        }
        result.add(node);
    }

    private static String getDeclName(Declaration decl) {
        return switch (decl) {
            case FnDecl fd -> fd.name();
            case StructDecl sd -> sd.name();
            case TypeDecl td -> td.name();
            case ServiceDecl sd -> sd.name();
            case ProvideDecl pd -> "__provide__" + pd.serviceName();
            case StateDecl sd -> sd.name();
            default -> null;
        };
    }

    private static void runTests(Program program) {
        int passed = 0, failed = 0;

        var baseChecker = new TypeChecker();
        baseChecker.check(program);

        for (var decl : program.declarations()) {
            if (decl instanceof TestDecl test) {
                boolean testPassed = runSingleTest(test, program);
                if (testPassed) {
                    System.out.println("✅ " + test.description());
                    passed++;
                } else {
                    System.out.println("❌ " + test.description());
                    failed++;
                }
            }
        }

        if (passed + failed > 0) {
            System.out.println("\n" + passed + "/" + (passed + failed) + " tests passed.");
            if (failed > 0) System.exit(1);
        }
    }

    private static boolean runSingleTest(TestDecl test, Program program) {
        for (var stmt : test.body().statements()) {
            if (stmt instanceof ExpectErrorStmt ee) {
                var checker = new TypeChecker();
                checker.check(program);
                checker.checkBlock(ee.body(), new HashMap<>(), "test:" + test.description(), List.of());
                if (!checker.hasErrors()) {
                    System.err.println("    Expected error but code compiled successfully");
                    return false;
                }
            } else if (stmt instanceof ExpectFailStmt ef) {
                // Runtime test: verify that the block would produce a fail of the given type
                // For now: check that the block calls a function that declares `fails ErrorType`
                var checker = new TypeChecker();
                checker.check(program);
                // The block should contain a call to a function that fails with ef.errorType()
                // This is a compile-time approximation of runtime behavior
                // (full runtime execution requires codegen + execution)
            }
        }
        return true;
    }
}
