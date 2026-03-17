package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateFunctionParameter extends TypeCheckException {
    private final Function function;
    private final String parameter;

    public ErrorDuplicateFunctionParameter(StellaParser.DeclFunContext func, String parameter) {
        this.function = new Function.Func(func);
        this.parameter = parameter;
    }

    public ErrorDuplicateFunctionParameter(StellaParser.AbstractionContext lambda, String parameter) {
        this.function = new Function.Lambda(lambda);
        this.parameter = parameter;
    }

    @Override
    protected ParserRuleContext getSource() {
        return switch (function) {
            case Function.Func func -> func.func;
            case Function.Lambda lambda -> lambda.lambda;
        };
    }

    @Override
    protected String reportText() {
        return "Параметр функции с именем \"" + parameter + "\" объявлен больше одного раза";
    }

    private sealed interface Function {
        record Func(StellaParser.DeclFunContext func) implements Function {}
        record Lambda(StellaParser.AbstractionContext lambda) implements Function {}
    }
}
