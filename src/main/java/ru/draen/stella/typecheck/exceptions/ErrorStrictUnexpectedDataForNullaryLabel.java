package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorStrictUnexpectedDataForNullaryLabel extends ErrorUnexpectedDataForNullaryLabel {
    private final StellaParser.VariantContext variant;
    private final StellaType expected;
    private final String label;


    public ErrorStrictUnexpectedDataForNullaryLabel(StellaParser.VariantContext variant, StellaType expected, String label) {
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
        return "Ожидается, что тэг с именем \"" + label + "\" не содержит данных"
                + ";\nОжидаемый тип выражения-варианта: " + expected;
    }
}
