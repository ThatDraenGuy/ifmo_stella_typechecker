package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
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
    protected ParserRuleContext getSource() {
        return expr;
    }

    @Override
    protected String reportText() {
        return "Кортеж содержит " + itemCount
                + " компонент(ов), но обращение происходит к компоненту по индексу " + index;
    }
}
