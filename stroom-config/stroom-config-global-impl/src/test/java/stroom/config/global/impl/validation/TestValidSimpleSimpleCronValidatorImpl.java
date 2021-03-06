package stroom.config.global.impl.validation;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.shared.validation.ValidSimpleCron;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

class TestValidSimpleSimpleCronValidatorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestValidSimpleSimpleCronValidatorImpl.class);

    @Inject
    private Validator validator;

    @Test
    void test_null() {
        doValidationTest(null, true);
    }

    @Test
    void test_good() {
        doValidationTest("* * *", true);
    }

    @Test
    void test_bad() {
        doValidationTest("xxxxx", false);
    }

    void doValidationTest(final String value, boolean expectedResult) {
        final Injector injector = Guice.createInjector(new ValidationModule());
        injector.injectMembers(this);

        var myPojo = new Pojo();
        myPojo.simpleCron = value;

        final Set<ConstraintViolation<Pojo>> results = validator.validate(myPojo);

        results.forEach(violation -> {
            LOGGER.info(violation.getMessage());
        });

        Assertions.assertThat(results.isEmpty()).isEqualTo(expectedResult);
    }


    private static class Pojo {
        @ValidSimpleCron
        private String simpleCron;
    }

}