var I18N = {
  zh: {
    window_title: "Darkest Pixel Dungeon - 官方网站",
    menu_file: "文件(F)",
    menu_edit: "编辑(E)",
    menu_view: "查看(V)",
    menu_help: "帮助(H)",
    subtitle: "黑暗像素地牢 · 黑暗奇幻 Roguelike",
    marquee: "欢迎来到暗黑像素地牢官方网站！六合大枪正在地牢中等待……",
    download_title: "下载游戏",
    download_hint: "全部版本请前往 GitHub Releases 页面下载。",
    download_android: "Android 版（APK）",
    download_windows: "Windows 版（.exe）",
    download_jar: "桌面版（.jar）",
    stats_title: "服务器统计",
    stats_total: "红灵总数",
    stats_recent: "近 24 小时新增",
    stats_classes: "职业分布",
    recent_title: "最近的红灵",
    epitaph_title: "最近的遗言",
    victory_title: "最近的胜利感言",
    col_name: "角色名",
    col_class: "职业",
    col_level: "等级",
    col_depth: "深度",
    col_time: "时间",
    col_epitaph: "遗言",
    col_speech: "感言",
    links_title: "相关链接",
    link_qq: "玩家 QQ 群：818725226",
    link_email: "作者邮箱：xixi012.c@gmail.com",
    footer_view: "本站最佳浏览分辨率 800×600，建议使用 Internet Explorer 5.0 以上浏览器",
    footer_rights: "保留所有权利",
    loading: "加载中……",
    stats_failed: "统计服务暂时不可用",
    empty: "暂无数据"
  },
  zh_TW: {
    window_title: "Darkest Pixel Dungeon - 官方網站",
    menu_file: "檔案(F)",
    menu_edit: "編輯(E)",
    menu_view: "檢視(V)",
    menu_help: "說明(H)",
    subtitle: "黑暗像素地牢 · 黑暗奇幻 Roguelike",
    marquee: "歡迎來到暗黑像素地牢官方網站！紅靈正在地牢中徘徊……",
    download_title: "下載遊戲",
    download_hint: "全部版本請前往 GitHub Releases 頁面下載。",
    download_android: "Android 版（APK）",
    download_windows: "Windows 版（.exe）",
    download_jar: "桌面版（.jar）",
    stats_title: "伺服器統計",
    stats_total: "紅靈總數",
    stats_recent: "近 24 小時新增",
    stats_classes: "職業分佈",
    recent_title: "最近的紅靈",
    epitaph_title: "最近的遺言",
    victory_title: "最近的勝利感言",
    col_name: "角色名",
    col_class: "職業",
    col_level: "等級",
    col_depth: "深度",
    col_time: "時間",
    col_epitaph: "遺言",
    col_speech: "感言",
    links_title: "相關連結",
    link_qq: "玩家 QQ 群：818725226",
    link_email: "作者信箱：xixi012.c@gmail.com",
    footer_view: "本站最佳瀏覽解析度 800×600，建議使用 Internet Explorer 5.0 以上瀏覽器",
    footer_rights: "保留所有權利",
    loading: "載入中……",
    stats_failed: "統計服務暫時無法使用",
    empty: "暫無資料"
  },
  en: {
    window_title: "Darkest Pixel Dungeon - Official Site",
    menu_file: "File(F)",
    menu_edit: "Edit(E)",
    menu_view: "View(V)",
    menu_help: "Help(H)",
    subtitle: "A dark fantasy pixel roguelike",
    marquee: "Welcome to the official Darkest Pixel Dungeon site! Red spirits are roaming the dungeon...",
    download_title: "Download",
    download_hint: "All builds are available on the GitHub Releases page.",
    download_android: "Android (APK)",
    download_windows: "Windows (.exe)",
    download_jar: "Desktop (.jar)",
    stats_title: "Server Stats",
    stats_total: "Total red spirits",
    stats_recent: "Uploaded in last 24h",
    stats_classes: "Class breakdown",
    recent_title: "Recent Red Spirits",
    epitaph_title: "Recent Epitaphs",
    victory_title: "Recent Victory Words",
    col_name: "Hero",
    col_class: "Class",
    col_level: "Level",
    col_depth: "Depth",
    col_time: "Time",
    col_epitaph: "Epitaph",
    col_speech: "Speech",
    links_title: "Links",
    link_qq: "QQ group: 818725226",
    link_email: "Contact: xixi012.c@gmail.com",
    footer_view: "Best viewed at 800×600 with Internet Explorer 5.0 or later",
    footer_rights: "All rights reserved",
    loading: "Loading...",
    stats_failed: "Stats are temporarily unavailable",
    empty: "No data yet"
  }
};

var CLASS_NAMES = {
  WARRIOR: { zh: "战士", zh_TW: "戰士", en: "Warrior" },
  MAGE: { zh: "法师", zh_TW: "法師", en: "Mage" },
  ROGUE: { zh: "盗贼", zh_TW: "盜賊", en: "Rogue" },
  HUNTRESS: { zh: "女猎手", zh_TW: "女獵手", en: "Huntress" },
  SORCERESS: { zh: "女巫", zh_TW: "女巫", en: "Sorceress" },
  EXILE: { zh: "流放者", zh_TW: "流放者", en: "Exile" },
  UNKNOWN: { zh: "未知", zh_TW: "未知", en: "Unknown" }
};

var lang = "zh";

function t(key) {
  return (I18N[lang] && I18N[lang][key]) || I18N.zh[key] || key;
}

function className(code) {
  var entry = CLASS_NAMES[code] || CLASS_NAMES.UNKNOWN;
  return entry[lang] || entry.en;
}

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function applyLang(next) {
  lang = I18N[next] ? next : "zh";
  document.documentElement.lang = lang === "zh_TW" ? "zh-Hant" : (lang === "en" ? "en" : "zh-CN");

  var nodes = document.querySelectorAll("[data-i18n]");
  for (var i = 0; i < nodes.length; i++) {
    nodes[i].textContent = t(nodes[i].getAttribute("data-i18n"));
  }

  var links = document.querySelectorAll("[data-lang]");
  for (var j = 0; j < links.length; j++) {
    links[j].style.fontWeight = links[j].getAttribute("data-lang") === lang ? "bold" : "normal";
  }
}

function renderStats(data) {
  document.getElementById("stat-total").textContent = data.total;
  document.getElementById("stat-recent").textContent = data.recent_24h;

  var parts = [];
  var codes = Object.keys(data.by_class || {});
  for (var i = 0; i < codes.length; i++) {
    parts.push(className(codes[i]) + " " + data.by_class[codes[i]]);
  }
  document.getElementById("stat-classes").textContent = parts.length ? parts.join(" / ") : t("empty");

  var tbody = document.querySelector("#recent-table tbody");
  tbody.innerHTML = "";
  var recent = data.recent || [];
  if (!recent.length) {
    var emptyRow = document.createElement("tr");
    var emptyCell = document.createElement("td");
    emptyCell.colSpan = 5;
    emptyCell.textContent = t("empty");
    emptyRow.appendChild(emptyCell);
    tbody.appendChild(emptyRow);
    return;
  }

  for (var k = 0; k < recent.length; k++) {
    var item = recent[k];
    var row = document.createElement("tr");
    var cells = [
      escapeHtml(item.username || ""),
      escapeHtml(className(item.hero_class)),
      escapeHtml(item.level),
      escapeHtml(item.depth),
      escapeHtml(new Date((item.created_at || 0) * 1000).toLocaleString())
    ];
    for (var c = 0; c < cells.length; c++) {
      var td = document.createElement("td");
      td.innerHTML = cells[c];
      row.appendChild(td);
    }
    tbody.appendChild(row);
  }
}

function loadStats() {
  var xhr = new XMLHttpRequest();
  xhr.open("GET", "/api/v1/stats", true);
  xhr.onreadystatechange = function () {
    if (xhr.readyState !== 4) return;
    if (xhr.status >= 200 && xhr.status < 300) {
      try {
        renderStats(JSON.parse(xhr.responseText));
      } catch (e) {
        document.getElementById("recent-status").textContent = t("stats_failed");
      }
    } else {
      document.getElementById("recent-status").textContent = t("stats_failed");
    }
  };
  xhr.send();
}

function renderList(tableId, rows, textKey) {
  var tbody = document.querySelector("#" + tableId + " tbody");
  tbody.innerHTML = "";

  if (!rows || !rows.length) {
    var emptyRow = document.createElement("tr");
    var emptyCell = document.createElement("td");
    emptyCell.colSpan = 3;
    emptyCell.textContent = t("empty");
    emptyRow.appendChild(emptyCell);
    tbody.appendChild(emptyRow);
    return;
  }

  for (var i = 0; i < rows.length; i++) {
    var item = rows[i];
    var row = document.createElement("tr");
    var cells = [
      escapeHtml(item.name || ""),
      escapeHtml(item[textKey] || ""),
      escapeHtml(new Date((item.created_at || 0) * 1000).toLocaleString())
    ];
    for (var c = 0; c < cells.length; c++) {
      var td = document.createElement("td");
      td.innerHTML = cells[c];
      row.appendChild(td);
    }
    tbody.appendChild(row);
  }
}

function loadList(url, tableId, collectionKey, textKey) {
  var xhr = new XMLHttpRequest();
  xhr.open("GET", url, true);
  xhr.onreadystatechange = function () {
    if (xhr.readyState !== 4) return;
    var rows = [];
    if (xhr.status >= 200 && xhr.status < 300) {
      try {
        rows = JSON.parse(xhr.responseText)[collectionKey] || [];
      } catch (e) {
        rows = [];
      }
    }
    renderList(tableId, rows, textKey);
  };
  xhr.send();
}

(function () {
  var links = document.querySelectorAll("[data-lang]");
  for (var i = 0; i < links.length; i++) {
    links[i].addEventListener("click", function (event) {
      event.preventDefault();
      applyLang(this.getAttribute("data-lang"));
    });
  }
  applyLang(lang);
  loadStats();
  loadList("/api/v1/epitaphs/recent?n=20", "epitaph-table", "epitaphs", "text");
  loadList("/api/v1/victory/recent?n=20", "victory-table", "victory", "speech");
})();
