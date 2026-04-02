package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousThrowType extends TypeCheckException {
    private final StellaParser.ThrowContext throwCtx;

    public ErrorAmbiguousThrowType(StellaParser.ThrowContext throwCtx) {
        this.throwCtx = throwCtx;
    }

    @Override
    protected ParserRuleContext getSource() {
        return throwCtx;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить тип для throw-выражения";
    }
}
