package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorIncorrectNumberOfTypeArguments extends TypeCheckException {
    private final StellaParser.TypeApplicationContext appl;
    private final int expected;
    private final int actual;

    public ErrorIncorrectNumberOfTypeArguments(StellaParser.TypeApplicationContext appl, int expected, int actual) {
        this.appl = appl;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    protected ParserRuleContext getSource() {
        return appl;
    }

    @Override
    protected String reportText() {
        return "Число типовых аргументов в вызове функции некорректно;\n"
                + "Ожидаемое число аргументов:  " + expected
                + "Переданное число аргументов: " + actual;
    }
}
