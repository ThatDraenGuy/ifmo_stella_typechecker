package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorIncorrectNumberOfArguments extends TypeCheckException {
    private final ArgumentContext appl;

    public ErrorIncorrectNumberOfArguments(StellaParser.ApplicationContext appl) {
        this.appl = new ArgumentContext.Appl(appl);
    }

    public ErrorIncorrectNumberOfArguments(StellaParser.FixContext fix) {
        this.appl = new ArgumentContext.Fix(fix);
    }

    private sealed interface ArgumentContext {
        record Appl(StellaParser.ApplicationContext appl) implements ArgumentContext {}
        record Fix(StellaParser.FixContext fix) implements ArgumentContext {}
    }
}
