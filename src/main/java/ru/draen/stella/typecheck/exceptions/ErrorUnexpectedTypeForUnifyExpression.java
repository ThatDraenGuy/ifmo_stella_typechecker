package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTypeForUnifyExpression extends ErrorUnexpectedTypeForExpression {
    private final StellaParser.ExprContext expr;
    private final StellaType expected;
    private final StellaType actual;

    public ErrorUnexpectedTypeForUnifyExpression(StellaParser.ExprContext expr, StellaType expected, StellaType actual) {
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
        return "Тип выражения не может быть унифицирован с ожидаемым;"
                + "\nОжидаемый тип выражения: " + expected
                + "\nВыведенный тип выражения: " + actual;
    }
}
