#!/usr/bin/env python3
"""Check the licence registry the app ships, and refuse a build that would omit a notice.

The app shows its open-source licences from app/licenses/licenses.json and ships the texts in
app/licenses/texts. This validates the pair, because a component listed without its text is a
compliance hole that looks fine on the screen: it appears in the list and opens nothing.

It caught three of those — OpenJDK, ARSCLib and libsu were listed with no text attached.
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

# 2. every component must name a text file, and that file must exist and be non-trivial
for entry in libraries:
    name = entry.get("name") or "(unnamed)"
    text = entry.get("text")
    if not text:
        problems.append("%s has no licence text attached" % name)
        continue
    path = os.path.join(TEXTS, text)
    if not os.path.exists(path):
        problems.append("%s points at a missing text file %s" % (name, text))
    elif os.path.getsize(path) < 200:
        problems.append("%s points at a text file that is too small to be a licence: %s" % (name, text))

# 3. texts shipped but unreferenced are not an error, but worth printing: they are usually a
#    component that was removed and whose notice should go with it
referenced = {entry.get("text") for entry in libraries if entry.get("text")}
shipped = set(os.listdir(TEXTS)) if os.path.isdir(TEXTS) else set()
unreferenced = sorted(shipped - referenced)

print("components: %d, licence ids: %d, texts shipped: %d" % (len(libraries), len(licenses), len(shipped)))
for entry in libraries:
    print("  %-22s %-32s %s" % (entry.get("name"), ",".join(entry.get("licenses") or []),
                                entry.get("text") or "MISSING"))
if unreferenced:
    print("texts shipped but not referenced:", ", ".join(unreferenced))

# The components that must be present, because the APK embeds or links them.
required = ["openjdk", "arsclib", "libsu", "morphe"]
names = " ".join((entry.get("name") or "").lower() for entry in libraries)
for needed in required:
    if needed not in names:
        problems.append("no licence entry for %s, which ships inside the APK" % needed)

if problems:
    print("\nFAILED:")
    for p in problems:
        print("  -", p)
    sys.exit(1)

print("\nlicence registry validated")
