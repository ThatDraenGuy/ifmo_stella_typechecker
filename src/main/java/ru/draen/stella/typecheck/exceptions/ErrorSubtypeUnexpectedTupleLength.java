package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorSubtypeUnexpectedTupleLength extends ErrorUnexpectedTupleLength {
    private final ParserRuleContext tuple;
    private final int expectedCount;
    private final int actualCount;
    private final StellaType.Tuple expected;
    private final StellaType.Tuple actual;

    public ErrorSubtypeUnexpectedTupleLength(ParserRuleContext tuple, int expectedCount, int actualCount, StellaType.Tuple expected, StellaType.Tuple actual) {
        this.tuple = tuple;
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    protected ParserRuleContext getSource() {
        return tuple;
    }

    @Override
    protected String reportText() {
        return "Длина ожидаемого типа кортежа не совпадает с выведенным;"
                + "\nДлина ожидаемого кортежа: " + expectedCount
                + "\nДлина заданного кортежа: " + actualCount
                + "\nОжидаемый тип выражения: " + expected
                + "\nВыведенный тип выражения: " + actual;
    }
}
