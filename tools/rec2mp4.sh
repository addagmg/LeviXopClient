#!/data/data/com.termux/files/usr/bin/bash
# Convert LeviXopclient recordings (.avi MJPEG) to .mp4 that every phone player can open.
# Usage: bash rec2mp4.sh /path/to/recordings
# Needs:  pkg install ffmpeg
dir="${1:-.}"
for f in "$dir"/*.avi; do
  [ -e "$f" ] || { echo "no .avi files in $dir"; exit 1; }
  out="${f%.avi}.mp4"
  [ -e "$out" ] && continue
  ffmpeg -y -loglevel error -i "$f" -c:v libx264 -preset veryfast -crf 23 -pix_fmt yuv420p "$out" && echo "ok: $out"
done
