package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorIllegalEmptyMatching extends TypeCheckException {
    private final StellaParser.MatchContext match;

    public ErrorIllegalEmptyMatching(StellaParser.MatchContext match) {
        this.match = match;
    }

    @Override
    protected String reportText() {
        return reportSource(match) + "Match-выражение с пустым телом запрещено";
    }
}
