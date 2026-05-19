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

import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.terraform.api.tree.AttributeTree;
import org.sonar.iac.terraform.api.tree.BlockTree;
import org.sonar.iac.terraform.api.tree.FileTree;

/**
 * 1055 — flag long-lived compute / database / cache resources tagged as
 * non-prod when the file contains no scheduling resource (autoscaling
 * schedule, EventBridge-triggered Lambda, GCP Cloud Scheduler, Azure
 * Automation runbook). See {@code tfavoidalwaysonresources.md}.
 *
 * <p>The check is file-scoped (no cross-file resolution yet): a non-prod
 * resource whose scheduler lives in a sibling file is therefore (knowingly)
 * flagged here. Users can silence false positives with a tag
 * {@code eco-design:always-on=batch}.</p>
 */
@Rule(key = "GCI1055")
public class TfAvoidAlwaysOnResourcesCheck implements IacCheck {

  /** Long-lived resources we want to scale down off-hours. */
  private static final Set<String> ALWAYS_ON_RESOURCES = Set.of(
      "aws_instance", "aws_db_instance", "aws_rds_cluster",
      "aws_elasticache_cluster", "aws_elasticache_replication_group",
      "aws_redshift_cluster", "aws_opensearch_domain",
      "google_compute_instance", "google_sql_database_instance",
      "google_redis_instance",
      "azurerm_linux_virtual_machine", "azurerm_windows_virtual_machine",
      "azurerm_mssql_database", "azurerm_postgresql_flexible_server",
      "azurerm_mysql_flexible_server");

  /** Resources that prove a scale-down schedule is in place. */
  private static final Set<String> SCHEDULER_RESOURCES = Set.of(
      "aws_autoscaling_schedule",
      "aws_lambda_function", "aws_cloudwatch_event_rule",
      "aws_scheduler_schedule",
      "google_cloud_scheduler_job",
      "azurerm_automation_runbook", "azurerm_logic_app_workflow");

  /** Tag values considered non-prod (case-insensitive). */
  private static final Set<String> NONPROD_ENVS = Set.of(
      "dev", "development",
      "test", "testing",
      "staging", "stage",
      "preprod", "pre-prod", "preproduction",
      "qa", "uat", "sandbox");

  private static final Set<String> ENV_TAG_KEYS = Set.of("environment", "env", "tier");

  private static final Set<String> OPT_OUT_TAG_KEYS = Set.of(
      "eco-design:always-on", "always-on", "always_on");

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(FileTree.class, TfAvoidAlwaysOnResourcesCheck::check);
  }
}
