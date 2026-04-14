package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedSubtype extends TypeCheckException {
    private final ParserRuleContext expr;
    private final StellaType expected;
    private final StellaType actual;

    public ErrorUnexpectedSubtype(ParserRuleContext expr, StellaType expected, StellaType actual) {
        this.expr = expr;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    protected ParserRuleContext getSource() {
        return expr;
    }

    @Override
    protected String reportText() {
        return "Тип выражения не является подтипом ожидаемого;"
                + "\nОжидаемый тип выражения: " + expected
                + "\nВыведенный тип выражения: " + actual;
    }
}
