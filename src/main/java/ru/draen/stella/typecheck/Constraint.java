package ru.draen.stella.typecheck;

import org.antlr.v4.runtime.misc.Interval;
import ru.draen.stella.generated.StellaParser;

public record Constraint(StellaType actual, StellaType expected, StellaParser.ExprContext ctx) {
    @Override
    public String toString() {
        int start = ctx.start.getStartIndex();
        int end = ctx.stop.getStopIndex();
        return actual + " = " + expected + " (" + ctx.start.getInputStream().getText(new Interval(start, end)) + ")";
    }
}
