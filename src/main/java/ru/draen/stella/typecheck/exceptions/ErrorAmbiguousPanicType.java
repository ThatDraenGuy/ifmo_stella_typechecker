package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousPanicType extends TypeCheckException {
    private final StellaParser.PanicContext panic;

    public ErrorAmbiguousPanicType(StellaParser.PanicContext panic) {
        this.panic = panic;
    }

    @Override
    protected ParserRuleContext getSource() {
        return panic;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить тип для выражения-паники";
    }
}
