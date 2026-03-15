package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorNotATuple extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final StellaType type;

    public ErrorNotATuple(StellaParser.ExprContext expr, StellaType type) {
        this.expr = expr;
        this.type = type;
    }

    @Override
    protected ParserRuleContext getSource() {
        return expr;
    }

    @Override
    protected String reportText() {
        return "Выражение используется как кортеж, но не является им;\nВыведенный тип выражения: "
                + type;
    }
}
