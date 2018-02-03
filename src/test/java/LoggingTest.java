import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTest {
    private static Logger log = LoggerFactory.getLogger(LoggingTest.class.getName());


    public static void main(String[] args) {

        log.debug("...");

        log.info("Info");

    }
}
