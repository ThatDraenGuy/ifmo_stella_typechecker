package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

public abstract class TypeCheckException extends RuntimeException {
    public final String report() {
        return getErrorType() + ":\n"
                + (getSource() == null ? "" : reportSource(getSource()))
                + reportText();
    }

    protected final String reportPosition(Token at) {
        return "Строка " + at.getLine() + ", Столбец " + at.getCharPositionInLine();
    }
    protected final String reportSource(ParserRuleContext ctx) {
        int start = ctx.start.getStartIndex();
        int end = ctx.stop.getStopIndex();

        return reportPosition(ctx.start) + " - " + reportPosition(ctx.stop) + "\n"
                + ctx.start.getInputStream().getText(new Interval(start, end))
                + "\n";
    }
    protected abstract String reportText();
    protected abstract ParserRuleContext getSource();

    protected ErrorType getErrorType() {
        return ErrorType.getByException(getClass());
    }

    @Override
    public String getMessage() {
        return report();
    }
}
