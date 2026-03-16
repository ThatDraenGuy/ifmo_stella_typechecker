import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.draen.stella.typecheck.TypeChecker;
import ru.draen.stella.typecheck.exceptions.ErrorType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TypecheckerTest {
    private final TypeChecker typeChecker = new TypeChecker();

    private static Stream<Path> wellTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/well-typed")).toURI();
        Path wellTypedPath = Paths.get(uri);
        return Files.list(wellTypedPath);
    }

    private static Stream<IllTypedFile> illTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/ill-typed")).toURI();
        Path illTypedPath = Paths.get(uri);
        return Arrays.stream(ErrorType.values())
                .filter(errorType -> !errorType.isExtra())
                .flatMap(errorType -> {
                    Path errorTypePath = illTypedPath.resolve(errorType.name().toLowerCase().replace("error_", ""));
                    try {
                        return Files.list(errorTypePath).map(file -> new IllTypedFile(errorType, file));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static Stream<Path> extraWellTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/extra/")).toURI();
        Path extraPath = Paths.get(uri);
        return Files.walk(extraPath, 1).filter(Files::isDirectory).skip(1).flatMap(extraDir -> {
            try {
                return Files.list(extraDir.resolve("well-typed"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Stream<IllTypedFile> extraIllTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/extra/")).toURI();
        Path extraPath = Paths.get(uri);
        return Files.walk(extraPath, 1).filter(Files::isDirectory).skip(1).flatMap(extraDir -> {
            try {
                return Files.walk(extraDir.resolve("ill-typed"), 1).filter(Files::isDirectory).skip(1).flatMap(errorTypePath -> {
                    String error = errorTypePath.getFileName().toString();
                    ErrorType errorType = ErrorType.valueOf("ERROR_" + error.toUpperCase());
                    try {
                        return Files.list(errorTypePath).map(file -> new IllTypedFile(errorType, file));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("wellTyped")
    public void wellTyped(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("extraWellTyped")
    public void extraWellTyped(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("illTyped")
    public void illTyped(IllTypedFile illTyped) throws IOException {
        CharStream src = CharStreams.fromPath(illTyped.file);
        assertThrows(illTyped.errorType.getErrorClass(), () -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("extraIllTyped")
    public void extraIllTyped(IllTypedFile illTyped) throws IOException {
        CharStream src = CharStreams.fromPath(illTyped.file);
        assertThrows(illTyped.errorType.getErrorClass(), () -> typeChecker.checkTypes(src));
    }

    public record IllTypedFile(ErrorType errorType, Path file) {}
}
