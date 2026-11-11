#!/bin/bash
set -euo pipefail
release_id="${1:-4.0.2-r1}"
[[ "$release_id" =~ ^[0-9]+\.[0-9]+\.[0-9]+-r[0-9]+$ ]] || exit 2
base=/srv/pixel-dungeon-official
release="$base/releases/$release_id"
test -f "$release/public/play/version.json"
test -f "$release/server.mjs"
id pixeldungeon >/dev/null 2>&1 || useradd --system --home-dir "$base" --shell /usr/sbin/nologin pixeldungeon
chown -R root:pixeldungeon "$release"
find "$release" -type d -exec chmod 750 {} +
find "$release" -type f -exec chmod 640 {} +
chown root:pixeldungeon "$base" "$base/releases"
chmod 750 "$base" "$base/releases"
previous=$(test -L "$base/current" && readlink -f "$base/current" || true)
if test -n "$previous" && test "$previous" != "$release"; then
  [[ "$previous" == "$base/releases/"* ]] || exit 3
  ln -sfn "$previous" "$base/previous"
fi
ln -sfn "$release" "$base/current.next"
mv -Tf "$base/current.next" "$base/current"
cat > /etc/systemd/system/pixel-dungeon-official.service <<'UNIT'
[Unit]
Description=Official-source Pixel Dungeon browser port
After=network-online.target
Wants=network-online.target
[Service]
Type=simple
User=pixeldungeon
Group=pixeldungeon
WorkingDirectory=/srv/pixel-dungeon-official/current
Environment=STATIC_ROOT=/srv/pixel-dungeon-official/current/public
Environment=HOST=0.0.0.0
Environment=PORT=8082
ExecStart=/opt/node/bin/node --max-old-space-size=48 /srv/pixel-dungeon-official/current/server.mjs
Restart=on-failure
RestartSec=5
TimeoutStopSec=10
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
MemoryMax=96M
TasksMax=32
UMask=0077
[Install]
WantedBy=multi-user.target
UNIT
systemd-analyze verify /etc/systemd/system/pixel-dungeon-official.service
systemctl daemon-reload
systemctl enable pixel-dungeon-official
systemctl restart pixel-dungeon-official
systemctl is-active pixel-dungeon-official
