# LeviXopclient
Fabric client mod, Minecraft 1.21.1 - 1.21.11. Official theme: white + orange.

One jar per Minecraft version: LeviXopclient-mc<version>-<mod version>.jar
GitHub Actions builds all versions (Actions tab > latest run > Artifacts). Red job = that version still needs a fix: send its log.

Menu: Right Shift (rebind in Options > Controls). Fallback command: /lx
Tabs: Modules, Performance, Combat, Movement, Render, HUD, Misc, Recorder, Profiles, Settings.
Recorder: .minecraft/recordings/*.avi (MJPEG, no audio). Convert in Termux: bash tools/rec2mp4.sh <folder>

Version support notes
- 1.21.1 - 1.21.4: full feature set.
- 1.21.5 - 1.21.11: builds are first-pass. Recorder, Time Changer (1.21.9+) and HitBox are stubbed until ported.
Version specific code lives in src/compat/<group>/ (v1: 1.21.1, v2: 1.21.2-4, v3: 1.21.5, v4: 1.21.6-8, v5: 1.21.9-11).
