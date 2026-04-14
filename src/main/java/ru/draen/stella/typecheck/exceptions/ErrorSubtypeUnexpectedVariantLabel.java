package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorSubtypeUnexpectedVariantLabel extends ErrorUnexpectedVariantLabel {
    private final ParserRuleContext variant;
    private final StellaType.Variant expected;
    private final StellaType.Variant actual;
    private final String label;


    public ErrorSubtypeUnexpectedVariantLabel(ParserRuleContext variant, StellaType.Variant expected, StellaType.Variant actual, String label) {
        this.variant = variant;
        this.expected = expected;
        this.actual = actual;
        this.label = label;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не содержит тэг с именем \"" + label
                + "\";\nОжидаемый тип выражения: " + expected
                + "\";\nВыведенный тип выражения: " + actual;
    }
}
