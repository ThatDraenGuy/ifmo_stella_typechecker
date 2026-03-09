package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.UnsupportedException;

import java.util.List;
import java.util.stream.IntStream;

// да, я джавист. да, я люблю типы-суммы. да, мы существуем
public sealed interface StellaType {
    record Bool() implements StellaType {

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Bool;
        }
    }
    record Nat() implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Nat;
        }
    }
    record Func(List<StellaType> in, StellaType out) implements StellaType {
        public static Func fromDeclFun(StellaParser.DeclFunContext ctx) {
            return new Func(ctx.paramDecls.stream()
                    .map(decl -> StellaType.fromAst(decl.paramType))
                    .toList(),
                    StellaType.fromAst(ctx.returnType));
        }
        public static Func fromAbstraction(StellaParser.AbstractionContext ctx, StellaType returnType) {
            return new Func(ctx.paramDecls.stream()
                    .map(decl -> StellaType.fromAst(decl.paramType))
                    .toList(),
                    returnType);
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Func(List<StellaType> in2, StellaType out2)
                    && in.size() == in2.size()
                    && IntStream.range(0, in.size())
                    .mapToObj(i -> in.get(i).matches(in2.get(i)))
                    .allMatch(Boolean::booleanValue)
                    && out.matches(out2);
        }
    }
    record Unit() implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Unit;
        }
    }
    record Pair(StellaType fst, StellaType snd) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Pair(StellaType fst2, StellaType snd2) && fst.matches(fst2) && snd.matches(snd2);
        }
    }
    record Tuple(List<StellaType> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return false; //TODO
        }
    }
    record Record(List<Item> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return false; //TODO
        }

        record Item(String name, StellaType type) {}
    }
    record Sum(List<StellaType> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return false; //TODO
        }
    }

    static StellaType fromAst(StellaParser.StellatypeContext ctx) {
        return switch (ctx) {
            case StellaParser.TypeParensContext ctx2 -> fromAst(ctx2.type_);
            case StellaParser.TypeBoolContext ignored -> new Bool();
            case StellaParser.TypeNatContext ignored -> new Nat();
            case StellaParser.TypeFunContext fun -> new Func(fun.paramTypes.stream()
                    .map(StellaType::fromAst)
                    .toList(),
                    fromAst(fun.returnType));
            //TODO
            default -> throw new UnsupportedException();
        };
    }

    boolean matches(StellaType other);
}
