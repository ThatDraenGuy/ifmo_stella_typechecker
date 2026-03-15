package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorNotAList extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final StellaType type;

    public ErrorNotAList(StellaParser.ExprContext expr, StellaType type) {
        this.expr = expr;
        this.type = type;
    }

    @Override
    protected String reportText() {
        return reportSource(expr) + "Выражение используется как список, но не является им;\nВыведенный тип выражения: "
                + type;
    }
}
