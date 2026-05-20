#!/usr/bin/env python3
"""Cross-check that every <Lang>CheckRegistrar publishes the same set of
rule keys that the matching creedengo_way_profile_<lang>.json activates,
AND that every active rule has its HTML + JSON metadata reachable on the
classpath (either as a local resource or inside the
creedengo-rules-specifications classifier JAR).

Detects three classes of bug seen during creedengo-infra bring-up :

  1.  Profile activates a key but no Check class is registered.
  2.  Check class is registered but profile does not activate it.
  3.  Class is registered AND profile activates it, but the
      HTML/JSON metadata file is missing from both
      `src/main/resources/org/green-code-initiative/rules/<lang>/`
      and the dependency JAR -> SonarQube fails to boot with
      `IOException: Resource not found in the classpath: ...`.
"""
import json
import os
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHECKS_DIR = os.path.join(
    ROOT, "src/main/java/org/greencodeinitiative/creedengo/infra/checks"
)
INFRA_DIR = os.path.join(
    ROOT, "src/main/java/org/greencodeinitiative/creedengo/infra"
)
PROFILES_DIR = os.path.join(
    ROOT, "src/main/resources/org/greencodeinitiative/creedengo/profiles"
)
LOCAL_RES_DIR = os.path.join(
    ROOT, "src/main/resources/org/green-code-initiative/rules"
)
POM_FILE = os.path.join(ROOT, "pom.xml")
M2_REPO = os.environ.get(
    "M2_REPO", os.path.expanduser("~/.m2/repository")
)
SPEC_GROUP_PATH = "org/green-code-initiative/creedengo-rules-specifications"

# lang (as used in registrar / profile names) -> rules-specifications classifier
LANG_CLASSIFIER = {
    "Docker": "docker",
    "Kubernetes": "kubernetes",
    "Terraform": "terraform",
    "CloudFormation": "cloudformation",
    "Yaml": "yaml",
}


def _resolve_spec_version():
    """Read <version.creedengo-rules-specifications> from pom.xml."""
    try:
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        root = ET.parse(POM_FILE).getroot()
        for tag in ("m:properties/m:version.creedengo-rules-specifications",
                    "properties/version.creedengo-rules-specifications"):
            node = root.find(tag, ns) if tag.startswith("m:") else root.find(tag)
            if node is not None and node.text:
                return node.text.strip()
    except Exception:
        pass
    return None


SPEC_VERSION = _resolve_spec_version()


def _spec_jar_for(classifier):
    if not SPEC_VERSION:
        return None
    path = os.path.join(
        M2_REPO, SPEC_GROUP_PATH, SPEC_VERSION,
        "creedengo-rules-specifications-%s-%s.jar"
        % (SPEC_VERSION, classifier),
    )
    return path if os.path.exists(path) else None


# Cache of classifier -> set of resource paths inside the spec JAR
_jar_entries_cache = {}


def _jar_entries(classifier):
    if classifier in _jar_entries_cache:
        return _jar_entries_cache[classifier]
    entries = set()
    jar = _spec_jar_for(classifier)
    if jar:
        try:
            with zipfile.ZipFile(jar) as zf:
                entries = set(zf.namelist())
        except Exception as exc:
            print("WARN: cannot read %s: %s" % (jar, exc))
    _jar_entries_cache[classifier] = entries
    return entries


def _metadata_missing(key, classifier):
    """Return list of missing metadata files (html/json) for `key`."""
    missing = []
    local_dir = os.path.join(LOCAL_RES_DIR, classifier)
    entries = _jar_entries(classifier)
    for ext in ("html", "json"):
        local_path = os.path.join(local_dir, "%s.%s" % (key, ext))
        jar_path = "org/green-code-initiative/rules/%s/%s.%s" % (
            classifier, key, ext,
        )
        if not os.path.exists(local_path) and jar_path not in entries:
            missing.append("%s.%s" % (key, ext))
    return missing

# 1) class name -> @Rule(key=...)
key_of = {}
for fname in os.listdir(CHECKS_DIR):
    if not fname.endswith(".java"):
        continue
    txt = open(os.path.join(CHECKS_DIR, fname)).read()
    m = re.search(r'@Rule\(key\s*=\s*"([^"]+)"\)', txt)
    if m:
        key_of[fname[:-5]] = m.group(1)

ok = True
for lang in ["Docker", "Kubernetes", "Terraform", "CloudFormation", "Yaml"]:
    reg = os.path.join(INFRA_DIR, "Infra" + lang + "CheckRegistrar.java")
    prof = os.path.join(
        PROFILES_DIR, "creedengo_way_profile_" + lang.lower() + ".json"
    )
    if not (os.path.exists(reg) and os.path.exists(prof)):
        continue
    txt = open(reg).read()
    # Only ACTIVE (non-commented) class entries
    active = []
    for line in txt.splitlines():
        s = line.strip()
        if s.startswith("//") or s.startswith("/*") or s.startswith("*"):
            continue
        m = re.search(r"checks\.(\w+Check)\.class", line)
        if m:
            active.append(m.group(1))
    reg_keys = set(key_of.get(c, "?" + c) for c in active)
    pkeys = set(json.load(open(prof))["ruleKeys"])

    missing_in_reg = sorted(pkeys - reg_keys)
    missing_in_prof = sorted(reg_keys - pkeys)

    # 3) For every key that is BOTH registered AND activated, make sure
    #    SonarQube will be able to load its HTML+JSON metadata at runtime.
    classifier = LANG_CLASSIFIER.get(lang)
    metadata_missing = {}
    if classifier:
        for key in sorted(reg_keys & pkeys):
            miss = _metadata_missing(key, classifier)
            if miss:
                metadata_missing[key] = miss
    else:
        print("WARN: no classifier mapping for lang=%s" % lang)

    status = (
        "OK"
        if not missing_in_reg
        and not missing_in_prof
        and not metadata_missing
        else "MISMATCH"
    )
    print(
        "[%s] %-15s profile=%d registrar=%d "
        "missing_in_registrar=%s missing_in_profile=%s "
        "missing_metadata=%s"
        % (
            status,
            lang,
            len(pkeys),
            len(reg_keys),
            missing_in_reg,
            missing_in_prof,
            metadata_missing or {},
        )
    )
    if status != "OK":
        ok = False

sys.exit(0 if ok else 1)


