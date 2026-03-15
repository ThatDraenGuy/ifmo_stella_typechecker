package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorIncorrectNumberOfArguments extends TypeCheckException {
    private final ArgumentContext appl;
    private final int expected;
    private final int actual;

    public ErrorIncorrectNumberOfArguments(StellaParser.ApplicationContext appl, int expected, int actual) {
        this.appl = new ArgumentContext.Appl(appl);
        this.expected = expected;
        this.actual = actual;
    }

    public ErrorIncorrectNumberOfArguments(StellaParser.FixContext fix, int expected, int actual) {
        this.appl = new ArgumentContext.Fix(fix);
        this.expected = expected;
        this.actual = actual;
    }

    private sealed interface ArgumentContext {
        record Appl(StellaParser.ApplicationContext appl) implements ArgumentContext {}
        record Fix(StellaParser.FixContext fix) implements ArgumentContext {}
    }

    @Override
    protected ParserRuleContext getSource() {
        return switch (appl) {
            case ArgumentContext.Appl appl1 -> appl1.appl;
            case ArgumentContext.Fix fix -> fix.fix;
        };
    }

    @Override
    protected String reportText() {
        return switch (appl) {
            case ArgumentContext.Appl ignored -> "Число аргументов в вызове функции некорректно;\n"
                    + "Ожидаемое число аргументов:  " + expected
                    + "Переданное число аргументов: " + actual;
            case ArgumentContext.Fix ignored -> "Функция в fix-выражении должна принимать ровно 1 аргумент;\n"
                    + "Переданное число аргументов: " + actual;
        };
    }
}
