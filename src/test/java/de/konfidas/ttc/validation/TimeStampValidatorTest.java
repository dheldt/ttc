package de.konfidas.ttc.validation;

import de.konfidas.ttc.exceptions.BadFormatForTARException;
import de.konfidas.ttc.tars.LogMessageArchive;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Collection;
import java.util.LinkedList;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TimeStampValidatorTest {
    final static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    final File file;
    final int expectedNumberOfErrors;

    @Before
    public void initialize() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Parameterized.Parameters
    public static LinkedList<Object[]> filesToTest() {
        LinkedList<Object[]> parameters = new LinkedList<>();

        parameters.add(new Object[]{new File("testdata/positive/positive_bdr_tse_web.tar"),0});

        return parameters;
    }

    public TimeStampValidatorTest(File file, int expectedNumberOfErrors) {
        this.file = file;
        this.expectedNumberOfErrors = expectedNumberOfErrors;
    }

    @Test
    public void parse() {
        logger.info("");
        logger.info("============================================================================");
        logger.info("testing tar file {}:", file.getName());

        try {
            TimeStampValidator validator = new TimeStampValidator();

            LogMessageArchive tar = new LogMessageArchive(this.file);

            Collection<ValidationException>  errors = validator.validate(tar);

            assert(errors.size() == expectedNumberOfErrors);

        } catch (IOException | BadFormatForTARException e) {
            fail();
        }
    }
}
