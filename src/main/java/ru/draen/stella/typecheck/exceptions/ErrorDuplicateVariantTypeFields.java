package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateVariantTypeFields extends TypeCheckException {
    private final StellaParser.TypeVariantContext variant;
    private final String field;

    public ErrorDuplicateVariantTypeFields(StellaParser.TypeVariantContext variant, String field) {
        this.variant = variant;
        this.field = field;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Тэг \"" + field + "\" объявлен больше одного раза";
    }
}
