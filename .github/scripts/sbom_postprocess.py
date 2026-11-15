#!/usr/bin/env python3
"""Complete and check the CycloneDX SBOM the Gradle plugin produces.

The plugin walks the resolved release runtime classpath. That misses two kinds of component, and an
SBOM that silently omits them is worse than no SBOM:

  * the three embedded JREs, which a Gradle task downloads rather than resolves;
  * the vendored composites (ARSCLib, smali), which Gradle reports as project dependencies without
    coordinates or licences.

Both are added here, from the same pins the build uses. The script then refuses to write an SBOM that
is empty, thin, or missing any of them — the previous approach produced a 678-byte document that CI
happily uploaded.
"""
import json
import os
import sys

IN = "app/build/reports/bom.json"
OUT = "max-patcher-sbom.cdx.json"

# Must match embeddedJvms in app/build.gradle.kts and vendor/PINS.md.
EMBEDDED_JRE = [
    ("OpenJDK 25 (Android, arm64-v8a)", "25", "d3eb7afe2240c26728a1bb440502c5f18ac3883e932d202dd7f0c9bcbbce4c37"),
    ("OpenJDK 25 (Android, x86_64)", "25", "7fca862ee1b2d5fe23cd9c9c3d9b7ad3c241947ad1a6cc9464ef2e674867105d"),
    ("OpenJDK 17 (Android, armeabi-v7a)", "17", "4a9134f1ebf6340dd855805d351712462cddab6f3c2684da7e7da10ccf06648d"),
]
VENDORED = [
    ("ARSCLib", "675bed2d", "Apache-2.0",
     "Resource table library, forked from REAndroid/ARSCLib",
     "pkg:github/MorpheApp/ARSCLib@675bed2d"),
    ("smali", "bbabf5f9", "GPL-3.0-only",
     "Assembler and dexlib2, forked by Morphe; vendored as source",
     "pkg:github/MorpheApp/smali@bbabf5f9"),
    ("libsu", "5.2.2", "Apache-2.0",
     "Root shell library by topjohnwu", "pkg:github/topjohnwu/libsu"),
]
MIN_COMPONENTS = 50

try:
    bom = json.load(open(IN))
except FileNotFoundError:
    sys.exit("%s is missing: run :app:cyclonedxBom first" % IN)

components = bom.get("components") or []
if not components:
    sys.exit("the CycloneDX document has no components; refusing to publish it")

have = {(c.get("name") or "").lower() for c in components}


def add(component):
    components.append(component)
    have.add(component["name"].lower())


for name, version, sha in EMBEDDED_JRE:
    if name.lower() in have:
        continue
    add({
        "type": "library",
        "bom-ref": "jre:%s" % name,
        "name": name,
        "version": version,
        "description": "Embedded OpenJDK the app runs patching in, downloaded at build time and "
                       "verified against a pinned SHA-256.",
        "licenses": [{"license": {"id": "GPL-2.0-with-classpath-exception"}}],
        "purl": "pkg:generic/openjdk@%s" % version,
        "properties": [
            {"name": "max:download-sha256", "value": sha},
            {"name": "max:discovered-by", "value": "manual: not in the Gradle dependency graph"},
        ],
    })

for name, version, licence, description, purl in VENDORED:
    if name.lower() in have:
        continue
    add({
        "type": "library",
        "bom-ref": "vendored:%s@%s" % (name, version),
        "name": name,
        "version": version,
        "description": description,
        "licenses": [{"license": {"id": licence}}],
        "purl": purl,
        "properties": [
            {"name": "max:discovered-by", "value": "manual: vendored, not resolved from a repository"},
        ],
    })

# Every component must be identifiable. A component with no name or version is a hole in the
# inventory, and the point of this file is that it has no holes.
nameless = [c for c in components if not (c.get("name") or "").strip()]
versionless = [c for c in components if not (c.get("version") or "").strip()]
if nameless:
    sys.exit("%d components have no name" % len(nameless))
if versionless:
    names = ", ".join((c.get("name") or "?") for c in versionless[:10])
    sys.exit("%d components have no version: %s" % (len(versionless), names))
if len(components) < MIN_COMPONENTS:
    sys.exit("only %d components (expected at least %d); the SBOM looks incomplete"
             % (len(components), MIN_COMPONENTS))

for required in ["arsc", "smali", "libsu", "openjdk"]:
    if not any(required in (c.get("name") or "").lower() for c in components):
        sys.exit("the SBOM has no component matching '%s'" % required)

bom["components"] = sorted(components, key=lambda c: (c.get("name") or "").lower())
bom.setdefault("metadata", {})
bom["metadata"]["properties"] = (bom["metadata"].get("properties") or []) + [
    {"name": "max:generated-from", "value": "Gradle releaseRuntimeClasspath, completed by "
                                            ".github/scripts/sbom_postprocess.py"},
]
with open(OUT, "w") as f:
    json.dump(bom, f, indent=2)
    f.write("\n")

with_license = sum(1 for c in components if c.get("licenses"))
print("SBOM written to %s" % OUT)
print("components: %d (%d with licence metadata)" % (len(components), with_license))
print("specVersion: %s, bomFormat: %s" % (bom.get("specVersion"), bom.get("bomFormat")))
