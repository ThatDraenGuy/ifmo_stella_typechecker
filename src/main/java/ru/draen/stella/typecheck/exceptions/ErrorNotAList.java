package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorNotAList extends TypeCheckException {
    private final StellaParser.ExprContext expr;

    public ErrorNotAList(StellaParser.ExprContext expr) {
        this.expr = expr;
    }
}
