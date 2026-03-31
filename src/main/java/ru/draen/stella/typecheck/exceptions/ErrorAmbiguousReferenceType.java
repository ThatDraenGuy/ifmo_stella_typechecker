package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousReferenceType extends TypeCheckException {
    private final StellaParser.ConstMemoryContext mem;

    public ErrorAmbiguousReferenceType(StellaParser.ConstMemoryContext mem) {
        this.mem = mem;
    }

    @Override
    protected ParserRuleContext getSource() {
        return mem;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить тип ссылки для адреса в памяти";
    }
}
