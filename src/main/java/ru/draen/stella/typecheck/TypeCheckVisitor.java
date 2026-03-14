package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.generated.StellaParserBaseVisitor;
import ru.draen.stella.typecheck.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TypeCheckVisitor extends StellaParserBaseVisitor<StellaType> {
    private static final String MAIN_FUNC_NAME = "main";

    private final TypeCheckRegistry registry;

    public TypeCheckVisitor(TypeCheckRegistry registry) {
        this.registry = registry;
    }

    private void checkTypeMismatch(StellaType expected, StellaType actual, StellaParser.ExprContext expr) {
        if (!expected.matches(actual)) {
            throw new ErrorUnexpectedTypeForExpression(expr, expected);
        }
    }

    private StellaType returnChecked(StellaType result, StellaParser.ExprContext expr) {
        registry.consumeExpectedType().ifPresent(expected -> {
            checkTypeMismatch(expected, result, expr);
        });
        return result;
    }

    @Override
    public StellaType visitProgram(StellaParser.ProgramContext ctx) {
        StellaType res = visitChildren(ctx);
        if (registry.getVar(MAIN_FUNC_NAME).isEmpty()) {
            throw new ErrorMissingMain();
        }
        return res;
    }

    @Override
    public StellaType visitParenthesisedExpr(StellaParser.ParenthesisedExprContext ctx) {
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
        if (registry.addVar(ctx.name.getText(), funcType)) {
            throw new ErrorDuplicateFunctionDeclaration(ctx);
        }

        registry.enterScope(ctx.name.getText());
        for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
            registry.addVar(paramDecl.name.getText(), StellaType.fromAst(paramDecl.paramType));
        }
        registry.addExpectedType(funcType.out());
        ctx.returnExpr.accept(this);
        registry.exitScope();
        return funcType;
    }

    @Override
    public StellaType visitAbstraction(StellaParser.AbstractionContext ctx) {
        registry.enterScope(ctx.getText());
        Optional<StellaType.Func> maybeExpectedFunc = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.Func expectedFunc)) {
                throw new ErrorUnexpectedLambda(ctx, expected);
            }

            if (ctx.paramDecls.size() != expectedFunc.in().size()) {
                throw new ErrorUnexpectedNumberOfParametersInLambda(ctx, expected);
            }

            IntStream.range(0, ctx.paramDecls.size()).forEach(i -> {
                StellaParser.ParamDeclContext paramDecl = ctx.paramDecls.get(i);
                StellaType paramType = StellaType.fromAst(paramDecl.paramType);
                if (!paramType.matches(expectedFunc.in().get(i))) {
                    throw new ErrorUnexpectedTypeForParameter(paramDecl, expectedFunc.in().get(i));
                }
            });

            return expectedFunc;
        });

        for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
            StellaType paramType = StellaType.fromAst(paramDecl.paramType);
            registry.addVar(paramDecl.name.getText(), paramType);
        }

        maybeExpectedFunc.ifPresent(expectedFunc -> registry.addExpectedType(expectedFunc.out()));
        StellaType returnType = ctx.returnExpr.accept(this);
        registry.exitScope();

        maybeExpectedFunc.ifPresent(registry::addExpectedType);
        return returnChecked(StellaType.Func.fromAbstraction(ctx, returnType), ctx);
    }

    @Override
    public StellaType visitApplication(StellaParser.ApplicationContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.fun.accept(this);
        if (!(type instanceof StellaType.Func(List<StellaType> inTypes, StellaType outType))) {
            throw new ErrorNotAFunction(ctx.fun);
        }

        if (ctx.args.size() != inTypes.size()) {
            throw new ErrorIncorrectNumberOfArguments(ctx);
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
        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType falseType = ctx.elseExpr.accept(this);

        checkTypeMismatch(trueType, falseType, ctx.elseExpr);

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
        Optional<StellaType.Tuple> maybeExpectedTuple = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.Tuple expectedTuple)) {
                throw new ErrorUnexpectedTuple(ctx, expected);
            }

            if (ctx.exprs.size() != expectedTuple.items().size()) {
                throw new ErrorUnexpectedTupleLength(ctx, expected);
            }
            return expectedTuple;
        });

        List<StellaType> types = IntStream.range(0, ctx.exprs.size()).mapToObj(i -> {
            StellaParser.ExprContext expr = ctx.exprs.get(i);
            maybeExpectedTuple.ifPresent(expected -> registry.addExpectedType(expected.items().get(i)));
            return expr.accept(this);
        }).toList();
        return new StellaType.Tuple(types);
    }

    @Override
    public StellaType visitDotTuple(StellaParser.DotTupleContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Tuple(List<StellaType> itemTypes))) {
            throw new ErrorNotATuple(ctx.expr_);
        }
        int index = Integer.parseInt(ctx.index.getText());
        if (index > itemTypes.size()) {
            throw new ErrorTupleIndexOutOfBounds(ctx.expr_, index);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(itemTypes.get(index - 1), ctx);
    }
    //endregion

    //region records
    @Override
    public StellaType visitRecord(StellaParser.RecordContext ctx) {
        Optional<StellaType.Record> maybeExpectedRecord = registry.consumeExpectedType().map(expected -> {
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
                    throw new ErrorMissingRecordFields(ctx, expected, expectedField);
                }
            }
            for (String actualField : actualFields.keySet()) {
                if (!expectedRecord.items().containsKey(actualField)) {
                    throw new ErrorUnexpectedRecordFields(ctx, expected, actualField);
                }
            }

            return expectedRecord;
        });

        Map<String, StellaType.Record.Item> items = ctx.bindings.stream().map(binding -> {
            maybeExpectedRecord.ifPresent(expectedRecord -> registry.addExpectedType(
                    expectedRecord.items().get(binding.name.getText()).type()));
            return new StellaType.Record.Item(binding.name.getText(), binding.rhs.accept(this));
        }).collect(Collectors.toMap(
                StellaType.Record.Item::name,
                Function.identity(),
                (binding1, binding2) -> {
                    throw new ErrorDuplicateRecordFields(ctx, binding1.name());
                }
        ));
        return new StellaType.Record(items);
    }

    @Override
    public StellaType visitDotRecord(StellaParser.DotRecordContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Record(Map<String, StellaType.Record.Item> items))) {
            throw new ErrorNotARecord(ctx.expr_);
        }
        String label = ctx.label.getText();
        StellaType result = Optional.ofNullable( items.get(label))
                .orElseThrow(() -> new ErrorUnexpectedFieldAccess(ctx.expr_, label))
                .type();

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(result, ctx);
    }
    //endregion

    //region itemType sum + variant + match
    @Override
    public StellaType visitInl(StellaParser.InlContext ctx) {
        Optional<StellaType.Sum> maybeExpectedSum = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.Sum expectedSum)) {
                throw new ErrorUnexpectedInjection(ctx, expected);
            }
            return expectedSum;
        });

        maybeExpectedSum.ifPresent(expectedSum -> registry.addExpectedType(expectedSum.inl()));
        ctx.expr_.accept(this);
        return maybeExpectedSum.orElseThrow(() -> new ErrorAmbiguousSumType(ctx));
    }

    @Override
    public StellaType visitInr(StellaParser.InrContext ctx) {
        Optional<StellaType.Sum> maybeExpectedSum = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.Sum expectedSum)) {
                throw new ErrorUnexpectedInjection(ctx, expected);
            }
            return expectedSum;
        });

        maybeExpectedSum.ifPresent(expectedSum -> registry.addExpectedType(expectedSum.inr()));
        ctx.expr_.accept(this);
        return maybeExpectedSum.orElseThrow(() -> new ErrorAmbiguousSumType(ctx));
    }

    @Override
    public StellaType visitVariant(StellaParser.VariantContext ctx) {
        Optional<StellaType.Variant> maybeExpectedVariant = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.Variant expectedVariant)) {
                throw new ErrorUnexpectedVariant(ctx, expected);
            }

            return expectedVariant;
        });

        String label = ctx.label.getText();
        maybeExpectedVariant.ifPresent(expectedVariant -> {
            if (!expectedVariant.items().containsKey(label)) {
                throw new ErrorUnexpectedVariantLabel(ctx, expectedVariant, label);
            }
            registry.addExpectedType(expectedVariant.items().get(label).type());
        });
        ctx.rhs.accept(this);
        return maybeExpectedVariant.orElseThrow(() -> new ErrorAmbiguousVariantType(ctx));
    }

    private Map<String, StellaType> resolveMatchVars(StellaParser.PatternContext pattern, StellaType type) {
        switch (type) {
            case StellaType.Bool bool -> {
            }
            case StellaType.Func func -> {
            }
            case StellaType.Nat nat -> {
            }
            case StellaType.Record record -> {
            }
            case StellaType.StellaList stellaList -> {
            }
            case StellaType.Sum sum -> {
            }
            case StellaType.Tuple tuple -> {
            }
            case StellaType.Unit unit -> {
            }
            case StellaType.Variant variant -> {
            }
        }
        return switch (pattern) {
            case StellaParser.PatternVarContext var -> Map.of(var.name.getText(), type);
            default -> throw new IllegalStateException("Unexpected value: " + pattern);
        };
    }

    @Override
    public StellaType visitMatch(StellaParser.MatchContext ctx) {
        //TODO rework this whole thing
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.expr_.accept(this);
        Map<String, StellaType> variants = switch (type) {
            case StellaType.Sum sum -> Map.of(
                    StellaType.Sum.INL, sum.inl(),
                    StellaType.Sum.INR, sum.inr()
            );
            case StellaType.Variant variant -> variant.items().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().type()
            ));
            default -> Map.of();
        };

        if (ctx.cases.isEmpty()) {
            throw new ErrorIllegalEmptyMatching(ctx);
        }
        Map<String, StellaParser.MatchCaseContext> cases = ctx.cases.stream().collect(Collectors.toMap(
                matchCase -> switch (matchCase.pattern()) {
                    case StellaParser.PatternInlContext inl -> StellaType.Sum.INL;
                    case StellaParser.PatternInrContext inr -> StellaType.Sum.INR;
                    case StellaParser.PatternVariantContext variant -> variant.label.getText();
                    default -> throw new IllegalStateException("Unexpected value: " + matchCase.pattern());
                },
                Function.identity()
        ));

        List<String> missingPatterns = new ArrayList<>();
        for (String label : variants.keySet()) {
            if (!cases.containsKey(label)) {
                missingPatterns.add(label);
            }
        }
        if (!missingPatterns.isEmpty()) {
            throw new ErrorNonexhaustiveMatchPatterns(ctx, missingPatterns);
        }

        for (String label : cases.keySet()) {
            if (!variants.containsKey(label)) {
                throw new ErrorUnexpectedPatternForType(cases.get(label));
            }
        }

        List<StellaType> exprTypes = cases.keySet().stream().map(label -> {
            StellaParser.MatchCaseContext matchCase = cases.get(label);
            StellaType variantType = variants.get(label);
            Map<String, StellaType> vars = resolveMatchVars(matchCase.pattern_, variantType);
            registry.enterScope(matchCase.getText());
            for (var var : vars.entrySet()) {
                registry.addVar(var.getKey(), var.getValue());
            }
            maybeExpected.ifPresent(registry::addExpectedType);
            StellaType result = matchCase.expr_.accept(this);
            registry.exitScope();
            return result;
        }).toList();

        StellaType expected = maybeExpected.orElse(exprTypes.getFirst());
        for (StellaType exprType : exprTypes) {
            checkTypeMismatch(expected, exprType, null); //TODO
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(expected, ctx);
    }

    //endregion

    //region list

    @Override
    public StellaType visitConsList(StellaParser.ConsListContext ctx) {
        Optional<StellaType.StellaList> maybeExpectedList = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.StellaList expectedList)) {
                throw new ErrorUnexpectedList(ctx, expected);
            }
            return expectedList;
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
        Optional<StellaType.StellaList> maybeExpectedList = registry.consumeExpectedType().map(expected -> {
            if (!(expected instanceof StellaType.StellaList expectedList)) {
                throw new ErrorUnexpectedList(ctx, expected);
            }
            return expectedList;
        });

        if (ctx.exprs.isEmpty()) {
            return maybeExpectedList.orElseThrow(() -> new ErrorAmbiguousList(ctx));
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
            throw new ErrorNotAList(ctx.list);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(itemType, ctx);
    }

    @Override
    public StellaType visitTail(StellaParser.TailContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.list.accept(this);
        if (!(type instanceof StellaType.StellaList(StellaType itemType))) {
            throw new ErrorNotAList(ctx.list);
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        return returnChecked(new StellaType.StellaList(itemType), ctx);
    }

    @Override
    public StellaType visitIsEmpty(StellaParser.IsEmptyContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType type = ctx.list.accept(this);
        if (!(type instanceof StellaType.StellaList)) {
            throw new ErrorNotAList(ctx.list);
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

    //region let
     private Map<String, StellaType> resolveLetVars(StellaParser.PatternContext pattern, StellaType type) {
        return switch (pattern) {
            case StellaParser.PatternVarContext var -> Map.of(var.name.getText(), type);
            default -> throw new IllegalStateException("Unexpected value: " + pattern);
        };
     }

    @Override
    public StellaType visitLet(StellaParser.LetContext ctx) {
        Optional<StellaType> maybeExpected = registry.consumeExpectedType();
        StellaType exprType = ctx.patternBinding.rhs.accept(this);
        Map<String, StellaType> vars = resolveLetVars(ctx.patternBinding.pat, exprType);
        registry.enterScope(ctx.getText());
        for (var var : vars.entrySet()) {
            registry.addVar(var.getKey(), var.getValue());
        }

        maybeExpected.ifPresent(registry::addExpectedType);
        StellaType result = ctx.body.accept(this);
        registry.exitScope();
        return returnChecked(result, ctx);
    }
    //endregion
}
