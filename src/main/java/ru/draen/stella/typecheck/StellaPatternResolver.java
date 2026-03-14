package ru.draen.stella.typecheck;

import ru.draen.stella.Utils;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateRecordPatternFields;
import ru.draen.stella.typecheck.exceptions.ErrorUnexpectedPatternForType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StellaPatternResolver {
    private final Map<String, StellaType> vars = new HashMap<>();
    private final StellaParser.PatternContext pattern;
    private final StellaType type;

    public record Result(Map<String, StellaType> vars, List<StellaPattern> notExhausted) {}

    public StellaPatternResolver(StellaParser.PatternContext pattern, StellaType type) {
        this.pattern = pattern;
        this.type = type;
    }

    public Result resolve(List<StellaPattern> current) {
        if (current.isEmpty()) {
            current = List.of(new StellaPattern.NoPattern());
        }
        List<StellaPattern> notExhausted = actualExhaust(pattern, type, current).toList();
        return new Result(vars, notExhausted);
    }

    private<T> Stream<T> exhaust(T value, boolean condition) {
        return condition ? Stream.empty() : Stream.of(value);
    }

    private Stream<StellaPattern> resolveExhaust(StellaParser.PatternContext pattern, StellaType type, StellaPattern possible) {
        if (pattern instanceof StellaParser.PatternVarContext var) {
            vars.put(var.name.getText(), type);
            return Stream.of();
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
                case StellaParser.PatternSuccContext succ -> {
                    if (!(possible instanceof StellaPattern.SuccPattern(Optional<StellaPattern> inner))) {
                        yield Stream.of(possible); // zero-pattern
                    }

                    Stream<StellaPattern> remaining = actualExhaust(succ.pattern_, nat, inner);
                    yield remaining.map(newInner -> new StellaPattern.SuccPattern(newInner));
                }
                case StellaParser.PatternIntContext intCtx -> {
                    int n = Integer.parseInt(intCtx.n.getText());
                    if (n == 0) {
                        yield exhaust(possible, possible instanceof StellaPattern.ZeroPattern);
                    }

                    //TODO non zero pattern
                    yield Stream.of(possible);
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

                    StellaType innerType = Optional.ofNullable(variantType.items().get(name))
                            .orElseThrow(() -> new ErrorUnexpectedPatternForType(pattern, type))
                             .type();
                    Stream<StellaPattern> remaining = actualExhaust(variantCtx.pattern_, innerType, inner);
                    yield remaining.map(newInner -> new StellaPattern.VariantPattern(name, newInner));
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

            case StellaType.StellaList stellaList -> switch (pattern) {
                case StellaParser.PatternConsContext consCtx -> {
                    if (!(possible instanceof StellaPattern.ConsPattern(Optional<StellaPattern> head, Optional<StellaPattern> tail))) {
                        yield Stream.of(possible); // list-pattern
                    }

                    List<StellaPattern> remainingHead = actualExhaust(consCtx.head, stellaList.itemType(), head).toList();
                    List<StellaPattern> remainingTail = actualExhaust(consCtx.tail, stellaList, tail).toList();

                    if (remainingHead.isEmpty() && remainingTail.isEmpty()) {
                        yield Stream.empty(); // "съели" весь паттерн
                    }

                    if (head.isPresent() && remainingHead.size() == 1 && head.get().matches(remainingHead.getFirst())) {
                        yield Stream.of(possible); // паттерн не подходит
                    }
                    if (tail.isPresent() && remainingTail.size() == 1 && tail.get().matches(remainingTail.getFirst())) {
                        yield Stream.of(possible); // паттерн не подходит
                    }

                    //частично "съеденный" паттерн

                    List<Stream<StellaPattern>> toBeMultiplied = List.of(
                            Stream.concat(head.stream(), remainingHead.stream()),
                            Stream.concat(tail.stream(), remainingTail.stream())
                    );
                    Stream<List<StellaPattern>> products = Utils.productStream(toBeMultiplied);
                    yield products.map(product -> {
                        return (StellaPattern) (new StellaPattern.ConsPattern(product.get(0), product.get(1)));
                    }).skip(1);
                }

                case StellaParser.PatternListContext listCtx -> {
                    if (listCtx.patterns.isEmpty()) {
                        yield exhaust(possible, possible instanceof StellaPattern.ListPattern);
                    }

                    //TODO non empty list pattern
                    yield Stream.of(possible);
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };
            case StellaType.Unit unitType -> switch (pattern) {
                case StellaParser.PatternUnitContext unitCtx -> {
                    yield exhaust(possible, possible instanceof StellaPattern.UnitPattern);
                }
                default -> throw new ErrorUnexpectedPatternForType(pattern, type);
            };
            case StellaType.Func func -> {
                throw new ErrorUnexpectedPatternForType(pattern, type);
            }
        };
    }

    private Stream<StellaPattern> actualExhaust(StellaParser.PatternContext pattern, StellaType type, Optional<StellaPattern> current) {
        return actualExhaust(pattern, type, current.map(List::of).orElseGet(type::allPossiblePatterns));
    }
    private Stream<StellaPattern> actualExhaust(StellaParser.PatternContext pattern, StellaType type, List<StellaPattern> current) {
        return current.stream()
                .flatMap(item -> resolveExhaust(pattern, type, item));
    }
}
