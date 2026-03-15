package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTupleLength extends TypeCheckException {
    private final StellaParser.TupleContext tuple;
    private final int expected;
    private final int actual;
    private final StellaType type;

    public ErrorUnexpectedTupleLength(StellaParser.TupleContext tuple, int expected, int actual, StellaType type) {
        this.tuple = tuple;
        this.expected = expected;
        this.actual = actual;
        this.type = type;
    }

    @Override
    protected ParserRuleContext getSource() {
        return tuple;
    }

    @Override
    protected String reportText() {
        return "Длина ожидаемого типа кортежа не совпадает с заданным;"
                + "\nДлина ожидаемого кортежа: " + expected
                + "\nДлина заданного кортежа: " + actual
                + "\nОжидаемый тип выражения: " + type;
    }
}
