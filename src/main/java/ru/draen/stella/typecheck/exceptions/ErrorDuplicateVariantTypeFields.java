package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateVariantTypeFields extends TypeCheckException {
    private final StellaParser.TypeVariantContext variant;
    private final String field;

    public ErrorDuplicateVariantTypeFields(StellaParser.TypeVariantContext variant, String field) {
        this.variant = variant;
        this.field = field;
    }

    @Override
    protected String reportText() {
        return reportSource(variant) + "Тэг \"" + field + "\" объявлен больше одного раза";
    }
}
