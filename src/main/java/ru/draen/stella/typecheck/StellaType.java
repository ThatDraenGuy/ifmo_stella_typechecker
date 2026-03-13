package ru.draen.stella.typecheck;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateRecordTypeFields;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateVariantTypeFields;
import ru.draen.stella.typecheck.exceptions.UnsupportedException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// да, я джавист. да, я люблю типы-суммы. да, мы существуем
public sealed interface StellaType {
    //равенство типов
    boolean matches(StellaType other);

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
                    && listMatches(in, in2)
                    && out.matches(out2);
        }
    }
    record Unit() implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Unit;
        }
    }
    record Tuple(List<StellaType> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Tuple(List<StellaType> items2)
                    && listMatches(items, items2);
        }
    }
    record Record(Map<String, Item> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Record(Map<String, Item> items2)
                    && mapMatches(items, items2, Item::type);
        }

        record Item(String name, StellaType type) {}
    }
    record Sum(StellaType inl, StellaType inr) implements StellaType {
        public static final String INL = "inl";
        public static final String INR = "inr";

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Sum(StellaType inl2, StellaType inr2) && inl.matches(inl2) && inr.matches(inr2);
        }
    }
    record Variant(Map<String, Item> items) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof Variant(Map<String, Item> items2)
                    && mapMatches(items, items2, Item::type);
        }

        record Item(String name, StellaType type) {}

    }
    record StellaList(StellaType itemType) implements StellaType {
        @Override
        public boolean matches(StellaType other) {
            return other instanceof StellaList(StellaType itemType2) && itemType.matches(itemType2);
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
            case StellaParser.TypeUnitContext ignored -> new Unit();
            case StellaParser.TypeTupleContext tuple -> new Tuple(
                    tuple.types.stream().map(StellaType::fromAst).toList()
            );
            case StellaParser.TypeRecordContext record -> new Record(
                    record.fieldTypes.stream().collect(Collectors.toMap(
                            fieldType -> fieldType.label.getText(),
                            fieldType -> new Record.Item(fieldType.label.getText(), StellaType.fromAst(fieldType.type_)),
                            (fieldType1, fieldType2) -> {
                                throw new ErrorDuplicateRecordTypeFields(record, fieldType1.name());
                            }
                    )));
            case StellaParser.TypeSumContext sum -> new Sum(
                    StellaType.fromAst(sum.left),
                    StellaType.fromAst(sum.right)
            );
            case StellaParser.TypeVariantContext variant -> new Variant(
                    variant.fieldTypes.stream().collect(Collectors.toMap(
                            fieldType -> fieldType.label.getText(),
                            fieldType -> new Variant.Item(fieldType.label.getText(), StellaType.fromAst(fieldType.type_)),
                            (fieldType1, fieldType2) -> {
                                throw new ErrorDuplicateVariantTypeFields(variant, fieldType1.name());
                            }
                    ))
            );
            case StellaParser.TypeListContext list -> new StellaList(StellaType.fromAst(list.type_));
            default -> throw new UnsupportedException();
        };
    }

    static boolean listMatches(List<StellaType> fst, List<StellaType> snd) {
        return fst.size() == snd.size()
                && IntStream.range(0, fst.size())
                .mapToObj(i -> fst.get(i).matches(snd.get(i)))
                .allMatch(Boolean::booleanValue);
    }

    static <T>boolean mapMatches(Map<String, T> fst, Map<String, T> snd, Function<T, StellaType> typeGetter) {
        return fst.keySet().equals(snd.keySet()) && fst.keySet().stream()
                .allMatch(name ->
                        typeGetter.apply(fst.get(name)).matches(typeGetter.apply(snd.get(name))));
    }
}
