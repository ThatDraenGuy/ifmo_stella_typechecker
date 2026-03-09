package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorTupleIndexOutOfBounds extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final int index;

    public ErrorTupleIndexOutOfBounds(StellaParser.ExprContext expr, int index) {
        this.expr = expr;
        this.index = index;
    }
}
