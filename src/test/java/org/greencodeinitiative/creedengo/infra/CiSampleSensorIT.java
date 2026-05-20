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
package org.greencodeinitiative.creedengo.infra;

/**
 * @deprecated Renamed to {@link CiSampleSensorTest} so that the default
 *     surefire pattern (`*Test.java`) picks it up — the failsafe plugin is
 *     commented out in the {@code creedengo-infra} pom. This stub is kept
 *     to avoid leaving a dangling file in version control; it intentionally
 *     declares no {@code @Test} method and is therefore a no-op.
 */
@Deprecated(forRemoval = true)
final class CiSampleSensorIT {
  private CiSampleSensorIT() {
    // no-op placeholder
  }
}
