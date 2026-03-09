package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorUndefinedVariable extends TypeCheckException {
    private final StellaParser.VarContext var;

    public ErrorUndefinedVariable(StellaParser.VarContext var) {
        this.var = var;
    }
}
