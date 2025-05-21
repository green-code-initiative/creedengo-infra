package org.greencodeinitiative.creedengo.infra.checks;

import java.util.regex.Pattern;

public class AvoidRegexPatternNotStaticValid3 {

    private final Pattern pattern;

    public AvoidRegexPatternNotStaticValid3() {
        pattern = Pattern.compile("foo"); // Compliant
    }

    public boolean foo() {
        return pattern.matcher("foo").find();
    }
}
