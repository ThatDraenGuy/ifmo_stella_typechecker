package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedRecord extends TypeCheckException {
    private final StellaParser.RecordContext record;
    private final StellaType expected;

    public ErrorUnexpectedRecord(StellaParser.RecordContext record, StellaType expected) {
        this.record = record;
        this.expected = expected;
    }

    @Override
    protected ParserRuleContext getSource() {
        return record;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не является записью;\nОжидаемый тип выражения: " + expected;
    }
}
