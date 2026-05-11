package dev.hll.ast;

import dev.hll.parser.HllBaseVisitor;
import dev.hll.parser.HllParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AstBuilder extends HllBaseVisitor<Object> {

    public Node.Program buildProgram(HllParser.ProgramContext ctx) {
        Optional<Node.ModuleDecl> module = Optional.empty();
        if (ctx.moduleDecl() != null) {
            String name = ctx.moduleDecl().qualifiedName().IDENT().stream()
                    .map(t -> t.getText()).collect(Collectors.joining("."));
            module = Optional.of(new Node.ModuleDecl(name));
        }
        var decls = ctx.declaration().stream()
                .map(d -> (Node.Declaration) visitDeclaration(d))
                .filter(d -> d != null)
                .collect(Collectors.toList());
        return new Node.Program(module, decls);
    }

    public Node.Declaration visitDeclaration(HllParser.DeclarationContext ctx) {
        if (ctx.typeDecl() != null) return (Node.Declaration) visit(ctx.typeDecl());
        if (ctx.unionTypeDecl() != null) return (Node.Declaration) visit(ctx.unionTypeDecl());
        if (ctx.structDecl() != null) return (Node.Declaration) visit(ctx.structDecl());
        if (ctx.fnDecl() != null) return (Node.Declaration) visit(ctx.fnDecl());
        if (ctx.importDecl() != null) return (Node.Declaration) visit(ctx.importDecl());
        if (ctx.testDecl() != null) return (Node.Declaration) visit(ctx.testDecl());
        if (ctx.serviceDecl() != null) return visitServiceDecl(ctx.serviceDecl());
        if (ctx.provideDecl() != null) return visitProvideDecl(ctx.provideDecl());
        if (ctx.exportDecl() != null) return visitExportDecl(ctx.exportDecl());
        if (ctx.stateDecl() != null) {
            var sc = ctx.stateDecl();
            String name = sc.IDENT().getText();
            var variants = sc.stateVariant().stream()
                    .map(sv -> {
                        String stateName = sv.IDENT().getText();
                        var methods = sv.stateFn().stream()
                                .map(sf -> new Node.StateFn(
                                        sf.IDENT(0).getText(),
                                        sf.params() != null ? sf.params().param().stream()
                                                .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                                                .collect(Collectors.toList()) : List.of(),
                                        sf.IDENT(1).getText()))
                                .collect(Collectors.toList());
                        return new Node.StateVariant(stateName, methods);
                    })
                    .collect(Collectors.toList());
            return new Node.StateDecl(name, variants);
        }
        if (ctx.effectDecl() != null) {
            var ec = ctx.effectDecl();
            String name = ec.IDENT().getText();
            var fields = ec.fieldDecl().stream()
                    .map(f -> new Node.Field(f.IDENT().getText(), buildTypeExpr(f.typeExpr())))
                    .collect(Collectors.toList());
            return new Node.StructDecl("__effect__" + name, fields);
        }
        return null;
    }

    public Node.ExportDecl visitExportDecl(HllParser.ExportDeclContext ctx) {
        Node.Declaration inner = null;
        if (ctx.fnDecl() != null) inner = (Node.Declaration) visit(ctx.fnDecl());
        else if (ctx.structDecl() != null) inner = (Node.Declaration) visit(ctx.structDecl());
        else if (ctx.typeDecl() != null) inner = (Node.Declaration) visit(ctx.typeDecl());
        else if (ctx.serviceDecl() != null) inner = visitServiceDecl(ctx.serviceDecl());
        else if (ctx.provideDecl() != null) inner = visitProvideDecl(ctx.provideDecl());
        else if (ctx.stateDecl() != null) {
            var sc = ctx.stateDecl();
            String name = sc.IDENT().getText();
            var variants = sc.stateVariant().stream()
                    .map(sv -> {
                        String stateName = sv.IDENT().getText();
                        var methods = sv.stateFn().stream()
                                .map(sf -> new Node.StateFn(
                                        sf.IDENT(0).getText(),
                                        sf.params() != null ? sf.params().param().stream()
                                                .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                                                .collect(Collectors.toList()) : List.of(),
                                        sf.IDENT(1).getText()))
                                .collect(Collectors.toList());
                        return new Node.StateVariant(stateName, methods);
                    })
                    .collect(Collectors.toList());
            inner = new Node.StateDecl(name, variants);
        }
        return new Node.ExportDecl(inner);
    }

    public Node.ServiceDecl visitServiceDecl(HllParser.ServiceDeclContext ctx) {
        String name = ctx.IDENT().getText();
        var methods = ctx.serviceFn().stream()
                .map(sf -> {
                    String fnName = sf.IDENT().getText();
                    List<Node.Param> params = sf.params() != null
                            ? sf.params().param().stream()
                                .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                                .collect(Collectors.toList())
                            : List.of();
                    Node.TypeExpr retType = buildTypeExpr(sf.typeExpr());
                    List<String> fails = sf.failsClause() != null
                            ? sf.failsClause().IDENT().stream().map(t -> t.getText()).collect(Collectors.toList())
                            : List.of();
                    return new Node.ServiceFn(fnName, params, retType, fails);
                })
                .collect(Collectors.toList());
        return new Node.ServiceDecl(name, methods);
    }

    public Node.ProvideDecl visitProvideDecl(HllParser.ProvideDeclContext ctx) {
        String serviceName = ctx.IDENT().getText();
        List<Node.NeedsDecl> needs = new ArrayList<>();
        List<Node.FnDecl> methods = new ArrayList<>();
        for (var body : ctx.provideBody()) {
            if (body.needsDecl() != null) {
                var nd = body.needsDecl();
                needs.add(new Node.NeedsDecl(nd.IDENT(0).getText(), nd.IDENT(1).getText()));
            } else if (body.fnDecl() != null) {
                methods.add((Node.FnDecl) visit(body.fnDecl()));
            }
        }
        return new Node.ProvideDecl(serviceName, needs, methods);
    }

    @Override
    public Object visitTypeDecl(HllParser.TypeDeclContext ctx) {
        String name = ctx.IDENT().getText();
        String baseType = ctx.typeExpr().getText();
        Optional<Node.WhereConstraint> constraint = Optional.empty();
        if (ctx.whereConstraint() != null) {
            var qc = ctx.whereConstraint().qualifiedCall();
            var idents = qc.IDENT();
            String qualifier = idents.size() > 1
                    ? idents.subList(0, idents.size() - 1).stream()
                        .map(t -> t.getText()).reduce((a, b) -> a + "." + b).orElse("")
                    : "";
            String method = idents.get(idents.size() - 1).getText();
            List<Node.Expr> args = qc.args() != null
                    ? qc.args().expr().stream().map(e -> (Node.Expr) visit(e)).collect(Collectors.toList())
                    : List.of();
            constraint = Optional.of(new Node.WhereConstraint(qualifier, method, args));
        }
        return new Node.TypeDecl(name, baseType, constraint);
    }

    @Override
    public Object visitStructDecl(HllParser.StructDeclContext ctx) {
        String name = ctx.IDENT().getText();
        var fields = ctx.fieldDecl().stream()
                .map(f -> new Node.Field(f.IDENT().getText(), buildTypeExpr(f.typeExpr())))
                .collect(Collectors.toList());
        return new Node.StructDecl(name, fields);
    }

    @Override
    public Object visitUnionTypeDecl(HllParser.UnionTypeDeclContext ctx) {
        String name = ctx.IDENT(0).getText();
        String variants = ctx.IDENT().stream().skip(1).map(t -> t.getText()).reduce((a,b) -> a + " | " + b).orElse("");
        return new Node.TypeDecl(name, variants, Optional.empty());
    }

    @Override
    public Object visitFnDecl(HllParser.FnDeclContext ctx) {
        String name = ctx.IDENT().getText();
        List<Node.Param> params = ctx.params() != null
                ? ctx.params().param().stream()
                    .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                    .collect(Collectors.toList())
                : List.of();
        Optional<Node.TypeExpr> returnType = ctx.typeExpr() != null
                ? Optional.of(buildTypeExpr(ctx.typeExpr()))
                : Optional.empty();
        List<String> fails = ctx.failsClause() != null
                ? ctx.failsClause().IDENT().stream().map(t -> t.getText()).collect(Collectors.toList())
                : List.of();
        List<String> effects = List.of();
        if (ctx.effectsClause() != null) {
            effects = ctx.effectsClause().IDENT().stream()
                    .map(t -> t.getText())
                    .collect(Collectors.toList());
        }
        Node.Block body = buildBlock(ctx.block());
        return new Node.FnDecl(name, params, returnType, fails, effects, body);
    }

    @Override
    public Object visitImportDecl(HllParser.ImportDeclContext ctx) {
        if (ctx.STRING() != null) {
            // import java "class.path" as Alias { ... }
            String javaClass = ctx.STRING().getText().replace("\"", "");
            String alias = ctx.IDENT().getText();
            var mappings = ctx.javaMapping().stream()
                    .map(m -> new Node.JavaMapping(
                            m.IDENT().getText(),
                            m.params() != null ? m.params().param().stream()
                                    .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                                    .collect(Collectors.toList()) : List.of(),
                            buildTypeExpr(m.typeExpr())))
                    .collect(Collectors.toList());
            return new Node.ImportDecl(javaClass, alias, mappings, List.of());
        } else if (ctx.importList() != null) {
            // import module.{Symbol1, Symbol2}
            String qualName = ctx.qualifiedName().IDENT().stream()
                    .map(t -> t.getText()).collect(Collectors.joining("."));
            List<String> symbols = ctx.importList().IDENT().stream()
                    .map(t -> t.getText()).collect(Collectors.toList());
            return new Node.ImportDecl(qualName, "", List.of(), symbols);
        } else if (ctx.IDENT() != null) {
            // import module.path as alias
            String qualName = ctx.qualifiedName().IDENT().stream()
                    .map(t -> t.getText()).collect(Collectors.joining("."));
            String alias = ctx.IDENT().getText();
            return new Node.ImportDecl(qualName, alias, List.of(), List.of());
        } else {
            // import module.Symbol
            String qualName = ctx.qualifiedName().IDENT().stream()
                    .map(t -> t.getText()).collect(Collectors.joining("."));
            // Last segment is the symbol name
            var parts = ctx.qualifiedName().IDENT();
            String modulePath = parts.stream().limit(parts.size() - 1)
                    .map(t -> t.getText()).collect(Collectors.joining("."));
            String symbol = parts.get(parts.size() - 1).getText();
            return new Node.ImportDecl(modulePath, "", List.of(), List.of(symbol));
        }
    }

    private Node.TypeExpr buildTypeExpr(HllParser.TypeExprContext ctx) {
        String name = ctx.IDENT().getText();
        Optional<Node.TypeExpr> typeArg = Optional.empty();
        if (ctx.typeExpr() != null && !ctx.typeExpr().isEmpty()) {
            typeArg = Optional.of(buildTypeExpr(ctx.typeExpr(0)));
        }
        return new Node.TypeExpr(name, typeArg);
    }

    private Node.Block buildBlock(HllParser.BlockContext ctx) {
        var stmts = ctx.statement().stream()
                .map(s -> (Node.Statement) visit(s))
                .collect(Collectors.toList());
        return new Node.Block(stmts);
    }

    @Override
    public Object visitLetStmt(HllParser.LetStmtContext ctx) {
        String name = ctx.IDENT().getText();
        Optional<Node.TypeExpr> type = ctx.typeExpr() != null
                ? Optional.of(buildTypeExpr(ctx.typeExpr()))
                : Optional.empty();
        Node.Expr value = (Node.Expr) visit(ctx.expr());
        boolean hasHandler = ctx.errorHandler() != null;
        List<Node.ErrorHandler> handlers = new ArrayList<>();
        if (hasHandler) {
            var eh = ctx.errorHandler();
            for (int i = 0; i < eh.IDENT().size(); i += 2) {
                String errorType = eh.IDENT(i).getText();
                String binding = eh.IDENT(i + 1).getText();
                Node.Expr body = (Node.Expr) visit(eh.expr(i / 2));
                handlers.add(new Node.ErrorHandler(errorType, binding, body));
            }
        }
        return new Node.LetStmt(name, type, value, hasHandler, handlers);
    }

    @Override
    public Object visitAssignStmt(HllParser.AssignStmtContext ctx) {
        String name = ctx.IDENT().getText();
        Node.Expr value = (Node.Expr) visit(ctx.expr());
        return new Node.AssignStmt(name, value);
    }

    @Override
    public Object visitWhileStmt(HllParser.WhileStmtContext ctx) {
        Node.Expr cond = (Node.Expr) visit(ctx.expr());
        Node.Block body = buildBlock(ctx.block());
        return new Node.WhileStmt(cond, body);
    }

    @Override
    public Object visitForInStmt(HllParser.ForInStmtContext ctx) {
        String varName = ctx.IDENT().getText();
        Node.Expr iterable = (Node.Expr) visit(ctx.expr());
        List<Node.Expr> wheres = ctx.whenClause().stream()
                .map(w -> (Node.Expr) visit(w.expr()))
                .collect(Collectors.toList());
        Optional<Node.Expr> take = ctx.takeClause() != null
                ? Optional.of((Node.Expr) visit(ctx.takeClause().expr()))
                : Optional.empty();
        Node.Block body = buildBlock(ctx.block());
        return new Node.ForInStmt(varName, iterable, wheres, take, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(body));
    }

    @Override
    public Object visitTestDecl(HllParser.TestDeclContext ctx) {
        String desc = ctx.STRING().getText().replace("\"", "");
        Node.Block body = buildBlock(ctx.block());
        return new Node.TestDecl(desc, body);
    }

    @Override
    public Object visitAssertStmt(HllParser.AssertStmtContext ctx) {
        Node.Expr cond = (Node.Expr) visit(ctx.expr());
        return new Node.AssertStmt(cond);
    }

    @Override
    public Object visitMockStmt(HllParser.MockStmtContext ctx) {
        String serviceName = ctx.IDENT().getText();
        List<Node.FnDecl> methods = ctx.fnDecl().stream()
                .map(f -> (Node.FnDecl) visit(f))
                .collect(Collectors.toList());
        return new Node.MockStmt(serviceName, methods);
    }

    @Override
    public Object visitExpectErrorStmt(HllParser.ExpectErrorStmtContext ctx) {
        Node.Block body = buildBlock(ctx.block());
        return new Node.ExpectErrorStmt(body);
    }

    @Override
    public Object visitExpectFailStmt(HllParser.ExpectFailStmtContext ctx) {
        String errorType = ctx.IDENT().getText();
        Node.Block body = buildBlock(ctx.block());
        return new Node.ExpectFailStmt(errorType, body);
    }

    @Override
    public Object visitReturnStmt(HllParser.ReturnStmtContext ctx) {
        Optional<Node.Expr> value = ctx.expr() != null
                ? Optional.of((Node.Expr) visit(ctx.expr()))
                : Optional.empty();
        return new Node.ReturnStmt(value);
    }

    @Override
    public Object visitIfStmt(HllParser.IfStmtContext ctx) {
        Node.Expr cond = (Node.Expr) visit(ctx.expr());
        Node.Block thenBlock = buildBlock(ctx.block(0));
        Optional<Node.Block> elseBlock = ctx.block().size() > 1
                ? Optional.of(buildBlock(ctx.block(1)))
                : Optional.empty();
        return new Node.IfStmt(cond, thenBlock, elseBlock);
    }

    @Override
    public Object visitExprStmt(HllParser.ExprStmtContext ctx) {
        return new Node.ExprStmt((Node.Expr) visit(ctx.expr()));
    }

    private Node.Pattern buildPattern(HllParser.PatternContext ctx) {
        if (ctx.getText().equals("None")) return new Node.NonePattern();
        if (ctx.getText().equals("_")) return new Node.WildcardPattern();
        if (ctx.patternName() != null && ctx.IDENT() != null) {
            return new Node.SomePattern(ctx.IDENT().getText());
        }
        return new Node.WildcardPattern();
    }

    // --- Expressions ---

    @Override
    public Object visitPrimaryExpr(HllParser.PrimaryExprContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public Object visitFieldAccess(HllParser.FieldAccessContext ctx) {
        Node.Expr obj = (Node.Expr) visit(ctx.expr());
        return new Node.FieldAccess(obj, ctx.IDENT().getText());
    }

    @Override
    public Object visitMethodCall(HllParser.MethodCallContext ctx) {
        Node.Expr obj = (Node.Expr) visit(ctx.expr());
        List<Node.Expr> args = ctx.args() != null
                ? ctx.args().expr().stream().map(e -> (Node.Expr) visit(e)).collect(Collectors.toList())
                : List.of();
        return new Node.MethodCall(obj, ctx.IDENT().getText(), args);
    }

    @Override
    public Object visitOptionPropagate(HllParser.OptionPropagateContext ctx) {
        return new Node.OptionPropagate((Node.Expr) visit(ctx.expr()));
    }

    @Override
    public Object visitMulDiv(HllParser.MulDivContext ctx) {
        return new Node.BinaryOp((Node.Expr) visit(ctx.expr(0)), ctx.op.getText(), (Node.Expr) visit(ctx.expr(1)));
    }

    @Override
    public Object visitAddSub(HllParser.AddSubContext ctx) {
        return new Node.BinaryOp((Node.Expr) visit(ctx.expr(0)), ctx.op.getText(), (Node.Expr) visit(ctx.expr(1)));
    }

    @Override
    public Object visitComparison(HllParser.ComparisonContext ctx) {
        return new Node.BinaryOp((Node.Expr) visit(ctx.expr(0)), ctx.op.getText(), (Node.Expr) visit(ctx.expr(1)));
    }

    @Override
    public Object visitLogicAnd(HllParser.LogicAndContext ctx) {
        return new Node.BinaryOp((Node.Expr) visit(ctx.expr(0)), "&&", (Node.Expr) visit(ctx.expr(1)));
    }

    @Override
    public Object visitLogicOr(HllParser.LogicOrContext ctx) {
        return new Node.BinaryOp((Node.Expr) visit(ctx.expr(0)), "||", (Node.Expr) visit(ctx.expr(1)));
    }

    @Override
    public Object visitLogicNot(HllParser.LogicNotContext ctx) {
        return new Node.UnaryOp("!", (Node.Expr) visit(ctx.expr()));
    }

    @Override
    public Object visitFnCall(HllParser.FnCallContext ctx) {
        String name = ctx.IDENT().getText();
        List<Node.Expr> args = ctx.args() != null
                ? ctx.args().expr().stream().map(e -> (Node.Expr) visit(e)).collect(Collectors.toList())
                : List.of();
        return new Node.FnCall(name, args);
    }

    @Override
    public Object visitIdentifier(HllParser.IdentifierContext ctx) {
        return new Node.Identifier(ctx.IDENT().getText());
    }

    @Override
    public Object visitStringLit(HllParser.StringLitContext ctx) {
        String raw = ctx.STRING().getText();
        return new Node.StringLit(raw.substring(1, raw.length() - 1));
    }

    @Override
    public Object visitNumberLit(HllParser.NumberLitContext ctx) {
        return new Node.NumberLit(Integer.parseInt(ctx.NUMBER().getText()));
    }

    @Override
    public Object visitFloatLit(HllParser.FloatLitContext ctx) {
        return new Node.FloatLit(Double.parseDouble(ctx.FLOAT().getText()));
    }

    @Override
    public Object visitBoolLit(HllParser.BoolLitContext ctx) {
        return new Node.BoolLit(ctx.getText().equals("true"));
    }

    @Override
    public Object visitNoneLit(HllParser.NoneLitContext ctx) {
        return new Node.Identifier("None");
    }

    @Override
    public Object visitOkExpr(HllParser.OkExprContext ctx) {
        return new Node.FnCall("Ok", List.of((Node.Expr) visit(ctx.expr())));
    }

    @Override
    public Object visitErrExpr(HllParser.ErrExprContext ctx) {
        return new Node.FnCall("Err", List.of((Node.Expr) visit(ctx.expr())));
    }

    @Override
    public Object visitSomeExpr(HllParser.SomeExprContext ctx) {
        return new Node.FnCall("Some", List.of((Node.Expr) visit(ctx.expr())));
    }

    @Override
    public Object visitFailExpr(HllParser.FailExprContext ctx) {
        String errorType = ctx.IDENT().getText();
        List<Node.Expr> args = ctx.args() != null
                ? ctx.args().expr().stream().map(e -> (Node.Expr) visit(e)).collect(Collectors.toList())
                : List.of();
        return new Node.FailExpr(errorType, args);
    }

    @Override
    public Object visitSpawnExpr(HllParser.SpawnExprContext ctx) {
        return new Node.SpawnExpr(ctx.IDENT().getText());
    }

    @Override
    public Object visitForYieldExpr(HllParser.ForYieldExprContext ctx) {
        String varName = ctx.IDENT().getText();
        Node.Expr iterable = (Node.Expr) visit(ctx.expr());
        List<Node.Expr> whens = ctx.whenClause().stream()
                .map(w -> (Node.Expr) visit(w.expr()))
                .collect(Collectors.toList());
        Optional<Node.Expr> take = ctx.takeClause() != null
                ? Optional.of((Node.Expr) visit(ctx.takeClause().expr()))
                : Optional.empty();
        Node.Expr yieldExpr = (Node.Expr) visit(ctx.yieldClause().expr());
        return new Node.ForExpr(varName, iterable, whens, take, Optional.of(yieldExpr), Optional.empty(), Optional.empty());
    }

    @Override
    public Object visitForIntoExpr(HllParser.ForIntoExprContext ctx) {
        String varName = ctx.IDENT().getText();
        Node.Expr iterable = (Node.Expr) visit(ctx.expr());
        List<Node.Expr> whens = ctx.whenClause().stream()
                .map(w -> (Node.Expr) visit(w.expr()))
                .collect(Collectors.toList());
        Optional<Node.Expr> take = ctx.takeClause() != null
                ? Optional.of((Node.Expr) visit(ctx.takeClause().expr()))
                : Optional.empty();
        String intoFn = ctx.intoClause().IDENT().getText();
        Node.Expr intoArg = (Node.Expr) visit(ctx.intoClause().expr());
        return new Node.ForExpr(varName, iterable, whens, take, Optional.empty(), Optional.of(intoFn), Optional.of(intoArg));
    }

    @Override
    public Object visitAwaitExpr(HllParser.AwaitExprContext ctx) {
        return new Node.AwaitExpr((Node.Expr) visit(ctx.expr()));
    }

    @Override
    public Object visitMatchExpr(HllParser.MatchExprContext ctx) {
        Node.Expr subject = (Node.Expr) visit(ctx.expr());
        var arms = ctx.matchArm().stream()
                .map(a -> {
                    Node.Pattern pattern = buildPattern(a.pattern());
                    Node.Expr body = a.expr() != null
                            ? (Node.Expr) visit(a.expr())
                            : new Node.BlockExpr(buildBlock(a.block()));
                    return new Node.MatchArm(pattern, body);
                })
                .collect(Collectors.toList());
        return new Node.MatchExpr(subject, arms);
    }

    @Override
    public Object visitParenExpr(HllParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitRaiseExpr(HllParser.RaiseExprContext ctx) {
        String name = ctx.IDENT().getText();
        List<Node.Expr> args = ctx.args() != null
                ? ctx.args().expr().stream().map(e -> (Node.Expr) visit(e)).collect(Collectors.toList())
                : List.of();
        return new Node.FnCall("__raise__" + name, args);
    }

    @Override
    public Object visitHandleExpr(HllParser.HandleExprContext ctx) {
        Node.Expr subject = (Node.Expr) visit(ctx.expr());
        return new Node.FnCall("__handle__", List.of(subject));
    }
}
