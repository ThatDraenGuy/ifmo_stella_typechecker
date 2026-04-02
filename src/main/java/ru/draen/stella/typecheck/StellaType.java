package ru.draen.stella.typecheck;

import ru.draen.stella.Utils;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateRecordTypeFields;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateVariantTypeFields;
import ru.draen.stella.typecheck.exceptions.UnsupportedException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// да, я джавист. да, я люблю типы-суммы. да, мы существуем
public sealed interface StellaType {
    boolean matches(StellaType other); //точное равенство типов

    default boolean isSubtypeOf(StellaType other) {
        return matches(other) || other instanceof Top;
    }

    List<StellaPattern> allPossiblePatterns();

    record Bool() implements StellaType {
        @Override
        public String toString() {
            return "Bool";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Bool;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.FalsePattern(), new StellaPattern.TruePattern());
        }
    }
    record Nat() implements StellaType {
        @Override
        public String toString() {
            return "Nat";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Nat;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.ZeroPattern(), new StellaPattern.SuccPattern());
        }
    }
    record Func(List<StellaType> in, StellaType out) implements StellaType {
        @Override
        public String toString() {
            return "(" + in.stream().map(Objects::toString).collect(Collectors.joining(", ")) + ") -> " + out;
        }

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

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Func(List<StellaType> in2, StellaType out2)
                            && listIsSubtypeOf(in2, in)
                            && out.isSubtypeOf(out2)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }
    }
    record Unit() implements StellaType {
        @Override
        public String toString() {
            return "Unit";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Unit;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.UnitPattern());
        }
    }
    record Tuple(List<StellaType> items) implements StellaType {
        @Override
        public String toString() {
            return "{" + items.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "}";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Tuple(List<StellaType> items2)
                    && listMatches(items, items2);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Tuple(List<StellaType> items1) && listIsSubtypeOf(items, items1)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            List<List<StellaPattern>> patterns = items.stream().map(StellaType::allPossiblePatterns).toList();
            return Utils.productList(patterns)
                    .map(set -> (StellaPattern)(new StellaPattern.TuplePattern(set)))
                    .toList();
        }
    }
    record Record(Map<String, Item> items) implements StellaType {
        @Override
        public String toString() {
            return "{" + items.values().stream()
                    .map(Item::toString)
                    .collect(Collectors.joining(", ")) + "}";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Record(Map<String, Item> items2)
                    && mapMatches(items, items2, Item::type);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Record(Map<String, Item> items1) && mapIsSubtypeOf(items1, items, Item::type)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            List<String> names = items.keySet().stream().toList();
            List<List<StellaPattern>> products = names.stream().map(name -> items.get(name).type.allPossiblePatterns()).toList();
            return Utils.productList(products)
                    .map(set -> (StellaPattern) (new StellaPattern.RecordPattern(
                            IntStream.range(0, names.size()).mapToObj(i -> new StellaPattern.RecordPattern.Item(
                                    names.get(i), set.get(i)
                            )).collect(Collectors.toMap(
                                    StellaPattern.RecordPattern.Item::name,
                                    Function.identity()
                            ))
                    )))
                    .toList();
        }

        record Item(String name, StellaType type) {
            @Override
            public String toString() {
                return name + " : " + type;
            }
        }
    }
    record Sum(StellaType inl, StellaType inr) implements StellaType {
        @Override
        public String toString() {
            return inl + " + " + inr;
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Sum(StellaType inl2, StellaType inr2) && inl.matches(inl2) && inr.matches(inr2);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Sum(StellaType inl1, StellaType inr1)
                            && inl.isSubtypeOf(inl1)
                            && inr.isSubtypeOf(inr1)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.InlPattern(), new StellaPattern.InrPattern());
        }
    }
    record Variant(Map<String, Item> items) implements StellaType {
        @Override
        public String toString() {
            return "<|" + items.values().stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ")) + "|>";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Variant(Map<String, Item> items2)
                    && mapMatchesWithOptionals(items, items2, Item::type);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Variant(Map<String, Item> items1) && mapIsSubtypeOfWithOptionals(items, items1, Item::type)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return items.keySet().stream().map(name -> (StellaPattern) (new StellaPattern.VariantPattern(name)))
                    .toList();
        }

        record Item(String name, Optional<StellaType> type) {
            @Override
            public String toString() {
                return name + type.map(stellaType -> " : " + stellaType).orElse("");
            }
        }
    }
    record StellaList(StellaType itemType) implements StellaType {
        @Override
        public String toString() {
            return "[" + itemType + "]";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof StellaList(StellaType itemType2) && itemType.matches(itemType2);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof StellaList(StellaType itemType2) && itemType.isSubtypeOf(itemType2)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return Stream.concat(Stream.of(
                    (StellaPattern) new StellaPattern.EmptyListPattern()),
                    itemType.allPossiblePatterns().stream().map(StellaPattern.ConsPattern::new)
            ).toList();
        }
    }

    record Ref(StellaType inner) implements StellaType {
        @Override
        public String toString() {
            return "&" + inner;
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Ref(StellaType inner1) && inner.matches(inner1);
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return StellaType.super.isSubtypeOf(other) || (
                    other instanceof Ref(StellaType inner1) && inner.isSubtypeOf(inner1)
            );
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }
    }

    record Top() implements StellaType {
        @Override
        public String toString() {
            return "Top";
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Top;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }
    }

    record Bottom() implements StellaType {
        @Override
        public String toString() {
            return "Bot";
        }

        @Override
        public boolean isSubtypeOf(StellaType other) {
            return true;
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof Bottom;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
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
                            fieldType -> new Variant.Item(fieldType.label.getText(),
                                    Optional.ofNullable(fieldType.type_).map(StellaType::fromAst)),
                            (fieldType1, fieldType2) -> {
                                throw new ErrorDuplicateVariantTypeFields(variant, fieldType1.name());
                            }
                    ))
            );
            case StellaParser.TypeListContext list -> new StellaList(StellaType.fromAst(list.type_));
            case StellaParser.TypeRefContext ref -> new Ref(StellaType.fromAst(ref.type_));
            case StellaParser.TypeTopContext ignored -> new Top();
            case StellaParser.TypeBottomContext ignored -> new Bottom();
            default -> throw new UnsupportedException();
        };
    }

    static boolean listMatches(List<StellaType> fst, List<StellaType> snd) {
        return fst.size() == snd.size()
                && IntStream.range(0, fst.size())
                .mapToObj(i -> fst.get(i).matches(snd.get(i)))
                .allMatch(Boolean::booleanValue);
    }

    static boolean listIsSubtypeOf(List<StellaType> fst, List<StellaType> snd) {
        return fst.size() == snd.size()
                && IntStream.range(0, fst.size())
                .mapToObj(i -> fst.get(i).isSubtypeOf(snd.get(i)))
                .allMatch(Boolean::booleanValue);
    };

    static <T>boolean mapMatches(Map<String, T> fst, Map<String, T> snd, Function<T, StellaType> typeGetter) {
        return fst.keySet().equals(snd.keySet()) && fst.keySet().stream()
                .allMatch(name ->
                        typeGetter.apply(fst.get(name)).matches(typeGetter.apply(snd.get(name))));
    }

    static <T>boolean mapIsSubtypeOf(Map<String, T> fst, Map<String, T> snd, Function<T, StellaType> typeGetter) {
        return fst.keySet().stream().allMatch(name ->
                snd.get(name) != null &&
                        typeGetter.apply(snd.get(name))
                                .isSubtypeOf(typeGetter.apply(fst.get(name))));
    }

    static <T>boolean mapMatchesWithOptionals(Map<String, T> fst, Map<String, T> snd, Function<T, Optional<StellaType>> typeGetter) {
        return fst.keySet().equals(snd.keySet()) && fst.keySet().stream()
                .allMatch(name -> {
                    Optional<StellaType> first = typeGetter.apply(fst.get(name));
                    Optional<StellaType> second = typeGetter.apply(snd.get(name));
                    return first.isPresent() == second.isPresent() && (first.isEmpty() || first.get().matches(second.get()));
                });
    }

    static <T>boolean mapIsSubtypeOfWithOptionals(Map<String, T> fst, Map<String, T> snd, Function<T, Optional<StellaType>> typeGetter) {
        return fst.keySet().stream().allMatch(name -> {
            if (snd.get(name) == null) return false;
            Optional<StellaType> first = typeGetter.apply(fst.get(name));
            Optional<StellaType> second = typeGetter.apply(snd.get(name));
            return first.isPresent() == second.isPresent() && (first.isEmpty() || first.get().isSubtypeOf(second.get()));
        });
    }
}
