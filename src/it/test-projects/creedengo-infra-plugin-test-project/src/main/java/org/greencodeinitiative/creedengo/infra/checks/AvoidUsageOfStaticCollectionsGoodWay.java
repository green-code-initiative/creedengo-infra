package org.greencodeinitiative.creedengo.infra.checks;

import java.util.*;

/**
 * Compliant
 */
public class AvoidUsageOfStaticCollectionsGoodWay {
    public static volatile AvoidUsageOfStaticCollectionsGoodWay INSTANCE = new AvoidUsageOfStaticCollectionsGoodWay();

    public final List<String> LIST = new ArrayList<String>(); // Compliant
    public final Set<String> SET = new HashSet<String>(); // Compliant
    public final Map<String, String> MAP = new HashMap<String, String>(); // Compliant

    private AvoidUsageOfStaticCollectionsGoodWay() {
    }
}
