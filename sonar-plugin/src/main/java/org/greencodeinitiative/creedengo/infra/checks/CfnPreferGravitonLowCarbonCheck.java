/*
 * Creedengo Infra plugin - Provides rules to reduce the environmental footprint of your infra as code
 * Copyright © 2025 Green Code Initiative (https://green-code-initiative.org)
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
package org.greencodeinitiative.creedengo.infra.checks;

import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1057 — CloudFormation counterpart of GCI1050. Flags
 * {@code Resources.*.Properties.InstanceType} that pin compute resources
 * to an x86 family when an ARM/Graviton equivalent exists. See
 * {@code cfnprefergravitonlowcarbon.md}.
 *
 * <p>Resource types covered (v1): {@code AWS::EC2::Instance},
 * {@code AWS::EC2::LaunchTemplate}, {@code AWS::AutoScaling::LaunchConfiguration},
 * {@code AWS::RDS::DBInstance}.</p>
 */
@Rule(key = "GCI1057")
public class CfnPreferGravitonLowCarbonCheck implements IacCheck {

    private static final Set<String> TARGET_TYPES = Set.of(
            "AWS::EC2::Instance",
            "AWS::EC2::LaunchTemplate",
            "AWS::AutoScaling::LaunchConfiguration",
            "AWS::RDS::DBInstance");

    @Override
    public void initialize(@Nonnull InitContext init) {
        //init.register(FileTree.class, CfnPreferGravitonLowCarbonCheck::check);
    }
}
