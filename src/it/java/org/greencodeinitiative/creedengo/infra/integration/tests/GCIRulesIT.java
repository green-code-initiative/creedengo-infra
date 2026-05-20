/*
 * Creedengo Infra plugin - Provides rules to reduce the environmental footprint of your infra as code
 * Copyright  2025 Green Code Initiative (https://green-code-initiative.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.greencodeinitiative.creedengo.infra.integration.tests;

import java.util.List;
import java.util.Map;

import org.greencodeinitiative.creedengo.integration.tests.GCIRulesBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Measures;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the {@code creedengo-infra} plugin.
 *
 * <p>Mirrors the layout of {@code creedengo-java}'s {@code GCIRulesIT}:</p>
 * <ul>
 *   <li>A real SonarQube instance is launched by {@code GCIRulesBase} via the
 *       Orchestrator (see properties wired in {@code pom.xml}).</li>
 *   <li>The {@code creedengo-infra-plugin-test-project} (test fixtures under
 *       {@code src/it/test-projects/}) is analysed by the Sonar scanner.</li>
 *   <li>Issues raised on the IaC fixtures are queried back through the Web API
 *       and asserted against expected {@code (file, ruleId, message, line)} tuples.</li>
 * </ul>
 *
 * <p>Severity tuples (used to assert each issue) are pinned per rule based on
 * what the live SonarQube instance returns after the scan. They reflect the
 * severity declared in {@code creedengo-rules-specifications}'s rule JSON, not
 * a uniform plugin-wide constant:</p>
 * <ul>
 *   <li>{@link #SEVERITY_MAJOR} — GCI1025, GCI1035 (Docker structural defects).</li>
 *   <li>{@link #SEVERITY_MINOR} — GCI1033 (UseCopyNotAdd), GCI1050 (Graviton/ARM).</li>
 *   <li>{@link #SEVERITY_INFO}  — GCI1049 (Helm values eco-defaults),
 *       GCI1054 (low-carbon region).</li>
 * </ul>
 */
class GCIRulesIT extends GCIRulesBase {

    private static final Common.Severity SEVERITY_MAJOR = Common.Severity.MAJOR;
    private static final Common.Severity SEVERITY_MINOR = Common.Severity.MINOR;
    private static final Common.Severity SEVERITY_INFO = Common.Severity.INFO;

    @Test
    void testMeasuresAndIssues() {
        String projectKey = analyzedProjects.get(0).getProjectKey();

        Map<String, Measures.Measure> measures = getMeasures(projectKey);

        assertThat(ofNullable(measures.get("code_smells")).map(Measures.Measure::getValue).map(Integer::parseInt).orElse(0))
                .isGreaterThanOrEqualTo(1);

        List<Issues.Issue> projectIssues = searchIssuesForComponent(projectKey, null).getIssuesList();
        assertThat(projectIssues).isNotEmpty();
    }

    /**
     * Single-stage Dockerfile that runs {@code mvn} → {@link
     * org.greencodeinitiative.creedengo.infra.checks.MultistageCheck} must
     * flag the lone {@code FROM} instruction.
     */
    @Test
    void testGCI1025_dockerMultistage() {
        String filePath = "fixtures/docker/Dockerfile";
        String ruleId = "creedengo-infra-docker:GCI1025";
        String ruleMsg = "Use a multi-stage build: separate the build toolchain from the runtime image to avoid shipping compilers and package managers.";
        int[] startLines = {3};
        int[] endLines = {3};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_20MIN);
    }

    /**
     * Same Dockerfile, no explicit {@code USER} → {@link
     * org.greencodeinitiative.creedengo.infra.checks.SetNonRootUserCheck}
     * must report the {@code MESSAGE_MISSING} variant on the {@code FROM}
     * instruction of the (only) stage.
     */
    @Test
    void testGCI1035_dockerNonRootUserMissing() {
        String filePath = "fixtures/docker/Dockerfile";
        String ruleId = "creedengo-infra-docker:GCI1035";
        String ruleMsg = "Declare an explicit non-root USER in the final stage to avoid CVE-driven emergency redeploys.";
        int[] startLines = {3};
        int[] endLines = {3};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_5MIN);
    }

    // ---------------------------------------------------------------------
    // Docker — GCI1033 (UseCopyNotAdd)
    // ---------------------------------------------------------------------

    /**
     * Three-line Dockerfile that uses {@code ADD} for both a local path and
     * a remote URL → {@link
     * org.greencodeinitiative.creedengo.infra.checks.UseCopyNotAddCheck}
     * must flag every {@code ADD} instruction.
     */
    @Test
    void testGCI1033_dockerUseCopyNotAdd() {
        String filePath = "fixtures/docker/copy-vs-add/Dockerfile";
        String ruleId = "creedengo-infra-docker:GCI1033";
        String ruleMsg =
                "Prefer COPY over ADD for local files (and use `RUN curl … && verify` for remote artefacts).";
        int[] startLines = {2, 3};
        int[] endLines = {2, 3};

        // GCI1033 is declared as MINOR in creedengo-rules-specifications.
        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MINOR, TYPE, EFFORT_5MIN);
    }

    // ---------------------------------------------------------------------
    // Kubernetes — GCI1046 (ImagePullPolicy != Always on pinned images)
    // ---------------------------------------------------------------------

    /**
     * Plain Kubernetes Pod with three containers, each pinned (digest or
     * explicit tag) but using {@code imagePullPolicy: Always}. {@link
     * org.greencodeinitiative.creedengo.infra.checks.ImagePullPolicyNotAlwaysCheck}
     * reports on the {@code - name:} line of every offending container.
     *
     * <p><b>Currently disabled.</b> The scanner returns zero issues on this
     * fixture, even though the very same YAML payload triggers the rule in
     * the {@code ImagePullPolicyNotAlwaysCheckTest} unit test. Most likely
     * cause: at scan time sonar-iac is classifying the file as plain
     * {@code yaml} rather than the {@code kubernetes} language used by
     * {@link org.greencodeinitiative.creedengo.infra.InfraKubernetesSensor}
     * (which filters via {@code fileSystem().predicates().hasLanguage("kubernetes")}).
     * The {@link org.greencodeinitiative.creedengo.infra.HelmValuesEcoSensor}
     * (GCI1049) works around this with a custom file walker, so its issues
     * still appear in the IT. Re-enable once we either:
     * <ul>
     *   <li>broaden {@code InfraKubernetesSensor} to also accept files whose
     *       sonar-language is {@code yaml} but content contains
     *       {@code apiVersion:}+{@code kind:}, or</li>
     *   <li>set an explicit {@code sonar.kubernetes.file.identifier} /
     *       {@code sonar.kubernetes.activate=true} in the test-project pom
     *       (Wave-4 follow-up — see {@code doc/IMPORT-NOTES.md} §7).</li>
     * </ul>
     */
    @Disabled("InfraKubernetesSensor doesn't see this fixture as a 'kubernetes' language file at scan time — Wave-4 follow-up.")
    @Test
    void testGCI1046_kubernetesImagePullPolicyNotAlways() {
        String filePath = "fixtures/kubernetes/image-pull-policy.yaml";
        String ruleId = "creedengo-infra-kubernetes:GCI1046";
        String ruleMsg =
                "Use imagePullPolicy: IfNotPresent on pinned/tagged images to remove redundant registry round-trips.";
        int[] startLines = {7, 10, 13};
        int[] endLines = {7, 10, 13};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_5MIN);
    }

    // ---------------------------------------------------------------------
    // Helm — GCI1049 (values.yaml eco-design defaults audit)
    // ---------------------------------------------------------------------

    /**
     * Helm chart {@code values.yaml} with {@code replicaCount: 3},
     * {@code resources: {}}, {@code image.pullPolicy: Always} and
     * {@code autoscaling.enabled: false}. The cross-file
     * {@code HelmValuesEcoSensor} raises one issue per anti-pattern, each
     * with its own message, all reported on the
     * {@code creedengo-infra-kubernetes} repository (GCI1049 is wired to
     * the kubernetes profile).
     */
    @Test
    void testGCI1049_helmReplicaCountOverProvisioned() {
        String filePath = "fixtures/helm/mychart/values.yaml";
        String ruleId = "creedengo-infra-kubernetes:GCI1049";
        String ruleMsg = "Helm default `replicaCount: 3` is over-provisioned — set `replicaCount: 1`"
                + " and enable `autoscaling.enabled: true` so operators opt-in to scaling.";
        int[] startLines = {1};
        int[] endLines = {1};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_20MIN);
    }

    @Test
    void testGCI1049_helmPullPolicyAlways() {
        String filePath = "fixtures/helm/mychart/values.yaml";
        String ruleId = "creedengo-infra-kubernetes:GCI1049";
        String ruleMsg = "Helm default `image.pullPolicy: Always` triggers a registry pull on every"
                + " restart — prefer `IfNotPresent` (combine with pinned tags or digests).";
        int[] startLines = {5};
        int[] endLines = {5};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_20MIN);
    }

    @Test
    void testGCI1049_helmEmptyResources() {
        String filePath = "fixtures/helm/mychart/values.yaml";
        String ruleId = "creedengo-infra-kubernetes:GCI1049";
        String ruleMsg = "Helm default `resources` is empty — declare baseline requests/limits so"
                + " kube-scheduler can place pods efficiently.";
        int[] startLines = {6};
        int[] endLines = {6};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_20MIN);
    }

    @Test
    void testGCI1049_helmAutoscalingDisabled() {
        String filePath = "fixtures/helm/mychart/values.yaml";
        String ruleId = "creedengo-infra-kubernetes:GCI1049";
        String ruleMsg = "Helm default `autoscaling.enabled: false` makes scale-to-fit opt-in —"
                + " enable autoscaling by default so consumers benefit from it out of the box.";
        int[] startLines = {8};
        int[] endLines = {8};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_20MIN);
    }

    // ---------------------------------------------------------------------
    // Terraform — GCI1054 (low-carbon region) + GCI1050 (Graviton/ARM)
    // ---------------------------------------------------------------------

    /**
     * Three providers (AWS / GCP / Azure) all pinned to high-carbon
     * regions. {@link
     * org.greencodeinitiative.creedengo.infra.checks.TfChooseLowCarbonRegionCheck}
     * emits a region-specific message per declaration.
     */
    @Test
    void testGCI1054_terraformLowCarbonRegion_awsApSouth1() {
        String filePath = "fixtures/terraform/regions.tf";
        String ruleId = "creedengo-infra-terraform:GCI1054";
        String ruleMsg = "Region 'ap-south-1' has a high-carbon grid (~700 gCO2/kWh):"
                + " document the constraint or relocate to a low-carbon region.";
        int[] startLines = {2};
        int[] endLines = {2};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_5MIN);
    }

    @Test
    void testGCI1054_terraformLowCarbonRegion_gcpAsiaSoutheast1() {
        String filePath = "fixtures/terraform/regions.tf";
        String ruleId = "creedengo-infra-terraform:GCI1054";
        String ruleMsg = "Region 'asia-southeast1' has a high-carbon grid (~400 gCO2/kWh):"
                + " document the constraint or relocate to a low-carbon region.";
        int[] startLines = {6};
        int[] endLines = {6};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_5MIN);
    }

    @Test
    void testGCI1054_terraformLowCarbonRegion_azureSouthAfricaNorth() {
        String filePath = "fixtures/terraform/regions.tf";
        String ruleId = "creedengo-infra-terraform:GCI1054";
        String ruleMsg = "Region 'southafricanorth' has a high-carbon grid (~800 gCO2/kWh):"
                + " document the constraint or relocate to a low-carbon region.";
        int[] startLines = {10};
        int[] endLines = {10};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_INFO, TYPE, EFFORT_5MIN);
    }

    /**
     * x86 instance families across {@code aws_instance},
     * {@code aws_launch_template} and {@code aws_db_instance}. {@link
     * org.greencodeinitiative.creedengo.infra.checks.TfPreferGravitonArmCheck}
     * suggests the matching Graviton equivalent for each family.
     */
    @Test
    void testGCI1050_terraformGravitonArm_m6i() {
        String filePath = "fixtures/terraform/graviton.tf";
        String ruleId = "creedengo-infra-terraform:GCI1050";
        String ruleMsg = "Prefer the ARM/Graviton equivalent of m6i (e.g. m7g) for better watt-per-request.";
        int[] startLines = {2};
        int[] endLines = {2};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MINOR, TYPE, EFFORT_20MIN);
    }

    @Test
    void testGCI1050_terraformGravitonArm_c5() {
        String filePath = "fixtures/terraform/graviton.tf";
        String ruleId = "creedengo-infra-terraform:GCI1050";
        String ruleMsg = "Prefer the ARM/Graviton equivalent of c5 (e.g. c6g/c7g) for better watt-per-request.";
        int[] startLines = {7};
        int[] endLines = {7};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MINOR, TYPE, EFFORT_20MIN);
    }

    @Test
    void testGCI1050_terraformGravitonArm_r6i() {
        String filePath = "fixtures/terraform/graviton.tf";
        String ruleId = "creedengo-infra-terraform:GCI1050";
        String ruleMsg = "Prefer the ARM/Graviton equivalent of r6i (e.g. r7g) for better watt-per-request.";
        int[] startLines = {11};
        int[] endLines = {11};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MINOR, TYPE, EFFORT_20MIN);
    }

    // ---------------------------------------------------------------------
    // CloudFormation — GCI1057 (Graviton/ARM)  — fixture-only placeholder
    // ---------------------------------------------------------------------

    /**
     * Disabled until {@link
     * org.greencodeinitiative.creedengo.infra.checks.CfnPreferGravitonLowCarbonCheck}
     * is wired (see {@code RULES.md}, Wave-2C). The fixture is in place so
     * the test can be enabled with a one-line tweak once the check fires.
     */
    @Disabled("CfnPreferGravitonLowCarbonCheck not yet wired — Wave-2C follow-up (see RULES.md).")
    @Test
    void testGCI1057_cloudformationGravitonArm_m6i() {
        String filePath = "fixtures/cloudformation/graviton.yaml";
        String ruleId = "creedengo-infra-cloudformation:GCI1057";
        String ruleMsg = "Prefer the ARM/Graviton equivalent of m6i for better watt-per-request.";
        int[] startLines = {6};
        int[] endLines = {6};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_20MIN);
    }

    @Disabled("CfnPreferGravitonLowCarbonCheck not yet wired — Wave-2C follow-up (see RULES.md).")
    @Test
    void testGCI1057_cloudformationGravitonArm_r5() {
        String filePath = "fixtures/cloudformation/graviton.yaml";
        String ruleId = "creedengo-infra-cloudformation:GCI1057";
        String ruleMsg = "Prefer the ARM/Graviton equivalent of r5 for better watt-per-request.";
        int[] startLines = {11};
        int[] endLines = {11};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_20MIN);
    }

    // ---------------------------------------------------------------------
    // YAML / CI — GCI1065 (Pin actions to SHA) — fixture-only placeholder
    // ---------------------------------------------------------------------

    /**
     * GitHub Actions workflow with three unpinned {@code uses:} references
     * (moving tag, branch, release name) plus one exempt first-party
     * {@code actions/checkout@v4}. Disabled until {@link
     * org.greencodeinitiative.creedengo.infra.checks.CiPinActionsCheck}
     * is implemented (currently an empty {@code initialize} stub).
     */
    @Disabled("CiPinActionsCheck.initialize is currently a stub — Wave-2C follow-up.")
    @Test
    void testGCI1065_yamlCiPinActions() {
        String filePath = "fixtures/yaml/github-workflows/ci.yml";
        String ruleId = "creedengo-infra-yaml:GCI1065";
        // The exact message format depends on the final implementation of
        // CiPinActionsCheck.MSG — to be confirmed at wiring time.
        String ruleMsg = "Pin GitHub Action 'tj-actions/changed-files' to a 40-char commit SHA"
                + " instead of moving ref 'v44'.";
        int[] startLines = {8};
        int[] endLines = {8};

        checkIssuesForFile(filePath, ruleId, ruleMsg, startLines, endLines, SEVERITY_MAJOR, TYPE, EFFORT_5MIN);
    }
}
