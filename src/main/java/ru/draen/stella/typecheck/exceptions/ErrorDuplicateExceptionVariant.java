package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateExceptionVariant extends TypeCheckException {
    private final StellaParser.DeclExceptionVariantContext exception;
    private final String variantName;

    public ErrorDuplicateExceptionVariant(StellaParser.DeclExceptionVariantContext exception, String variantName) {
        this.exception = exception;
        this.variantName = variantName;
    }

    @Override
    protected ParserRuleContext getSource() {
        return exception;
    }

    @Override
    protected String reportText() {
        return "В одной области видимости объявлена повторяющаяся метка варианта ошибки;\nКод метки: " + variantName;
    }
}
