<?php

function cfg() {
    return $GLOBALS['CFG'];
}

function cfgv($key) {
    return $GLOBALS['CFG'][$key];
}

function is_assoc($arr) {
    if (!is_array($arr)) return false;
    if (count($arr) === 0) return false;
    return array_keys($arr) !== range(0, count($arr) - 1);
}

function db() {
    static $pdo = null;
    if ($pdo === null) {
        $c = cfg();
        $dir = dirname($c['db_path']);
        if (!is_dir($dir)) @mkdir($dir, 0775, true);
        $pdo = new PDO('sqlite:' . $c['db_path']);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        @$pdo->exec('PRAGMA journal_mode=WAL');
        @$pdo->exec('PRAGMA busy_timeout=5000');
    }
    return $pdo;
}

function send_status($status) {
    $texts = array(
        400 => 'Bad Request',
        403 => 'Forbidden',
        404 => 'Not Found',
        413 => 'Payload Too Large',
        429 => 'Too Many Requests',
        500 => 'Internal Server Error',
    );
    $text = isset($texts[$status]) ? $texts[$status] : 'OK';
    header('HTTP/1.1 ' . $status . ' ' . $text);
}

function json_out($data, $status) {
    if ($status === null) $status = 200;
    send_status($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data);
    exit;
}

function fail($status, $detail) {
    json_out(array('detail' => $detail), $status);
}

function gunzip_body($data) {
    if (strlen($data) >= 2 && ord($data[0]) === 0x1f && ord($data[1]) === 0x8b) {
        $out = @gzinflate(substr($data, 10, -8));
        if ($out !== false) return $out;
    }
    return $data;
}

function read_body() {
    $raw = file_get_contents('php://input');
    if ($raw === false) $raw = '';
    if (strlen($raw) > (int) cfgv('max_body_bytes')) fail(413, 'payload too large');
    return $raw;
}

function device_id() {
    $id = isset($_SERVER['HTTP_X_DEVICE_ID']) ? $_SERVER['HTTP_X_DEVICE_ID'] : '';
    if ($id === '' || strlen($id) > 64 || preg_match('/[\x00-\x1F\x7F]/', $id)) {
        fail(400, 'missing device id');
    }
    return $id;
}

function str_len($s) {
    return function_exists('mb_strlen') ? mb_strlen($s, 'UTF-8') : strlen($s);
}

function sanitize_text($text, $limit) {
    if ($text === null) return '';
    if (!is_string($text)) return null;
    $cleaned = preg_replace('/[\x00-\x1F\x7F]/u', '', $text);
    if ($cleaned === null) return null;
    $cleaned = trim($cleaned);
    if (str_len($cleaned) > $limit) return null;
    return $cleaned;
}

function sanitize_username($name) {
    if (!is_string($name)) return null;
    $name = sanitize_text($name, 64);
    if ($name === null || $name === '') return null;
    return $name;
}

function allowed_prefixes() {
    return array(
        'com.egoal.darkestpixeldungeon.items.',
        'com.egoal.darkestpixeldungeon.actors.hero.perks.',
    );
}

function hero_classes() {
    return array('WARRIOR', 'MAGE', 'ROGUE', 'HUNTRESS', 'SORCERESS', 'EXILE');
}

function classes_allowed($node) {
    if (is_array($node)) {
        if (isset($node['__className'])) {
            $cn = $node['__className'];
            $ok = false;
            if (is_string($cn)) {
                foreach (allowed_prefixes() as $p) {
                    if (strpos($cn, $p) === 0) { $ok = true; break; }
                }
            }
            if (!$ok) return false;
        }
        if (isset($node['class'])) {
            $hc = $node['class'];
            if (!is_string($hc) || !in_array($hc, hero_classes(), true)) return false;
        }
        foreach ($node as $value) {
            if (!classes_allowed($value)) return false;
        }
        return true;
    }
    return true;
}

function validate_record(&$record) {
    if (!is_array($record) || !is_assoc($record)) return array(false, 'record is not an object', array());
    if (!classes_allowed($record)) return array(false, 'disallowed class name', array());

    $depth = isset($record['depth']) ? $record['depth'] : null;
    $level = isset($record['level']) ? $record['level'] : null;
    if (!is_int($depth) || $depth < 0 || $depth > 10) return array(false, 'bad depth', array());
    if (!is_int($level) || $level < 1 || $level > 50) return array(false, 'bad level', array());
    if (!array_key_exists('heldby', $record) || $record['heldby'] !== -1) return array(false, 'record is not free', array());
    if (!isset($record['perk']) || !is_array($record['perk']) || !isset($record['perk']['__className']) || !is_string($record['perk']['__className'])) {
        return array(false, 'missing perk', array());
    }

    $username = sanitize_username(isset($record['username']) ? $record['username'] : null);
    if ($username === null) return array(false, 'bad username', array());
    $record['username'] = $username;

    $hero_class = (isset($record['class']) && is_string($record['class'])) ? $record['class'] : null;
    return array(true, null, array(
        'depth' => $depth,
        'level' => $level,
        'username' => $username,
        'hero_class' => $hero_class,
    ));
}

function moderate($name, $text) {
    $c = cfg();
    if ($c['ai_base_url'] === '' || $c['ai_model'] === '') return 'failed';

    $base = rtrim($c['ai_base_url'], '/');
    if (substr($base, -17) !== '/chat/completions') $base .= '/chat/completions';

    $prompt = "根据中国法律，以下文本是否既有意义、又适合作为游戏id或墓志铭？只回答“是”或“否”：\n"
        . "游戏id：" . $name . "\n墓志铭：" . $text;

    $payload = json_encode(array(
        'model' => $c['ai_model'],
        'temperature' => 0,
        'messages' => array(array('role' => 'user', 'content' => $prompt)),
    ));

    $ch = curl_init($base);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array(
        'Content-Type: application/json',
        'Authorization: Bearer ' . $c['ai_api_key'],
    ));
    curl_setopt($ch, CURLOPT_TIMEOUT, (int) $c['ai_timeout']);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 8);
    $body = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($body === false || $err !== '') return 'failed';
    if ($code === 402) return 'insufficient';
    if ($code < 200 || $code >= 300) {
        $low = strtolower($body);
        foreach (array('insufficient', 'balance', 'quota', 'credit', '余额', '欠费') as $m) {
            if (strpos($low, $m) !== false) return 'insufficient';
        }
        return 'failed';
    }

    $data = json_decode($body, true);
    if (!is_array($data) || !isset($data['choices'][0]['message']['content'])) return 'failed';
    $content = trim($data['choices'][0]['message']['content']);
    if ($content === '是') return 'approved';
    if ($content === '否') return 'rejected';
    return 'failed';
}

function generate_seeds() {
    $names = array(
        '无名者', '提灯人', '枯骨旅人', '拾荒的罗兰', '灰袍法师',
        '断剑骑士', '夜行者', '迷路的矿工', '旧日勇者', '黑曜石之影',
        '第七个矮人', '半途而废者', '提灯的卡莲', '无名剑客', '地牢速通者',
        '妄图登顶者', '贪财的冒险家', '身负重甲者', '不会游泳的人', '火把商人',
        '深渊学徒', '啃面包的旅人', '数金币的人', '初来乍到者', '沉默的猎人',
        '墙角的盗贼', '喝错药水的人', '走了回头路的人', '没带火把的人', '记性很差的人',
    );
    $texts = array(
        '被粘咕消化',
        '中毒致死',
        '燃烧殆尽',
        '流血致死',
        '被 DM-300 碾压致死',
        '窒息而死',
        '被白雾腐蚀',
        '爽局暴毙',
        '饥饿致死',
        '被溶解',
        '被豺狼人围殴致死',
        '踩到了陷阱',
        '被自己的法术反噬',
        '在黑暗中迷失',
        '被诅咒的装备拖垮',
        '试图和石像鬼讲道理',
        '和食人鱼掰手腕',
        '被火元素烤熟',
        '我说疯脸电击等于即死',
        '把升级卷轴浪费在木棍上',
        '把治疗药水留到真正需要的时候。',
    );
    $pairs = array();
    for ($i = 0; $i < 100; $i++) {
        $pairs[] = array($names[$i % count($names)], $texts[($i * 7) % count($texts)]);
    }
    return $pairs;
}

function init_db() {
    $pdo = db();
    $pdo->exec('CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT)');

    $flag = $pdo->query("SELECT v FROM meta WHERE k = 'schema_v1'")->fetchColumn();
    if ($flag === '1') return;

    $pdo->exec('CREATE TABLE IF NOT EXISTS spirits (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        depth INTEGER NOT NULL,
        hero_class TEXT,
        level INTEGER,
        username TEXT,
        epitaph TEXT,
        record_json TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        claimed_at INTEGER,
        claimed_by TEXT
    )');
    $pdo->exec('CREATE INDEX IF NOT EXISTS idx_spirits_unclaimed ON spirits(claimed_at)');

    $pdo->exec('CREATE TABLE IF NOT EXISTS pending (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        kind TEXT NOT NULL,
        payload_json TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )');

    $pdo->exec('CREATE TABLE IF NOT EXISTS victory (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        username TEXT,
        speech TEXT,
        created_at INTEGER NOT NULL
    )');

    $pdo->exec('CREATE TABLE IF NOT EXISTS malice (
        device_id TEXT PRIMARY KEY,
        count INTEGER NOT NULL DEFAULT 0,
        banned_until INTEGER NOT NULL DEFAULT 0
    )');

    $pdo->exec('CREATE TABLE IF NOT EXISTS seed_epitaphs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        text TEXT NOT NULL
    )');

    $pdo->exec('CREATE TABLE IF NOT EXISTS epitaphs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        name TEXT,
        text TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )');

    $pdo->exec('CREATE TABLE IF NOT EXISTS upload_log (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )');
    $pdo->exec('CREATE INDEX IF NOT EXISTS idx_upload_device ON upload_log(device_id, created_at)');

    $count = (int) $pdo->query('SELECT COUNT(*) FROM seed_epitaphs')->fetchColumn();
    if ($count === 0) {
        $pdo->beginTransaction();
        $stmt = $pdo->prepare('INSERT INTO seed_epitaphs(name, text) VALUES (?, ?)');
        foreach (generate_seeds() as $pair) {
            $stmt->execute($pair);
        }
        $pdo->commit();
    }

    $pdo->exec("REPLACE INTO meta(k, v) VALUES('schema_v1', '1')");
}

function get_meta($key) {
    $stmt = db()->prepare('SELECT v FROM meta WHERE k = ?');
    $stmt->execute(array($key));
    $v = $stmt->fetchColumn();
    return $v === false ? null : $v;
}

function set_meta($key, $value) {
    $stmt = db()->prepare('REPLACE INTO meta(k, v) VALUES (?, ?)');
    $stmt->execute(array($key, $value));
}

function is_banned($device) {
    $stmt = db()->prepare('SELECT banned_until FROM malice WHERE device_id = ?');
    $stmt->execute(array($device));
    $until = $stmt->fetchColumn();
    return $until !== false && (int) $until > time();
}

function penalize($device) {
    $pdo = db();
    $stmt = $pdo->prepare('INSERT OR IGNORE INTO malice(device_id, count, banned_until) VALUES (?, 0, 0)');
    $stmt->execute(array($device));
    $stmt = $pdo->prepare('UPDATE malice SET count = count + 1 WHERE device_id = ?');
    $stmt->execute(array($device));
    $stmt = $pdo->prepare('SELECT count FROM malice WHERE device_id = ?');
    $stmt->execute(array($device));
    $count = (int) $stmt->fetchColumn();
    if ($count > (int) cfgv('malice_limit')) {
        $stmt = $pdo->prepare('UPDATE malice SET banned_until = ? WHERE device_id = ?');
        $stmt->execute(array(time() + (int) cfgv('ban_seconds'), $device));
    }
}

function queue_pending($device, $kind, $payload) {
    $stmt = db()->prepare('INSERT INTO pending(device_id, kind, payload_json, created_at) VALUES (?, ?, ?, ?)');
    $stmt->execute(array($device, $kind, json_encode($payload), time()));
}

function publish_spirit($device, $record, $meta) {
    $stmt = db()->prepare('INSERT INTO spirits(device_id, depth, hero_class, level, username, epitaph, record_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
    $stmt->execute(array(
        $device,
        $meta['depth'],
        $meta['hero_class'],
        $meta['level'],
        $meta['username'],
        isset($record['epitaph']) ? $record['epitaph'] : '',
        json_encode($record),
        time(),
    ));
}

function publish_victory($device, $username, $speech) {
    $stmt = db()->prepare('INSERT INTO victory(device_id, username, speech, created_at) VALUES (?, ?, ?, ?)');
    $stmt->execute(array($device, $username, $speech, time()));
}

function publish_epitaph($device, $name, $text) {
    $stmt = db()->prepare('INSERT INTO epitaphs(device_id, name, text, created_at) VALUES (?, ?, ?, ?)');
    $stmt->execute(array($device, $name, $text, time()));
}

function judged_text($name, $text) {
    return trim($text) !== '' ? $text : $name;
}

function flush_pending() {
    $pdo = db();
    $rows = $pdo->query('SELECT id, device_id, kind, payload_json FROM pending ORDER BY id LIMIT 20')->fetchAll();
    foreach ($rows as $row) {
        if (get_meta('ai_paused') === '1') return;
        $payload = json_decode($row['payload_json'], true);
        if (!is_array($payload)) continue;
        $kind = $row['kind'];
        if ($kind === 'spirit') {
            $name = isset($payload['username']) ? $payload['username'] : '';
            $text = isset($payload['epitaph']) ? $payload['epitaph'] : '';
        } elseif ($kind === 'epitaph') {
            $name = isset($payload['name']) ? $payload['name'] : '';
            $text = isset($payload['text']) ? $payload['text'] : '';
        } else {
            $name = isset($payload['username']) ? $payload['username'] : '';
            $text = isset($payload['speech']) ? $payload['speech'] : '';
        }
        $result = moderate($name, judged_text($name, $text));
        if ($result === 'insufficient') { set_meta('ai_paused', '1'); return; }
        if ($result === 'failed') continue;

        if ($result === 'approved') {
            if ($kind === 'spirit') {
                $meta = array(
                    'depth' => isset($payload['depth']) ? $payload['depth'] : 0,
                    'hero_class' => isset($payload['class']) ? $payload['class'] : null,
                    'level' => isset($payload['level']) ? $payload['level'] : 1,
                    'username' => $name,
                );
                publish_spirit($row['device_id'], $payload, $meta);
            } elseif ($kind === 'epitaph') {
                publish_epitaph($row['device_id'], $name, $text);
            } else {
                publish_victory($row['device_id'], $name, $text);
            }
        } else {
            penalize($row['device_id']);
        }
        $stmt = $pdo->prepare('DELETE FROM pending WHERE id = ?');
        $stmt->execute(array($row['id']));
    }
}

function moderate_or_queue($kind, $device, $name, $text, $payload) {
    if (is_banned($device)) return 'banned';

    $result = moderate($name, judged_text($name, $text));
    if ($result === 'insufficient') {
        set_meta('ai_paused', '1');
        queue_pending($device, $kind, $payload);
        return 'pending';
    }
    if ($result === 'failed') {
        queue_pending($device, $kind, $payload);
        return 'pending';
    }

    if (get_meta('ai_paused') === '1') {
        set_meta('ai_paused', '0');
        flush_pending();
    }

    if ($result === 'rejected') {
        penalize($device);
        return 'rejected';
    }
    return 'approved';
}

function too_many_uploads($device) {
    $stmt = db()->prepare('SELECT COUNT(*) FROM upload_log WHERE device_id = ? AND created_at >= ?');
    $stmt->execute(array($device, time() - 3600));
    return (int) $stmt->fetchColumn() >= (int) cfgv('max_uploads_per_hour');
}

function note_upload($device) {
    $pdo = db();
    $stmt = $pdo->prepare('INSERT INTO upload_log(device_id, created_at) VALUES (?, ?)');
    $stmt->execute(array($device, time()));
    if (mt_rand(1, 100) === 1) {
        $stmt = $pdo->prepare('DELETE FROM upload_log WHERE created_at < ?');
        $stmt->execute(array(time() - 7200));
    }
}
