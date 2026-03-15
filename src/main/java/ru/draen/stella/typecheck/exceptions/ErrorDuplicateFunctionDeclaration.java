package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateFunctionDeclaration extends TypeCheckException {
    private final StellaParser.DeclFunContext func;

    public ErrorDuplicateFunctionDeclaration(StellaParser.DeclFunContext func) {
        this.func = func;
    }

    @Override
    protected ParserRuleContext getSource() {
        return func;
    }

    @Override
    protected String reportText() {
        return "Функция с данным именем уже была объявлена";
    }
}
