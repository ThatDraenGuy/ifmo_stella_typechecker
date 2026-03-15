package ru.draen.stella;


import org.antlr.v4.runtime.CharStreams;
import ru.draen.stella.typecheck.TypeChecker;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            new TypeChecker().checkTypes(CharStreams.fromStream(System.in));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}