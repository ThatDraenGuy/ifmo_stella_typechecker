package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorUndefinedVariable extends TypeCheckException {
    private final StellaParser.VarContext var;

    public ErrorUndefinedVariable(StellaParser.VarContext var) {
        this.var = var;
    }

    @Override
    protected ParserRuleContext getSource() {
        return var;
    }

    @Override
    protected String reportText() {
        return "Переменная с именем " + var.name.getText() + " не объявлена";
    }
}
