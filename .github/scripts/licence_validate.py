#!/usr/bin/env python3
"""Check the licence registry the app ships, and refuse a build that would omit a notice.

The app shows its open-source licences from app/licenses/licenses.json and ships the texts in
app/licenses/texts. This validates the pair, because a component listed without its text is a
compliance hole that looks fine on the screen: it appears in the list and opens nothing.

It caught four of those: OpenJDK, ARSCLib and libsu were listed with no text attached, ARSCLib and
libsu referenced a licence id that the registry did not define, and the app's own NOTICE pointed at a
file that was not shipped.
"""
import json
import os
import sys

REGISTRY = "app/licenses/licenses.json"
TEXTS = "app/licenses/texts"

try:
    data = json.load(open(REGISTRY))
except FileNotFoundError:
    sys.exit("%s is missing" % REGISTRY)

licenses = data.get("licenses") or {}
libraries = data.get("libraries") or []
if not libraries:
    sys.exit("%s lists no libraries; the licence screen would be empty" % REGISTRY)

problems = []

# 1. every licence id used by a component must be defined
for entry in libraries:
    for licence_id in entry.get("licenses") or []:
        if licence_id not in licenses:
            problems.append("%s uses undefined licence id %s" % (entry.get("name"), licence_id))

# 2. every component must name a text, and that text must exist and be non-trivial.
#    A text can come from the texts directory, or from a file the build copies in itself: licensee
#    copies the repository NOTICE into the APK as NOTICE.txt, so duplicating it in texts/ makes that
#    copy collide with itself at build time (FileAlreadyExistsException on debugLicensesAssets).
for entry in libraries:
    name = entry.get("name") or "(unnamed)"
    text = entry.get("text")
    if not text:
        problems.append("%s has no licence text attached" % name)
        continue
    source = entry.get("source")
    path = source or os.path.join(TEXTS, text)
    if not os.path.exists(path):
        problems.append("%s points at a missing text %s" % (name, path))
    elif os.path.getsize(path) < 200:
        problems.append("%s points at a file too small to be a licence: %s" % (name, path))

# 3. texts shipped but unreferenced are usually a component that was removed and whose notice should
#    go with it; printed, not fatal
referenced = {entry.get("text") for entry in libraries if entry.get("text")}
shipped = set(os.listdir(TEXTS)) if os.path.isdir(TEXTS) else set()
unreferenced = sorted(shipped - referenced)

print("components: %d, licence ids: %d, texts shipped: %d"
      % (len(libraries), len(licenses), len(shipped)))
for entry in libraries:
    print("  %-22s %-32s %s" % (entry.get("name"),
                                ",".join(entry.get("licenses") or []),
                                entry.get("text") or "MISSING"))
if unreferenced:
    print("texts shipped but not referenced:", ", ".join(unreferenced))

# 4. the components that must be present, because the APK embeds or links them
names = " ".join((entry.get("name") or "").lower() for entry in libraries)
for needed in ["openjdk", "arsclib", "libsu", "morphe"]:
    if needed not in names:
        problems.append("no licence entry for %s, which ships inside the APK" % needed)

if problems:
    print("\nFAILED:")
    for p in problems:
        print("  -", p)
    sys.exit(1)

print("\nlicence registry validated")
