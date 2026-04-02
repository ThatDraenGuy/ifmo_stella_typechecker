package ru.draen.stella.typecheck;

import ru.draen.stella.Utils;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StellaPatternResolver {
    private final Map<String, StellaType> vars = new HashMap<>();
    private final StellaParser.PatternContext pattern;
    private final StellaType type;

    public record Result(Map<String, StellaType> vars, List<StellaPattern> notExhausted, StellaType type) {}

    public StellaPatternResolver(StellaParser.PatternContext pattern, StellaType type) {
        this.pattern = pattern;
        this.type = type;
    }

    public Result resolve(List<StellaPattern> current) {
        checkPatternType(pattern, type);
        if (current.isEmpty()) {
            return new Result(vars, current, type);
        }
        List<StellaPattern> notExhausted = actualExhaust(pattern, type, current).toList();
        return new Result(vars, notExhausted, type);
    }

    private<T> Stream<T> exhaust(T value, boolean condition) {
        return condition ? Stream.empty() : Stream.of(value);
    }

    private void checkPatternType(StellaParser.PatternContext pattern, StellaType type) {
        if (pattern instanceof StellaParser.PatternVarContext var) {
            if (vars.put(var.name.getText(), type) != null) {
                throw new ErrorDuplicatePatternVariable(pattern, var.name.getText());
            }
            return;
        }

        if (pattern instanceof StellaParser.PatternAscContext asc) {
            if (!type.matches(StellaType.fromAst(asc.type_))) {
                throw new ErrorUnexpectedPatternForType(pattern, type);
            }
            checkPatternType(asc.pattern_, type);
            return;
        }

        switch (type) {
            case StellaType.Bool bool -> {
                switch (pattern) {
                    case StellaParser.PatternFalseContext ignored -> {}
                    case StellaParser.PatternTrueContext ignored -> {}
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                };
            }
            case StellaType.Nat nat -> {
                switch (pattern) {
                    case StellaParser.PatternIntContext ignored -> {}
                    case StellaParser.PatternSuccContext succ -> checkPatternType(succ.pattern_, nat);
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            case StellaType.Tuple tupleType -> {
                switch (pattern) {
                    case StellaParser.PatternTupleContext tupleCtx -> {
                        if (tupleCtx.patterns.size() != tupleType.items().size()) throw new ErrorUnexpectedPatternForType(pattern, type);
                        IntStream.range(0, tupleType.items().size()).forEach(i ->
                                checkPatternType(tupleCtx.patterns.get(i), tupleType.items().get(i)));
                    }
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            case StellaType.Record recordType -> {
                switch (pattern) {
                    case StellaParser.PatternRecordContext recordCtx -> {
                        Map<String, StellaParser.LabelledPatternContext> patterns = recordCtx.patterns.stream().collect(Collectors.toMap(
                                patt -> patt.label.getText(),
                                Function.identity(),
                                (patt1, patt2) -> {
                                    throw new ErrorDuplicateRecordPatternFields(recordCtx, patt1.label.getText());
                                }
                        ));
                        patterns.values().forEach(patt -> {
                            StellaType innerType = Optional.ofNullable(recordType.items().get(patt.label.getText()))
                                    .orElseThrow(() -> new ErrorUnexpectedPatternForType(pattern, type)).type();
                            checkPatternType(patt.pattern_, innerType);
                        });
                        for (String name : recordType.items().keySet()) {
                            // только паттерны со всеми полями
                            if (!patterns.containsKey(name)) throw new ErrorUnexpectedPatternForType(pattern, type);
                        }
                    }
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }

            case StellaType.Sum sumType -> {
                switch (pattern) {
                    case StellaParser.PatternInlContext inl -> {
                        checkPatternType(inl.pattern_, sumType.inl());
                    }
                    case StellaParser.PatternInrContext inr -> {
                        checkPatternType(inr.pattern_, sumType.inr());
                    }
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            case StellaType.Variant variantType -> {
                switch (pattern) {
                    case StellaParser.PatternVariantContext variantCtx -> {
                        String name = variantCtx.label.getText();
                        Optional<StellaType> innerType = Optional.ofNullable(variantType.items().get(name))
                                .orElseThrow(() -> new ErrorUnexpectedPatternForType(pattern, type))
                                .type();

                        if (innerType.isEmpty() && variantCtx.pattern_ != null) {
                            throw new ErrorUnexpectedNonNullaryVariantPattern(variantCtx, variantType, name);
                        }
                        if (innerType.isPresent() && variantCtx.pattern_ == null) {
                            throw new ErrorUnexpectedNullaryVariantPattern(variantCtx, variantType, name);
                        }
                        innerType.ifPresent(stellaType -> checkPatternType(variantCtx.pattern_, stellaType));
                    }
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            case StellaType.StellaList listType -> {
                switch (pattern) {
                    case StellaParser.PatternConsContext consCtx -> {
                        checkPatternType(consCtx.head, listType.itemType());
                        checkPatternType(consCtx.tail, listType);
                    }
                    case StellaParser.PatternListContext listCtx -> {
                        listCtx.patterns.forEach(patt -> checkPatternType(patt, listType.itemType()));
                    }
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            case StellaType.Unit unitType -> {
                switch (pattern) {
                    case StellaParser.PatternUnitContext ignored -> {}
                    default -> throw new ErrorUnexpectedPatternForType(pattern, type);
                }
            }
            default -> throw new ErrorUnexpectedPatternForType(pattern, type);
        };
    }

    private Stream<StellaPattern> resolveExhaust(StellaParser.PatternContext pattern, StellaType type, StellaPattern possible) {
        if (pattern instanceof StellaParser.PatternVarContext var) {
            return Stream.empty();
        }

        if (pattern instanceof StellaParser.PatternAscContext asc) {
            if (!type.matches(StellaType.fromAst(asc.type_))) {
                throw new ErrorUnexpectedPatternForType(pattern, type);
            }
            return resolveExhaust(asc.pattern_, type, possible);
        }

        return switch (type) {
            case StellaType.Bool bool -> switch (pattern) {
                case StellaParser.PatternFalseContext falseCtx ->
                        exhaust(possible, possible instanceof StellaPattern.FalsePattern);
                case StellaParser.PatternTrueContext trueCtx ->
                        exhaust(possible, possible instanceof StellaPattern.TruePattern);
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };

            case StellaType.Nat nat -> switch (pattern) {
                case StellaParser.PatternSuccContext succ ->
                        switch (possible) {
                            case StellaPattern.SuccPattern(Optional<StellaPattern> inner) -> {
                                Stream<StellaPattern> remaining = actualExhaust(succ.pattern_, nat, inner);
                                yield remaining.map(StellaPattern.SuccPattern::new);
                            }
                            case StellaPattern.RangePattern(int start, int end) -> {
                                if (start == 0) {
                                    int unsucc = end - 1;
                                    Optional<StellaPattern> unsuccRange = unsucc == 0
                                            ? Optional.of(new StellaPattern.ZeroPattern())
                                            : Optional.of(new StellaPattern.RangePattern(start, unsucc));
                                    Stream<StellaPattern> remaining = actualExhaust(succ.pattern_, nat, unsuccRange)
                                            .map(StellaPattern.SuccPattern::new);
                                    yield Stream.concat(Stream.of(new StellaPattern.ZeroPattern()), remaining);
                                }

                                int unsucc = end - 1;
                                Optional<StellaPattern> unsuccRange = unsucc == 0
                                        ? Optional.of(new StellaPattern.ZeroPattern())
                                        : Optional.of(new StellaPattern.RangePattern(start - 1, unsucc));
                                Stream<StellaPattern> remaining = actualExhaust(succ.pattern_, nat, unsuccRange);
                                yield remaining.map(StellaPattern.SuccPattern::new);
                            }
                            case StellaPattern.BeamPattern(int start) -> {
                                int unsucc = start - 1;
                                Optional<StellaPattern> unsuccRange = unsucc == 0
                                        ? Optional.empty()
                                        : Optional.of(new StellaPattern.BeamPattern(unsucc));
                                Stream<StellaPattern> remaining = actualExhaust(succ.pattern_, nat, unsuccRange);
                                yield remaining.map(StellaPattern.SuccPattern::new);
                            }

                            default -> Stream.of(possible); // zero-pattern
                        };
                case StellaParser.PatternIntContext intCtx -> {
                    int n = Integer.parseInt(intCtx.n.getText());
                    if (n == 0) {
                        yield switch (possible) {
                            case StellaPattern.ZeroPattern ignored -> Stream.empty();
                            case StellaPattern.RangePattern(int start, int end) -> {
                                if (start == 0) {
                                    int unsucc = end - 1;
                                    yield Stream.of(
                                            new StellaPattern.SuccPattern(unsucc == 0
                                                    ? new StellaPattern.ZeroPattern()
                                                    : new StellaPattern.RangePattern(start, unsucc))
                                    );
                                }

                                int unsucc = end - 1;
                                yield Stream.of(
                                        new StellaPattern.SuccPattern(unsucc == 0
                                                ? new StellaPattern.ZeroPattern()
                                                : new StellaPattern.RangePattern(start - 1, unsucc))
                                );
                            }
                            case StellaPattern.BeamPattern(int start) -> {
                                int unsucc = start - 1;
                                yield Stream.of(
                                        new StellaPattern.SuccPattern(unsucc == 0
                                                ? new StellaPattern.SuccPattern()
                                                : new StellaPattern.BeamPattern(unsucc))
                                );
                            }
                            default -> Stream.of(possible); // succ-pattern
                        };
                    }

                    //n != 0
                    int unsucc = n;
                    Optional<StellaPattern> current = Optional.of(possible);
                    int succCount = 0;
                    while (unsucc != 0 && current.isPresent() && current.get() instanceof StellaPattern.SuccPattern(Optional<StellaPattern> inner)) {
                        current = inner;
                        unsucc = unsucc - 1;
                        succCount++;
                    }

                    int finalSuccCount = succCount;
                    Function<Stream<StellaPattern>, Stream<StellaPattern>> yieldSucced = result -> {
                        for (int i = 0; i < finalSuccCount; i++) {
                            result = result.map(StellaPattern.SuccPattern::new);
                        }
                        return result;
                    };

                    // unsucc == 0 || current.isEmpty() || current.get() != SuccPattern
                    if (current.isEmpty()) {
                        yield unsucc == 0
                                ? yieldSucced.apply(Stream.of(new StellaPattern.SuccPattern()))
                                :
                                yieldSucced.apply(Stream.of(
                                        new StellaPattern.RangePattern(0, unsucc - 1),
                                        new StellaPattern.BeamPattern(unsucc + 1))
                                );
                    }

                    // unsucc == 0 || current.get() != SuccPattern
                    if (unsucc == 0 && current.get() instanceof StellaPattern.SuccPattern(Optional<StellaPattern> inner)) {
                        yield Stream.of(possible);
                    }

                    //current.get() != SuccPattern
                    yield switch (current.get()) {
                        case StellaPattern.RangePattern(int start, int end) -> {
                            if (start < unsucc && unsucc < end) {
                                yield yieldSucced.apply(Stream.of(
                                        new StellaPattern.RangePattern(start, unsucc - 1),
                                        new StellaPattern.RangePattern(unsucc + 1, end)
                                ));
                            }
                            if (unsucc == start) {
                                yield yieldSucced.apply(start == end
                                        ? Stream.empty()
                                        : Stream.of(new StellaPattern.RangePattern(start + 1, end)));
                            }
                            if (unsucc == end) {
                                yield yieldSucced.apply(Stream.of(new StellaPattern.RangePattern(start, end - 1)));
                            }
                            yield Stream.of(possible); //TODO think
                        }
                        case StellaPattern.BeamPattern(int start) -> {
                            if (unsucc > start) {
                                yield yieldSucced.apply(Stream.of(
                                        new StellaPattern.RangePattern(start, unsucc - 1),
                                        new StellaPattern.BeamPattern(unsucc + 1)
                                ));
                            }
                            if (unsucc == start) {
                                yield yieldSucced.apply(Stream.of(
                                        new StellaPattern.BeamPattern(unsucc + 1)
                                ));
                            }
                            yield Stream.of(possible); //TODO think
                        }
                        case StellaPattern.ZeroPattern ignored -> {
                            if (unsucc == 0) yield yieldSucced.apply(Stream.empty());
                            else yield Stream.of(possible);
                        }
                        default -> Stream.of(possible);
                    };
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };

            case StellaType.Sum sum -> switch (pattern) {
                case StellaParser.PatternInlContext inl -> {
                    if (!(possible instanceof StellaPattern.InlPattern(Optional<StellaPattern> inner))) {
                        yield Stream.of(possible); // inr-pattern
                    }

                    Stream<StellaPattern> remaining = actualExhaust(inl.pattern_, sum.inl(), inner);
                    yield remaining.map(newInner -> new StellaPattern.InlPattern(newInner));
                }
                case StellaParser.PatternInrContext inr -> {
                    if (!(possible instanceof StellaPattern.InrPattern(Optional<StellaPattern> inner))) {
                        yield Stream.of(possible); // inl-pattern
                    }

                    Stream<StellaPattern> remaining = actualExhaust(inr.pattern_, sum.inr(), inner);
                    yield remaining.map(newInner -> new StellaPattern.InrPattern(newInner));
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };

            case StellaType.Variant variantType -> switch (pattern) {
                case StellaParser.PatternVariantContext variantCtx -> {
                    if (!(possible instanceof StellaPattern.VariantPattern(String name, Optional<StellaPattern> inner))) {
                        yield Stream.of(possible);
                    }

                    if (!name.equals(variantCtx.label.getText())) {
                        yield Stream.of(possible);
                    }

                    Optional<StellaType> innerType = Optional.ofNullable(variantType.items().get(name))
                            .orElseThrow(() -> new ErrorUnexpectedPatternForType(pattern, type))
                             .type();

                    if (innerType.isEmpty() && variantCtx.pattern_ != null) {
                        throw new ErrorUnexpectedNonNullaryVariantPattern(variantCtx, variantType, name);
                    }
                    if (innerType.isPresent() && variantCtx.pattern_ == null) {
                        throw new ErrorUnexpectedNullaryVariantPattern(variantCtx, variantType, name);
                    }

                    if (variantCtx.pattern_ != null) {
                        Stream<StellaPattern> remaining = actualExhaust(variantCtx.pattern_, innerType.get(), inner);
                        yield remaining.map(newInner -> new StellaPattern.VariantPattern(name, newInner));
                    }
                    yield Stream.empty();
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };

            case StellaType.Tuple tupleType -> switch (pattern) {
                case StellaParser.PatternTupleContext tupleCtx -> {
                    if (!(possible instanceof StellaPattern.TuplePattern(List<StellaPattern> product))) {
                        yield Stream.of(possible);
                    }

                    if (tupleCtx.patterns.size() != tupleType.items().size()) throw new ErrorUnexpectedPatternForType(pattern, type);

                    List<List<StellaPattern>> remaining = IntStream.range(0, tupleType.items().size())
                            .mapToObj(i ->
                                    resolveExhaust(tupleCtx.patterns.get(i), tupleType.items().get(i), product.get(i)))
                            .map(Stream::toList)
                            .toList();

                    if (remaining.stream().allMatch(List::isEmpty)) {
                        yield Stream.empty(); // "съели" весь паттерн
                    }

                    if (IntStream.range(0, remaining.size()).anyMatch(i ->
                            remaining.get(i).size() == 1 && remaining.get(i).getFirst().matches(product.get(i)))) {
                        yield Stream.of(possible); // паттерн не подходит
                    }

                    //частично "съеденный" паттерн
                    List<Stream<StellaPattern>> toBeMultiplied = IntStream.range(0, remaining.size())
                            .filter(i -> !remaining.get(i).isEmpty())
                            .mapToObj(i -> {
                                StellaPattern fromProduct = product.get(i);
                                List<StellaPattern> fromRemaining = remaining.get(i);
                                return Stream.concat(Stream.of(fromProduct), fromRemaining.stream());
                            }).toList();
                    Stream<List<StellaPattern>> newProducts = Utils.productStream(toBeMultiplied);
                    yield newProducts.map(newProduct -> {
                        //устал от функциональщины. бывает)))
                        List<StellaPattern> newTuple = new ArrayList<>();
                        int j = 0;
                        for (int i = 0; i < remaining.size(); i++) {
                            if (remaining.get(i).isEmpty()) {
                                newTuple.add(product.get(i));
                            } else {
                                newTuple.add(newProduct.get(j++));
                            }
                        }
                        return (StellaPattern) (new StellaPattern.TuplePattern(newTuple));
                    }).skip(1);
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };
            case StellaType.Record recordType -> switch (pattern) {
                case StellaParser.PatternRecordContext recordCtx -> {
                    if (!(possible instanceof StellaPattern.RecordPattern(Map<String, StellaPattern.RecordPattern.Item> product))) {
                        yield Stream.of(possible);
                    }

                    Map<String, StellaParser.LabelledPatternContext> patterns = recordCtx.patterns.stream().collect(Collectors.toMap(
                            patt -> patt.label.getText(),
                            Function.identity(),
                            (patt1, patt2) -> {
                                throw new ErrorDuplicateRecordPatternFields(recordCtx, patt1.label.getText());
                            }
                    ));
                    for (String name : recordType.items().keySet()) {
                        // только паттерны со всеми полями
                        if (!patterns.containsKey(name)) throw new ErrorUnexpectedPatternForType(pattern, type);
                    }

                    Map<String, List<StellaPattern>> remaining = patterns.keySet().stream().collect(Collectors.toMap(
                            Function.identity(),
                            name -> {
                                StellaType innerType = Optional.ofNullable(recordType.items().get(name))
                                        .orElseThrow(() -> new ErrorUnexpectedPatternForType(pattern, type)).type();
                                return resolveExhaust(patterns.get(name).pattern_, innerType, product.get(name).pattern()).toList();
                            }
                    ));

                    if (remaining.values().stream().allMatch(List::isEmpty)) {
                        yield Stream.empty(); // "съели" весь паттерн
                    }

                    if (remaining.keySet().stream().anyMatch(name ->
                            remaining.get(name).size() == 1 && remaining.get(name).getFirst().matches(product.get(name).pattern()))) {
                        yield Stream.of(possible); // паттерн не подходит
                    }

                    //частично "съеденный" паттерн
                    List<String> names = recordType.items().keySet().stream().toList();
                    List<Stream<StellaPattern>> toBeMultiplied = names.stream()
                            .filter(name -> !remaining.get(name).isEmpty())
                            .map(name -> {
                                StellaPattern fromProduct = product.get(name).pattern();
                                List<StellaPattern> fromRemaining = remaining.get(name);
                                return Stream.concat(Stream.of(fromProduct), fromRemaining.stream());
                            }).toList();
                    Stream<List<StellaPattern>> newProducts = Utils.productStream(toBeMultiplied);
                    yield newProducts.map(newProduct -> {
                        //устал от функциональщины. бывает)))
                        Map<String, StellaPattern.RecordPattern.Item> newRecord = new HashMap<>();
                        int j = 0;
                        for (String name : names) {
                            if (remaining.get(name).isEmpty()) {
                                newRecord.put(name, product.get(name));
                            } else {
                                newRecord.put(name, new StellaPattern.RecordPattern.Item(name, newProduct.get(j++)));
                            }
                        }
                        return (StellaPattern) (new StellaPattern.RecordPattern(newRecord));
                    }).skip(1);
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };

            case StellaType.StellaList listType -> switch (pattern) {
                case StellaParser.PatternConsContext consCtx ->
                        resolveListExhaust(new MaybeFakeListPattern.Real(consCtx), listType, possible);
                case StellaParser.PatternListContext listCtx -> {
                    if (listCtx.patterns.isEmpty()) {
                        yield exhaust(possible, possible instanceof StellaPattern.EmptyListPattern);
                    }

                    MaybeFakeListPattern fakeCtx = new MaybeFakeListPattern.FakeEnd(listCtx);
                    for (StellaParser.PatternContext patt : listCtx.patterns.reversed()) {
                        fakeCtx = new MaybeFakeListPattern.Fake(patt, fakeCtx, listCtx);
                    }
                    yield resolveListExhaust(fakeCtx, listType, possible);
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };
            case StellaType.Unit unitType -> switch (pattern) {
                case StellaParser.PatternUnitContext unitCtx ->
                        exhaust(possible, possible instanceof StellaPattern.UnitPattern);
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };
            default -> throw new ErrorUnexpectedPatternForType(pattern, type);
        };
    }

    private Stream<StellaPattern> actualExhaust(StellaParser.PatternContext pattern, StellaType type, Optional<StellaPattern> current) {
        return actualExhaust(pattern, type, current.map(List::of).orElseGet(type::allPossiblePatterns));
    }
    private Stream<StellaPattern> actualExhaust(StellaParser.PatternContext pattern, StellaType type, List<StellaPattern> current) {
        return current.stream()
                .flatMap(item -> resolveExhaust(pattern, type, item));
    }


    // костыли для листов
    private Stream<StellaPattern> actualExhaust(MaybeFakeListPattern pattern, StellaType.StellaList type, Optional<StellaPattern> current) {
        return actualExhaust(pattern, type, current.map(List::of).orElseGet(type::allPossiblePatterns));
    }
    private Stream<StellaPattern> actualExhaust(MaybeFakeListPattern pattern, StellaType.StellaList type, List<StellaPattern> current) {
        return current.stream()
                .flatMap(item -> resolveListExhaust(pattern, type, item));
    }

    private Stream<StellaPattern> resolveListExhaust(MaybeFakeListPattern listCtx, StellaType.StellaList listType, StellaPattern possible) {
        if (listCtx instanceof MaybeFakeListPattern.FakeEnd) {
            return exhaust(possible, possible instanceof StellaPattern.EmptyListPattern);
        }

        if (!(possible instanceof StellaPattern.ConsPattern(StellaPattern head, Optional<StellaPattern> tail))) {
            return Stream.of(possible); // list-pattern
        }

        List<StellaPattern> remainingHead = switch (listCtx) {
            case MaybeFakeListPattern.Fake fake -> actualExhaust(fake.head, listType.itemType(), Optional.of(head)).toList();
            case MaybeFakeListPattern.Real real -> actualExhaust(real.consCtx.head, listType.itemType(), Optional.of(head)).toList();
            default -> throw new IllegalStateException();
        };
        List<StellaPattern> remainingTail = switch (listCtx) {
            case MaybeFakeListPattern.Fake fake -> actualExhaust(fake.tail, listType, tail).toList();
            case MaybeFakeListPattern.Real real -> actualExhaust(real.consCtx.tail, listType, tail).toList();
            default -> throw new IllegalStateException();
        };

        if (remainingHead.isEmpty() && remainingTail.isEmpty()) {
            return Stream.empty(); // "съели" весь паттерн
        }

        if (remainingHead.size() == 1 && head.matches(remainingHead.getFirst())) {
            return Stream.of(possible); // паттерн не подходит
        }
        if (tail.isPresent() && remainingTail.size() == 1 && tail.get().matches(remainingTail.getFirst())) {
            return Stream.of(possible); // паттерн не подходит
        }

        //частично "съеденный" паттерн
        List<Stream<StellaPattern>> toBeMultiplied = List.of(
                Stream.concat(Stream.of(head), remainingHead.stream()),
                Stream.concat(tail.stream(), remainingTail.stream())
        );
        Stream<List<StellaPattern>> products = Utils.productStream(toBeMultiplied);

        return products.map(product -> {
            return (StellaPattern) (new StellaPattern.ConsPattern(product.get(0), product.get(1)));
        }).skip(tail.isEmpty() ? 0 : 1);
    }
    private sealed interface MaybeFakeListPattern {
        record Real(StellaParser.PatternConsContext consCtx) implements MaybeFakeListPattern {}
        record Fake(StellaParser.PatternContext head, MaybeFakeListPattern tail, StellaParser.PatternListContext actual) implements MaybeFakeListPattern {}
        record FakeEnd(StellaParser.PatternListContext actual) implements MaybeFakeListPattern {}
    }
}
