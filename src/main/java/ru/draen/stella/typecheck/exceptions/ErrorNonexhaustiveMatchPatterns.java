package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaPattern;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ErrorNonexhaustiveMatchPatterns extends TypeCheckException {
    private final StellaParser.MatchContext match;
    private final List<StellaPattern> missingPatterns;

    public ErrorNonexhaustiveMatchPatterns(StellaParser.MatchContext match, List<StellaPattern> missingPatterns) {
        this.match = match;
        this.missingPatterns = missingPatterns;
    }

    @Override
    protected ParserRuleContext getSource() {
        return match;
    }

    @Override
    public String reportText() {
        return "Match-выражение не покрывает все возможные паттерны.\nСписок непокрытых паттернов: \n"
                + missingPatterns.stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }
}
