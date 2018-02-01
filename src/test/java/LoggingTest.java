import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTest {
    private static Logger log = LoggerFactory.getLogger(LoggingTest.class.getName()); // 1. Объявляем переменную логгера

    public static void main(String[] args) {

        log.debug("...");

        log.info("Info");

    }
}
