package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorSubtypeUnexpectedNumberOfParametersInLambda extends ErrorUnexpectedNumberOfParametersInLambda {
    private final ParserRuleContext lambda;
    private final int expectedCount;
    private final int actualCount;
    private final StellaType.Func expected;
    private final StellaType.Func actual;

    public ErrorSubtypeUnexpectedNumberOfParametersInLambda(ParserRuleContext lambda, int expectedCount, int actualCount, StellaType.Func expected, StellaType.Func actual) {
        this.lambda = lambda;
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    protected ParserRuleContext getSource() {
        return lambda;
    }

    @Override
    protected String reportText() {
        return "Число аргументов в ожидаемом типе выражения не равно заданному;"
                + "\nОжидаемое число аргументов: " + expectedCount
                + "\nВыведенное число аргументов: " + actualCount
                + "\nОжидаемый тип выражения: " + expected
                + "\nВыведенный тип выражения: " + actual;
    }
}
