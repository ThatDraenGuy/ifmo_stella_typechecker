package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTypeForExpression extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final StellaType expected;
    private final StellaType actual;

    public ErrorUnexpectedTypeForExpression(StellaParser.ExprContext expr, StellaType expected, StellaType actual) {
        this.expr = expr;
        this.expected = expected;
        this.actual = actual;
    }
}
