package ru.draen.stella.typecheck.exceptions;


import org.antlr.v4.runtime.ParserRuleContext;

public class ErrorIncorrectArityOfMain extends TypeCheckException {
    @Override
    protected String reportText() {
        return "main-функция должна принимать ровно один аргумент";
    }

    @Override
    protected ParserRuleContext getSource() {
        return null;
    }
}
