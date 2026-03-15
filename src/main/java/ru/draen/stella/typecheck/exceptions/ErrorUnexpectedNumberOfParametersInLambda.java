package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedNumberOfParametersInLambda extends TypeCheckException {
    private final StellaParser.AbstractionContext lambda;
    private final int expected;
    private final int actual;
    private final StellaType type;

    public ErrorUnexpectedNumberOfParametersInLambda(StellaParser.AbstractionContext lambda, int expected, int actual, StellaType type) {
        this.lambda = lambda;
        this.expected = expected;
        this.actual = actual;
        this.type = type;
    }


    @Override
    protected String reportText() {
        return reportSource(lambda) + "Число аргументов в ожидаемом типе выражения не равно заданному;"
                + "\nОжидаемое число аргументов: " + expected
                + "\nЗаданное число аргументов: " + actual
                + "\nОжидаемый тип выражения: " + type;
    }
}
