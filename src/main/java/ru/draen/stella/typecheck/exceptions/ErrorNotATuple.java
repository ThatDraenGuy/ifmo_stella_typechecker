package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorNotATuple extends TypeCheckException {
    private final StellaParser.ExprContext expr;

    public ErrorNotATuple(StellaParser.ExprContext expr) {
        this.expr = expr;
    }
}
