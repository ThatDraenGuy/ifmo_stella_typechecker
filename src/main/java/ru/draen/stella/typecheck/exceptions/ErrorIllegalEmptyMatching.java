package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorIllegalEmptyMatching extends TypeCheckException {
    private final StellaParser.MatchContext match;

    public ErrorIllegalEmptyMatching(StellaParser.MatchContext match) {
        this.match = match;
    }
}
