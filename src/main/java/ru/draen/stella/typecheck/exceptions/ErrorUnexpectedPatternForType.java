package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

import java.util.List;

public class ErrorUnexpectedPatternForType extends TypeCheckException {
    private final StellaParser.MatchCaseContext matchCase;

    public ErrorUnexpectedPatternForType(StellaParser.MatchCaseContext matchCase) {
        this.matchCase = matchCase;
    }
}
