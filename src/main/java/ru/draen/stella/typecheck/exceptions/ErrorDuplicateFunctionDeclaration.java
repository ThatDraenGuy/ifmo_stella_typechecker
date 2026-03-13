package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateFunctionDeclaration extends TypeCheckException {
    private final StellaParser.DeclFunContext func;

    public ErrorDuplicateFunctionDeclaration(StellaParser.DeclFunContext func) {
        this.func = func;
    }
}
