<?php

require dirname(__FILE__) . '/lib.php';

$config_path = dirname(__FILE__) . '/config.php';
if (!file_exists($config_path)) $config_path = dirname(__FILE__) . '/config.sample.php';
$GLOBALS['CFG'] = require $config_path;

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: *');

init_db();

function handle_health() {
    json_out(array('ok' => true), 200);
}

function handle_version() {
    $path = dirname(__FILE__) . '/version.json';
    $raw = @file_get_contents($path);
    $data = is_string($raw) ? json_decode($raw, true) : null;
    if (!is_array($data) || !isset($data['versionCode'])) {
        fail(500, 'version info unavailable');
    }
    $mtime = @filemtime($path);
    if ($mtime !== false) $data['updatedAt'] = $mtime;
    header('Cache-Control: no-cache, no-store, must-revalidate');
    json_out($data, 200);
}

function handle_stats() {
    $pdo = db();
    $since = time() - 24 * 3600;
    $total = (int) $pdo->query('SELECT COUNT(*) FROM spirits')->fetchColumn();
    $stmt = $pdo->prepare('SELECT COUNT(*) FROM spirits WHERE created_at >= ?');
    $stmt->execute(array($since));
    $recent_count = (int) $stmt->fetchColumn();

    $class_rows = $pdo->query('SELECT hero_class, COUNT(*) AS c FROM spirits GROUP BY hero_class')->fetchAll();
    $recent_rows = $pdo->query('SELECT username, hero_class, level, depth, created_at FROM spirits ORDER BY id DESC LIMIT 20')->fetchAll();

    $by_class = array();
    foreach ($class_rows as $row) {
        $key = $row['hero_class'] !== null && $row['hero_class'] !== '' ? $row['hero_class'] : 'UNKNOWN';
        $by_class[$key] = (int) $row['c'];
    }
    $recent = array();
    foreach ($recent_rows as $row) {
        $recent[] = array(
            'username' => $row['username'],
            'hero_class' => $row['hero_class'],
            'level' => $row['level'] !== null ? (int) $row['level'] : null,
            'depth' => $row['depth'] !== null ? (int) $row['depth'] : null,
            'created_at' => (int) $row['created_at'],
        );
    }

    json_out(array(
        'total' => $total,
        'recent_24h' => $recent_count,
        'by_class' => $by_class,
        'recent' => $recent,
    ), 200);
}

function clamp_n($value, $default, $max) {
    $n = isset($value) ? (int) $value : $default;
    if ($n < 1) $n = 1;
    if ($n > $max) $n = $max;
    return $n;
}

function handle_recent_epitaphs() {
    $n = clamp_n(isset($_GET['n']) ? $_GET['n'] : null, 20, 50);

    $stmt = db()->prepare('SELECT id, name, text, created_at FROM epitaphs WHERE text <> \'\' ORDER BY id DESC LIMIT ?');
    $stmt->bindValue(1, $n, PDO::PARAM_INT);
    $stmt->execute();

    $epitaphs = array();
    foreach ($stmt->fetchAll() as $row) {
        $epitaphs[] = array(
            'id' => (int) $row['id'],
            'name' => $row['name'],
            'text' => $row['text'],
            'created_at' => (int) $row['created_at'],
        );
    }
    json_out(array('epitaphs' => $epitaphs), 200);
}

function handle_recent_victory() {
    $n = clamp_n(isset($_GET['n']) ? $_GET['n'] : null, 20, 50);

    $stmt = db()->prepare('SELECT id, username, speech, created_at FROM victory WHERE speech <> \'\' ORDER BY id DESC LIMIT ?');
    $stmt->bindValue(1, $n, PDO::PARAM_INT);
    $stmt->execute();

    $victory = array();
    foreach ($stmt->fetchAll() as $row) {
        $victory[] = array(
            'id' => (int) $row['id'],
            'name' => $row['username'],
            'speech' => $row['speech'],
            'created_at' => (int) $row['created_at'],
        );
    }
    json_out(array('victory' => $victory), 200);
}

function handle_upload_spirit() {
    $device = device_id();
    if (too_many_uploads($device)) fail(429, 'rate limited');

    $raw = gunzip_body(read_body());
    $data = json_decode($raw, true);
    if (!is_array($data) || !isset($data['count']) || (int) $data['count'] !== 1) {
        fail(400, 'expected exactly one record');
    }

    $record = isset($data['record0']) ? $data['record0'] : null;
    $check = validate_record($record);
    if (!$check[0]) fail(400, $check[1] !== null ? $check[1] : 'bad record');
    $meta = $check[2];

    $epitaph = sanitize_text(isset($record['epitaph']) ? $record['epitaph'] : null, (int) cfgv('max_epitaph'));
    if ($epitaph === null) fail(400, 'bad epitaph');
    $record['epitaph'] = $epitaph;

    $decision = moderate_or_queue('spirit', $device, $meta['username'], $epitaph, $record);
    if ($decision === 'banned') json_out(array('ok' => false, 'reason' => 'banned'), 200);
    if ($decision === 'pending') { note_upload($device); json_out(array('ok' => true, 'pending' => true), 200); }
    if ($decision === 'rejected') { note_upload($device); json_out(array('ok' => false, 'reason' => 'rejected'), 200); }

    publish_spirit($device, $record, $meta);
    note_upload($device);
    json_out(array('ok' => true), 200);
}

function handle_upload_victory() {
    $device = device_id();
    if (too_many_uploads($device)) fail(429, 'rate limited');

    $data = json_decode(read_body(), true);
    if (!is_array($data)) fail(400, 'bad payload');

    $username = sanitize_username(isset($data['username']) ? $data['username'] : null);
    $speech = sanitize_text(isset($data['speech']) ? $data['speech'] : null, (int) cfgv('max_speech'));
    if ($username === null) fail(400, 'bad username');
    if ($speech === null) fail(400, 'bad speech');

    $payload = array('username' => $username, 'speech' => $speech);
    $decision = moderate_or_queue('victory', $device, $username, $speech, $payload);
    if ($decision === 'banned') json_out(array('ok' => false, 'reason' => 'banned'), 200);
    if ($decision === 'pending') { note_upload($device); json_out(array('ok' => true, 'pending' => true), 200); }
    if ($decision === 'rejected') { note_upload($device); json_out(array('ok' => false, 'reason' => 'rejected'), 200); }

    publish_victory($device, $username, $speech);
    note_upload($device);
    json_out(array('ok' => true), 200);
}

function handle_upload_epitaph() {
    $device = device_id();
    if (too_many_uploads($device)) fail(429, 'rate limited');

    $data = json_decode(read_body(), true);
    if (!is_array($data)) fail(400, 'bad payload');

    $name = sanitize_text(isset($data['name']) ? $data['name'] : null, (int) cfgv('max_epitaph'));
    $text = sanitize_text(isset($data['text']) ? $data['text'] : null, (int) cfgv('max_epitaph'));
    if ($name === null) fail(400, 'bad name');
    if ($text === null || trim($text) === '') fail(400, 'bad epitaph');

    $payload = array('name' => $name, 'text' => $text);
    $decision = moderate_or_queue('epitaph', $device, $name, $text, $payload);
    if ($decision === 'banned') json_out(array('ok' => false, 'reason' => 'banned'), 200);
    if ($decision === 'pending') { note_upload($device); json_out(array('ok' => true, 'pending' => true), 200); }
    if ($decision === 'rejected') { note_upload($device); json_out(array('ok' => false, 'reason' => 'rejected'), 200); }

    publish_epitaph($device, $name, $text);
    note_upload($device);
    json_out(array('ok' => true), 200);
}

function handle_batch_spirits() {
    $device = device_id();
    $n = isset($_GET['n']) ? (int) $_GET['n'] : 10;
    if ($n < 1) $n = 1;
    if ($n > 50) $n = 50;

    $pdo = db();
    $records = array();
    $pdo->exec('BEGIN IMMEDIATE');
    try {
        $ids = $pdo->query('SELECT id FROM spirits WHERE claimed_at IS NULL ORDER BY RANDOM() LIMIT ' . $n)->fetchAll(PDO::FETCH_COLUMN, 0);
        if (count($ids) > 0) {
            $in = implode(',', array_map('intval', $ids));
            $stmt = $pdo->prepare('UPDATE spirits SET claimed_at = ?, claimed_by = ? WHERE id IN (' . $in . ')');
            $stmt->execute(array(time(), $device));
            $rows = $pdo->query('SELECT record_json FROM spirits WHERE id IN (' . $in . ')')->fetchAll(PDO::FETCH_COLUMN, 0);
            foreach ($rows as $json) {
                $records[] = json_decode($json, true);
            }
        }
        $pdo->exec('COMMIT');
    } catch (Exception $e) {
        @$pdo->exec('ROLLBACK');
        throw $e;
    }

    $payload = array('count' => count($records));
    for ($i = 0; $i < count($records); $i++) {
        $payload['record' . $i] = $records[$i];
    }

    send_status(200);
    header('Content-Type: application/octet-stream');
    echo gzencode(json_encode($payload), 6);
    exit;
}

function handle_random_epitaphs() {
    $n = isset($_GET['n']) ? (int) $_GET['n'] : 100;
    if ($n < 1) $n = 1;
    if ($n > 500) $n = 500;

    $sql = 'SELECT name, text FROM ('
        . ' SELECT username AS name, epitaph AS text FROM spirits WHERE epitaph IS NOT NULL AND epitaph <> \'\''
        . ' UNION ALL SELECT name, text FROM epitaphs'
        . ' UNION ALL SELECT name, text FROM seed_epitaphs'
        . ') t ORDER BY RANDOM() LIMIT ' . $n;

    $rows = db()->query($sql)->fetchAll();
    $epitaphs = array();
    foreach ($rows as $row) {
        $epitaphs[] = array('name' => $row['name'], 'text' => $row['text']);
    }
    json_out(array('epitaphs' => $epitaphs), 200);
}

function require_admin() {
    $token = isset($_SERVER['HTTP_X_ADMIN_TOKEN']) ? $_SERVER['HTTP_X_ADMIN_TOKEN'] : '';
    $admin = cfgv('admin_token');
    if ($admin === '' || $token !== $admin) fail(403, 'forbidden');
}

function handle_delete_spirit($id) {
    require_admin();

    $stmt = db()->prepare('DELETE FROM spirits WHERE id = ?');
    $stmt->execute(array((int) $id));
    json_out(array('deleted' => $stmt->rowCount()), 200);
}

function handle_delete_victory($id) {
    require_admin();

    $stmt = db()->prepare('DELETE FROM victory WHERE id = ?');
    $stmt->execute(array((int) $id));
    json_out(array('deleted' => $stmt->rowCount()), 200);
}

function handle_delete_epitaph($id) {
    require_admin();

    $stmt = db()->prepare('DELETE FROM epitaphs WHERE id = ?');
    $stmt->execute(array((int) $id));
    json_out(array('deleted' => $stmt->rowCount()), 200);
}

/** admin-only dry run of the moderation: checks a text without publishing it */
function handle_admin_moderate() {
    require_admin();

    $data = json_decode(read_body(), true);
    if (!is_array($data)) fail(400, 'bad payload');

    $name = sanitize_username(isset($data['name']) ? $data['name'] : null);
    if ($name === null) $name = 'test';
    $text = sanitize_text(isset($data['text']) ? $data['text'] : null, (int) cfgv('max_speech'));
    if ($text === null) fail(400, 'bad text');

    $raw = null;
    $verdict = moderate($name, $text, $raw);
    json_out(array('verdict' => $verdict, 'raw' => $raw), 200);
}

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$path = trim($path, '/');

if (strpos($path, 'api/') === 0) {
    $path = substr($path, 4);
}

if ($method === 'GET' && $path === 'health') {
    handle_health();
} elseif ($method === 'GET' && $path === 'v1/version') {
    handle_version();
} elseif ($method === 'GET' && $path === 'v1/stats') {
    handle_stats();
} elseif ($method === 'GET' && $path === 'v1/epitaphs/recent') {
    handle_recent_epitaphs();
} elseif ($method === 'GET' && $path === 'v1/victory/recent') {
    handle_recent_victory();
} elseif ($method === 'POST' && $path === 'v1/spirits') {
    handle_upload_spirit();
} elseif ($method === 'POST' && $path === 'v1/victory') {
    handle_upload_victory();
} elseif ($method === 'POST' && $path === 'v1/epitaphs') {
    handle_upload_epitaph();
} elseif ($method === 'GET' && $path === 'v1/spirits/batch') {
    handle_batch_spirits();
} elseif ($method === 'GET' && $path === 'v1/epitaphs/random') {
    handle_random_epitaphs();
} elseif ($method === 'DELETE' && preg_match('#^v1/admin/spirits/([0-9]+)$#', $path, $m)) {
    handle_delete_spirit($m[1]);
} elseif ($method === 'DELETE' && preg_match('#^v1/admin/victory/([0-9]+)$#', $path, $m)) {
    handle_delete_victory($m[1]);
} elseif ($method === 'DELETE' && preg_match('#^v1/admin/epitaphs/([0-9]+)$#', $path, $m)) {
    handle_delete_epitaph($m[1]);
} elseif ($method === 'POST' && $path === 'v1/admin/moderate') {
    handle_admin_moderate();
} else {
    fail(404, 'not found');
}
