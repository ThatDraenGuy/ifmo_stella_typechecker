package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedReference extends TypeCheckException {
    private final StellaParser.RefContext ref;
    private final StellaType expected;

    public ErrorUnexpectedReference(StellaParser.RefContext ref, StellaType expected) {
        this.ref = ref;
        this.expected = expected;
    }

    @Override
    protected ParserRuleContext getSource() {
        return ref;
    }

    @Override
    protected String reportText() {
        return "Ожидаемый тип выражения не является ссылкой;\nОжидаемый тип выражения: " + expected;
    }
}
