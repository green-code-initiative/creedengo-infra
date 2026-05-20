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

import java.util.List;
import java.util.Optional;
import org.sonar.iac.terraform.api.tree.AttributeTree;
import org.sonar.iac.terraform.api.tree.BlockTree;
import org.sonar.iac.terraform.api.tree.BodyTree;
import org.sonar.iac.terraform.api.tree.ExpressionTree;
import org.sonar.iac.terraform.api.tree.FileTree;
import org.sonar.iac.terraform.api.tree.LabelTree;
import org.sonar.iac.terraform.api.tree.LiteralExprTree;
import org.sonar.iac.terraform.api.tree.ObjectElementTree;
import org.sonar.iac.terraform.api.tree.ObjectTree;
import org.sonar.iac.terraform.api.tree.StatementTree;
import org.sonar.iac.terraform.api.tree.TupleTree;
import org.sonar.iac.terraform.api.tree.VariableExprTree;

/**
 * Tiny helpers for navigating the sonar-iac Terraform tree without bringing
 * a fluent symbol DSL into this module. Keeps each Wave-2D Terraform check
 * focused on its rule logic.
 */
final class TerraformCheckUtils {

  private TerraformCheckUtils() {
    // utility class
  }

  /** Stream top-level statements of a Terraform file (every {@code resource}, {@code provider}, …). */
  static java.util.stream.Stream<StatementTree> statements(FileTree file) {
    return file.properties().stream();
  }

  /** Return only the {@link BlockTree}s of a file, optionally filtered by block type (e.g. {@code "resource"}, {@code "provider"}). */
  static java.util.stream.Stream<BlockTree> blocks(FileTree file, String blockType) {
    return statements(file)
      .filter(BlockTree.class::isInstance)
      .map(BlockTree.class::cast)
      .filter(b -> blockType == null || blockType.equals(keyName(b)));
  }

  /** Same as {@link #blocks(FileTree, String)} for any block type. */
  static java.util.stream.Stream<BlockTree> blocks(FileTree file) {
    return blocks(file, null);
  }

  /** Resolve the block's key (its type token, e.g. {@code "resource"}). */
  static String keyName(BlockTree block) {
    return block.key() != null ? block.key().value() : null;
  }

  /** Resolve the block's first label (e.g. {@code "aws_instance"} on {@code resource "aws_instance" "x"}). */
  static String firstLabel(BlockTree block) {
    List<LabelTree> labels = block.labels();
    return labels.isEmpty() ? null : labels.get(0).value();
  }

  /** Resolve the attribute named {@code name} inside a block's body, if any. */
  static Optional<AttributeTree> attribute(BlockTree block, String name) {
    BodyTree body = block.value();
    if (body == null) {
      return Optional.empty();
    }
    return body.statements().stream()
      .filter(AttributeTree.class::isInstance)
      .map(AttributeTree.class::cast)
      .filter(a -> name.equals(a.key().value()))
      .findFirst();
  }

  /** Resolve the body's nested {@link BlockTree} children (e.g. {@code lifecycle_rule { ... }} blocks inside a resource). */
  static java.util.stream.Stream<BlockTree> nestedBlocks(BlockTree block) {
    BodyTree body = block.value();
    if (body == null) {
      return java.util.stream.Stream.empty();
    }
    return body.statements().stream()
      .filter(BlockTree.class::isInstance)
      .map(BlockTree.class::cast);
  }

  /** Resolve an attribute value as a plain string literal (strips surrounding quotes). */
  static Optional<String> stringValue(AttributeTree attribute) {
    return stringValue(attribute.value());
  }

  /** Resolve an expression as a plain string literal (strips surrounding quotes). */
  static Optional<String> stringValue(ExpressionTree expr) {
    if (expr instanceof LiteralExprTree literal) {
      String raw = literal.value();
      if (raw == null) {
        return Optional.empty();
      }
      return Optional.of(unquote(raw));
    }
    return Optional.empty();
  }

  /** Resolve a tuple of string literals (e.g. {@code architectures = ["arm64"]}). */
  static List<String> tupleStrings(ExpressionTree expr) {
    if (!(expr instanceof TupleTree tuple)) {
      return List.of();
    }
    java.util.List<String> out = new java.util.ArrayList<>();
    for (ExpressionTree el : tuple) {
      stringValue(el).ifPresent(out::add);
    }
    return out;
  }

  /**
   * Walk an attribute-style object expression ({@code tags = { key = "value", ... }})
   * and stream its {@link ObjectElementTree} children. Returns an empty stream when
   * the attribute value is not an {@link ObjectTree} (e.g. a function call or variable).
   */
  static java.util.stream.Stream<ObjectElementTree> objectEntries(AttributeTree attribute) {
    if (attribute == null || !(attribute.value() instanceof ObjectTree object)) {
      return java.util.stream.Stream.empty();
    }
    return object.properties().stream();
  }

  /** Resolve the (string) key name of an object entry — strips quotes from quoted keys. */
  static String objectEntryKey(ObjectElementTree entry) {
    ExpressionTree key = entry.key();
    if (key instanceof VariableExprTree variable) {
      return variable.name();
    }
    if (key instanceof LiteralExprTree literal && literal.value() != null) {
      return unquote(literal.value());
    }
    // Fallback: best-effort via toString().
    return key == null ? null : unquote(key.toString().trim());
  }

  /** Resolve the (string) value of an object entry, when it is a literal string. */
  static Optional<String> objectEntryStringValue(ObjectElementTree entry) {
    return stringValue(entry.value());
  }

  private static String unquote(String raw) {
    if (raw.length() >= 2) {
      char first = raw.charAt(0);
      char last = raw.charAt(raw.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return raw.substring(1, raw.length() - 1);
      }
    }
    return raw;
  }
}


