# Official-source browser port

Base: official Shattered Pixel Dungeon **v4.0.2**, commit
`57a4e06a4caf162446d1c28caa7983f0493fecf0` from
https://github.com/00-Evan/shattered-pixel-dungeon .

This checkout starts at that official tag, rather than a community Web fork.
The browser target is `web/`; official `core/`, assets and native Bundle save
serialization are retained. Two narrowly audited changes are required:

1. GamesInProgress uses one active save slot.
2. DeviceCompat detects the libGDX application type; browser compilation cannot
   read the native SharedLibraryLoader OS field.

The Web target uses the open-source libGDX TeaVM backend 1.5.6. The pinned
TeaVM static-initializer compatibility workaround was informed by broqdev's
community Web port. No community-fork gameplay, multiplayer implementation or
save-file serialization replaces the official game code here.

## Build and audit on Windows

Requires JDK 21, Node.js and Gradle 9.4.0. This workspace has portable tools in
the sibling `.tools` directory; alternatively set JAVA_HOME and use the wrapper.

    powershell -File web/tools/build.ps1

The standalone Gradle target is `gradle -p web test buildWeb`; it reads game
version/code and libGDX versions from the untouched official build.gradle.
`web/tools/audit-upstream.mjs` compares modified official files with the locked
official tag and permits only documented compatibility changes.

Output: `web/build/public`, including version provenance, compressed resources,
license, and a ZIP of the corresponding actual source files.

## Updates

Commit current work first, then:

    powershell -File web/tools/update-upstream.ps1
    powershell -File web/tools/update-upstream.ps1 -Tag v4.0.3

The explicit tag above is illustrative and must exist as an official stable
release. The script defaults to GitHub's latest stable release. It creates a
separate update branch, fetches only the official upstream, hydrates official
source from its tag archive, merges while retaining the adapter, records the
new upstream commit and runs audit/tests/build. Conflicts and failed validations
stop on the update branch, preserving the previous working release.

The script does not deploy automatically. Before switching a public release,
use a copy of real previous-version saves to verify continued play, native
save/reload, inventory/talents and floor transitions. Future upstream changes
may require additional compatibility work; they are never silently discarded.

## Saves and rollback

One profile is stored under `portal-pd.current.v1` at a stable browser origin.
Native files remain byte-identical Bundle payloads, encoded as base64 in an
atomic localStorage snapshot. Browser preferences use the same profile. Save
errors preserve the prior durable snapshot. Progress is saved on stable turns
periodically and before normal page exit. The wrapper offers export/import.

For an older profile, the new engine initially works on an in-memory copy.
It commits migration only after the native game has restored a playable hero,
keeping the pre-upgrade snapshot at `portal-pd.upgrade.v1`. Older engines refuse
newer profiles. Restore the pre-upgrade snapshot when rolling the engine back;
never feed an upgraded save to an older engine. A writer lease blocks ordinary
concurrent tabs; expired writers must reload instead of resuming stale state.

The previous 3.3.8 sample's data uses different storage keys and remains intact.
Its exported JSON can be explicitly imported; the adapter extracts native game1
bytes and settings, then delegates gameplay migration to the official engine.
Changing the scheme/host/port changes browser storage origin, so export/import
is required when moving to a domain or HTTPS. No cloud synchronization is added.

GPLv3-or-later applies to the game and this derivative. Preserve the original
credits and supply corresponding modified source with hosted browser code.
