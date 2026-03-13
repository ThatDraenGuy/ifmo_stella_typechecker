package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedTypeForParameter extends TypeCheckException {
    private final StellaParser.ParamDeclContext param;
    private final StellaType expected;

    public ErrorUnexpectedTypeForParameter(StellaParser.ParamDeclContext param, StellaType expected) {
        this.param = param;
        this.expected = expected;
    }
}
