package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorStrictUnexpectedVariantLabel extends ErrorUnexpectedVariantLabel {
    private final StellaParser.VariantContext variant;
    private final StellaType expected;
    private final String label;


    public ErrorStrictUnexpectedVariantLabel(StellaParser.VariantContext variant, StellaType expected, String label) {
        this.variant = variant;
        this.expected = expected;
        this.label = label;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не содержит тэг с именем \"" + label
                + "\";\nОжидаемый тип выражения: " + expected;
    }
}
