package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorOccursCheckInfiniteType extends TypeCheckException {
    private final ParserRuleContext ctx;
    private final StellaType expected;
    private final StellaType actual;

    public ErrorOccursCheckInfiniteType(ParserRuleContext ctx, StellaType expected, StellaType actual) {
        this.ctx = ctx;
        this.expected = expected;
        this.actual = actual;
    }


    @Override
    protected ParserRuleContext getSource() {
        return ctx;
    }

    @Override
    protected String reportText() {
        return "Невозможно построить бесконечный тип при унификации с ожидаемым"
                + "\nОжидаемый тип выражения: " + expected
                + "\nВыведенный тип выражения: " + actual;
    }

}
