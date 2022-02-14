package de.jplag;

import static de.jplag.options.JPlagOptions.DEFAULT_COMPARISON_MODE;
import static de.jplag.options.JPlagOptions.DEFAULT_SHOWN_COMPARISONS;
import static de.jplag.options.JPlagOptions.DEFAULT_SIMILARITY_THRESHOLD;
import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import de.jplag.options.LanguageOption;
import de.jplag.strategy.ComparisonMode;

/**
 * Command line arguments for the JPlag CLI. Each argument is defined through an enumeral.
 * @author Timur Saglam
 */
public enum CommandLineArgument {
    ROOT_DIRECTORY("rootDir", NumberOfArgumentValues.ONE_OR_MORE_VALUES, String.class),
    LANGUAGE("-l", String.class, LanguageOption.getDefault().getDisplayName(), LanguageOption.getAllDisplayNames()),
    BASE_CODE("-bc", String.class),
    VERBOSITY("-v", String.class, "quiet", List.of("quiet", "long")), // TODO SH: Replace verbosity when integrating a real logging library
    DEBUG("-d", Boolean.class),
    SUBDIRECTORY("-S", String.class),
    SUFFIXES("-p", String.class),
    EXCLUDE_FILE("-x", String.class),
    MIN_TOKEN_MATCH("-t", Integer.class),
    SIMILARITY_THRESHOLD("-m", Float.class, DEFAULT_SIMILARITY_THRESHOLD),
    SHOWN_COMPARISONS("-n", Integer.class, DEFAULT_SHOWN_COMPARISONS),
    RESULT_FOLDER("-r", String.class, "result"),
    COMPARISON_MODE("-c", String.class, DEFAULT_COMPARISON_MODE.getName(), ComparisonMode.allNames());

    private final String flag;
    private final NumberOfArgumentValues numberOfValues;
    private final String description;
    private final Optional<Object> defaultValue;
    private final Optional<Collection<String>> choices;
    private final Class<?> type;

    private CommandLineArgument(String flag, Class<?> type) {
        this(flag, NumberOfArgumentValues.SINGLE_VALUE, type, Optional.empty(), Optional.empty());
    }

    private CommandLineArgument(String flag, NumberOfArgumentValues numberOfValues, Class<?> type) {
        this(flag, numberOfValues, type, Optional.empty(), Optional.empty());
    }

    private CommandLineArgument(String flag, Class<?> type, Object defaultValue) {
        this(flag, NumberOfArgumentValues.SINGLE_VALUE, type, Optional.of(defaultValue), Optional.empty());
    }

    private CommandLineArgument(String flag, Class<?> type, Object defaultValue, Collection<String> choices) {
        this(flag, NumberOfArgumentValues.SINGLE_VALUE, type, Optional.of(defaultValue), Optional.of(choices));
    }

    private CommandLineArgument(String flag, NumberOfArgumentValues numberOfValues, Class<?> type, Optional<Object> defaultValue,
            Optional<Collection<String>> choices) {
        this.flag = flag;
        this.numberOfValues = numberOfValues;
        this.type = type;
        this.defaultValue = defaultValue;
        this.choices = choices;
        this.description = retrieveDescriptionFromMessages();
    }

    /**
     * @return the flag name of the command line argument.
     */
    public String flag() {
        return flag;
    }

    /**
     * @return the flag name of the command line argument without leading dashes.
     */
    public String flagWithoutDash() {
        return flag.replace("-", "");
    }

    /**
     * Returns the value of this argument for arguments with a single value. Convenience method for
     * {@link Namespace#get(String)} and {@link CommandLineArgument#flagWithoutDash()}.
     * @param <T> is the argument type.
     * @param namespace stores a value for the argument.
     * @return the argument value.
     */
    public <T> T getFrom(Namespace namespace) {
        return namespace.get(flagWithoutDash());
    }

    /**
     * Returns the value of this argument for arguments that allow more than a single value. Convenience method for
     * {@link Namespace#getList(String)} and {@link CommandLineArgument#flagWithoutDash()}.
     * <p>
     * Depending on the action of the option, result types may change.
     * </p>
     * @param <T> is the argument type.
     * @param namespace stores a value for the argument.
     * @return the argument value.
     */
    public <T> List<T> getListFrom(Namespace namespace) {
        return namespace.getList(flagWithoutDash());
    }

    /**
     * Parses the command line argument with a specific parser.
     * @param parser is that parser.
     */
    public void parseWith(ArgumentParser parser) {
        Argument argument = parser.addArgument(flag).help(description);
        choices.ifPresent(it -> argument.choices(it));
        defaultValue.ifPresent(it -> argument.setDefault(it));
        argument.type(type);
        if (type == Boolean.class) {
            argument.action(storeTrue());
        }
        if (numberOfValues == NumberOfArgumentValues.ONE_OR_MORE_VALUES) {
            argument.nargs(numberOfValues.toString());
        }
    }

    /**
     * Dynamically loads the description from the message file. For an option named <code>NEW_OPTION</code> the messages key
     * should be <code>CommandLineArgument.NewOption</code>.
     */
    private String retrieveDescriptionFromMessages() {
        StringBuilder builder = new StringBuilder();
        for (String substring : toString().toLowerCase().split("_")) {
            builder.append(substring.substring(0, 1).toUpperCase());
            builder.append(substring.substring(1));
        }
        return Messages.getString(getClass().getSimpleName() + "." + builder.toString());
    }
}
