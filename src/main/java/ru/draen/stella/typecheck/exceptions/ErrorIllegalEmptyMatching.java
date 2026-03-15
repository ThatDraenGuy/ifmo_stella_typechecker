package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorIllegalEmptyMatching extends TypeCheckException {
    private final StellaParser.MatchContext match;

    public ErrorIllegalEmptyMatching(StellaParser.MatchContext match) {
        this.match = match;
    }

    @Override
    protected ParserRuleContext getSource() {
        return match;
    }

    @Override
    protected String reportText() {
        return "Match-выражение с пустым телом запрещено";
    }
}
