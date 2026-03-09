package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorNotAFunction extends TypeCheckException {
    private final StellaParser.ExprContext expr;

    public ErrorNotAFunction(StellaParser.ExprContext expr) {
        this.expr = expr;
    }
}
