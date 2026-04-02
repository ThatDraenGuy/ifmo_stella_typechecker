package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorIllegalLocalExceptionType extends TypeCheckException {
    private final StellaParser.DeclExceptionTypeContext exception;

    public ErrorIllegalLocalExceptionType(StellaParser.DeclExceptionTypeContext exception) {
        this.exception = exception;
    }

    @Override
    protected ParserRuleContext getSource() {
        return exception;
    }

    @Override
    protected String reportText() {
        return "Тип исключения не может быть задан в локальной области видимости";
    }
}
