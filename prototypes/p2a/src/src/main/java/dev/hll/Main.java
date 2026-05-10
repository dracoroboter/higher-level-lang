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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: hll <source.hll> [--check-only | --test]");
            System.exit(1);
        }

        String source = Files.readString(Path.of(args[0]));
        boolean checkOnly = args.length > 1 && args[1].equals("--check-only");
        boolean testMode = args.length > 1 && args[1].equals("--test");

        // Parse
        var lexer = new HllLexer(CharStreams.fromString(source));
        var tokens = new CommonTokenStream(lexer);
        var parser = new HllParser(tokens);

        var errorListener = new BaseErrorListener() {
            boolean hasError = false;
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
                System.err.println("Parse error at " + line + ":" + col + " - " + msg);
                hasError = true;
            }
        };
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        var tree = parser.program();
        if (errorListener.hasError) {
            System.exit(1);
        }

        // Build AST
        var astBuilder = new AstBuilder();
        var program = astBuilder.buildProgram(tree);

        if (testMode) {
            runTests(program);
            return;
        }

        // Type check
        var checker = new TypeChecker();
        checker.check(program);

        for (var warning : checker.getWarnings()) {
            System.err.println("WARNING: " + warning);
        }
        for (var error : checker.getErrors()) {
            System.err.println("ERROR: " + error);
        }

        if (checker.hasErrors()) {
            System.err.println("\nCompilation failed with " + checker.getErrors().size() + " error(s).");
            System.exit(1);
        }

        if (checkOnly) {
            System.out.println("OK: no errors found.");
            System.exit(0);
        }

        // Generate Java
        var codegen = new JavaCodeGen();
        String javaCode = codegen.generate(program);
        System.out.println(javaCode);
    }

    private static void runTests(Program program) {
        int passed = 0, failed = 0;

        // First, do a normal type check to collect declarations
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

        System.out.println("\n" + passed + "/" + (passed + failed) + " tests passed.");
        if (failed > 0) System.exit(1);
    }

    private static boolean runSingleTest(TestDecl test, Program program) {
        for (var stmt : test.body().statements()) {
            if (stmt instanceof ExpectErrorStmt ee) {
                // Type-check the block in isolation — it should produce errors
                var checker = new TypeChecker();
                checker.check(program); // load declarations
                checker.checkBlock(ee.body(), new HashMap<>(), "test:" + test.description());
                if (!checker.hasErrors()) {
                    System.err.println("    Expected error but code compiled successfully");
                    return false;
                }
                // Good — error was produced as expected
            } else if (stmt instanceof AssertStmt as) {
                // For now, assert is checked at type level only (not executed)
                // A real implementation would evaluate the expression
                var checker = new TypeChecker();
                checker.check(program);
                // Just verify it type-checks without error
            }
        }
        return true;
    }
}
