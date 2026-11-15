#!/usr/bin/env python3
"""Fail on an SBOM that is valid JSON but useless.

cyclonedx-cli checks the schema. This checks the things a schema cannot: that the document actually
describes this app's build, has a plausible number of components, names the components the APK
embeds, and carries licence metadata for them.
"""
import json
import sys

PATH = "max-patcher-sbom.cdx.json"
MIN_COMPONENTS = 50

try:
    bom = json.load(open(PATH))
except FileNotFoundError:
    sys.exit("%s is missing" % PATH)

components = bom.get("components") or []
print("components: %d" % len(components))
print("bomFormat: %s, specVersion: %s" % (bom.get("bomFormat"), bom.get("specVersion")))

problems = []
if bom.get("bomFormat") != "CycloneDX":
    problems.append("bomFormat is not CycloneDX")
if not bom.get("specVersion"):
    problems.append("specVersion is missing")
if len(components) < MIN_COMPONENTS:
    problems.append("only %d components; the document is incomplete" % len(components))

names = " ".join((c.get("name") or "").lower() for c in components)
for required in ("arsc", "smali", "libsu", "openjdk"):
    if required not in names:
        problems.append("no component matching '%s'" % required)

with_license = sum(1 for c in components if c.get("licenses"))
if with_license < len(components) // 2:
    problems.append("only %d of %d components carry licence metadata" % (with_license, len(components)))

if problems:
    for p in problems:
        print("  -", p)
    sys.exit(1)

print("SBOM is complete: %d components, %d with licence metadata" % (len(components), with_license))
