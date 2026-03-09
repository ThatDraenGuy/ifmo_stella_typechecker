package ru.draen.stella;


import org.antlr.v4.runtime.CharStreams;
import ru.draen.stella.typecheck.TypeChecker;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        new TypeChecker().checkTypes(CharStreams.fromStream(System.in));
    }
}