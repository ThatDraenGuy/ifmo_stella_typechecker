package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousVariantType extends TypeCheckException {
    private final StellaParser.VariantContext variant;

    public ErrorAmbiguousVariantType(StellaParser.VariantContext variant) {
        this.variant = variant;
    }

    @Override
    protected String reportText() {
        return reportSource(variant) + "Не удалось определить полный тип типа-варианта";
    }
}
