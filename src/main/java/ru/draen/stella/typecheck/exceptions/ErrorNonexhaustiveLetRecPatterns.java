package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaPattern;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ErrorNonexhaustiveLetRecPatterns extends TypeCheckException {
    private final StellaParser.LetRecContext let;
    private final List<StellaPattern> missingPatterns;

    public ErrorNonexhaustiveLetRecPatterns(StellaParser.LetRecContext let, List<StellaPattern> missingPatterns) {
        this.let = let;
        this.missingPatterns = missingPatterns;
    }

    @Override
    protected ParserRuleContext getSource() {
        return let;
    }

    @Override
    public String reportText() {
        return "LetRec-паттерн не покрывает все возможные паттерны. Число непокрытых паттернов: "
                + missingPatterns.size() + "\nСписок непокрытых паттернов: \n"
                + missingPatterns.stream().map(Objects::toString).collect(Collectors.joining("\n"));
    }
}
