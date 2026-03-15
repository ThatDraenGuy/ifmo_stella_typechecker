package ru.draen.stella.typecheck.exceptions;


public class ErrorIncorrectArityOfMain extends TypeCheckException {
    @Override
    protected String reportText() {
        return "main-функция должна принимать ровно один аргумент";
    }
}
