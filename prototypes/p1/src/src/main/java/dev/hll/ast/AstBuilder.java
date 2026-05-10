package dev.hll.ast;

import dev.hll.parser.HllBaseVisitor;
import dev.hll.parser.HllParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AstBuilder extends HllBaseVisitor<Object> {

    public Node.Program buildProgram(HllParser.ProgramContext ctx) {
        var decls = ctx.declaration().stream()
                .map(d -> (Node.Declaration) visit(d))
                .collect(Collectors.toList());
        return new Node.Program(decls);
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
        Node.Block body = buildBlock(ctx.block());
        return new Node.FnDecl(name, params, returnType, body);
    }

    @Override
    public Object visitImportDecl(HllParser.ImportDeclContext ctx) {
        String alias = ctx.IDENT().getText();
        if (ctx.STRING() != null) {
            // import java "class.path" as Alias { ... }
            String javaClass = ctx.STRING().getText().replace("\"", "");
            var mappings = ctx.javaMapping().stream()
                    .map(m -> new Node.JavaMapping(
                            m.IDENT().getText(),
                            m.params() != null ? m.params().param().stream()
                                    .map(p -> new Node.Param(p.IDENT().getText(), buildTypeExpr(p.typeExpr())))
                                    .collect(Collectors.toList()) : List.of(),
                            buildTypeExpr(m.typeExpr())))
                    .collect(Collectors.toList());
            return new Node.ImportDecl(javaClass, alias, mappings);
        } else {
            // import hll.validation as validate
            String qualName = ctx.qualifiedName().IDENT().stream()
                    .map(t -> t.getText()).reduce((a, b) -> a + "." + b).orElse("");
            return new Node.ImportDecl(qualName, alias, List.of());
        }
    }

    private Node.TypeExpr buildTypeExpr(HllParser.TypeExprContext ctx) {
        String name = ctx.IDENT().getText();
        Optional<Node.TypeExpr> typeArg = ctx.typeExpr() != null
                ? Optional.of(buildTypeExpr(ctx.typeExpr()))
                : Optional.empty();
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
        return new Node.LetStmt(name, type, value);
    }

    @Override
    public Object visitReturnStmt(HllParser.ReturnStmtContext ctx) {
        Optional<Node.Expr> value = ctx.expr() != null
                ? Optional.of((Node.Expr) visit(ctx.expr()))
                : Optional.empty();
        return new Node.ReturnStmt(value);
    }

    @Override
    public Object visitMatchStmt(HllParser.MatchStmtContext ctx) {
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
        return new Node.MatchStmt(subject, arms);
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
        if (ctx.IDENT().size() == 2) return new Node.SomePattern(ctx.IDENT(1).getText());
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
    public Object visitParenExpr(HllParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }
}
