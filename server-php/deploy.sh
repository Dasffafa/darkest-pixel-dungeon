#!/usr/bin/env bash
set -euo pipefail

: "${FTP_HOST:?set FTP_HOST (e.g. x0911.gotoftp1.com)}"
: "${FTP_USER:?set FTP_USER}"
: "${FTP_PASS:?set FTP_PASS}"
FTP_ROOT="${FTP_ROOT:-wwwroot}"
BASE="ftp://${FTP_USER}:${FTP_PASS}@${FTP_HOST}"

DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="$DIR/backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

backup() {
  local remote="$1"
  local dest="$BACKUP_DIR/$2"
  mkdir -p "$(dirname "$dest")"
  if curl -fsS -m 60 "$BASE/$remote" -o "$dest"; then
    echo "<- $remote"
  else
    rm -f "$dest"
    echo "warning: skip backup $remote (not found or transfer failed)"
  fi
}

echo "backing up remote files to $BACKUP_DIR ..."
backup "data/spirits.db" "data/spirits.db"
backup "$FTP_ROOT/api/config.php" "api/config.php"
backup "$FTP_ROOT/api/index.php" "api/index.php"
backup "$FTP_ROOT/api/lib.php" "api/lib.php"
backup "$FTP_ROOT/.htaccess" ".htaccess"
backup "$FTP_ROOT/index.html" "index.html"
backup "$FTP_ROOT/app.js" "app.js"
backup "$FTP_ROOT/style.css" "style.css"

put() {
  echo "-> $2"
  curl -sS -m 60 -T "$1" "$BASE/$2"
}

curl -sS -m 30 "$BASE/" -Q "MKD /data" >/dev/null 2>&1 || true
curl -sS -m 30 "$BASE/$FTP_ROOT/" -Q "MKD /$FTP_ROOT/api" >/dev/null 2>&1 || true
curl -sS -m 30 "$BASE/$FTP_ROOT/" -Q "MKD /$FTP_ROOT/filing" >/dev/null 2>&1 || true

put "$DIR/src/wwwroot/index.html" "$FTP_ROOT/index.html"
put "$DIR/src/wwwroot/app.js" "$FTP_ROOT/app.js"
put "$DIR/src/wwwroot/style.css" "$FTP_ROOT/style.css"
put "$DIR/src/wwwroot/filing/index.html" "$FTP_ROOT/filing/index.html"
put "$DIR/src/.htaccess" "$FTP_ROOT/.htaccess"
put "$DIR/src/api/lib.php" "$FTP_ROOT/api/lib.php"
put "$DIR/src/api/index.php" "$FTP_ROOT/api/index.php"
put "$DIR/src/api/config.sample.php" "$FTP_ROOT/api/config.sample.php"
put "$DIR/src/api/version.json" "$FTP_ROOT/api/version.json"

if [ -f "$DIR/src/api/config.php" ]; then
  put "$DIR/src/api/config.php" "$FTP_ROOT/api/config.php"
else
  echo "warning: $DIR/src/api/config.php not found; AI/db config will fall back to sample"
fi

echo "deployed"
