package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorTupleIndexOutOfBounds extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final int itemCount;
    private final int index;

    public ErrorTupleIndexOutOfBounds(StellaParser.ExprContext expr, int itemCount, int index) {
        this.expr = expr;
        this.itemCount = itemCount;
        this.index = index;
    }

    @Override
    protected String reportText() {
        return reportSource(expr) + "Кортеж содержит " + itemCount
                + " компонент(ов), но обращение происходит к компоненту по индексу " + index;
    }
}
