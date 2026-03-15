package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedVariant extends TypeCheckException {
    private final StellaParser.VariantContext variant;
    private final StellaType expected;


    public ErrorUnexpectedVariant(StellaParser.VariantContext variant, StellaType expected) {
        this.variant = variant;
        this.expected = expected;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не является вариантом;\nОжидаемый тип выражения: " + expected;
    }
}
