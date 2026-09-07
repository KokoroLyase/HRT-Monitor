/* 激素监测（Web）—— 启动：路由 / 主题 / 语言 / 弹窗 / 首启引导 / PWA */
(function () {
  'use strict';
  const HRT = window.HRT;
  const doc = document;

  HRT.VERSION = '1.1.1';

  function $(sel, root) { return (root || doc).querySelector(sel); }
  function $all(sel, root) { return Array.prototype.slice.call((root || doc).querySelectorAll(sel)); }

  const state = HRT.state = {
    data: null,
    prefs: null,
    chart: { h: 'e2', rangeDays: Infinity, unit: '', selected: null },
    rangeH: 'e2',
    route: 'home'
  };

  /* ---------- 主题 / 语言 ---------- */
  function detectLang(prefs) {
    if (prefs.lang === 'zh-CN' || prefs.lang === 'zh-TW') return prefs.lang;
    const nav = (navigator.language || 'en').toLowerCase();
    if (/^zh[-_](tw|hk|mo)/i.test(nav)) return 'zh-TW';
    return 'zh-CN';
  }

  HRT.applyLang = function () {
    HRT.lang = detectLang(state.prefs);
    doc.documentElement.setAttribute('lang', HRT.lang);
    updateNavLabels();
    HRT.render();
  };

  HRT.applyTheme = function () {
    let dark;
    if (state.prefs.theme === 'dark') dark = true;
    else if (state.prefs.theme === 'light') dark = false;
    else dark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    doc.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    const meta = $('meta[name=theme-color]');
    if (meta) meta.setAttribute('content', dark ? '#151119' : '#FBF8FE');
  };

  function updateNavLabels() {
    const map = { home: 'nav_home', charts: 'nav_charts', timeline: 'nav_timeline', report: 'nav_report', settings: 'nav_settings' };
    Object.keys(map).forEach(function (k) {
      const el = $('#nav-' + k + ' .nav-label');
      if (el) el.textContent = HRT.t(map[k]);
    });
  }

  /* ---------- 路由 ---------- */
  function parseRoute() {
    const h = (location.hash || '').replace(/^#\/?/, '');
    return h || 'home';
  }

  HRT.render = function () {
    if (HRT._chartResize) {
      window.removeEventListener('resize', HRT._chartResize);
      HRT._chartResize = null;
    }
    const route = parseRoute();
    state.route = route;
    const data = state.data;
    const prefs = state.prefs;
    const main = $('#main');

    switch (route) {
      case 'home': main.innerHTML = HRT.renderHome(data, prefs); HRT.bindHome(); break;
      case 'history': main.innerHTML = HRT.renderHistory(data, prefs); HRT.bindHistory(); break;
      case 'charts': main.innerHTML = HRT.renderCharts(data); HRT.bindCharts(data); break;
      case 'timeline': main.innerHTML = HRT.renderTimeline(data); HRT.bindTimeline(); break;
      case 'report': main.innerHTML = HRT.renderReport(data); HRT.bindReport(data); break;
      case 'settings': main.innerHTML = HRT.renderSettings(data, prefs); HRT.bindSettings(data, prefs); break;
      case 'ranges': main.innerHTML = HRT.renderRanges(data); HRT.bindRanges(data); break;
      default: location.hash = '#/home'; return;
    }
    updateChrome(route);
    main.scrollTop = 0;
  };

  function updateChrome(route) {
    const tb = $('#topbar');
    const backRoutes = { history: 'history_title', ranges: 'ranges_title' };
    if (backRoutes[route]) {
      tb.innerHTML = '<button class="tb-back" id="tb-back" aria-label="back">‹</button><h1>' +
        HRT.esc(HRT.t(backRoutes[route])) + '</h1>';
      $('#tb-back').addEventListener('click', function () {
        location.hash = route === 'ranges' ? '#/settings' : '#/home';
      });
    } else {
      tb.innerHTML = '<h1>' + HRT.esc(HRT.t(route === 'home' ? 'home_title' : 'nav_' + route)) + '</h1>';
    }
    const tabs = ['home', 'charts', 'timeline', 'report', 'settings'];
    tabs.forEach(function (k) {
      const el = $('#nav-' + k);
      if (el) el.classList.toggle('active', k === route);
    });
    const fab = $('#fab');
    if (route === 'home' || route === 'timeline') {
      fab.style.display = 'flex';
    } else {
      fab.style.display = 'none';
    }
  }

  /* ---------- 弹窗 / 提示 ---------- */
  HRT.openModal = function (html) {
    const root = $('#modal-root');
    root.innerHTML = '<div class="modal-overlay" id="modal-ov"><div class="modal">' + html + '</div></div>';
    const ov = $('#modal-ov');
    ov.addEventListener('click', function (e) {
      if (e.target === ov) HRT.closeModal();
    });
    $all('[data-close]', root).forEach(function (el) {
      el.addEventListener('click', HRT.closeModal);
    });
  };

  HRT.closeModal = function () {
    const root = $('#modal-root');
    if (root) root.innerHTML = '';
  };

  HRT.confirm = function (title, text, onOk) {
    HRT.openModal(
      '<h2>' + HRT.esc(title) + '</h2>' +
      (text ? '<p class="muted small">' + HRT.esc(text) + '</p>' : '') +
      '<div class="btn-row"><button class="btn primary" id="btn-confirm-ok">' + HRT.esc(HRT.t('ok')) + '</button>' +
      '<button class="btn" data-close="1">' + HRT.esc(HRT.t('cancel')) + '</button></div>'
    );
    const ok = $('#btn-confirm-ok');
    if (ok) ok.addEventListener('click', function () {
      HRT.closeModal();
      onOk();
    });
  };

  HRT.toast = function (msg) {
    const el = $('#toast');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(el._timer);
    el._timer = setTimeout(function () { el.classList.remove('show'); }, 2400);
  };

  HRT.downloadFile = function (name, content, mime) {
    const blob = new Blob([content], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = doc.createElement('a');
    a.href = url;
    a.download = name;
    doc.body.appendChild(a);
    a.click();
    setTimeout(function () {
      URL.revokeObjectURL(url);
      a.remove();
    }, 1000);
  };

  /* ---------- 首启引导 ---------- */
  function firstRun() {
    if (!state.prefs.agreed) {
      HRT.openModal(
        '<h2>' + HRT.esc(HRT.t('first_launch_title')) + '</h2>' +
        '<div class="disclaimer-text">' + HRT.esc(HRT.t('disclaimer_full')) + '</div>' +
        '<button class="btn primary wide" id="btn-agree">' + HRT.esc(HRT.t('first_launch_agree')) + '</button>'
      );
      const agree = $('#btn-agree');
      if (agree) agree.addEventListener('click', function () {
        state.prefs.agreed = true;
        HRT.savePrefs(state.prefs);
        HRT.closeModal();
        if (!state.prefs.profileAsked) HRT.openProfileEditor(true);
      });
    } else if (!state.prefs.profileAsked) {
      HRT.openProfileEditor(true);
    }
  }

  /* ---------- 启动 ---------- */
  function boot() {
    state.data = HRT.loadData();
    state.prefs = HRT.loadPrefs();
    state.chart.unit = HRT.baseUnit(state.chart.h);
    HRT.lang = detectLang(state.prefs);
    doc.documentElement.setAttribute('lang', HRT.lang);
    updateNavLabels();
    HRT.applyTheme();

    window.addEventListener('hashchange', HRT.render);
    const fab = $('#fab');
    if (fab) fab.addEventListener('click', function () {
      if (state.route === 'home') HRT.openRecordEditor(null);
      else if (state.route === 'timeline') HRT.openEventEditor(null);
    });

    // 底部导航
    $all('#bottomnav .nav-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const target = btn.getAttribute('data-route');
        if (target) location.hash = target;
      });
    });

    // 复制按钮（委托）
    doc.addEventListener('click', function (e) {
      const btn = e.target.closest ? e.target.closest('[data-copy]') : null;
      if (!btn) return;
      const pre = btn.parentElement ? btn.parentElement.querySelector('pre') : null;
      if (!pre) return;
      const text = pre.textContent || '';
      const done = function () { HRT.toast(HRT.t('copied')); };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () { fallbackCopy(text, done); });
      } else {
        fallbackCopy(text, done);
      }
    });

    function fallbackCopy(text, done) {
      const ta = doc.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      doc.body.appendChild(ta);
      ta.select();
      try { doc.execCommand('copy'); done(); } catch (err) { /* 忽略 */ }
      ta.remove();
    }

    HRT.render();
    firstRun();

    if ('serviceWorker' in navigator &&
      (location.protocol === 'https:' || location.hostname === 'localhost' || location.hostname === '127.0.0.1')) {
      navigator.serviceWorker.register('sw.js').catch(function () { /* 忽略注册失败 */ });
    }
  }

  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
