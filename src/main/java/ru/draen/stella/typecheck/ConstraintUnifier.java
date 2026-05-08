package ru.draen.stella.typecheck;

import ru.draen.stella.typecheck.exceptions.ErrorOccursCheckInfiniteType;
import ru.draen.stella.typecheck.exceptions.ErrorUnexpectedTypeForUnifyExpression;

import java.util.*;
import java.util.stream.Collectors;

public class ConstraintUnifier {
    private final Queue<Constraint> constraints = new ArrayDeque<>();

    public void add(Constraint constraint) {
        constraints.add(constraint);
    }

    public Deque<TypeMapping> unify() {
        return unify(constraints);
    }

    private static Queue<Constraint> replace(Queue<Constraint> constraints, TypeMapping mapping) {
        return constraints.stream()
                .map(constraint ->
                        new Constraint(constraint.actual().replace(mapping), constraint.expected().replace(mapping), constraint.ctx()))
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    private static Deque<TypeMapping> unify(Queue<Constraint> constraints) {
        if (constraints.isEmpty()) return new LinkedList<>();
        Constraint constraint = constraints.poll();
        StellaType lhs = constraint.actual();
        StellaType rhs = constraint.expected();
        if (lhs.matches(rhs))
            return unify(constraints);
        if (lhs instanceof StellaType.FreshVar var) {
            if (rhs.contains(var))
                throw new ErrorOccursCheckInfiniteType(constraint.ctx(), constraint.expected(), constraint.actual());
            TypeMapping mapping = new TypeMapping(var, rhs);
            var res = unify(replace(constraints, mapping));
            res.addFirst(mapping);
            return res;
        }
        if (rhs instanceof StellaType.FreshVar var) {
            if (lhs.contains(var))
                throw new ErrorOccursCheckInfiniteType(constraint.ctx(), constraint.expected(), constraint.actual());
            TypeMapping mapping = new TypeMapping(var, lhs);
            var res = unify(replace(constraints, mapping));
            res.addFirst(mapping);
            return res;
        }

        switch (lhs) {
            case StellaType.Sum(StellaType lhsInl, StellaType lhsInr) -> {
                if (!(rhs instanceof StellaType.Sum(StellaType rhsInl, StellaType rhsInr)))
                    break;
                constraints.add(new Constraint(lhsInl, rhsInl, constraint.ctx()));
                constraints.add(new Constraint(lhsInr, rhsInr, constraint.ctx()));
                return unify(constraints);
            }
            case StellaType.Tuple(List<StellaType> lhsItems) -> {
                if (!(rhs instanceof StellaType.Tuple(List<StellaType> rhsItems)))
                    break;
                if (lhsItems.size() != 2 || rhsItems.size() != 2)
                    throw new IllegalStateException(); //TODO
                constraints.add(new Constraint(lhsItems.getFirst(), rhsItems.getFirst(), constraint.ctx()));
                constraints.add(new Constraint(lhsItems.getLast(), rhsItems.getLast(), constraint.ctx()));
                return unify(constraints);
            }
            case StellaType.Func(List<StellaType> lhsIn, StellaType lhsOut) -> {
                if (!(rhs instanceof StellaType.Func(List<StellaType> rhsIn, StellaType rhsOut)))
                    break;
                if (lhsIn.size() != 1 || rhsIn.size() != 1)
                    throw new IllegalStateException(); //TODO
                constraints.add(new Constraint(lhsIn.getFirst(), rhsIn.getFirst(), constraint.ctx()));
                constraints.add(new Constraint(lhsOut, rhsOut, constraint.ctx()));
                return unify(constraints);
            }
            case StellaType.Nat ignored -> {
                if (!(rhs instanceof StellaType.Nat))
                    break;
                return unify(constraints);
            }
            case StellaType.Bool ignored -> {
                if (!(rhs instanceof StellaType.Bool))
                    break;
                return unify(constraints);
            }
            case StellaType.Unit ignored -> {
                if (!(rhs instanceof StellaType.Unit))
                    break;
                return unify(constraints);
            }
            default -> {
                throw new IllegalStateException("Реконструкция типов не поддерживается для типа " + lhs);
            }
        }
        throw new ErrorUnexpectedTypeForUnifyExpression(constraint.ctx(), constraint.expected(), constraint.actual());
    }
}
