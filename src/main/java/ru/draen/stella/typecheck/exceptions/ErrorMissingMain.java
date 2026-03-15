package ru.draen.stella.typecheck.exceptions;

public class ErrorMissingMain extends TypeCheckException {
    @Override
    protected String reportText() {
        return "В программе отсутствует main-функция";
    }
}
