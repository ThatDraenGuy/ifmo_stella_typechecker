package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedNonNullaryVariantPattern extends TypeCheckException {
    private final StellaParser.PatternVariantContext variant;
    private final StellaType type;
    private final String label;


    public ErrorUnexpectedNonNullaryVariantPattern(StellaParser.PatternVariantContext variant, StellaType type, String label) {
        this.variant = variant;
        this.type = type;
        this.label = label;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Тэг с именем \"" + label + "\" в паттерне содержит данные, но в типе разбираемого выражения этот тэг не содержит данных"
                + ";\nВыведенный тип выражения: " + type;
    }
}
