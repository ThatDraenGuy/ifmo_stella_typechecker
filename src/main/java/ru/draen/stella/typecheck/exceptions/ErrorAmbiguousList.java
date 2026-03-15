package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousList extends TypeCheckException {
    private final StellaParser.ListContext list;


    public ErrorAmbiguousList(StellaParser.ListContext list) {
        this.list = list;
    }

    @Override
    protected String reportText() {
        return reportSource(list) + "Не удалось определить тип списка";
    }
}
