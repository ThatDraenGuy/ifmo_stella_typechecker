package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorIllegalLocalOpenVariantException extends TypeCheckException {
    private final StellaParser.DeclExceptionVariantContext exception;

    public ErrorIllegalLocalOpenVariantException(StellaParser.DeclExceptionVariantContext exception) {
        this.exception = exception;
    }

    @Override
    protected ParserRuleContext getSource() {
        return exception;
    }

    @Override
    protected String reportText() {
        return "Открытый вариант исключения не может быть задан в локальной области видимости";
    }
}
