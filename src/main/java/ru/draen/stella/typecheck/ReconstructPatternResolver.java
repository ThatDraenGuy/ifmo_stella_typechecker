package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicatePatternVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReconstructPatternResolver implements StellaPatternResolver {
    private final Map<String, StellaType> vars = new HashMap<>();
    private final TypeCheckRegistry registry;
    private final StellaParser.PatternContext pattern;
    private final StellaType type;

    public ReconstructPatternResolver(TypeCheckRegistry registry, StellaParser.ExprContext ctx, StellaParser.PatternContext pattern, StellaType type) {
        this.registry = registry;
        this.pattern = pattern;
        this.type = type;
    }

    @Override
    public Result resolve(List<StellaPattern> current) {
        addConstraints(pattern, type);
        return new Result(vars, List.of());
    }

    private void addConstraints(StellaParser.PatternContext pattern, StellaType type) {
        switch (pattern) {
            case StellaParser.PatternVarContext var -> {
                if (vars.put(var.name.getText(), type) != null) {
                    throw new ErrorDuplicatePatternVariable(pattern, var.name.getText());
                }
            }
            case StellaParser.PatternAscContext asc -> {
                registry.addConstraint(new Constraint(StellaType.fromAst(asc.type_), type, asc));
                addConstraints(asc.pattern_, type);
            }
            case StellaParser.PatternTrueContext truePat -> {
                registry.addConstraint(new Constraint(new StellaType.Bool(), type, truePat));
            }
            case StellaParser.PatternFalseContext falsePat -> {
                registry.addConstraint(new Constraint(new StellaType.Bool(), type, falsePat));
            }
            case StellaParser.PatternIntContext intPat -> {
                registry.addConstraint(new Constraint(new StellaType.Nat(), type, intPat));
            }
            case StellaParser.PatternSuccContext succ -> {
                StellaType.Nat nat = new StellaType.Nat();
                registry.addConstraint(new Constraint(nat, type, succ));
                addConstraints(succ.pattern_, nat);
            }
            case StellaParser.PatternUnitContext unit -> {
                registry.addConstraint(new Constraint(new StellaType.Unit(), type, unit));
            }
            case StellaParser.PatternTupleContext tuple -> {
                if (tuple.patterns.size() != 2)
                    throw new IllegalStateException("Реконструкция типов не поддерживается для кортежей");
                StellaType.Tuple pair = new StellaType.Tuple(List.of(StellaType.FreshVar.create(tuple), StellaType.FreshVar.create(tuple)));
                registry.addConstraint(new Constraint(pair, type, tuple));
                addConstraints(tuple.patterns.getFirst(), pair.items().getFirst());
                addConstraints(tuple.patterns.getLast(), pair.items().getLast());
            }
            case StellaParser.PatternInlContext inl -> {
                StellaType.Sum sum = new StellaType.Sum(StellaType.FreshVar.create(inl), StellaType.FreshVar.create(inl));
                registry.addConstraint(new Constraint(sum, type, inl));
                addConstraints(inl.pattern_, sum.inl());
            }
            case StellaParser.PatternInrContext inr -> {
                StellaType.Sum sum = new StellaType.Sum(StellaType.FreshVar.create(inr), StellaType.FreshVar.create(inr));
                registry.addConstraint(new Constraint(sum, type, inr));
                addConstraints(inr.pattern_, sum.inr());
            }
            case StellaParser.PatternConsContext cons -> {
                StellaType.StellaList list = new StellaType.StellaList(StellaType.FreshVar.create(cons));
                registry.addConstraint(new Constraint(list, type, cons));
                addConstraints(cons.head, list.itemType());
                addConstraints(cons.tail, list);
            }
            case StellaParser.PatternListContext listPat -> {
                StellaType.StellaList list = new StellaType.StellaList(StellaType.FreshVar.create(listPat));
                registry.addConstraint(new Constraint(list, type, listPat));
                for (var pat : listPat.patterns) {
                    addConstraints(pat, list.itemType());
                }
            }
            default -> {
                throw new IllegalStateException("Реконструкция типов не поддерживается для паттерна " + pattern);
            }
        }
    }
}
