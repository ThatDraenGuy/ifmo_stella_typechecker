package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedNumberOfParametersInLambda extends TypeCheckException {
    private final StellaParser.AbstractionContext lambda;
    private final StellaType expected;

    public ErrorUnexpectedNumberOfParametersInLambda(StellaParser.AbstractionContext lambda, StellaType expected) {
        this.lambda = lambda;
        this.expected = expected;
    }
}
