package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

import java.util.List;

public class ErrorNonexhaustiveMatchPatterns extends TypeCheckException {
    private final StellaParser.MatchContext match;
    private final List<String> missingPatterns;

    public ErrorNonexhaustiveMatchPatterns(StellaParser.MatchContext match, List<String> missingPatterns) {
        this.match = match;
        this.missingPatterns = missingPatterns;
    }
}
