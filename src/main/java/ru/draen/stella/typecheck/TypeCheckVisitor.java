package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.generated.StellaParserBaseVisitor;
import ru.draen.stella.typecheck.exceptions.*;

import java.util.List;

public class TypeCheckVisitor extends StellaParserBaseVisitor<StellaType> {
    private static final String MAIN_FUNC_NAME = "main";

    private final TypeCheckRegistry registry;

    public TypeCheckVisitor(TypeCheckRegistry registry) {
        this.registry = registry;
    }

    private void checkTypeMismatch(StellaType fst, StellaType snd) {
        if (!fst.matches(snd)) {
            throw new IllegalStateException(); //TODO proper error
        }
    }
    @Override
    public StellaType visitProgram(StellaParser.ProgramContext ctx) {
        StellaType res = visitChildren(ctx);
        if (registry.getVar(MAIN_FUNC_NAME).isEmpty()) {
            throw new ErrorMissingMain();
        }
        return res;
    }

    //region funcs
    @Override
    public StellaType visitDeclFun(StellaParser.DeclFunContext ctx) {
        StellaType.Func funcType = StellaType.Func.fromDeclFun(ctx);
        registry.addVar(ctx.name.getText(), funcType);
//        registry.addFunc(new StellaFunction(ctx.name.getText(), funcType));

        registry.enterScope(ctx.name.getText());
        for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
            registry.addVar(paramDecl.name.getText(), StellaType.fromAst(paramDecl.paramType));
        }
        registry.addExpectedType(funcType.out());
        visitChildren(ctx);
        registry.exitScope();
        return funcType;
    }

    @Override
    public StellaType visitAbstraction(StellaParser.AbstractionContext ctx) {
        registry.enterScope("lambda");
        registry.consumeExpectedType().ifPresent(expected -> {
            if (!(expected instanceof StellaType.Func expectedFunc)) {
                throw new ErrorUnexpectedLambda(ctx, expected);
            }
            //TODO check params
            registry.addExpectedType(expectedFunc.out());
        });

        for (StellaParser.ParamDeclContext paramDecl : ctx.paramDecls) {
            registry.addVar(paramDecl.name.getText(), StellaType.fromAst(paramDecl.paramType));
        }
        StellaType returnType = ctx.returnExpr.accept(this);
        registry.exitScope();
        return StellaType.Func.fromAbstraction(ctx, returnType);
    }

    @Override
    public StellaType visitApplication(StellaParser.ApplicationContext ctx) {
        StellaType type = ctx.fun.accept(this);
        if (!(type instanceof StellaType.Func(List<StellaType> inTypes, StellaType outType))) {
            throw new ErrorNotAFunction(ctx.fun);
        }

        List<StellaType> argTypes = ctx.args.stream().map(arg -> arg.accept(this)).toList();
        if (argTypes.size() != inTypes.size()) {
            throw new ErrorIncorrectNumberOfArguments(ctx);
        }

        for (int i = 0; i < argTypes.size(); i++) {
            checkTypeMismatch(argTypes.get(i), inTypes.get(i));
        }

        registry.consumeExpectedType().ifPresent(expected -> {
            checkTypeMismatch(expected, outType);
        });
        return outType;
    }

    //endregion

    @Override
    public StellaType visitVar(StellaParser.VarContext ctx) {
        return registry.getVar(ctx.name.getText()).orElseThrow(() -> new ErrorUndefinedVariable(ctx));
    }

    //region bools
    @Override
    public StellaType visitConstTrue(StellaParser.ConstTrueContext ctx) {
        return new StellaType.Bool();
    }

    @Override
    public StellaType visitConstFalse(StellaParser.ConstFalseContext ctx) {
        return new StellaType.Bool();
    }

    @Override
    public StellaType visitIf(StellaParser.IfContext ctx) {
        StellaType condType = ctx.condition.accept(this);
        StellaType trueType = ctx.thenExpr.accept(this);
        StellaType falseType = ctx.elseExpr.accept(this);

        checkTypeMismatch(condType, new StellaType.Bool());
        checkTypeMismatch(trueType, falseType);

        registry.consumeExpectedType().ifPresent(expected -> {
            checkTypeMismatch(expected, trueType);
        });
        return trueType;
    }
    //endregion

    //region unit
    @Override
    public StellaType visitConstUnit(StellaParser.ConstUnitContext ctx) {
        registry.consumeExpectedType().ifPresent(expected -> {
            checkTypeMismatch(expected, new StellaType.Unit());
        });
        return new StellaType.Unit();
    }
    //endregion

    //region tuple
    @Override
    public StellaType visitTuple(StellaParser.TupleContext ctx) {
        List<StellaType> types = ctx.exprs.stream().map(expr -> expr.accept(this)).toList();
        if (types.size() == 2) {
            return new StellaType.Pair(types.get(0), types.get(1));
        }
        return new StellaType.Tuple(types);
    }

    @Override
    public StellaType visitDotTuple(StellaParser.DotTupleContext ctx) {
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Tuple(List<StellaType> itemTypes))) {
            throw new ErrorNotATuple(ctx.expr_);
        }
        int index = Integer.parseInt(ctx.index.getText());
        if (itemTypes.size() <= index) {
            throw new ErrorTupleIndexOutOfBounds(ctx.expr_, index);
        }
        return itemTypes.get(index - 1);
    }
    //endregion

    //region records
    @Override
    public StellaType visitRecord(StellaParser.RecordContext ctx) {
        List<StellaType.Record.Item> items = ctx.bindings.stream().map(binding ->
                        new StellaType.Record.Item(binding.name.getText(), binding.rhs.accept(this)))
                .toList();
        return new StellaType.Record(items);
    }

    @Override
    public StellaType visitDotRecord(StellaParser.DotRecordContext ctx) {
        StellaType type = ctx.expr_.accept(this);
        if (!(type instanceof StellaType.Record(List<StellaType.Record.Item> items))) {
            throw new ErrorNotARecord(ctx.expr_);
        }
        String label = ctx.label.getText();
        return items.stream()
                .filter(item -> item.name().equals(label))
                .findFirst()
                .orElseThrow(() -> new ErrorUnexpectedFieldAccess(ctx.expr_, label))
                .type();
    }
    //endregion

}
