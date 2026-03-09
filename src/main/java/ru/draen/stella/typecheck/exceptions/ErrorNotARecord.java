package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorNotARecord extends TypeCheckException {
    private final StellaParser.ExprContext expr;

    public ErrorNotARecord(StellaParser.ExprContext expr) {
        this.expr = expr;
    }
}
