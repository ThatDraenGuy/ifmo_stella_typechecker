import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.draen.stella.typecheck.TypeChecker;
import ru.draen.stella.typecheck.exceptions.ErrorType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class TypecheckerTestBase {
    private final TypeChecker typeChecker = new TypeChecker();

    protected abstract String stage();
    protected boolean extraDisabled() {
        return false;
    }

    protected Stream<Path> wellTypedSrc() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTestBase.class.getResource("test_suite/" + stage() +"/well-typed")).toURI();
        Path wellTypedPath = Paths.get(uri);
        return Files.list(wellTypedPath);
    }

    protected Stream<IllTypedFile> illTypedSrc() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTestBase.class.getResource("test_suite/" + stage() + "/ill-typed")).toURI();
        Path illTypedPath = Paths.get(uri);
        return Files.walk(illTypedPath, 1).filter(Files::isDirectory).skip(1).flatMap(errorTypePath -> {
            String error = errorTypePath.getFileName().toString();
            ErrorType errorType = ErrorType.valueOf("ERROR_" + error.toUpperCase());
            try {
                return Files.list(errorTypePath).map(file -> new IllTypedFile(errorType, file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected Stream<Path> extraWellTypedSrc() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTestBase.class.getResource("test_suite/" + stage() + "/extra/")).toURI();
        Path extraPath = Paths.get(uri);
        return Files.walk(extraPath, 1).filter(Files::isDirectory).skip(1).flatMap(extraDir -> {
            try {
                return Files.list(extraDir.resolve("well-typed"));
            } catch (NoSuchFileException e) {
                return Stream.empty();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected Stream<IllTypedFile> extraIllTypedSrc() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTestBase.class.getResource("test_suite/" + stage() + "/extra/")).toURI();
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
    @MethodSource("wellTypedSrc")
    public void wellTyped(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("extraWellTypedSrc")
    @DisabledIf("extraDisabled")
    public void extraWellTyped(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("illTypedSrc")
    public void illTyped(IllTypedFile illTyped) throws IOException {
        CharStream src = CharStreams.fromPath(illTyped.file);
        assertThrows(illTyped.errorType.getErrorClass(), () -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("extraIllTypedSrc")
    @DisabledIf("extraDisabled")
    public void extraIllTyped(IllTypedFile illTyped) throws IOException {
        CharStream src = CharStreams.fromPath(illTyped.file);
        assertThrows(illTyped.errorType.getErrorClass(), () -> typeChecker.checkTypes(src));
    }

    public record IllTypedFile(ErrorType errorType, Path file) {}
}
