<?php

return array(
    'db_path' => dirname(dirname(dirname(__FILE__))) . '/data/spirits.db',

    'admin_token' => 'CHANGE_ME',

    'ai_base_url' => 'https://api.deepseek.com/v1',
    'ai_api_key' => 'CHANGE_ME',
    'ai_model' => 'deepseek-chat',
    'ai_timeout' => 10,

    'max_body_bytes' => 65536,
    'max_uploads_per_hour' => 30,
    'max_epitaph' => 200,
    'max_speech' => 300,
    'malice_limit' => 10,
    'ban_seconds' => 172800,

    'suggestion_max_length' => 1000,
    'suggestion_max_per_hour' => 5,
    'suggestion_cooldown_seconds' => 60,

    'smtp_host' => 'smtp.feishu.cn',
    'smtp_port' => 465,
    'smtp_secure' => 'ssl',
    'smtp_user' => 'CHANGE_ME@example.com',
    'smtp_pass' => 'CHANGE_ME',
    'smtp_to' => 'CHANGE_ME@example.com',
);
