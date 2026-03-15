package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTypeForParameter extends TypeCheckException {
    private final StellaParser.ParamDeclContext param;
    private final StellaType expected;
    private final StellaType actual;

    public ErrorUnexpectedTypeForParameter(StellaParser.ParamDeclContext param, StellaType expected, StellaType actual) {
        this.param = param;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    protected ParserRuleContext getSource() {
        return param;
    }

    @Override
    protected String reportText() {
        return "Тип параметра не совпадает с ожидаемым;"
                + "\nОжидаемый тип параметра: " + expected
                + "\nВыведенный тип параметра: " + actual;
    }
}
