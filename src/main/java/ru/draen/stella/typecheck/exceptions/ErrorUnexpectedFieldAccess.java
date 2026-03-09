package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorUnexpectedFieldAccess extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final String label;

    public ErrorUnexpectedFieldAccess(StellaParser.ExprContext expr, String label) {
        this.expr = expr;
        this.label = label;
    }
}
