package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedPatternForType extends TypeCheckException {
    private final StellaParser.PatternContext pattern;
    private final StellaType type;

    public ErrorUnexpectedPatternForType(StellaParser.PatternContext pattern, StellaType type) {
        this.pattern = pattern;
        this.type = type;
    }

    @Override
    protected ParserRuleContext getSource() {
        return pattern;
    }

    @Override
    protected String reportText() {
        return "Паттерн не соответствует типу разбираемого выражения;\nВыведенный тип выражения: " + type;
    }
}
