package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaPattern;

import java.util.List;

public class ErrorNonexhaustiveMatchPatterns extends TypeCheckException {
    private final StellaParser.MatchContext match;
    private final List<StellaPattern> missingPatterns;

    public ErrorNonexhaustiveMatchPatterns(StellaParser.MatchContext match, List<StellaPattern> missingPatterns) {
        this.match = match;
        this.missingPatterns = missingPatterns;
    }
}
