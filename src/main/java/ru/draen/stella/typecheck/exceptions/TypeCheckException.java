package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class TypeCheckException extends RuntimeException {
    public String report() {
        return ErrorType.getByException(getClass()) + ":\n" + reportText();
    }

    protected final String reportPosition(Token at) {
        return "at Ln " + at.getLine() + ", Col " + at.getCharPositionInLine();
    }
    protected final String reportSource(ParserRuleContext ctx) {
        return reportPosition(ctx.start) + "\n" + ctx.getText() + "\n";
    }
    protected String reportText() {
        return "Текст ошибки не указан";
    }

    @Override
    public String getMessage() {
        return report();
    }
}
