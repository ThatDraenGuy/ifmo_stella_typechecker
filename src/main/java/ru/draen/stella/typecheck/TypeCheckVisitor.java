package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.generated.StellaParserBaseVisitor;
import ru.draen.stella.typecheck.exceptions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TypeCheckVisitor extends StellaParserBaseVisitor<StellaType> {
    private static final String MAIN_FUNC_NAME = "main";
    private static final String SUBTYPING_EXT = "#structural-subtyping";
    private static final String AMBIGUOUS_BOT_EXT = "#ambiguous-type-as-bottom";

    private final TypeCheckRegistry registry;

    public TypeCheckVisitor(TypeCheckRegistry registry) {
        this.registry = registry;
    }

    private void checkTypeMismatch(StellaType expected, StellaType actual, StellaParser.ExprContext expr) {
        if (registry.isSubtypingEnabled()) {
            if (!actual.isSubtypeOf(expected, expr)) {
                throw new ErrorUnexpectedSubtype(expr, expected, actual);
            }
        } else {
            if (!expected.matches(actual)) {
                throw new ErrorUnexpectedTypeForExpression(expr, expected, actual);
            }
        }
    }

    private StellaType returnChecked(StellaType result, StellaParser.ExprContext expr) {
        registry.consumeExpectedType().ifPresent(expected -> checkTypeMismatch(expected, result, expr));
        return result;
    }

    private<T extends StellaType> StellaType resolveAmbiguity(Optional<T> expected, Supplier<TypeCheckException> error) {
        if (expected.isPresent()) return expected.get();
        if (registry.isAmbiguousBottomEnabled()) {
            return new StellaType.Bottom();
        } else {
            throw error.get();
        }
    }

    @Override
    public StellaType visitProgram(StellaParser.ProgramContext ctx) {
        ctx.extensions.forEach(ext -> ext.accept(this));

        registry.setDeclarationPass(true);
        ctx.decls.forEach(decl -> decl.accept(this));
        registry.setDeclarationPass(false);
        ctx.decls.forEach(decl -> decl.accept(this));

        StellaType mainFunc = registry.getVar(MAIN_FUNC_NAME).orElseThrow(ErrorMissingMain::new);
        if (!(mainFunc instanceof StellaType.Func(List<StellaType> in, StellaType out))) {
            throw new ErrorMissingMain();
        }

        if (in.size() != 1) {
            throw new ErrorIncorrectArityOfMain();
        }
        return new StellaType.Bottom();
    }

    @Override
    public StellaType visitAnExtension(StellaParser.AnExtensionContext ctx) {
        for (var ext : ctx.extensionNames) {
            String name = ext.getText();
            if (name.equals(SUBTYPING_EXT)) {
                registry.setSubtypingEnabled(true);
            }
            if (name.equals(AMBIGUOUS_BOT_EXT)) {
                registry.setAmbiguousBottomEnabled(true);
            }
        }
        return new StellaType.Bottom();
    }

    @Override
    public StellaType visitParenthesisedExpr(StellaParser.ParenthesisedExprContext ctx) {
        return ctx.expr_.accept(this);
    }

    @Override
    public StellaType visitTerminatingSemicolon(StellaParser.TerminatingSemicolonContext ctx) {
        return ctx.expr_.accept(this);
    }

    @Override
    public StellaType visitVar(StellaParser.VarContext ctx) {
        return returnChecked(registry.getVar(ctx.name.getText())
                        .orElseThrow(() -> new ErrorUndefinedVariable(ctx)),
                ctx);
    }

    //region funcs
    @Override
    public StellaType visitDeclFun(StellaParser.DeclFunContext ctx) {
        StellaType.Func funcType = StellaType.Func.fromDeclFun(ctx);
        if (registry.isDeclarationPass()) {
            if (registry.addVar(ctx.name.getText(), funcType)) {
                throw new ErrorDuplicateFunctionDeclaration(ctx);
            }
            return funcType;
        }

        try {
            registry.enterScope(ctx.name.getText());
            for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
                if (registry.addVar(paramDecl.name.getText(), StellaType.fromAst(paramDecl.paramType))) {
                    throw new ErrorDuplicateFunctionParameter(ctx, paramDecl.name.getText());
                }
            }

            registry.setDeclarationPass(true);
            ctx.localDecls.forEach(decl -> decl.accept(this));
            registry.setDeclarationPass(false);
            ctx.localDecls.forEach(decl -> decl.accept(this));

            registry.addExpectedType(funcType.out());
            ctx.returnExpr.accept(this);
        } finally {
            registry.exitScope();
        }

        return funcType;
    }

    @Override
    public StellaType visitAbstraction(StellaParser.AbstractionContext ctx) {
        Optional<StellaType.Func> maybeExpectedFunc = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Func expectedFunc)) {
                throw new ErrorUnexpectedLambda(ctx, expected);
            }

            if (ctx.paramDecls.size() != expectedFunc.in().size()) {
                throw new ErrorStrictUnexpectedNumberOfParametersInLambda(ctx, expectedFunc.in().size(), ctx.paramDecls.size(), expected);
            }

            IntStream.range(0, ctx.paramDecls.size()).forEach(i -> {
                StellaParser.ParamDeclContext paramDecl = ctx.paramDecls.get(i);
                StellaType paramType = StellaType.fromAst(paramDecl.paramType);
                try {
                    checkTypeMismatch(paramType, expectedFunc.in().get(i), ctx);
                } catch (ErrorUnexpectedTypeForExpression ignored) {
                    throw new ErrorUnexpectedTypeForParameter(paramDecl, expectedFunc.in().get(i), paramType);
                }
            });

            return Optional.of(expectedFunc);
        });

        StellaType returnType;
        try {
            registry.enterScope(ctx.getText());
            for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
                StellaType paramType = StellaType.fromAst(paramDecl.paramType);
                if (registry.addVar(paramDecl.name.getText(), paramType)) {
                    throw new ErrorDuplicateFunctionParameter(ctx, paramDecl.name.getText());
                }
            }

            maybeExpectedFunc.ifPresent(expectedFunc -> registry.addExpectedType(expectedFunc.out()));
            returnType = ctx.returnExpr.accept(this);
        } finally {
            registry.exitScope();
        }

        maybeExpectedFunc.ifPresent(registry::addExpectedType);
        return returnChecked(StellaType.Func.fromAbstraction(ctx, returnType), ctx);
    }

    @Override
    public StellaType visitApplication(StellaParser.ApplicationContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.fun.accept(this);
        if (!(type instanceof StellaType.Func(List<StellaType> inTypes, StellaType outType))) {
            throw new ErrorNotAFunction(ctx.fun, type);
        }

        if (ctx.args.size() != inTypes.size()) {
            throw new ErrorIncorrectNumberOfArguments(ctx, inTypes.size(), ctx.args.size());
        }

        IntStream.range(0, inTypes.size()).forEach(i -> {
            registry.addExpectedType(inTypes.get(i));
            ctx.args.get(i).accept(this);
        });

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(outType, ctx);
    }
    //endregion

    //region bools
    @Override
    public StellaType visitConstTrue(StellaParser.ConstTrueContext ctx) {
        return returnChecked(new StellaType.Bool(), ctx);
    }

    @Override
    public StellaType visitConstFalse(StellaParser.ConstFalseContext ctx) {
        return returnChecked(new StellaType.Bool(), ctx);
    }

    @Override
    public StellaType visitIf(StellaParser.IfContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        registry.addExpectedType(new StellaType.Bool());
        ctx.condition.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType trueType = ctx.thenExpr.accept(this);
        registry.addExpectedType(trueType);
        ctx.elseExpr.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(trueType, ctx);
    }
    //endregion

    //region nat
    @Override
    public StellaType visitConstInt(StellaParser.ConstIntContext ctx) {
        return returnChecked(new StellaType.Nat(), ctx);
    }

    @Override
    public StellaType visitSucc(StellaParser.SuccContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        StellaType type = new StellaType.Nat();
        registry.addExpectedType(type);
        ctx.n.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(type, ctx);
    }

    @Override
    public StellaType visitPred(StellaParser.PredContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        StellaType type = new StellaType.Nat();
        registry.addExpectedType(type);
        ctx.n.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(type, ctx);
    }

    @Override
    public StellaType visitIsZero(StellaParser.IsZeroContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        registry.addExpectedType(new StellaType.Nat());
        ctx.n.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.Bool(), ctx);
    }

    @Override
    public StellaType visitNatRec(StellaParser.NatRecContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        registry.addExpectedType(new StellaType.Nat());
        ctx.n.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType initialType = ctx.initial.accept(this);

        registry.addExpectedType(new StellaType.Func(
                List.of(new StellaType.Nat()),
                new StellaType.Func(
                        List.of(initialType),
                        initialType
                )));
        ctx.step.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(initialType, ctx);
    }

    //endregion

    //region unit
    @Override
    public StellaType visitConstUnit(StellaParser.ConstUnitContext ctx) {
        return returnChecked(new StellaType.Unit(), ctx);
    }
    //endregion

    //region tuple
    @Override
    public StellaType visitTuple(StellaParser.TupleContext ctx) {
        Optional<StellaType.Tuple> maybeExpectedTuple = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Tuple expectedTuple)) {
                throw new ErrorUnexpectedTuple(ctx, expected);
            }

            if (ctx.exprs.size() != expectedTuple.items().size()) {
                throw new ErrorStrictUnexpectedTupleLength(ctx, expectedTuple.items().size(), ctx.exprs.size(), expected);
            }
            return Optional.of(expectedTuple);
        });

        List<StellaType> types = IntStream.range(0, ctx.exprs.size()).mapToObj(i -> {
            StellaParser.ExprContext expr = ctx.exprs.get(i);
            maybeExpectedTuple.ifPresent(expected -> registry.addExpectedType(expected.items().get(i)));
            return expr.accept(this);
        }).toList();

        maybeExpectedTuple.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.Tuple(types), ctx);
    }

    @Override
    public StellaType visitDotTuple(StellaParser.DotTupleContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Tuple(List<StellaType> itemTypes))) {
            throw new ErrorNotATuple(ctx.expr_, type);
        }
        int index = Integer.parseInt(ctx.index.getText());
        if (index > itemTypes.size()) {
            throw new ErrorTupleIndexOutOfBounds(ctx.expr_, itemTypes.size(), index);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(itemTypes.get(index - 1), ctx);
    }
    //endregion

    //region records
    @Override
    public StellaType visitRecord(StellaParser.RecordContext ctx) {
        Optional<StellaType.Record> maybeExpectedRecord = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Record expectedRecord)) {
                throw new ErrorUnexpectedRecord(ctx, expected);
            }

            Map<String, StellaParser.BindingContext> actualFields = ctx.bindings.stream().collect(Collectors.toMap(
                    binding -> binding.name.getText(),
                    Function.identity(),
                    (binding1, binding2) -> {
                        throw new ErrorDuplicateRecordFields(ctx, binding1.name.getText());
                    }
            ));
            for (String expectedField : expectedRecord.items().keySet()) {
                if (!actualFields.containsKey(expectedField)) {
                    throw new ErrorStrictMissingRecordFields(ctx, expected, expectedField);
                }
            }

            if (!registry.isSubtypingEnabled()) {
                for (String actualField : actualFields.keySet()) {
                    if (!expectedRecord.items().containsKey(actualField)) {
                        throw new ErrorUnexpectedRecordFields(ctx, expected, actualField);
                    }
                }
            }

            return Optional.of(expectedRecord);
        });

        Map<String, StellaType.Record.Item> items = ctx.bindings.stream().map(binding -> {
            maybeExpectedRecord.flatMap(expectedRecord ->
                            Optional.ofNullable(expectedRecord.items().get(binding.name.getText())))
                    .map(StellaType.Record.Item::type)
                    .ifPresent(registry::addExpectedType);
            return new StellaType.Record.Item(binding.name.getText(), binding.rhs.accept(this));
        }).collect(Collectors.toMap(
                StellaType.Record.Item::name,
                Function.identity(),
                (binding1, binding2) -> {
                    throw new ErrorDuplicateRecordFields(ctx, binding1.name());
                }
        ));

        maybeExpectedRecord.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.Record(items), ctx);
    }

    @Override
    public StellaType visitDotRecord(StellaParser.DotRecordContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Record(Map<String, StellaType.Record.Item> items))) {
            throw new ErrorNotARecord(ctx.expr_, type);
        }
        String label = ctx.label.getText();
        StellaType result = Optional.ofNullable( items.get(label))
                .orElseThrow(() -> new ErrorUnexpectedFieldAccess(ctx.expr_, label, type))
                .type();

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(result, ctx);
    }
    //endregion

    //region itemType sum + variant + match
    @Override
    public StellaType visitInl(StellaParser.InlContext ctx) {
        Optional<StellaType.Sum> maybeExpectedSum = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Sum expectedSum)) {
                throw new ErrorUnexpectedInjection(ctx, expected);
            }
            return Optional.of(expectedSum);
        });

        maybeExpectedSum.ifPresent(expectedSum -> registry.addExpectedType(expectedSum.inl()));
        ctx.expr_.accept(this);
        return resolveAmbiguity(
                maybeExpectedSum,
                () -> new ErrorAmbiguousSumType(ctx)
        );
    }

    @Override
    public StellaType visitInr(StellaParser.InrContext ctx) {
        Optional<StellaType.Sum> maybeExpectedSum = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Sum expectedSum)) {
                throw new ErrorUnexpectedInjection(ctx, expected);
            }
            return Optional.of(expectedSum);
        });

        maybeExpectedSum.ifPresent(expectedSum -> registry.addExpectedType(expectedSum.inr()));
        ctx.expr_.accept(this);
        return resolveAmbiguity(
                maybeExpectedSum,
                () -> new ErrorAmbiguousSumType(ctx)
        );
    }

    @Override
    public StellaType visitVariant(StellaParser.VariantContext ctx) {
        Optional<StellaType.Variant> maybeExpectedVariant = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.Variant expectedVariant)) {
                throw new ErrorUnexpectedVariant(ctx, expected);
            }

            return Optional.of(expectedVariant);
        });

        String label = ctx.label.getText();
        maybeExpectedVariant.ifPresent(expectedVariant -> {
            if (!expectedVariant.items().containsKey(label)) {
                throw new ErrorStrictUnexpectedVariantLabel(ctx, expectedVariant, label);
            }
            Optional<StellaType> expectedInner = expectedVariant.items().get(label).type();

            if (expectedInner.isEmpty() && ctx.rhs != null) {
                throw new ErrorStrictUnexpectedDataForNullaryLabel(ctx, expectedVariant, label);
            }

            if (expectedInner.isPresent() && ctx.rhs == null) {
                throw new ErrorStrictMissingDataForLabel(ctx, expectedVariant, label);
            }

            expectedInner.ifPresent(registry::addExpectedType);
        });
        if (ctx.rhs != null) {
            ctx.rhs.accept(this);
        }
        return resolveAmbiguity(
                maybeExpectedVariant,
                () -> new ErrorAmbiguousVariantType(ctx)
        );
    }

    @Override
    public StellaType visitMatch(StellaParser.MatchContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);

        if (ctx.cases.isEmpty()) {
            throw new ErrorIllegalEmptyMatching(ctx);
        }

        List<StellaPattern> notExhausted = type.allPossiblePatterns();
        for (StellaParser.MatchCaseContext matchCase : ctx.cases) {
            var result = new StellaPatternResolver(registry, matchCase.pattern_, type).resolve(notExhausted);
            notExhausted = result.notExhausted();
            try {
                registry.enterScope(matchCase.getText());
                for (var var : result.vars().entrySet()) {
                    registry.addVar(var.getKey(), var.getValue());
                }
                maybeExpected.ifPresent(registry::addExpectedType);
                StellaType exprType = matchCase.expr_.accept(this);
                if (maybeExpected.isEmpty()) {
                    maybeExpected = Optional.of(exprType);
                }
            } finally {
                registry.exitScope();
            }
        }
        if (!notExhausted.isEmpty() && !notExhausted.getFirst().matches(new StellaPattern.NoPattern())) {
            throw new ErrorNonexhaustiveMatchPatterns(ctx, notExhausted);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(maybeExpected.get(), ctx);
    }

    //endregion

    //region list
    @Override
    public StellaType visitConsList(StellaParser.ConsListContext ctx) {
        Optional<StellaType.StellaList> maybeExpectedList = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.StellaList expectedList)) {
                throw new ErrorUnexpectedList(ctx, expected);
            }
            return Optional.of(expectedList);
        });

        maybeExpectedList.ifPresent(expectedList -> registry.addExpectedType(expectedList.itemType()));
        StellaType headType = ctx.head.accept(this);
        StellaType listType = new StellaType.StellaList(headType);

        registry.addExpectedType(listType);
        ctx.tail.accept(this);

        maybeExpectedList.ifPresent(registry::addExpectedType);
        return returnChecked(listType, ctx);
    }

    @Override
    public StellaType visitList(StellaParser.ListContext ctx) {
        Optional<StellaType.StellaList> maybeExpectedList = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (!(expected instanceof StellaType.StellaList expectedList)) {
                throw new ErrorUnexpectedList(ctx, expected);
            }
            return Optional.of(expectedList);
        });

        if (ctx.exprs.isEmpty()) {
            return resolveAmbiguity(
                    maybeExpectedList,
                    () -> new ErrorAmbiguousList(ctx)
            );
        }

        maybeExpectedList.ifPresent(expectedList -> registry.addExpectedType(expectedList.itemType()));
        StellaType itemType = ctx.exprs.getFirst().accept(this);
        for (int i = 1; i < ctx.exprs.size(); i++) {
            registry.addExpectedType(itemType);
            ctx.exprs.get(i).accept(this);
        }
        return returnChecked(new StellaType.StellaList(itemType), ctx);
    }

    @Override
    public StellaType visitHead(StellaParser.HeadContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.list.accept(this);
        if (!(type instanceof StellaType.StellaList(StellaType itemType))) {
            throw new ErrorNotAList(ctx.list, type);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(itemType, ctx);
    }

    @Override
    public StellaType visitTail(StellaParser.TailContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.list.accept(this);
        if (!(type instanceof StellaType.StellaList(StellaType itemType))) {
            throw new ErrorNotAList(ctx.list, type);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.StellaList(itemType), ctx);
    }

    @Override
    public StellaType visitIsEmpty(StellaParser.IsEmptyContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.list.accept(this);
        if (!(type instanceof StellaType.StellaList)) {
            throw new ErrorNotAList(ctx.list, type);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.Bool(), ctx);
    }
    //endregion

    //region type asc
    @Override
    public StellaType visitTypeAsc(StellaParser.TypeAscContext ctx) {
        StellaType ascription = returnChecked(StellaType.fromAst(ctx.stellatype()), ctx);

        registry.addExpectedType(ascription);
        ctx.expr_.accept(this);

        return ascription;
    }
    //endregion

    //region type cast
    @Override
    public StellaType visitTypeCast(StellaParser.TypeCastContext ctx) {
        StellaType castType = returnChecked(StellaType.fromAst(ctx.stellatype()), ctx);

        ctx.expr_.accept(this);

        return castType;
    }

    @Override
    public StellaType visitTryCastAs(StellaParser.TryCastAsContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType castType = StellaType.fromAst(ctx.stellatype());

        ctx.tryExpr.accept(this);

        StellaPatternResolver.Result patResult = new StellaPatternResolver(registry, ctx.pattern_, castType)
                .resolve(castType.allPossiblePatterns());

        StellaType outType;
        try {
            registry.enterScope(ctx.pattern_.getText());
            for (var var : patResult.vars().entrySet()) {
                registry.addVar(var.getKey(), var.getValue());
            }
            maybeExpected.ifPresent(registry::addExpectedType);
            outType = ctx.expr_.accept(this);
        } finally {
            registry.exitScope();
        }

        registry.addExpectedType(outType);
        ctx.fallbackExpr.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(outType, ctx);
    }

    //endregion

    //region fix
    @Override
    public StellaType visitFix(StellaParser.FixContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        maybeExpected.ifPresent(type -> registry.addExpectedType(new StellaType.Func(List.of(type), type)));
        StellaType innerType = ctx.expr_.accept(this);
        if (!(innerType instanceof StellaType.Func(List<StellaType> inTypes, StellaType outType))) {
            throw new ErrorNotAFunction(ctx.expr_, innerType);
        }

        if (inTypes.size() != 1) {
            throw new ErrorIncorrectNumberOfArguments(ctx, 1, inTypes.size());
        }

        if (!inTypes.getFirst().matches(outType)) {
            throw new ErrorUnexpectedTypeForExpression(ctx.expr_,
                    new StellaType.Func(inTypes, inTypes.getFirst()),
                    new StellaType.Func(inTypes, outType)
            );
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(outType, ctx);
    }
    //endregion

    //region let
    @Override
    public StellaType visitLet(StellaParser.LetContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType exprType = ctx.patternBinding.rhs.accept(this);

        StellaPatternResolver.Result patResult;
        try {
            patResult = new StellaPatternResolver(registry, ctx.patternBinding.pat, exprType)
                    .resolve(exprType.allPossiblePatterns());
        } catch (ErrorDuplicatePatternVariable e) {
            throw new ErrorDuplicateLetBinding(ctx, e.getVariable());
        }

        if (!patResult.notExhausted().isEmpty()) {
            throw new ErrorNonexhaustiveLetPatterns(ctx, patResult.notExhausted());
        }

        StellaType result;
        try {
            registry.enterScope(ctx.getText());
            for (var var : patResult.vars().entrySet()) {
                registry.addVar(var.getKey(), var.getValue());
            }

            maybeExpected.ifPresent(registry::addExpectedType);
            result = ctx.body.accept(this);
        } finally {
            registry.exitScope();
        }

        return returnChecked(result, ctx);
    }

    private StellaType resolvePatternType(StellaParser.PatternContext ctx) {
        return switch (ctx) {
            case StellaParser.PatternVarContext var -> throw new ErrorAmbiguousPatternType(var);
            case StellaParser.PatternAscContext asc -> StellaType.fromAst(asc.type_);
            case StellaParser.PatternSuccContext ignored -> new StellaType.Nat();
            case StellaParser.PatternIntContext ignored -> new StellaType.Nat();
            case StellaParser.PatternFalseContext ignored -> new StellaType.Bool();
            case StellaParser.PatternTrueContext ignored -> new StellaType.Bool();
            case StellaParser.PatternUnitContext ignored -> new StellaType.Unit();
            case StellaParser.PatternTupleContext tuple -> {
                List<StellaType> innerTypes = tuple.patterns.stream().map(this::resolvePatternType).toList();
                yield new StellaType.Tuple(innerTypes);
            }
            case StellaParser.PatternRecordContext record -> {
                Map<String, StellaType.Record.Item> fields = record.patterns.stream()
                        .map(pat ->
                                new StellaType.Record.Item(pat.label.getText(),
                                        resolvePatternType(pat.pattern_)))
                        .collect(Collectors.toMap(
                                StellaType.Record.Item::name,
                                Function.identity(),
                                (item, item2) -> {
                                    throw new ErrorDuplicateRecordPatternFields(record, item.name());
                                }
                        ));
                yield new StellaType.Record(fields);
            }
            case StellaParser.PatternInlContext inl -> {
                resolvePatternType(inl.pattern_);
                throw new ErrorAmbiguousPatternType(inl);
            }
            case StellaParser.PatternInrContext inr -> {
                resolvePatternType(inr.pattern_);
                throw new ErrorAmbiguousPatternType(inr);
            }
            case StellaParser.PatternVariantContext variant -> {
                resolvePatternType(variant.pattern_);
                throw new ErrorAmbiguousPatternType(variant);
            }
            case StellaParser.PatternConsContext cons -> {
                StellaType head = resolvePatternType(cons.head);
                StellaType headList = new StellaType.StellaList(head);
                StellaType tail = resolvePatternType(cons.tail);
                if (!tail.matches(headList)) {
                    throw new ErrorUnexpectedPatternForType(cons.tail, headList);
                }
                yield headList;
            }
            case StellaParser.PatternListContext list -> {
                if (list.patterns.isEmpty()) throw new ErrorAmbiguousPatternType(list);
                StellaType first = resolvePatternType(list.patterns.getFirst());
                for (int i = 1; i < list.patterns.size(); i++) {
                    StellaParser.PatternContext pattern = list.patterns.get(i);
                    if (!resolvePatternType(pattern).matches(first)) {
                        throw new ErrorUnexpectedPatternForType(pattern, first);
                    }
                }
                yield new StellaType.StellaList(first);
            }
            default -> throw new UnsupportedException();
        };
    }

    @Override
    public StellaType visitLetRec(StellaParser.LetRecContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType patternType = resolvePatternType(ctx.patternBinding.pat);

        StellaPatternResolver.Result patResult;
        try {
            patResult = new StellaPatternResolver(registry, ctx.patternBinding.pat, patternType)
                    .resolve(patternType.allPossiblePatterns());
        } catch (ErrorDuplicatePatternVariable e) {
            throw new ErrorDuplicateLetBinding(ctx, e.getVariable());
        }
        if (!patResult.notExhausted().isEmpty()) {
            throw new ErrorNonexhaustiveLetRecPatterns(ctx, patResult.notExhausted());
        }

        StellaType result;
        try {
            registry.enterScope(ctx.getText());
            for (var var : patResult.vars().entrySet()) {
                registry.addVar(var.getKey(), var.getValue());
            }

            registry.addExpectedType(patternType);
            ctx.patternBinding.rhs.accept(this);

            maybeExpected.ifPresent(registry::addExpectedType);
            result = ctx.body.accept(this);
        } finally {
            registry.exitScope();
        }

        return returnChecked(result, ctx);
    }

    //endregion

    //region refs

    @Override
    public StellaType visitRef(StellaParser.RefContext ctx) {
        Optional<StellaType.Ref> maybeExpectedRef = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (expected instanceof StellaType.Source(StellaType inner)) {
                return Optional.of(new StellaType.Ref(inner));
            }
            if (!(expected instanceof StellaType.Ref expectedRef)) {
                throw new ErrorUnexpectedReference(ctx, expected);
            }
            return Optional.of(expectedRef);
        });

        maybeExpectedRef.ifPresent(expectedRef -> registry.addExpectedType(expectedRef.inner()));
        StellaType inner = ctx.expr_.accept(this);

        return new StellaType.Ref(inner);
    }

    @Override
    public StellaType visitDeref(StellaParser.DerefContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        maybeExpected.ifPresent(expected -> registry.addExpectedType(new StellaType.Source(expected)));
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Ref(StellaType inner))) {
            throw new ErrorNotAReference(ctx.expr_, type);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(inner, ctx);
    }

    @Override
    public StellaType visitAssign(StellaParser.AssignContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType targetType = ctx.lhs.accept(this);
        if (!(targetType instanceof StellaType.Ref(StellaType valueType))) {
            throw new ErrorNotAReference(ctx.lhs, targetType);
        }

        registry.addExpectedType(valueType);
        ctx.rhs.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.Unit(), ctx);
    }

    @Override
    public StellaType visitSequence(StellaParser.SequenceContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        registry.addExpectedType(new StellaType.Unit());
        ctx.expr1.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return ctx.expr2.accept(this);
    }

    @Override
    public StellaType visitConstMemory(StellaParser.ConstMemoryContext ctx) {
        Optional<StellaType.Ref> maybeExpectedRef = registry.consumeExpectedType().flatMap(expected -> {
            if (registry.isSubtypingEnabled() && expected instanceof StellaType.Top) return Optional.empty();

            if (expected instanceof StellaType.Source(StellaType inner)) {
                return Optional.of(new StellaType.Ref(inner));
            }
            if (!(expected instanceof StellaType.Ref expectedRef)) {
                throw new ErrorUnexpectedMemoryAddress(ctx, expected);
            }
            return Optional.of(expectedRef);
        });

        return resolveAmbiguity(
                maybeExpectedRef,
                () -> new ErrorAmbiguousReferenceType(ctx)
        );
    }
    //endregion

    //region exceptions
    @Override
    public StellaType visitDeclExceptionType(StellaParser.DeclExceptionTypeContext ctx) {
        if (registry.isDeclarationPass()) {
            if (registry.isInLocalScope()) throw new ErrorIllegalLocalExceptionType(ctx);
            Optional<StellaExceptionType> prevType = registry.getExceptionType();
            prevType.ifPresent(prev -> {
                switch (prev) {
                    case StellaExceptionType.OpenVariant ignored -> throw new ErrorConflictingExceptionDeclarations(ctx);
                    case StellaExceptionType.Type ignored -> throw new ErrorDuplicateExceptionType(ctx);
                }
            });
            registry.setExceptionType(new StellaExceptionType.Type(StellaType.fromAst(ctx.exceptionType)));
        }
        return new StellaType.Bottom();
    }

    @Override
    public StellaType visitDeclExceptionVariant(StellaParser.DeclExceptionVariantContext ctx) {
        if (registry.isDeclarationPass()) {
            if (registry.isInLocalScope()) throw new ErrorIllegalLocalOpenVariantException(ctx);
            Optional<StellaExceptionType> prevType = registry.getExceptionType();
            Optional<StellaType.Variant> maybePrev = prevType.map(prev -> switch (prev) {
                    case StellaExceptionType.OpenVariant(StellaType.Variant prevVariant) ->
                            prevVariant;
                    case StellaExceptionType.Type ignored ->
                            throw new ErrorConflictingExceptionDeclarations(ctx);
            });
            StellaType.Variant newVariant = StellaType.Variant.ofVariant(ctx.name.getText(), Optional.of(StellaType.fromAst(ctx.variantType)));
            registry.setExceptionType(new StellaExceptionType.OpenVariant(
                    maybePrev.map(prev -> prev.merge(newVariant, (item1, item2) -> {
                        throw new ErrorDuplicateExceptionVariant(ctx, item1.name());
                    })).orElse(newVariant)
            ));
        }
        return new StellaType.Bottom();
    }

    @Override
    public StellaType visitPanic(StellaParser.PanicContext ctx) {
        return resolveAmbiguity(
                registry.consumeExpectedType(),
                () -> new ErrorAmbiguousPanicType(ctx)
        );
    }

    @Override
    public StellaType visitThrow(StellaParser.ThrowContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        StellaExceptionType exceptionType = registry.getExceptionType()
                .orElseThrow(() -> new ErrorExceptionTypeNotDeclared(ctx));

        registry.addExpectedType(exceptionType.type());
        ctx.expr_.accept(this);

        return resolveAmbiguity(
                maybeExpected,
                () -> new ErrorAmbiguousThrowType(ctx)
        );
    }

    @Override
    public StellaType visitTryWith(StellaParser.TryWithContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType tryType = ctx.tryExpr.accept(this);

        registry.addExpectedType(tryType);
        ctx.fallbackExpr.accept(this);

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(tryType, ctx);
    }

    @Override
    public StellaType visitTryCatch(StellaParser.TryCatchContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();

        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType tryType = ctx.tryExpr.accept(this);

        StellaExceptionType exceptionType = registry.getExceptionType()
                .orElseThrow(() -> new ErrorExceptionTypeNotDeclared(ctx));
        StellaPatternResolver.Result patResult = new StellaPatternResolver(registry, ctx.pat, exceptionType.type())
                .resolve(exceptionType.type().allPossiblePatterns());

        try {
            registry.enterScope(ctx.getText());
            for (var var : patResult.vars().entrySet()) {
                registry.addVar(var.getKey(), var.getValue());
            }

            registry.addExpectedType(tryType);
            ctx.fallbackExpr.accept(this);
        } finally {
            registry.exitScope();
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(tryType, ctx);
    }
    //endregion
}
