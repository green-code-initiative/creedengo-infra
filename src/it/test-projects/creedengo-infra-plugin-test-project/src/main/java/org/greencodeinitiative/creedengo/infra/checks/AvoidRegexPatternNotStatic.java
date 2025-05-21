package org.greencodeinitiative.creedengo.infra.checks;

import java.util.regex.Pattern;

public class AvoidRegexPatternNotStatic {

    public boolean foo() {
        final Pattern pattern = Pattern.compile("foo"); // Noncompliant {{Avoid using Pattern.compile() in a non-static context.}}
        return pattern.matcher("foo").find();
    }
}
