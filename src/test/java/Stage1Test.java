import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Stage1Test extends TypecheckerTestBase {
    @Override
    protected String stage() {
        return "stage1";
    }
}
