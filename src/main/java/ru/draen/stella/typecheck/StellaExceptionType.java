package ru.draen.stella.typecheck;

public sealed interface StellaExceptionType {
    StellaType type();
    record Type(StellaType type) implements StellaExceptionType {};
    record OpenVariant(StellaType.Variant type) implements StellaExceptionType {};
}
