package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;

public class ErrorMissingMain extends TypeCheckException {
    @Override
    protected String reportText() {
        return "В программе отсутствует main-функция";
    }

    @Override
    protected ParserRuleContext getSource() {
        return null;
    }
}
