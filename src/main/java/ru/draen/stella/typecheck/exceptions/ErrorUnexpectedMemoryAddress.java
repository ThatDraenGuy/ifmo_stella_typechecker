package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedMemoryAddress extends TypeCheckException {
    private final StellaParser.ConstMemoryContext mem;
    private final StellaType expected;

    public ErrorUnexpectedMemoryAddress(StellaParser.ConstMemoryContext mem, StellaType expected) {
        this.mem = mem;
        this.expected = expected;
    }

    @Override
    protected ParserRuleContext getSource() {
        return mem;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не является ссылкой;\nОжидаемый тип выражения: " + expected;
    }
}
