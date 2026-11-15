#!/usr/bin/env python3
"""Summarise the licence inventory the app ships, from licensee's report.

licensee writes app/licenses/licenses.json as `licenses` (id -> name/url) and `libraries` (a list of
entries, each naming the licences it is under and the text file shipped for it). That report is what
the in-app licence screen shows, so this summary describes exactly what a user can see. A release
without it is not releasable.
"""
import collections
import json
import sys

PATH = "app/licenses/licenses.json"

try:
    data = json.load(open(PATH))
except FileNotFoundError:
    sys.exit("licence inventory missing: %s not generated" % PATH)

licenses = data.get("licenses") or {}
libraries = data.get("libraries") or []
if not licenses or not libraries:
    sys.exit("%s has an unexpected shape: %s" % (PATH, list(data)))

counts = collections.Counter()
rows = []
for entry in libraries:
    for licence_id in entry.get("licenses") or ["unknown"]:
        name = (licenses.get(licence_id) or {}).get("name") or licence_id
        counts[name] += 1
        rows.append((entry.get("name") or "unnamed", name))

print("licence texts shipped: %d, licences in use: %d" % (len(libraries), len(counts)))
for name, n in counts.most_common():
    print("  %-45s %d" % (name, n))

with open("licence-inventory.txt", "w") as out:
    out.write("component\tlicence\n")
    for component, name in sorted(rows):
        out.write("%s\t%s\n" % (component, name))

copyleft = sorted({n for n in counts if any(k in n.lower() for k in ("gpl", "agpl", "mpl", "epl"))})
print("copyleft-style licences present:", ", ".join(copyleft) or "none")
missing_text = [e.get("name") for e in libraries if not e.get("text")]
if missing_text:
    print("components with no licence text shipped:", ", ".join(map(str, missing_text)))
