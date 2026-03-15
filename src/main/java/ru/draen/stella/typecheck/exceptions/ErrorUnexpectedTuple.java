package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTuple extends TypeCheckException {
    private final StellaParser.TupleContext tuple;
    private final StellaType expected;

    public ErrorUnexpectedTuple(StellaParser.TupleContext tuple, StellaType expected) {
        this.tuple = tuple;
        this.expected = expected;
    }

    @Override
    protected ParserRuleContext getSource() {
        return tuple;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не является кортежом;\nОжидаемый тип выражения: " + expected;
    }
}
