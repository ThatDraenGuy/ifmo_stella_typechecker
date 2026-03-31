import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Stage2Test extends TypecheckerTestBase {
    @Override
    protected String stage() {
        return "stage2";
    }

    @Override
    protected boolean extraDisabled() {
        return true;
    }
}
