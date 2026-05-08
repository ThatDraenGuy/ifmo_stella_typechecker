package ru.draen.stella.typecheck;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import ru.draen.stella.Utils;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.ErrorAmbiguousTypeVar;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateRecordTypeFields;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateVariantTypeFields;
import ru.draen.stella.typecheck.exceptions.UnsupportedException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// да, я джавист. да, я люблю типы-суммы. да, мы существуем
public sealed interface StellaType {
    //равенство типов
    boolean matches(StellaType other);
    List<StellaPattern> allPossiblePatterns();
    boolean contains(TypeVar var);
    StellaType replace(TypeMapping mapping);
    default void checkAmbiguity() {}

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

        @Override
        public boolean contains(TypeVar var) {
            return false;
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return this;
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

        @Override
        public boolean contains(TypeVar var) {
            return false;
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return this;
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
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }

        @Override
        public boolean contains(TypeVar var) {
            return in.stream().anyMatch(type -> type.contains(var)) || out.contains(var);
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new Func(
                    in.stream().map(type -> type.replace(mapping)).toList(),
                    out.replace(mapping)
            );
        }

        @Override
        public void checkAmbiguity() {
            in.forEach(StellaType::checkAmbiguity);
            out.checkAmbiguity();
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

        @Override
        public boolean contains(TypeVar var) {
            return false;
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return this;
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
        public List<StellaPattern> allPossiblePatterns() {
            List<List<StellaPattern>> patterns = items.stream().map(StellaType::allPossiblePatterns).toList();
            return Utils.productList(patterns)
                    .map(set -> (StellaPattern)(new StellaPattern.TuplePattern(set)))
                    .toList();
        }

        @Override
        public boolean contains(TypeVar var) {
            return items.stream().anyMatch(type -> type.contains(var));
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new Tuple(
                    items.stream().map(type -> type.replace(mapping)).toList()
            );
        }

        @Override
        public void checkAmbiguity() {
            items.forEach(StellaType::checkAmbiguity);
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

        @Override
        public boolean contains(TypeVar var) {
            return items.values().stream().anyMatch(item -> item.type.contains(var));
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new Record(items.entrySet().stream()
                    .peek(entry ->
                            entry.setValue(new Item(entry.getKey(), entry.getValue().type().replace(mapping))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        @Override
        public void checkAmbiguity() {
            items.values().forEach(item -> item.type.checkAmbiguity());
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
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.InlPattern(), new StellaPattern.InrPattern());
        }

        @Override
        public boolean contains(TypeVar var) {
            return inl.contains(var) || inr.contains(var);
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new Sum(inl.replace(mapping), inr.replace(mapping));
        }

        @Override
        public void checkAmbiguity() {
            inl.checkAmbiguity();
            inr.checkAmbiguity();
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
        public List<StellaPattern> allPossiblePatterns() {
            return items.keySet().stream().map(name -> (StellaPattern) (new StellaPattern.VariantPattern(name)))
                    .toList();
        }

        @Override
        public boolean contains(TypeVar var) {
            return items.values().stream().anyMatch(item -> item.type.isPresent() && item.type.get().contains(var));
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new Variant(items.entrySet().stream()
                    .peek(entry ->
                            entry.setValue(new Item(entry.getKey(), entry.getValue().type()
                                    .map(type -> type.replace(mapping)))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        @Override
        public void checkAmbiguity() {
            items.values().forEach(item -> item.type.ifPresent(StellaType::checkAmbiguity));
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
        public List<StellaPattern> allPossiblePatterns() {
            return Stream.concat(Stream.of(
                    (StellaPattern) new StellaPattern.EmptyListPattern()),
                    itemType.allPossiblePatterns().stream().map(StellaPattern.ConsPattern::new)
            ).toList();
        }

        @Override
        public boolean contains(TypeVar var) {
            return itemType.contains(var);
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return new StellaList(itemType.replace(mapping));
        }

        @Override
        public void checkAmbiguity() {
            itemType.checkAmbiguity();
        }
    }
    record Forall(Map<String, NamedVar> vars, StellaType inner) implements StellaType {
        @Override
        public String toString() {
            return "forall " + vars.values().stream().map(Objects::toString).collect(Collectors.joining(",")) + ". " + inner.toString();
        }

        @Override
        public boolean matches(StellaType other) {
            return false;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }

        @Override
        public boolean contains(TypeVar var) {
            return false; //TODO
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return null; //TODO
        }
    }

    sealed interface TypeVar extends StellaType {}
    record NamedVar(String name) implements TypeVar {
        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean matches(StellaType other) {
            return false;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }

        @Override
        public boolean contains(TypeVar var) {
            return this == var;
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return contains(mapping.var()) ? mapping.type() : this;
        }
    }
    record FreshVar(int id, ParserRuleContext source) implements TypeVar {
        private static int LAST_ID = 0;
        private static final List<StellaType> allFreshVars = new ArrayList<>();
        public static FreshVar create(ParserRuleContext source) {
            FreshVar result = new FreshVar(++LAST_ID, source);
            allFreshVars.add(result);
            return result;
        }
        public static List<StellaType> all() {
            return allFreshVars;
        }

        @Override
        public String toString() {
            return "?T" + id;
        }

        @Override
        public boolean matches(StellaType other) {
            return other instanceof FreshVar(int id1, ParserRuleContext source1) && id == id1;
        }

        @Override
        public List<StellaPattern> allPossiblePatterns() {
            return List.of(new StellaPattern.NoPattern());
        }

        @Override
        public boolean contains(TypeVar var) {
            return var instanceof FreshVar(int id1, ParserRuleContext ignored) && id == id1;
        }

        @Override
        public StellaType replace(TypeMapping mapping) {
            return contains(mapping.var()) ? mapping.type() : this;
        }

        @Override
        public void checkAmbiguity() {
            throw new ErrorAmbiguousTypeVar(this);
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
            case StellaParser.TypeAutoContext auto -> FreshVar.create(auto);
            case StellaParser.TypeForAllContext forall -> new Forall(
                    forall.types.stream().collect(Collectors.toMap(
                            Token::getText,
                            type -> new NamedVar(type.getText()) //TODO think
                    )),
                    StellaType.fromAst(forall.type_)
            );
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

    static <T>boolean mapMatchesWithOptionals(Map<String, T> fst, Map<String, T> snd, Function<T, Optional<StellaType>> typeGetter) {
        return fst.keySet().equals(snd.keySet()) && fst.keySet().stream()
                .allMatch(name -> {
                    Optional<StellaType> first = typeGetter.apply(fst.get(name));
                    Optional<StellaType> second = typeGetter.apply(snd.get(name));
                    return first.isPresent() == second.isPresent() && (first.isEmpty() || first.get().matches(second.get()));
                });
    }
}
