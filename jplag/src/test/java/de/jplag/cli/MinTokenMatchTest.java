package de.jplag.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import de.jplag.CommandLineArgument;
import de.jplag.ExitException;
import de.jplag.JPlag;

public class MinTokenMatchTest extends CommandLineInterfaceTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void testLanguageDefault() {
        // Language defaults not set yet:
        buildOptionsFromCLI(CURRENT_DIRECTORY);
        assertNull(options.getMinTokenMatch());
        assertNull(options.getLanguage());

        // Init JPlag:
        try {
            new JPlag(options);
        } catch (ExitException e) {
            e.printStackTrace();
            fail("JPlag intialization failed!");
        }

        // Now the language is set:
        assertNotNull(options.getLanguage());
        assertEquals(options.getLanguage().minimumTokenMatch(), options.getMinTokenMatch().intValue());
    }

    @Test
    public void testInvalidInput() {
        exit.expectSystemExitWithStatus(1);
        String argument = buildArgument(CommandLineArgument.MIN_TOKEN_MATCH, "Not an integer...");
        buildOptionsFromCLI(argument, CURRENT_DIRECTORY);
    }

    @Test
    public void testUpperBound() {
        exit.expectSystemExitWithStatus(1);
        String argument = buildArgument(CommandLineArgument.MIN_TOKEN_MATCH, "2147483648"); // max value plus one
        buildOptionsFromCLI(argument, CURRENT_DIRECTORY);
    }

    @Test
    public void testLowerBound() {
        String argument = buildArgument(CommandLineArgument.MIN_TOKEN_MATCH, Integer.toString(-1));
        buildOptionsFromCLI(argument, CURRENT_DIRECTORY);
        assertEquals(1, options.getMinTokenMatch().intValue());
    }

    @Test
    public void testValidThreshold() {
        int expectedValue = 50;
        String argument = buildArgument(CommandLineArgument.MIN_TOKEN_MATCH, Integer.toString(expectedValue));
        buildOptionsFromCLI(argument, CURRENT_DIRECTORY);
        assertEquals(expectedValue, options.getMinTokenMatch().intValue());
    }
}
