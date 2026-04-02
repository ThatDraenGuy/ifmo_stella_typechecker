package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateExceptionType extends TypeCheckException {
    private final StellaParser.DeclExceptionTypeContext exception;

    public ErrorDuplicateExceptionType(StellaParser.DeclExceptionTypeContext exception) {
        this.exception = exception;
    }

    @Override
    protected ParserRuleContext getSource() {
        return exception;
    }

    @Override
    protected String reportText() {
        return "В одной области видимости объявлено более одного типа исключений";
    }
}
