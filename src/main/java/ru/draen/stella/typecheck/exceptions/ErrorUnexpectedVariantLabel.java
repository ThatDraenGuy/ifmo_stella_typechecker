package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedVariantLabel extends TypeCheckException {
    private final StellaParser.VariantContext variant;
    private final StellaType expected;
    private final String label;


    public ErrorUnexpectedVariantLabel(StellaParser.VariantContext variant, StellaType expected, String label) {
        this.variant = variant;
        this.expected = expected;
        this.label = label;
    }

    @Override
    protected String reportText() {
        return reportSource(variant) + "Ожидаемый тип выражения не содержит тэг с именем \"" + label
                + "\";\nОжидаемый тип выражения: " + expected;
    }
}
