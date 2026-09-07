/* 激素监测（Web）—— 界面渲染（全部页面与编辑弹窗） */
(function (global) {
  'use strict';
  const HRT = global.HRT = global.HRT || {};
  const doc = global.document;

  function $(sel, root) { return (root || doc).querySelector(sel); }
  function $all(sel, root) { return Array.prototype.slice.call((root || doc).querySelectorAll(sel)); }

  HRT.esc = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  };
  const esc = HRT.esc;
  const t = function () { return HRT.t.apply(null, arguments); };

  /* ---------- 通用组件 ---------- */
  function statusChip(status, targetLabel) {
    const cls = status === 'LOW' ? 'low' : status === 'HIGH' ? 'high' : status === 'IN' ? 'in' : 'unknown';
    const label = t(status === 'LOW' ? 'low' : status === 'HIGH' ? 'high' : status === 'IN' ? 'in_range' : 'unknown');
    return '<span class="chip chip-' + cls + '">' + esc(label) + '</span>' +
      (targetLabel ? '<span class="chip-target">' + esc(targetLabel) + '</span>' : '');
  }

  function latestOf(data, hKey) {
    for (const r of data.records) {
      for (const m of r.items) if (m.h === hKey) return { m: m, r: r };
    }
    return null;
  }

  function recordSummary(r) {
    return r.items.map(function (m) {
      return m.h.toUpperCase() + ' ' + HRT.fmt(m.v) + ' ' + m.u;
    }).join(' · ');
  }

  function recordRowHtml(r, prefs) {
    const meta = HRT.metaLine(r.date, prefs);
    return '<div class="card rec-row" data-id="' + esc(r.id) + '">' +
      '<div class="rec-top"><span class="rec-date">' + esc(HRT.fmtDate(r.date)) + '</span>' +
      '<span class="rec-rel">' + esc(HRT.relativeDay(r.date)) + '</span></div>' +
      '<div class="rec-sum">' + esc(recordSummary(r)) + '</div>' +
      (r.note ? '<div class="rec-note">' + esc(r.note) + '</div>' : '') +
      (meta ? '<div class="rec-meta">' + esc(meta) + '</div>' : '') +
      '</div>';
  }

  /* ---------- 首页 ---------- */
  HRT.renderHome = function (data, prefs) {
    let cards = '';
    HRT.HORMONES.forEach(function (h) {
      const l = latestOf(data, h.key);
      const target = HRT.targetOf(data, h.key);
      let valHtml;
      if (l) {
        const base = HRT.toBase(h.key, l.m.v, l.m.u);
        valHtml = '<div class="hcard-value">' + esc(HRT.fmt(l.m.v) + ' ' + l.m.u) + '</div>' +
          '<div class="hcard-date">' + esc(HRT.relativeDay(l.r.date) + ' · ' + HRT.fmtDate(l.r.date)) + '</div>';
        valHtml += '<div class="hcard-status">' +
          statusChip(HRT.statusOf(base, target), target ? HRT.rangeLabel(target) : '') + '</div>';
      } else {
        valHtml = '<div class="hcard-value muted">' + esc(t('not_tested')) + '</div>';
      }
      cards += '<div class="card hcard">' +
        '<div class="hcard-name">' + esc(t(HRT.hormoneLabelKey(h.key))) + '</div>' + valHtml + '</div>';
    });

    let recent = '';
    if (data.records.length) {
      recent = data.records.slice(0, 5).map(function (r) { return recordRowHtml(r, prefs); }).join('');
    } else {
      recent = '<div class="card empty-card"><p>' + esc(t('no_records_yet')) + '</p>' +
        '<p class="muted">' + esc(t('no_records_hint')) + '</p></div>';
    }

    return '<div class="page">' +
      '<div class="home-header"><h1>' + esc(t('home_title')) + '</h1>' +
      '<p class="subtitle">' + esc(t('home_subtitle')) + '</p></div>' +
      '<div class="card-grid">' + cards + '</div>' +
      '<section class="recent">' +
      '<div class="row-between"><h2>' + esc(t('recent_records')) + '</h2>' +
      (data.records.length ? '<button class="link" id="btn-all">' + esc(t('all_records')) + '</button>' : '') +
      '</div>' + recent + '</section></div>';
  };

  HRT.bindHome = function () {
    const btn = $('#btn-all');
    if (btn) btn.addEventListener('click', function () { global.location.hash = '#/history'; });
  };

  /* ---------- 历史记录 ---------- */
  HRT.renderHistory = function (data, prefs) {
    let list;
    if (data.records.length) {
      list = '<p class="muted">' + esc(t('count_items', data.records.length)) + '</p>' +
        data.records.map(function (r) { return recordRowHtml(r, prefs); }).join('');
    } else {
      list = '<div class="card empty-card"><p>' + esc(t('history_empty')) + '</p></div>';
    }
    return '<div class="page"><div class="page-head"><h1>' + esc(t('history_title')) + '</h1>' +
      '<button class="btn small" id="btn-history-add">＋ ' + esc(t('add_record')) + '</button></div>' + list + '</div>';
  };

  HRT.bindHistory = function () {
    const add = $('#btn-history-add');
    if (add) add.addEventListener('click', function () { HRT.openRecordEditor(null); });
    $all('.rec-row').forEach(function (el) {
      el.addEventListener('click', function () {
        const id = el.getAttribute('data-id');
        const rec = HRT.state.data.records.filter(function (r) { return r.id === id; })[0];
        if (rec) HRT.openRecordEditor(rec);
      });
    });
  };

  /* ---------- 图表 ---------- */
  HRT.renderCharts = function (data) {
    const c = HRT.state.chart;
    const hKey = c.h;
    const chips = HRT.HORMONES.map(function (h) {
      return '<button class="chip-btn' + (h.key === hKey ? ' active' : '') + '" data-h="' + h.key + '">' + esc(h.short) + '</button>';
    }).join('');

    const rangeOpts = [
      { d: Infinity, k: 'chart_range_all' }, { d: 365, k: 'chart_range_1y' },
      { d: 183, k: 'chart_range_6m' }, { d: 92, k: 'chart_range_3m' }
    ];
    const rangeChips = rangeOpts.map(function (o) {
      return '<button class="chip-btn' + (c.rangeDays === o.d ? ' active' : '') + '" data-days="' + o.d + '">' + esc(t(o.k)) + '</button>';
    }).join('');

    const unitOpts = HRT.UNITS[hKey].map(function (u) {
      return '<option value="' + esc(u.label) + '"' + (u.label === c.unit ? ' selected' : '') + '>' + esc(u.label) + '</option>';
    }).join('');

    return '<div class="page">' +
      '<div class="page-head"><h1>' + esc(t('chart_title')) + '</h1></div>' +
      '<div class="chips" id="chip-hormones">' + chips + '</div>' +
      '<div class="chips" id="chip-ranges">' + rangeChips +
      '<label class="unit-select">' + esc(t('chart_unit')) + '：<select id="chart-unit">' + unitOpts + '</select></label>' +
      '</div>' +
      '<p class="hint" id="chart-hint">' + esc(t('chart_tap_hint')) + '</p>' +
      '<div class="chart-wrap"><canvas id="chart-canvas"></canvas></div>' +
      '<div id="chart-point-info"></div>' +
      '<div id="chart-legend"></div>' +
      '<div id="chart-stats"></div>' +
      '</div>';
  };

  /* 图表数据点（按时间升序，基准值） */
  HRT.chartPoints = function (data, c) {
    const cutoff = c.rangeDays === Infinity ? -Infinity : (Date.now() - c.rangeDays * 86400000);
    const pts = [];
    data.records.forEach(function (r) {
      if (r.date < cutoff) return;
      r.items.forEach(function (m) {
        if (m.h === c.h) pts.push({ t: r.date, v: HRT.toBase(c.h, m.v, m.u) });
      });
    });
    pts.sort(function (a, b) { return a.t - b.t; });
    return pts;
  };

  HRT.renderChartExtras = function (data) {
    const c = HRT.state.chart;
    const pts = HRT.chartPoints(data, c);
    const ranges = data.ranges.filter(function (r) { return r.h === c.h && r.visible; });
    const target = HRT.targetOf(data, c.h);

    // 图例
    let legend;
    if (!ranges.length) {
      legend = '<p class="muted">' + esc(t('chart_no_ranges')) + '</p>';
    } else {
      legend = '<h3>' + esc(t('chart_legend')) + '</h3>' + ranges.map(function (r) {
        return '<div class="legend-row"><span class="dot" style="background:' + HRT.bandColor(r) + '"></span>' +
          '<span class="legend-text">' + esc(HRT.rangeLabel(r) + '  ' + HRT.rangeSpanText(r)) + '</span>' +
          (r.id === (target && target.id) ? '<span class="star">★</span>' : '') + '</div>';
      }).join('');
    }

    // 统计
    let stats = '';
    if (pts.length) {
      const values = pts.map(function (p) { return p.v; });
      const min = Math.min.apply(null, values);
      const max = Math.max.apply(null, values);
      const avg = values.reduce(function (a, b) { return a + b; }, 0) / values.length;
      let ratioHtml = '';
      if (target) {
        const inCount = values.filter(function (v) { return HRT.statusOf(v, target) === 'IN'; }).length;
        ratioHtml = '<div class="stat-cell wide"><span class="stat-label">' + esc(t('stat_in_target')) + '</span>' +
          '<span class="stat-value">' + Math.floor(inCount * 100 / values.length) + '%（' + inCount + '/' + values.length + '）</span></div>';
      }
      stats = '<h3>' + esc(t('chart_stats_title')) + '</h3><div class="stats-grid">' +
        '<div class="stat-cell"><span class="stat-label">' + esc(t('stat_count')) + '</span><span class="stat-value">' + values.length + '</span></div>' +
        '<div class="stat-cell"><span class="stat-label">' + esc(t('stat_min')) + '</span><span class="stat-value">' + esc(HRT.fmt(HRT.fromBase(c.h, min, c.unit))) + '</span></div>' +
        '<div class="stat-cell"><span class="stat-label">' + esc(t('stat_max')) + '</span><span class="stat-value">' + esc(HRT.fmt(HRT.fromBase(c.h, max, c.unit))) + '</span></div>' +
        '<div class="stat-cell"><span class="stat-label">' + esc(t('stat_avg')) + '</span><span class="stat-value">' + esc(HRT.fmt(HRT.fromBase(c.h, avg, c.unit))) + '</span></div>' +
        ratioHtml + '</div>';
    }
    $('#chart-legend').innerHTML = legend;
    $('#chart-stats').innerHTML = stats;
    return { pts: pts, ranges: ranges, target: target };
  };

  HRT.bindCharts = function (data) {
    const c = HRT.state.chart;
    const canvas = $('#chart-canvas');
    if (!canvas) return;

    // 指标 / 时间范围 / 显示单位选择
    $all('#chip-hormones .chip-btn').forEach(function (el) {
      el.addEventListener('click', function () {
        c.h = el.getAttribute('data-h');
        c.selected = null;
        c.unit = HRT.baseUnit(c.h);
        HRT.render();
      });
    });
    $all('#chip-ranges .chip-btn').forEach(function (el) {
      el.addEventListener('click', function () {
        c.rangeDays = parseFloat(el.getAttribute('data-days'));
        c.selected = null;
        HRT.render();
      });
    });
    const unitSel = $('#chart-unit');
    if (unitSel) unitSel.addEventListener('change', function () {
      c.unit = unitSel.value;
      HRT.render();
    });

    let hitTest = null;
    const extra = HRT.renderChartExtras(data);
    const dark = (doc.documentElement.getAttribute('data-theme') || '') === 'dark';

    const draw = function () {
      const points = HRT.chartPoints(data, HRT.state.chart);
      if (!points.length) {
        canvas.style.display = 'none';
        $('#chart-hint').textContent = t('chart_no_data');
        $('#chart-point-info').innerHTML = '';
        $('#chart-legend').innerHTML = '';
        $('#chart-stats').innerHTML = '';
        return;
      }
      canvas.style.display = 'block';
      $('#chart-hint').textContent = t('chart_tap_hint');
      hitTest = HRT.drawChart(canvas, points, extra.ranges, extra.target, c.h, c.unit, c.selected, dark);
      const info = $('#chart-point-info');
      if (c.selected !== null && c.selected !== undefined && points[c.selected]) {
        const p = points[c.selected];
        const status = HRT.statusOf(p.v, extra.target);
        info.innerHTML = '<div class="card point-info">' +
          '<span>' + esc(t('chart_value_at', HRT.fmtDate(p.t), HRT.fmt(HRT.fromBase(c.h, p.v, c.unit)), c.unit)) + '</span>' +
          statusChip(status, '') + '</div>';
      } else {
        info.innerHTML = '';
      }
    };

    canvas.addEventListener('click', function (e) {
      if (!hitTest) return;
      const idx = hitTest(e.clientX, e.clientY);
      c.selected = idx;
      draw();
    });
    if (HRT._chartResize) global.removeEventListener('resize', HRT._chartResize);
    HRT._chartResize = draw;
    global.addEventListener('resize', draw);
    draw();
  };

  /* ---------- 时间线 ---------- */
  HRT.renderTimeline = function (data) {
    let list;
    if (data.events.length) {
      list = '<p class="muted">' + esc(t('count_items', data.events.length)) + '</p>' + data.events.map(function (e) {
        return '<div class="card ev-row" data-id="' + esc(e.id) + '">' +
          '<div class="ev-icon">' + esc(t('event_type_' + e.type).slice(0, 1)) + '</div>' +
          '<div class="ev-body"><div class="ev-title">' + esc(t('event_type_' + e.type)) + '</div>' +
          '<div class="ev-date">' + esc(HRT.fmtDate(e.date)) + '</div>' +
          (e.note ? '<div class="ev-note">' + esc(e.note) + '</div>' : '') + '</div></div>';
      }).join('');
    } else {
      list = '<div class="card empty-card"><p>' + esc(t('timeline_empty')) + '</p>' +
        '<p class="muted">' + esc(t('timeline_empty_hint')) + '</p></div>';
    }
    return '<div class="page"><div class="page-head"><h1>' + esc(t('timeline_title')) + '</h1></div>' + list + '</div>';
  };

  HRT.bindTimeline = function () {
    $all('.ev-row').forEach(function (el) {
      el.addEventListener('click', function () {
        const id = el.getAttribute('data-id');
        const ev = HRT.state.data.events.filter(function (e) { return e.id === id; })[0];
        if (ev) HRT.openEventEditor(ev);
      });
    });
  };

  /* ---------- 报告 ---------- */
  HRT.renderReport = function (data) {
    const aiConfigured = HRT.state.prefs.aiBaseUrl && HRT.state.prefs.aiKey && HRT.state.prefs.aiModel;
    let aiSection;
    if (!aiConfigured) {
      aiSection = '<div class="card"><p class="muted">' + esc(t('report_ai_not_configured')) + '</p>' +
        '<button class="btn small" id="btn-go-settings">' + esc(t('report_ai_go_settings')) + '</button></div>';
    } else {
      aiSection = '<button class="btn" id="btn-ai">' + esc(t('report_ai_btn')) + '</button>' +
        '<div id="ai-error" class="err-text"></div><div id="ai-result"></div>';
    }
    return '<div class="page"><div class="page-head"><h1>' + esc(t('report_title')) + '</h1></div>' +
      '<div class="banner-warn">⚠️ ' + esc(t('disclaimer_banner')) + '</div>' +
      '<section><h2>' + esc(t('report_local')) + '</h2>' +
      '<p class="muted">' + esc(t('report_local_desc')) + '</p>' +
      '<button class="btn" id="btn-gen-local">' + esc(t('report_generate')) + '</button>' +
      '<div id="local-result"></div></section>' +
      '<section><h2>' + esc(t('report_ai')) + '</h2>' +
      '<p class="muted">' + esc(t('report_ai_desc')) + '</p>' + aiSection + '</section></div>';
  };

  HRT.bindReport = function (data) {
    const gen = $('#btn-gen-local');
    if (gen) gen.addEventListener('click', function () {
      if (!data.records.length) { HRT.toast(t('report_no_data')); return; }
      const text = HRT.buildLocalReport(data);
      $('#local-result').innerHTML = '<div class="card"><pre class="report-text">' + esc(text) + '</pre>' +
        '<button class="btn small" data-copy="1">' + esc(t('copy')) + '</button></div>';
    });
    const goSettings = $('#btn-go-settings');
    if (goSettings) goSettings.addEventListener('click', function () { global.location.hash = '#/settings'; });

    const ai = $('#btn-ai');
    if (ai) ai.addEventListener('click', function () {
      if (!data.records.length) { HRT.toast(t('report_no_data')); return; }
      ai.disabled = true;
      ai.textContent = t('report_ai_loading');
      $('#ai-error').textContent = '';
      HRT.aiChat(HRT.state.prefs, t('ai_system_prompt'), HRT.buildAiPrompt(data)).then(function (text) {
        $('#ai-result').innerHTML = '<div class="card"><pre class="report-text">' + esc(text) + '</pre>' +
          '<p class="muted small">' + esc(t('report_ai_note')) + '</p>' +
          '<button class="btn small" data-copy="1">' + esc(t('copy')) + '</button></div>';
      }).catch(function (e) {
        $('#ai-error').textContent = t('report_ai_failed', String(e && e.message ? e.message : e));
      }).finally(function () {
        ai.disabled = false;
        ai.textContent = t('report_ai_btn');
      });
    });
  };

  /* ---------- 设置 ---------- */
  HRT.renderSettings = function (data, prefs) {
    const themeOpts = ['system', 'light', 'dark'];
    const langOpts = ['system', 'zh-CN', 'zh-TW'];
    const themeRadios = themeOpts.map(function (m) {
      return '<label class="radio"><input type="radio" name="theme" value="' + m + '"' +
        (prefs.theme === m ? ' checked' : '') + '> ' + esc(t('theme_' + m)) + '</label>';
    }).join('');
    const langRadios = langOpts.map(function (m) {
      return '<label class="radio"><input type="radio" name="lang" value="' + m + '"' +
        (prefs.lang === m ? ' checked' : '') + '> ' + esc(t('lang_' + m.replace('-', '_'))) + '</label>';
    }).join('');

    const version = HRT.VERSION || '1.1.1';

    return '<div class="page"><div class="page-head"><h1>' + esc(t('settings_title')) + '</h1></div>' +
      '<div class="card item" id="s-ranges"><span>' + esc(t('set_ranges')) + '</span><span class="muted">' + esc(t('set_ranges_desc')) + '</span><span class="arrow">›</span></div>' +
      '<div class="card item" id="s-profile"><span>' + esc(t('set_profile')) + '</span><span class="muted">' + esc(t('set_profile_desc')) + '</span><span class="arrow">›</span></div>' +
      '<div class="card"><div class="item-head" id="s-ai-head"><span>' + esc(t('set_ai')) + '</span><span class="muted">' + esc(t('set_ai_desc')) + '</span><span class="arrow" id="s-ai-arrow">▾</span></div>' +
      '<div id="s-ai-body" style="display:none">' +
      '<label class="field">' + esc(t('ai_base_url')) +
      '<input type="text" id="ai-base" placeholder="' + esc(t('ai_base_url_hint')) + '" value="' + esc(prefs.aiBaseUrl) + '"></label>' +
      '<label class="field">' + esc(t('ai_api_key')) +
      '<input type="password" id="ai-key" placeholder="' + esc(t('ai_api_key_hint')) + '" value="' + esc(prefs.aiKey) + '"></label>' +
      '<label class="field">' + esc(t('ai_model')) +
      '<input type="text" id="ai-model" value="' + esc(prefs.aiModel) + '"></label>' +
      '<div class="btn-row"><button class="btn small" id="btn-ai-test">' + esc(t('ai_test')) + '</button>' +
      '<button class="btn small primary" id="btn-ai-save">' + esc(t('ai_save')) + '</button></div>' +
      '</div></div>' +

      '<section><h2>' + esc(t('set_appearance')) + '</h2><div class="card">' +
      '<h3>' + esc(t('set_theme')) + '</h3>' + themeRadios +
      '<h3>' + esc(t('set_language')) + '</h3>' + langRadios +
      '</div></section>' +

      '<section><h2>' + esc(t('set_data')) + '</h2><div class="card">' +
      '<button class="row-btn" id="btn-exp-json">' + esc(t('data_export_json')) + '</button>' +
      '<button class="row-btn" id="btn-exp-csv">' + esc(t('data_export_csv')) + '</button>' +
      '<button class="row-btn" id="btn-import">' + esc(t('data_import')) + '</button>' +
      '<button class="row-btn danger" id="btn-clear">' + esc(t('data_clear')) + '</button>' +
      '<input type="file" id="file-input" accept=".json,application/json" style="display:none">' +
      '</div></section>' +

      '<section><h2>' + esc(t('set_about')) + '</h2><div class="card about-card">' +
      '<p class="muted small">' + esc(t('about_version', version)) + '</p>' +
      '<p><strong>' + esc(t('about_developer')) + '</strong></p>' +
      '<p class="muted">' + esc(t('about_contact')) + '</p>' +
      '<hr>' +
      '<h3>' + esc(t('about_privacy_title')) + '</h3><p class="muted small">' + esc(t('about_privacy_desc')) + '</p>' +
      '<hr>' +
      '<h3>' + esc(t('about_disclaimer_title')) + '</h3><p class="muted small">' + esc(t('disclaimer_full').replace(/\n/g, '<br>')) + '</p>' +
      '<hr><p class="muted small">' + esc(t('about_build')) + ' · ' + esc(t('install_hint')) + '</p>' +
      '</div></section></div>';
  };

  HRT.bindSettings = function (data, prefs) {
    const ranges = $('#s-ranges');
    if (ranges) ranges.addEventListener('click', function () { global.location.hash = '#/ranges'; });
    const profile = $('#s-profile');
    if (profile) profile.addEventListener('click', function () { HRT.openProfileEditor(false); });

    const aiHead = $('#s-ai-head');
    if (aiHead) aiHead.addEventListener('click', function () {
      const body = $('#s-ai-body');
      const arrow = $('#s-ai-arrow');
      const open = body.style.display === 'none';
      body.style.display = open ? 'block' : 'none';
      if (arrow) arrow.textContent = open ? '▴' : '▾';
    });

    const test = $('#btn-ai-test');
    if (test) test.addEventListener('click', function () {
      const p = { aiBaseUrl: $('#ai-base').value, aiKey: $('#ai-key').value, aiModel: $('#ai-model').value };
      test.disabled = true;
      test.textContent = t('ai_config_loading');
      HRT.aiChat(p, '你是连接测试助手。', '请仅回复：OK').then(function () {
        HRT.toast(t('ai_test_ok'));
      }).catch(function (e) {
        HRT.toast(t('ai_test_failed', String(e && e.message ? e.message : e)));
      }).finally(function () {
        test.disabled = false;
        test.textContent = t('ai_test');
      });
    });
    const saveAi = $('#btn-ai-save');
    if (saveAi) saveAi.addEventListener('click', function () {
      prefs.aiBaseUrl = $('#ai-base').value.trim();
      prefs.aiKey = $('#ai-key').value.trim();
      prefs.aiModel = $('#ai-model').value.trim();
      HRT.savePrefs(prefs);
      HRT.toast(t('ai_saved'));
    });

    $all('input[name=theme]').forEach(function (el) {
      el.addEventListener('change', function () {
        if (!el.checked) return;
        prefs.theme = el.value;
        HRT.savePrefs(prefs);
        HRT.applyTheme();
      });
    });
    $all('input[name=lang]').forEach(function (el) {
      el.addEventListener('change', function () {
        if (!el.checked) return;
        prefs.lang = el.value;
        HRT.savePrefs(prefs);
        HRT.applyLang();
      });
    });

    const expJson = $('#btn-exp-json');
    if (expJson) expJson.addEventListener('click', function () {
      HRT.downloadFile('hrt-monitor-backup-' + HRT.fmtDate(Date.now()).replace(/-/g, '') + '.json',
        HRT.serialize(data), 'application/json');
    });
    const expCsv = $('#btn-exp-csv');
    if (expCsv) expCsv.addEventListener('click', function () {
      HRT.downloadFile('hrt-monitor-records-' + HRT.fmtDate(Date.now()).replace(/-/g, '') + '.csv',
        HRT.buildRecordsCsv(data), 'text/csv;charset=utf-8');
    });
    const imp = $('#btn-import');
    const fileInput = $('#file-input');
    if (imp && fileInput) imp.addEventListener('click', function () { fileInput.click(); });
    if (fileInput) fileInput.addEventListener('change', function () {
      const f = fileInput.files && fileInput.files[0];
      fileInput.value = '';
      if (!f) return;
      const reader = new FileReader();
      reader.onload = function () {
        const parsed = HRT.parse(String(reader.result || ''));
        if (!parsed) { HRT.toast(t('data_import_failed', t('import_invalid'))); return; }
        HRT.openImportDialog(parsed);
      };
      reader.onerror = function () { HRT.toast(t('data_import_failed', 'read error')); };
      reader.readAsText(f);
    });
    const clear = $('#btn-clear');
    if (clear) clear.addEventListener('click', function () {
      HRT.confirm(t('clear_confirm'), t('clear_confirm_desc'), function () {
        HRT.state.data = HRT.defaultData();
        HRT.mergeBuiltins(HRT.state.data);
        HRT.saveData(HRT.state.data);
        HRT.toast(t('data_cleared'));
        HRT.render();
      });
    });
  };

  /* ---------- 参考范围管理 ---------- */
  HRT.renderRanges = function (data) {
    const c = HRT.state.rangeH;
    const chips = HRT.HORMONES.map(function (h) {
      return '<button class="chip-btn' + (h.key === c ? ' active' : '') + '" data-h="' + h.key + '">' + esc(h.short) + '</button>';
    }).join('');
    const list = data.ranges.filter(function (r) { return r.h === c; });
    let rows;
    if (!list.length) {
      rows = '<p class="muted">' + esc(t('range_no_ranges')) + '</p>';
    } else {
      const targetId = data.targets[c];
      rows = list.map(function (r) {
        return '<div class="card range-row" data-id="' + esc(r.id) + '">' +
          '<span class="dot" style="background:' + HRT.bandColor(r) + '"></span>' +
          '<div class="range-body">' +
          '<div class="range-title">' + esc(HRT.rangeLabel(r) || t('range_custom')) +
          (r.id === targetId ? ' <span class="star">★</span>' : '') + '</div>' +
          '<div class="muted small">' + esc(HRT.rangeSpanText(r)) + '</div>' +
          '<div class="muted tiny">' + esc(r.builtin ? t('range_builtin') : t('range_custom')) + '</div>' +
          '</div>' +
          '<label class="switch"><input type="checkbox" data-vis="' + esc(r.id) + '"' + (r.visible ? ' checked' : '') + '><span class="slider"></span></label>' +
          (r.builtin ? '' :
            '<button class="icon-btn r-edit" data-id="' + esc(r.id) + '">✎</button>' +
            '<button class="icon-btn r-del" data-id="' + esc(r.id) + '">🗑</button>') +
          '</div>';
      }).join('');
    }
    return '<div class="page"><div class="page-head"><h1>' + esc(t('ranges_title')) + '</h1></div>' +
      '<p class="muted small">' + esc(t('ranges_desc')) + '</p>' +
      '<div class="chips" id="chip-rh">' + chips + '</div>' +
      '<div id="range-list">' + rows + '</div>' +
      '<button class="btn" id="btn-add-range">＋ ' + esc(t('range_add')) + '</button></div>';
  };

  HRT.bindRanges = function (data) {
    $all('#chip-rh .chip-btn').forEach(function (el) {
      el.addEventListener('click', function () {
        HRT.state.rangeH = el.getAttribute('data-h');
        HRT.render();
      });
    });
    $all('.range-row').forEach(function (el) {
      const id = el.getAttribute('data-id');
      const range = data.ranges.filter(function (r) { return r.id === id; })[0];
      if (!range) return;
      el.addEventListener('click', function (e) {
        if (e.target.closest('.switch') || e.target.closest('.icon-btn')) return;
        const cur = data.targets[range.h];
        HRT.setTarget(data, range.h, cur === id ? null : id);
        HRT.saveData(data);
        HRT.render();
      });
      const vis = el.querySelector('input[data-vis]');
      if (vis) vis.addEventListener('change', function () {
        HRT.setRangeVisible(data, id, vis.checked);
        HRT.saveData(data);
      });
      const editBtn = el.querySelector('.r-edit');
      if (editBtn) editBtn.addEventListener('click', function () { HRT.openRangeEditor(range); });
      const delBtn = el.querySelector('.r-del');
      if (delBtn) delBtn.addEventListener('click', function () {
        HRT.confirm(t('range_delete_confirm'), '', function () {
          HRT.deleteRange(data, id);
          HRT.saveData(data);
          HRT.toast(t('range_deleted'));
          HRT.render();
        });
      });
    });
    const add = $('#btn-add-range');
    if (add) add.addEventListener('click', function () { HRT.openRangeEditor(null); });
  };

  /* ---------- 弹窗：化验记录编辑 ---------- */
  HRT.openRecordEditor = function (record) {
    const isNew = !record;
    const rec = record || { id: '', date: HRT.todayMillis(), note: '', items: [] };
    let rows = '';
    HRT.HORMONES.forEach(function (h) {
      let found = null;
      rec.items.forEach(function (m) { if (m.h === h.key) found = m; });
      const unit = found ? found.u : HRT.defaultUnit(h.key);
      const unitOpts = HRT.UNITS[h.key].map(function (u) {
        return '<option value="' + esc(u.label) + '"' + (u.label === unit ? ' selected' : '') + '>' + esc(u.label) + '</option>';
      }).join('');
      rows += '<div class="form-card"><div class="form-label">' + esc(t(HRT.hormoneLabelKey(h.key))) + '</div>' +
        '<div class="input-row"><input type="text" inputmode="decimal" class="val-input" id="rec-v-' + h.key + '"' +
        ' value="' + (found ? esc(HRT.fmt(found.v)) : '') + '" placeholder="—">' +
        '<select id="rec-u-' + h.key + '" data-h="' + h.key + '">' + unitOpts + '</select></div>' +
        '<div class="muted tiny" id="rec-hint-' + h.key + '">' + esc(t('record_empty_hint')) + '</div></div>';
    });
    HRT.openModal(
      '<h2>' + esc(t(isNew ? 'record_new' : 'record_edit')) + '</h2>' +
      '<label class="field">' + esc(t('record_date')) +
      '<input type="date" id="rec-date" value="' + esc(HRT.fmtDate(rec.date)) + '"></label>' +
      '<div id="rec-meta" class="muted tiny"></div>' +
      rows +
      '<label class="field">' + esc(t('record_note')) +
      '<textarea id="rec-note" rows="2" placeholder="' + esc(t('record_note_hint')) + '">' + esc(rec.note) + '</textarea></label>' +
      '<div class="btn-row"><button class="btn primary" id="btn-rec-save">' + esc(t('save')) + '</button>' +
      '<button class="btn" data-close="1">' + esc(t('cancel')) + '</button>' +
      (isNew ? '' : '<button class="btn danger" id="btn-rec-del">' + esc(t('delete')) + '</button>') + '</div>'
    );

    const updateHint = function (hKey) {
      const vEl = $('#rec-v-' + hKey);
      const uEl = $('#rec-u-' + hKey);
      const hint = $('#rec-hint-' + hKey);
      const v = parseFloat(String(vEl.value).trim());
      const base = (!isNaN(v) && v > 0) ? HRT.toBase(hKey, v, uEl.value) : null;
      hint.textContent = base !== null
        ? t('record_equals', HRT.fmt(base), HRT.baseUnit(hKey))
        : t('record_empty_hint');
    };
    HRT.HORMONES.forEach(function (h) {
      const vEl = $('#rec-v-' + h.key);
      const uEl = $('#rec-u-' + h.key);
      vEl.addEventListener('input', function () { updateHint(h.key); });
      uEl.addEventListener('change', function () { updateHint(h.key); });
      updateHint(h.key);
    });

    const updateMeta = function () {
      const ms = HRT.parseLocalDate($('#rec-date').value);
      $('#rec-meta').textContent = ms ? HRT.metaLine(ms, HRT.state.prefs) : '';
    };
    $('#rec-date').addEventListener('change', updateMeta);
    updateMeta();

    $('#btn-rec-save').addEventListener('click', function () {
      const date = HRT.parseLocalDate($('#rec-date').value);
      if (!date) { HRT.toast(t('invalid_value')); return; }
      const items = [];
      HRT.HORMONES.forEach(function (h) {
        const raw = String($('#rec-v-' + h.key).value).trim();
        if (!raw) return;
        const v = parseFloat(raw);
        if (isNaN(v) || !isFinite(v) || v <= 0) { HRT.toast(t('invalid_value')); items.push(null); return; }
        items.push({ h: h.key, v: v, u: $('#rec-u-' + h.key).value });
      });
      if (items.some(function (x) { return x === null; })) return;
      if (!items.length) { HRT.toast(t('record_need_one')); return; }
      const nr = { id: isNew ? HRT.uid() : rec.id, date: date, note: String($('#rec-note').value).trim(), items: items };
      HRT.upsertRecord(HRT.state.data, nr);
      HRT.saveData(HRT.state.data);
      HRT.toast(t('record_saved'));
      HRT.closeModal();
      HRT.render();
    });

    const del = $('#btn-rec-del');
    if (del) del.addEventListener('click', function () {
      HRT.confirm(t('delete_record_confirm'), t('delete_record_hint'), function () {
        HRT.deleteRecord(HRT.state.data, rec.id);
        HRT.saveData(HRT.state.data);
        HRT.toast(t('record_deleted'));
        HRT.closeModal();
        HRT.render();
      });
    });
  };

  /* ---------- 弹窗：事件编辑 ---------- */
  HRT.openEventEditor = function (event) {
    const isNew = !event;
    const ev = event || { id: '', date: HRT.todayMillis(), type: 'other', note: '' };
    const chips = HRT.EVENT_TYPES.map(function (k) {
      return '<button class="chip-btn' + (ev.type === k ? ' active' : '') + '" data-type="' + k + '">' +
        esc(t('event_type_' + k)) + '</button>';
    }).join('');
    HRT.openModal(
      '<h2>' + esc(t(isNew ? 'event_new' : 'event_edit')) + '</h2>' +
      '<div class="chips" id="ev-chips">' + chips + '</div>' +
      '<label class="field">' + esc(t('event_date')) +
      '<input type="date" id="ev-date" value="' + esc(HRT.fmtDate(ev.date)) + '"></label>' +
      '<label class="field">' + esc(t('event_note')) +
      '<textarea id="ev-note" rows="2" placeholder="' + esc(t('event_note_hint')) + '">' + esc(ev.note) + '</textarea></label>' +
      '<div class="btn-row"><button class="btn primary" id="btn-ev-save">' + esc(t('save')) + '</button>' +
      '<button class="btn" data-close="1">' + esc(t('cancel')) + '</button>' +
      (isNew ? '' : '<button class="btn danger" id="btn-ev-del">' + esc(t('delete')) + '</button>') + '</div>'
    );
    let curType = ev.type;
    $all('#ev-chips .chip-btn').forEach(function (el) {
      el.addEventListener('click', function () {
        curType = el.getAttribute('data-type');
        $all('#ev-chips .chip-btn').forEach(function (b) { b.classList.toggle('active', b === el); });
      });
    });
    $('#btn-ev-save').addEventListener('click', function () {
      const date = HRT.parseLocalDate($('#ev-date').value);
      if (!date) { HRT.toast(t('invalid_value')); return; }
      HRT.upsertEvent(HRT.state.data, {
        id: isNew ? HRT.uid() : ev.id,
        date: date,
        type: curType,
        note: String($('#ev-note').value).trim()
      });
      HRT.saveData(HRT.state.data);
      HRT.toast(t('event_saved'));
      HRT.closeModal();
      HRT.render();
    });
    const del = $('#btn-ev-del');
    if (del) del.addEventListener('click', function () {
      HRT.confirm(t('delete_event_confirm'), '', function () {
        HRT.deleteEvent(HRT.state.data, ev.id);
        HRT.saveData(HRT.state.data);
        HRT.toast(t('event_deleted'));
        HRT.closeModal();
        HRT.render();
      });
    });
  };

  /* ---------- 弹窗：参考范围编辑 ---------- */
  HRT.openRangeEditor = function (range) {
    const isNew = !range;
    const r = range || { id: '', h: HRT.state.rangeH, label: '', min: '', max: '', unit: HRT.defaultUnit(HRT.state.rangeH), visible: true };
    const unitOpts = HRT.UNITS[r.h].map(function (u) {
      return '<option value="' + esc(u.label) + '"' + (u.label === r.unit ? ' selected' : '') + '>' + esc(u.label) + '</option>';
    }).join('');
    const isTarget = HRT.state.data.targets[r.h] === r.id;
    HRT.openModal(
      '<h2>' + esc(t(isNew ? 'range_add' : 'range_edit')) + '</h2>' +
      '<p class="muted small">' + esc(t(HRT.hormoneLabelKey(r.h))) + '</p>' +
      '<label class="field">' + esc(t('range_name')) +
      '<input type="text" id="rng-name" value="' + esc(r.label || '') + '"></label>' +
      '<div class="input-row"><label class="field">' + esc(t('range_min')) +
      '<input type="text" inputmode="decimal" id="rng-min" value="' + (r.min !== '' ? esc(HRT.fmt(r.min)) : '') + '"></label>' +
      '<label class="field">' + esc(t('range_max')) +
      '<input type="text" inputmode="decimal" id="rng-max" value="' + (r.max !== '' ? esc(HRT.fmt(r.max)) : '') + '"></label></div>' +
      '<label class="field">' + esc(t('range_unit')) +
      '<select id="rng-unit">' + unitOpts + '</select></label>' +
      '<label class="check"><input type="checkbox" id="rng-target"' + (isTarget ? ' checked' : '') + '> ' +
      esc(t('range_target')) + '<span class="muted small">（' + esc(t('range_target_hint')) + '）</span></label>' +
      '<label class="check"><input type="checkbox" id="rng-visible"' + (r.visible ? ' checked' : '') + '> ' +
      esc(t('range_show_chart')) + '</label>' +
      '<div class="btn-row"><button class="btn primary" id="btn-rng-save">' + esc(t('save')) + '</button>' +
      '<button class="btn" data-close="1">' + esc(t('cancel')) + '</button></div>'
    );
    $('#btn-rng-save').addEventListener('click', function () {
      const min = parseFloat(String($('#rng-min').value).trim());
      const max = parseFloat(String($('#rng-max').value).trim());
      if (isNaN(min) || isNaN(max) || !isFinite(min) || !isFinite(max)) { HRT.toast(t('range_invalid_number')); return; }
      if (min >= max) { HRT.toast(t('range_invalid')); return; }
      const nr = {
        id: isNew ? HRT.uid() : r.id,
        h: r.h,
        label: String($('#rng-name').value).trim(),
        min: min,
        max: max,
        unit: $('#rng-unit').value,
        builtin: false,
        visible: $('#rng-visible').checked
      };
      HRT.upsertRange(HRT.state.data, nr);
      if ($('#rng-target').checked) HRT.setTarget(HRT.state.data, r.h, nr.id);
      else if (!isNew && isTarget) HRT.setTarget(HRT.state.data, r.h, null);
      HRT.saveData(HRT.state.data);
      HRT.toast(t('range_saved'));
      HRT.closeModal();
      HRT.render();
    });
  };

  /* ---------- 弹窗：个人资料 ---------- */
  HRT.openProfileEditor = function (allowSkip) {
    const p = HRT.state.prefs;
    HRT.openModal(
      '<h2>' + esc(t('profile_title')) + '</h2>' +
      '<p class="muted small">' + esc(t('profile_hint')) + '</p>' +
      '<label class="field">' + esc(t('profile_birthday')) +
      '<input type="date" id="pf-birthday" value="' + (p.birthday ? esc(HRT.fmtDate(p.birthday)) : '') + '"></label>' +
      '<label class="field">' + esc(t('profile_hrt_start')) +
      '<input type="date" id="pf-hrtstart" value="' + (p.hrtStart ? esc(HRT.fmtDate(p.hrtStart)) : '') + '"></label>' +
      '<div class="btn-row"><button class="btn primary" id="btn-pf-save">' + esc(t('save')) + '</button>' +
      '<button class="btn" id="btn-pf-skip">' + esc(t(allowSkip ? 'profile_skip' : 'cancel')) + '</button></div>'
    );
    $('#btn-pf-save').addEventListener('click', function () {
      p.birthday = HRT.parseLocalDate($('#pf-birthday').value);
      p.hrtStart = HRT.parseLocalDate($('#pf-hrtstart').value);
      p.profileAsked = true;
      HRT.savePrefs(p);
      HRT.toast(t('profile_saved'));
      HRT.closeModal();
      HRT.render();
    });
    $('#btn-pf-skip').addEventListener('click', function () {
      if (allowSkip) {
        p.profileAsked = true;
        HRT.savePrefs(p);
      }
      HRT.closeModal();
      HRT.render();
    });
  };

  /* ---------- 弹窗：导入方式 ---------- */
  HRT.openImportDialog = function (parsed) {
    HRT.openModal(
      '<h2>' + esc(t('data_import_title')) + '</h2>' +
      '<p class="muted small">' + esc(t('data_import_desc')) + '</p>' +
      '<div class="btn-row col"><button class="btn primary" id="btn-imp-merge">' + esc(t('import_merge')) + '</button>' +
      '<button class="btn" id="btn-imp-replace">' + esc(t('import_replace')) + '</button>' +
      '<button class="btn" data-close="1">' + esc(t('cancel')) + '</button></div>'
    );
    const doImport = function (replace) {
      HRT.state.data = HRT.importData(HRT.state.data, parsed, replace);
      HRT.saveData(HRT.state.data);
      const custom = parsed.ranges.length;
      HRT.toast(t('data_import_done', parsed.records.length, parsed.events.length, custom));
      HRT.closeModal();
      HRT.render();
    };
    $('#btn-imp-merge').addEventListener('click', function () { doImport(false); });
    $('#btn-imp-replace').addEventListener('click', function () { doImport(true); });
  };
})(typeof window !== 'undefined' ? window : globalThis);
