package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedLambda extends TypeCheckException {
    private final StellaParser.AbstractionContext lambda;
    private final StellaType expected;

    public ErrorUnexpectedLambda(StellaParser.AbstractionContext lambda, StellaType expected) {
        this.lambda = lambda;
        this.expected = expected;
    }
}
