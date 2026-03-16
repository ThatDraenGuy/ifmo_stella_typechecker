package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousPatternType extends TypeCheckException {
    private final StellaParser.PatternContext pattern;

    public ErrorAmbiguousPatternType(StellaParser.PatternContext pattern) {
        this.pattern = pattern;
    }


    @Override
    protected ParserRuleContext getSource() {
        return pattern;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить полный тип паттерна";
    }
}
