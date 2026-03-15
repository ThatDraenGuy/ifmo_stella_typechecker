package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedFieldAccess extends TypeCheckException {
    private final StellaParser.ExprContext expr;
    private final String label;
    private final StellaType type;

    public ErrorUnexpectedFieldAccess(StellaParser.ExprContext expr, String label, StellaType type) {
        this.expr = expr;
        this.label = label;
        this.type = type;
    }

    @Override
    protected ParserRuleContext getSource() {
        return expr;
    }

    @Override
    protected String reportText() {
        return "Поле с именем \"" + label + "\" отсутствует в записи;\nВыведенный тип записи: " + type;
    }
}
