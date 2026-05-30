package org.koikifw.libkoiki.batch.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Logback pattern converter that masks substrings of a rendered log message
 * matching one or more regular expressions, replacing each match with a fixed
 * mask token (default {@value #DEFAULT_MASK}).
 *
 * <p>This is the text-level masking hook for application logs, complementing the
 * value-level {@link Masker} used for audit attributes. It is Logback-specific
 * (Logback is an optional dependency of the framework) and self-contained: it is
 * created by Logback, not by Spring, so it takes its patterns from the converter
 * option list rather than from injected configuration.</p>
 *
 * <p>Register it in {@code logback-spring.xml} and supply the regex patterns as
 * options (the framework ships the mechanism; the patterns are application
 * supplied, since standard personal-data rules are deferred):</p>
 *
 * <pre>{@code
 * <conversionRule conversionWord="mask"
 *     converterClass="org.koikifw.libkoiki.batch.security.MaskingPatternConverter"/>
 * <pattern>%d %-5level %logger - %mask(%msg){\d{12,19}}%n</pattern>
 * }</pre>
 *
 * <p>With no options the converter is the identity (it returns the inner output
 * unchanged), which is the safe default. Invalid regular expressions are
 * reported via Logback's status system and skipped rather than failing logging.</p>
 */
public class MaskingPatternConverter extends CompositeConverter<ILoggingEvent> {

    /** Mask token used to replace each regex match. */
    public static final String DEFAULT_MASK = "***";

    private final List<Pattern> patterns = new ArrayList<>();

    @Override
    public void start() {
        for (String option : getOptionList() == null ? List.<String>of() : getOptionList()) {
            if (option == null || option.isBlank()) {
                continue;
            }
            try {
                patterns.add(Pattern.compile(option));
            } catch (PatternSyntaxException ex) {
                addError("Invalid masking pattern '" + option + "', it will be ignored", ex);
            }
        }
        super.start();
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || patterns.isEmpty()) {
            return in;
        }
        String result = in;
        String replacement = Matcher.quoteReplacement(DEFAULT_MASK);
        for (Pattern pattern : patterns) {
            result = pattern.matcher(result).replaceAll(replacement);
        }
        return result;
    }
}
