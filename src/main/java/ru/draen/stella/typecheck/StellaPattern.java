package ru.draen.stella.typecheck;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public sealed interface StellaPattern {
    boolean matches(StellaPattern other);

    sealed interface NatPattern extends StellaPattern {}
    record ZeroPattern() implements NatPattern {
        @Override
        public String toString() {
            return "0";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof ZeroPattern
                    || other instanceof RangePattern(int start, int end) && start == 0 && end == 0;
        }
    }
    record SuccPattern(Optional<StellaPattern> inner) implements NatPattern {
        SuccPattern() {
            this(Optional.empty());
        }
        SuccPattern(StellaPattern inner) {
            this(Optional.of(inner));
        }

        @Override
        public String toString() {
            return "succ(" + (inner.isPresent() ? inner.get() : "__something__") + ")";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return (
                    other instanceof SuccPattern(Optional<StellaPattern> inner1)
                            && inner.isEmpty() == inner1.isEmpty()
                            && (inner.isEmpty() || inner.get().matches(inner1.get()))
            ) || (
                    other instanceof BeamPattern(int start)
                            && (
                                    (
                                            start == 0 && inner.isEmpty()
                                    ) || (
                                            start != 0 && inner.isPresent() && inner.get().matches(new BeamPattern(start - 1))
                                    )
                            )
            ) || (
                    other instanceof RangePattern(int start, int end)
                            && start == end
                            && inner.isPresent()
                            && inner.get().matches(new RangePattern(start - 1, end - 1))
            );
        }
    }
    record RangePattern(int start, int end) implements NatPattern {
        @Override
        public String toString() {
            return start + "-" + end;
        }

        @Override
        public boolean matches(StellaPattern other) {
            return (
                    other instanceof RangePattern(int start1, int end1) && start == start1 && end == end1
            ) || (
                    other instanceof SuccPattern succ && succ.matches(this)
            ) || (
                    other instanceof ZeroPattern zero && zero.matches(this)
            );
        }
    }
    record BeamPattern(int start) implements NatPattern {
        @Override
        public String toString() {
            return start + "+";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return (
                    other instanceof BeamPattern(int start1) && start == start1
            ) || (
                    other instanceof SuccPattern succ && succ.matches(this)
            );
        }
    }

    sealed interface BoolPattern extends StellaPattern {}
    record FalsePattern() implements BoolPattern {
        @Override
        public String toString() {
            return "false";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof FalsePattern;
        }
    }
    record TruePattern() implements BoolPattern {
        @Override
        public String toString() {
            return "true";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof TruePattern;
        }
    }

    sealed interface SumPattern extends StellaPattern {}
    record InlPattern(Optional<StellaPattern> inner) implements SumPattern {
        InlPattern() {
            this(Optional.empty());
        }
        InlPattern(StellaPattern inner) {
            this(Optional.of(inner));
        }

        @Override
        public String toString() {
            return "inl(" + (inner.isPresent() ? inner.get() : "__something__") + ")";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof InlPattern(Optional<StellaPattern> inner1)
                    && inner.isEmpty() == inner1.isEmpty()
                    && (inner.isEmpty() || inner.get().matches(inner1.get()));
        }
    }
    record InrPattern(Optional<StellaPattern> inner) implements SumPattern {
        InrPattern() {
            this(Optional.empty());
        }
        InrPattern(StellaPattern inner) {
            this(Optional.of(inner));
        }

        @Override
        public String toString() {
            return "inr(" + (inner.isPresent() ? inner.get() : "__something__") + ")";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof InrPattern(Optional<StellaPattern> inner1)
                    && inner.isEmpty() == inner1.isEmpty()
                    && (inner.isEmpty() || inner.get().matches(inner1.get()));
        }
    }

    record VariantPattern(String name, Optional<StellaPattern> inner) implements StellaPattern {
        VariantPattern(String name) {
            this(name, Optional.empty());
        }
        VariantPattern(String name, StellaPattern inner) {
            this(name, Optional.of(inner));
        }

        @Override
        public String toString() {
            return "<| " + name + " = " + (inner.isPresent() ? inner.get() : "__something__") + " |>";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof VariantPattern(String name1, Optional<StellaPattern> inner1)
                    && name.equals(name1)
                    && inner.isEmpty() == inner1.isEmpty()
                    && (inner.isEmpty() || inner.get().matches(inner1.get()));
        }
    }

    record TuplePattern(List<StellaPattern> product) implements StellaPattern {
        @Override
        public String toString() {
            return "{" + product.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "}";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof TuplePattern(List<StellaPattern> product1)
                    && listMatches(product, product1);
        }
    }
    record RecordPattern(Map<String, Item> product) implements StellaPattern {
        @Override
        public String toString() {
            return "{" + product.values().stream()
                    .map(Item::toString)
                    .collect(Collectors.joining(", ")) + "}";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof RecordPattern(Map<String, Item> product1)
                    && mapMatches(product, product1, Item::pattern);
        }

        record Item(String name, StellaPattern pattern) {
            @Override
            public String toString() {
                return name + " = " + pattern;
            }
        }
    }

    sealed interface StellaListPattern extends StellaPattern {}
    record EmptyListPattern() implements StellaListPattern {
        @Override
        public String toString() {
            return "[]";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof EmptyListPattern
                    || other instanceof ListPattern(List<StellaPattern> items) && items.isEmpty();
        }
    }
    record ConsPattern(Optional<StellaPattern> head, Optional<StellaPattern> tail) implements StellaListPattern {
        ConsPattern() {
            this(Optional.empty(), Optional.empty());
        }
        ConsPattern(StellaPattern head, StellaPattern tail) {
            this(Optional.of(head), Optional.of(tail));
        }

        @Override
        public String toString() {
            return "cons(" + (head.isPresent() ? head.get() : "__something__") + ", " + (tail.isPresent() ? tail.get() : "__something__") + ")";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return (
                    other instanceof ConsPattern(Optional<StellaPattern> head1, Optional<StellaPattern> tail1)
                            && head.isEmpty() == head1.isEmpty()
                            && (head.isEmpty() || head.get().matches(head1.get()))
                            && tail.isEmpty() == tail1.isEmpty()
                            && (tail.isEmpty() || tail.get().matches(tail1.get()))
            ) || (
                    other instanceof ListPattern(List<StellaPattern> items)
                            && !items.isEmpty()
                            && head.isPresent() && head.get().matches(items.getFirst())
                            && tail.isPresent() && tail.get().matches(new ListPattern(items.stream().skip(1).toList()))
            );
        }
    }
    record ListPattern(List<StellaPattern> items) implements StellaListPattern {
        @Override
        public String toString() {
            return "[" + items.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "]";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return (
                    other instanceof ListPattern(List<StellaPattern> items1) && listMatches(items, items1)
            ) || (
                    other instanceof EmptyListPattern emptyList && emptyList.matches(this)
            ) || (
                    other instanceof ConsPattern cons && cons.matches(this)
            );
        }
    }

    record UnitPattern() implements StellaPattern {
        @Override
        public String toString() {
            return "unit";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof UnitPattern;
        }
    }

    record NoPattern() implements StellaPattern {
        @Override
        public String toString() {
            return "ALERT!";
        }

        @Override
        public boolean matches(StellaPattern other) {
            return other instanceof NoPattern;
        }
    }

    static boolean listMatches(List<StellaPattern> fst, List<StellaPattern> snd) {
        return fst.size() == snd.size()
                && IntStream.range(0, fst.size())
                .mapToObj(i -> fst.get(i).matches(snd.get(i)))
                .allMatch(Boolean::booleanValue);
    }

    static <T>boolean mapMatches(Map<String, T> fst, Map<String, T> snd, Function<T, StellaPattern> typeGetter) {
        return fst.keySet().equals(snd.keySet()) && fst.keySet().stream()
                .allMatch(name ->
                        typeGetter.apply(fst.get(name)).matches(typeGetter.apply(snd.get(name))));
    }
}
