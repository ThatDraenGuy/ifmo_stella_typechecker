package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorUndefinedVariable extends TypeCheckException {
    private final StellaParser.VarContext var;

    public ErrorUndefinedVariable(StellaParser.VarContext var) {
        this.var = var;
    }

    @Override
    protected String reportText() {
        return reportSource(var) + "Переменная с именем " + var.name.getText() + " не объявлена";
    }
}
