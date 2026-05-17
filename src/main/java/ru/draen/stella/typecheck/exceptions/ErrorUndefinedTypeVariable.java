package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorUndefinedTypeVariable extends TypeCheckException {
    private final StellaParser.TypeVarContext typeVar;

    public ErrorUndefinedTypeVariable(StellaParser.TypeVarContext typeVar) {
        this.typeVar = typeVar;
    }

    @Override
    protected ParserRuleContext getSource() {
        return typeVar;
    }

    @Override
    protected String reportText() {
        return "Переменная типа " + typeVar.name.getText() + " не определена";
    }
}
