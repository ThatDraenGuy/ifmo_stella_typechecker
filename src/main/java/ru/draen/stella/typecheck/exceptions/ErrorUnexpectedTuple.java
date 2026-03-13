package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTuple extends TypeCheckException {
    private final StellaParser.TupleContext tuple;
    private final StellaType expected;

    public ErrorUnexpectedTuple(StellaParser.TupleContext tuple, StellaType expected) {
        this.tuple = tuple;
        this.expected = expected;
    }
}
