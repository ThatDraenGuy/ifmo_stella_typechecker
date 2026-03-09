package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorIncorrectNumberOfArguments extends TypeCheckException {
    private final StellaParser.ApplicationContext appl;

    public ErrorIncorrectNumberOfArguments(StellaParser.ApplicationContext appl) {
        this.appl = appl;
    }
}
