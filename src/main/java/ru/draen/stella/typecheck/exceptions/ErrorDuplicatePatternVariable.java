package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicatePatternVariable extends TypeCheckException {
    private final StellaParser.PatternContext pattern;
    private final String variable;

    public ErrorDuplicatePatternVariable(StellaParser.PatternContext pattern, String variable) {
        this.pattern = pattern;
        this.variable = variable;
    }

    @Override
    protected ParserRuleContext getSource() {
        return pattern;
    }

    @Override
    protected String reportText() {
        return "Переменная \"" + variable + "\" объявлена в паттерне больше одного раза";
    }

    public String getVariable() {
        return variable;
    }
}
