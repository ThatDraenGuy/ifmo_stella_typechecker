package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.typecheck.StellaFunction;

public class ErrorDuplicateFunctionDeclaration extends TypeCheckException {
    private final StellaFunction firstDecl;
    private final StellaFunction secondDecl;

    public ErrorDuplicateFunctionDeclaration(StellaFunction firstDecl, StellaFunction secondDecl) {
        this.firstDecl = firstDecl;
        this.secondDecl = secondDecl;
    }
}
