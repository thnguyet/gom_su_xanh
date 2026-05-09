(function () {
  'use strict';

  /* ===== CONFIG ===== */
  var file = decodeURIComponent(location.pathname.split('/').pop() || 'index.html');
  var screen = document.querySelector('.screen');
  var screenWrap = document.querySelector('.screen-wrap');

  var pages = {
    home:        'trang-chu_417-354.html',
    login:       'dang-nhap_839-134.html',
    register:    'dang-ky_147-84.html',
    profile:     'ho-so-ca-nhan_147-86.html#profile',
    address:     'ho-so-ca-nhan_147-86.html#address',
    addressForm: 'them-dia-chi_1799-6770.html',
    security:    'ho-so-ca-nhan_147-86.html#security',
    history:     'ho-so-ca-nhan_147-86.html#history',
    workshop:    'trang-workshop-real_1719-3931.html',
    checkout:    'thanh-toan-workshop_1917-3719.html',
    logout:      'ho-so-ca-nhan_147-86.html#logout'
  };

  /* ===== OAUTH2 TOKEN CAPTURE ===== */
  (function () {
    var urlParams = new URLSearchParams(window.location.search);
    var oauthToken = urlParams.get('token');
    if (oauthToken) {
      localStorage.setItem('gsx_token', oauthToken);
      localStorage.setItem('gsx_logged_in', 'true');
      // Clean up URL to hide token
      var cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
      window.history.replaceState({}, document.title, cleanUrl);
      
      var needsUpdate = urlParams.get('needsUpdate') === 'true';

      // Delay toast to ensure DOM is ready
      setTimeout(function() {
        if (typeof toast === 'function') {
          if (needsUpdate) {
            toast('Đăng nhập thành công! Vui lòng cập nhật đầy đủ thông tin cá nhân.');
            // Redirect to profile after a short delay
            setTimeout(function() { go(pages.profile); }, 1500);
          } else {
            toast('Đăng nhập thành công!');
          }
        }
      }, 500);
    }
  })();

  /* ===== AUTH CHECK & CLEAN START (Run early) ===== */
  (function () {
    // Clear legacy/fake account data to ensure a clean start per browser session
    // BUT only if we are not currently capturing an OAuth token from the URL
    var urlParams = new URLSearchParams(window.location.search);
    var hasOAuthToken = urlParams.get('token');
    
    var hasInitiated = sessionStorage.getItem('gsx_app_initiated');
    if (!hasInitiated && !hasOAuthToken) {
      localStorage.removeItem('gsx_logged_in');
      localStorage.removeItem('gsx_token');
      localStorage.removeItem('adm_token');
      sessionStorage.setItem('gsx_app_initiated', 'true');
    } else if (hasOAuthToken) {
      // Mark as initiated if we're coming back with a token
      sessionStorage.setItem('gsx_app_initiated', 'true');
    }

    function check() {
      var publicPages = [
        pages.login, 
        pages.register, 
        'index.html',
        pages.home,
        've-chung-toi_1662-3482.html',
        pages.workshop,
        'trang-workshop_972-1348.html',
        'trang-blog_29-2.html',
        'trang-blog-2_1458-2868.html',
        'faqs_1458-3210.html',
        'huong-dan_275-212.html'
      ];
      var isPublic = false;
      publicPages.forEach(function(p) {
        if (file === p || (p === 'index.html' && file === '')) isPublic = true;
      });

      // Bổ sung: Tất cả các trang sản phẩm cũng là công khai
      if (file.indexOf('trang-san-pham') === 0) isPublic = true;
      if (file.indexOf('trang-san-pham-chi-tiet') === 0) isPublic = true;

      if (!isPublic) {
        var loggedIn = false;
        try {
          var raw = localStorage.getItem('gsx_logged_in');
          loggedIn = raw ? JSON.parse(raw) === true : false;
        } catch(e) {}
        var token = localStorage.getItem('gsx_token');

        if (!loggedIn || !token) {
          location.href = (location.pathname.indexOf('/pages/') === -1 ? 'pages/' : '') + pages.login;
        }
      }
    }
    check();
  })();

  /* ===== HELPERS ===== */
  function isPage(name) {
    return file === name;
  }

  function pageUrl(name) {
    return location.pathname.indexOf('/pages/') === -1 ? 'pages/' + name : name;
  }

  function go(name) {
    // Smooth page transition
    var overlay = document.getElementById('pageTransition');
    if (overlay) {
      overlay.classList.add('is-active');
      setTimeout(function () {
        location.href = pageUrl(name);
      }, 280);
    } else {
      location.href = pageUrl(name);
    }
  }

  function getNumber(value) {
    var parsed = parseFloat(value || '0');
    return isNaN(parsed) ? 0 : parsed;
  }

  /* ===== FIT SCREEN (scale to viewport) ===== */
  var resizeTimer = null;
  function fitScreen() {
    if (!screen || !screenWrap) return;

    var wrapStyle = window.getComputedStyle(screenWrap);
    var paddingX = getNumber(wrapStyle.paddingLeft) + getNumber(wrapStyle.paddingRight);
    var availableWidth = Math.max(1, screenWrap.clientWidth - paddingX);
    var baseWidth = screen.dataset.baseWidth ? Number(screen.dataset.baseWidth) : screen.offsetWidth;
    var baseHeight = screen.dataset.baseHeight ? Number(screen.dataset.baseHeight) : screen.offsetHeight;

    if (!screen.dataset.baseWidth) {
      screen.dataset.baseWidth = String(baseWidth);
      screen.dataset.baseHeight = String(baseHeight);
    }

    var scale = Math.min(1, availableWidth / baseWidth);
    var visualWidth = baseWidth * scale;
    var offset = Math.max(0, (availableWidth - visualWidth) / 2);

    screen.classList.toggle('is-responsive', scale < 1);
    screen.style.transform = 'scale(' + scale + ')';
    screen.style.marginLeft = scale < 1 ? offset + 'px' : '';
    screen.style.marginBottom = scale < 1 ? '-' + (baseHeight - baseHeight * scale) + 'px' : '';
    screenWrap.style.minHeight = Math.ceil(baseHeight * scale) + 'px';
  }

  function debouncedFitScreen() {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(fitScreen, 100);
  }

  /* ===== LOCAL STORAGE ===== */
  function save(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { }
  }

  function load(key, fallback) {
    try {
      var raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) {
      return fallback;
    }
  }

  /* ===== TOAST NOTIFICATION ===== */
  var toastTimer = null;
  function toast(message) {
    var node = document.querySelector('.app-toast');
    if (!node) {
      node = document.createElement('div');
      node.className = 'app-toast';
      document.body.appendChild(node);
    }
    node.textContent = message;
    // Force reflow for re-trigger animation
    node.classList.remove('is-visible');
    void node.offsetWidth;
    node.classList.add('is-visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      node.classList.remove('is-visible');
    }, 2600);
  }

  /* ===== DOM BUILDERS ===== */
  function place(el, box) {
    el.style.position = 'absolute';
    el.style.left = box.left + 'px';
    el.style.top = box.top + 'px';
    el.style.width = box.width + 'px';
    el.style.height = box.height + 'px';
    return el;
  }

  function input(box, attrs) {
    if (!screen) return null;
    var el = document.createElement('input');
    el.className = 'app-field';
    Object.keys(attrs || {}).forEach(function (key) {
      el.setAttribute(key, attrs[key]);
    });
    screen.appendChild(place(el, box));
    return el;
  }

  function check(box, attrs) {
    if (!screen) return null;
    var el = document.createElement('input');
    el.type = 'checkbox';
    el.className = 'app-check';
    Object.keys(attrs || {}).forEach(function (key) {
      el.setAttribute(key, attrs[key]);
    });
    screen.appendChild(place(el, box));
    return el;
  }

  function action(box, label, handler) {
    if (!screen) return null;
    var el = document.createElement('button');
    el.className = 'app-action';
    el.textContent = label;
    el.addEventListener('click', handler);
    screen.appendChild(place(el, box));
    return el;
  }

  /* ===== AUTH STATE ===== */
  function markLoggedIn() {
    save('gsx_logged_in', true);
  }

  function isLoggedIn() {
    var loggedIn = load('gsx_logged_in', false) === true;
    var token = localStorage.getItem('gsx_token');
    return loggedIn && token;
  }

  /* ============================================================
     NAVBAR INJECTION — injects global nav into all Figma pages
     ============================================================ */
  function injectNavbar() {
    // Skip: index page, pages that already have their own navbar (gsxpage),
    // or if already injected
    if (document.querySelector('.index') ||
        document.querySelector('.gsxpage') ||
        file.indexOf('frame-235') !== -1 ||
        document.querySelector('.gsx-nav')) return;

    var inPages = location.pathname.indexOf('/pages/') !== -1;
    var assetBase = inPages ? '../' : '';

    // Detect active nav section from filename
    var activeMap = {
      'trang-chu':         'home',
      've-chung-toi':      'about',
      'trang-workshop':    'workshop',
      've-tranh-gom':      'workshop',
      'thanh-toan':        'workshop',
      'trang-san-pham':    'product',
      'trang-blog':        'blog',
      'faqs':              'blog',
      'huong-dan':         'blog',
      'dang-nhap':         'account',
      'dang-ky':           'account',
      'ho-so-ca-nhan':     'account',
      'bao-mat':           'account',
      'dia-chi':           'account',
      'lich-su':           'account'
    };

    var activeKey = '';
    var lowerFile = file.toLowerCase();
    Object.keys(activeMap).forEach(function(key) {
      if (lowerFile.indexOf(key) === 0) activeKey = activeMap[key];
    });

    function href(page) { return inPages ? page : 'pages/' + page; }

    function mkLink(page, label, key) {
      var cls = 'gsx-nav-link' + (activeKey === key ? ' is-active' : '');
      return '<a class="' + cls + '" href="' + href(page) + '">' + label + '</a>';
    }

    var loggedIn = isLoggedIn();
    var accountLabel = loggedIn ? 'Hồ sơ' : 'Tài khoản';
    var accountPage  = loggedIn ? 'ho-so-ca-nhan_147-86.html#profile' : 'dang-nhap_839-134.html';
    var accountCls   = 'gsx-nav-btn' + (activeKey === 'account' ? ' is-active' : '');

    var html =
      '<nav class="gsx-nav" id="gsxGlobalNav">' +
        '<div class="gsx-nav-inner">' +
          '<a class="gsx-nav-brand" href="' + href('trang-chu_417-354.html') + '">' +
            '<img src="' + assetBase + 'assets/images/ea188f8baac92ab0012022a5ea61a0e65d086c2c.png" alt="Logo">' +
            '<span>GỐM SỨ XANH</span>' +
          '</a>' +
          '<div class="gsx-nav-links">' +
            mkLink('trang-chu_417-354.html',       'Trang chủ',    'home') +
            mkLink('ve-chung-toi_1662-3482.html',  'Về chúng tôi', 'about') +
            mkLink('trang-workshop-real_1719-3931.html', 'Workshop', 'workshop') +
            mkLink('trang-san-pham_945-1348.html', 'Sản phẩm',    'product') +
            mkLink('trang-blog_29-2.html',         'Blog',         'blog') +
          '</div>' +
          '<div class="gsx-nav-actions">' +
            '<a class="' + accountCls + '" href="' + href(accountPage) + '" id="gsxNavAccount">' +
              accountLabel +
            '</a>' +
            '<img class="gsx-nav-flag" src="' + assetBase + 'assets/images/9b34aad7bad2bbce2089b52760f4970386deffb6.png" alt="VN">' +
            '<img class="gsx-nav-flag" src="' + assetBase + 'assets/images/632be14a1b97bffb1dbc6c251283ddff51b3e613.png" alt="EN">' +
          '</div>' +
        '</div>' +
      '</nav>';

    if (!document.querySelector('.gsx-nav')) {
      document.body.insertAdjacentHTML('afterbegin', html);
    }
    document.body.classList.add('gsx-has-nav');

    // Hide old Figma absolute-positioned header elements
    var elements = document.querySelectorAll('.el, .hotspot');
    for (var i = 0; i < elements.length; i++) {
      var el = elements[i];
      var t = parseFloat(el.style.top);
      var h = parseFloat(el.style.height || el.style.minHeight);
      if (!isNaN(t) && !isNaN(h) && t <= 80 && h <= 80) {
        el.style.display = 'none';
      }
    }
  }

  /* ============================================================
     SYNC NAVBAR AUTH STATE — updates hardcoded navbars on gsxpage
     pages to reflect login/logout state consistently
     ============================================================ */
  function syncNavbarAuth() {
    // Find the account button in any navbar (injected or hardcoded)
    var accountBtns = document.querySelectorAll('#navAccount, #gsxNavAccount, .gsx-nav-btn[href*="dang-nhap"], .gsx-nav-btn[href*="ho-so-ca-nhan"]');
    var loggedIn = isLoggedIn();
    var inPages = location.pathname.indexOf('/pages/') !== -1;

    function href(page) { return inPages ? page : 'pages/' + page; }

    Array.prototype.forEach.call(accountBtns, function(btn) {
      if (loggedIn) {
        btn.textContent = 'Hồ sơ';
        btn.setAttribute('href', href('ho-so-ca-nhan_147-86.html#profile'));
      } else {
        btn.textContent = 'Tài khoản';
        btn.setAttribute('href', href('dang-nhap_839-134.html'));
      }
    });

    // Also update footer login/register button
    var footerLoginBtns = document.querySelectorAll('.footer-login-btn, .gsx-footer-login-btn');
    Array.prototype.forEach.call(footerLoginBtns, function(btn) {
      if (loggedIn) {
        btn.textContent = 'Hồ sơ của tôi';
        btn.setAttribute('href', href('ho-so-ca-nhan_147-86.html#profile'));
      } else {
        btn.textContent = 'Register or login';
        btn.setAttribute('href', href('dang-nhap_839-134.html'));
      }
    });
  }

  /* ============================================================
     FOOTER INJECTION — injects global footer into all pages
     ============================================================ */
  function injectFooter() {
    if (document.querySelector('.index') || 
        file.indexOf('frame-235') !== -1 || 
        document.querySelector('.gsx-global-footer')) return;

    // Remove legacy footers to prevent double footers (Standard footers)
    var oldFooters = document.querySelectorAll('footer:not(.gsx-global-footer), .footer');
    for (var i = 0; i < oldFooters.length; i++) {
      if (oldFooters[i] && oldFooters[i].parentNode) {
        oldFooters[i].parentNode.removeChild(oldFooters[i]);
      }
    }

    // Hide Figma absolute-positioned footer elements
    var allTexts = document.querySelectorAll('.el.text');
    var footerTopY = Infinity;
    
    for (var j = 0; j < allTexts.length; j++) {
      var text = allTexts[j].textContent.trim();
      var top = parseFloat(allTexts[j].style.top);
      
      if (!isNaN(top) && top > 1000) {
        if (text.indexOf('Copyright ©') !== -1 || text.indexOf('All rights reserved') !== -1) {
          footerTopY = Math.min(footerTopY, top - 600); // go much higher to catch background
        }
        if (text === 'Hỗ trợ khách hàng' || text === 'Liên hệ với chúng tôi' || text === 'Menu') {
          // If we find these footer column titles, the background shape is usually slightly above them
          footerTopY = Math.min(footerTopY, top - 200); 
        }
      }
    }

    if (footerTopY !== Infinity) {
      var allElements = document.querySelectorAll('.el, .hotspot');
      for (var k = 0; k < allElements.length; k++) {
        var elTop = parseFloat(allElements[k].style.top);
        if (!isNaN(elTop) && elTop >= footerTopY) {
          allElements[k].style.display = 'none';
        }
      }
    }

    var inPages = location.pathname.indexOf('/pages/') !== -1;
    function href(page) { return inPages ? page : 'pages/' + page; }
    var assetBase = inPages ? '../' : '';

    var fbIcon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2C6.477 2 2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.879V14.89h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.989C18.343 21.129 22 16.99 22 12c0-5.523-4.477-10-10-10z"/></svg>';
    
    var igIcon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/></svg>';
    
    var ttIcon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12.525.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93v7.02c-.01 1.63-.55 3.23-1.57 4.54-1.21 1.55-3.05 2.59-4.98 2.9-1.92.32-3.95.14-5.75-.68-1.74-.8-3.23-2.19-4.04-3.93-.83-1.78-1-3.83-.49-5.73.5-1.89 1.7-3.56 3.32-4.63 1.58-1.04 3.51-1.43 5.37-1.12.01 1.42-.01 2.84 0 4.26-1.06-.23-2.19-.18-3.18.27-1 .44-1.79 1.27-2.18 2.29-.4 1.05-.33 2.27.18 3.26.5.99 1.39 1.74 2.45 2.06 1.09.32 2.29.21 3.28-.27 1.02-.48 1.79-1.37 2.15-2.45.19-.58.26-1.2.26-1.81V.02h2.72z"/></svg>';

    var html =
      '<footer class="gsx-global-footer">' +
        '<div class="gsx-global-footer-inner">' +
          '<div class="gsx-footer-cols">' +
            // Menu
            '<div class="gsx-footer-col">' +
              '<h4 class="gsx-footer-title">Menu</h4>' +
              '<div class="gsx-footer-divider"></div>' +
              '<ul class="gsx-footer-links">' +
                '<li><a href="' + href('trang-chu_417-354.html') + '">Trang chủ</a></li>' +
                '<li><a href="' + href('ve-chung-toi_1662-3482.html') + '">Về chúng tôi</a></li>' +
                '<li><a href="' + href('trang-workshop-real_1719-3931.html') + '">Workshop</a></li>' +
                '<li><a href="' + href('trang-san-pham_945-1348.html') + '">Sản phẩm</a></li>' +
                '<li><a href="' + href('trang-blog_29-2.html') + '">Blog</a></li>' +
              '</ul>' +
              '<div class="gsx-footer-actions">' +
                '<a class="gsx-footer-login-btn" href="' + href('dang-nhap_839-134.html') + '">Register or login</a>' +
                '<div class="gsx-footer-social">' +
                  '<a href="https://www.facebook.com/profile.php?id=100095447280090" target="_blank">' + fbIcon + '</a>' +
                  '<a href="https://www.tiktok.com/@gomsuxanh" target="_blank">' + ttIcon + '</a>' +
                  '<a href="https://www.instagram.com/gomsuxanh/" target="_blank">' + igIcon + '</a>' +
                '</div>' +
              '</div>' +
            '</div>' +
            // Hỗ trợ khách hàng
            '<div class="gsx-footer-col">' +
              '<h4 class="gsx-footer-title">Hỗ trợ khách hàng</h4>' +
              '<div class="gsx-footer-divider"></div>' +
              '<ul class="gsx-footer-links">' +
                '<li><a href="' + href('faqs_1458-3210.html') + '">FAQ</a></li>' +
                '<li><a href="' + href('huong-dan_275-212.html') + '">Hướng dẫn</a></li>' +
              '</ul>' +
            '</div>' +
            // Liên hệ với chúng tôi
            '<div class="gsx-footer-col gsx-footer-col-contact">' +
              '<h4 class="gsx-footer-title">Liên hệ với chúng tôi</h4>' +
              '<div class="gsx-footer-divider"></div>' +
              '<p class="gsx-footer-text">Địa chỉ: Tầng 2, Hành lang bên phải, Bảo tàng Gốm Bát Tràng, 28 Đường Bát Tràng, Hà Nội.</p>' +
              '<p class="gsx-footer-text">Hotline: 0945661984 - 0946542120</p>' +
              '<p class="gsx-footer-text">Email: haphuongle@gmail.com</p>' +
              '<a href="https://maps.app.goo.gl/ssF1uQkJfW2vT8W16" target="_blank" class="gsx-footer-map-wrap">' +
                '<img class="gsx-footer-map" src="' + assetBase + 'assets/images/5440ed20e1d2722f023f3fac0d4278ac34dfdb53.png" alt="Bản đồ Bát Tràng">' +
              '</a>' +
            '</div>' +
          '</div>' + // end gsx-footer-cols
          '<div class="gsx-footer-bottom">' +
            'Copyright © 2025 gomsuxanh.vn. All rights reserved.' +
          '</div>' +
        '</div>' + // end inner
      '</footer>';

    // Insert at the very end of the body
    document.body.insertAdjacentHTML('beforeend', html);
  }


  document.addEventListener('click', function (event) {
    var link = event.target.closest ? event.target.closest('a.hotspot') : null;
    if (!link) return;

    var href = link.getAttribute('href') || '';
    var label = (link.getAttribute('aria-label') || link.getAttribute('title') || '').toLowerCase();

    // "Tài khoản" navbar → profile if logged in, login if not
    if ((label.indexOf('tài khoản') !== -1 || label.indexOf('tai khoan') !== -1) &&
        href.indexOf(pages.login) !== -1) {
      if (isLoggedIn()) {
        event.preventDefault();
        go(pages.profile);
        return;
      }
    }

    // "ĐĂNG XUẤT" sidebar link
    if ((label.indexOf('đăng xuất') !== -1 || label.indexOf('dang xuat') !== -1) &&
        href.indexOf(pages.profile) !== -1) {
      event.preventDefault();
      save('gsx_logged_in', false);
      toast('Đã đăng xuất thành công');
      setTimeout(function () { go(pages.home); }, 600);
      return;
    }

    // Page transition for internal links
    if (!link.getAttribute('target') && href.indexOf('http') !== 0) {
      event.preventDefault();
      var overlay = document.getElementById('pageTransition');
      if (overlay) {
        overlay.classList.add('is-active');
        setTimeout(function () { location.href = href; }, 280);
      } else {
        location.href = href;
      }
    }
  });

  /* ===== PAGE: ĐĂNG NHẬP & ĐĂNG KÝ (Legacy DOM injections removed) ===== */
  // These pages are now native HTML.

  /* ===== PAGE: BẢO MẬT TÀI KHOẢN ===== */
  if (isPage(pages.security)) {
    input({ left: 408, top: 416, width: 624, height: 58 },
      { type: 'password', placeholder: 'Mật khẩu hiện tại', autocomplete: 'current-password' });
    input({ left: 408, top: 516, width: 624, height: 58 },
      { type: 'password', placeholder: 'Mật khẩu mới', autocomplete: 'new-password' });
    input({ left: 408, top: 616, width: 624, height: 58 },
      { type: 'password', placeholder: 'Nhập lại mật khẩu', autocomplete: 'new-password' });
    action({ left: 636, top: 716, width: 180, height: 60 }, 'Cập nhật mật khẩu', function () {
      toast('Đã cập nhật mật khẩu!');
    });
  }

  /* ===== PAGE: THÊM ĐỊA CHỈ ===== */
  if (isPage(pages.addressForm)) {
    input({ left: 219, top: 87, width: 624, height: 58 },
      { type: 'text', placeholder: 'Họ và tên', autocomplete: 'name' });
    input({ left: 219, top: 157, width: 624, height: 58 },
      { type: 'text', placeholder: 'Địa chỉ', autocomplete: 'street-address' });
    input({ left: 219, top: 227, width: 624, height: 58 },
      { type: 'tel', placeholder: 'Số điện thoại', autocomplete: 'tel' });
    var saveAddress = action({ left: 862, top: 227, width: 150, height: 58 }, 'Lưu', function () {
      toast('Đã lưu địa chỉ!');
      setTimeout(function () { go(pages.address); }, 600);
    });
    if (saveAddress) {
      saveAddress.classList.add('app-primary');
      saveAddress.textContent = 'Lưu';
    }
  }

  /* ===== PAGE: ĐỊA CHỈ ===== */
  if (isPage(pages.address)) {
    action({ left: 408, top: 264, width: 180, height: 60 }, 'Thêm địa chỉ', function () {
      go(pages.addressForm);
    });
  }

  /* ===== PAGE: THANH TOÁN WORKSHOP ===== */
  if (isPage(pages.checkout)) {
    var paid = action({ left: 552, top: 1862, width: 336, height: 60 }, 'Xác nhận đã thanh toán', function () {
      save('gsx_order_paid', true);
      toast('Đã ghi nhận thanh toán!');
      setTimeout(function () { go(pages.history); }, 750);
    });
    if (paid) {
      paid.classList.add('app-primary');
      paid.textContent = 'Xác nhận đã thanh toán';
    }
  }

  /* ===== INLINE REGISTRATION MODAL ===== */
  var registrationFields = [
    { id: 'reg-name',     label: 'Họ và tên',                    type: 'text', placeholder: 'Nguyễn Văn A',       icon: '👤', required: true },
    { id: 'reg-phone',    label: 'Số điện thoại',                 type: 'tel',  placeholder: '0912 345 678',        icon: '📞', required: true },
    { id: 'reg-address',  label: 'Địa chỉ',                      type: 'text', placeholder: 'Số nhà, đường, quận…', icon: '📍', required: true },
    { id: 'reg-qty',      label: 'Số lượng người tham gia',       type: 'number', placeholder: '1',               icon: '👥', required: true },
    { id: 'reg-date',     label: 'Ngày tham gia',                 type: 'date', placeholder: '',                    icon: '📅', required: true },
    { id: 'reg-time',     label: 'Thời gian tham gia',            type: 'text', placeholder: '8h30 – 12h',          icon: '⏰', required: false },
    { id: 'reg-note',     label: 'Ghi chú',                      type: 'text', placeholder: 'Yêu cầu đặc biệt (nếu có)', icon: '📝', required: false }
  ];

  function buildModalHTML() {
    var fieldsHTML = '';
    registrationFields.forEach(function(f, i) {
      fieldsHTML +=
        '<div class="gsx-reg-field" style="animation-delay:' + (i * 60 + 200) + 'ms">' +
          '<label class="gsx-reg-label" for="' + f.id + '">' +
            '<span class="gsx-reg-icon">' + f.icon + '</span>' +
            f.label + (f.required ? ' <span class="gsx-reg-req">*</span>' : '') +
          '</label>' +
          '<input class="gsx-reg-input" id="' + f.id + '" ' +
            'type="' + f.type + '" ' +
            (f.id === 'reg-qty' ? 'min="1" ' : '') +
            (f.id === 'reg-phone' ? 'maxlength="11" ' : '') +
            'placeholder="' + f.placeholder + '" ' +
            (f.required ? 'required ' : '') +
            'autocomplete="off">' +
        '</div>';
    });

    return (
      '<div class="gsx-modal-overlay" id="gsxRegModal">' +
        '<div class="gsx-modal-backdrop"></div>' +
        '<div class="gsx-modal-card">' +
          /* Close button */
          '<button class="gsx-modal-x" id="gsxModalClose" aria-label="Đóng">&times;</button>' +
          /* Decorative top line */
          '<div class="gsx-modal-topline"></div>' +
          /* Header */
          '<div class="gsx-modal-header">' +
            '<h2 class="gsx-modal-title">PHIẾU ĐĂNG KÝ</h2>' +
            '<p class="gsx-modal-subtitle" id="gsxModalSubtitle">Workshop Gốm Sứ Xanh — Trải nghiệm làm gốm thủ công</p>' +
          '</div>' +
          /* Divider */
          '<div class="gsx-modal-divider"></div>' +
          /* Error Message Container */
          '<div id="gsxModalError" class="gsx-modal-error" style="display:none; color: #d32f2f; background: #ffebee; padding: 12px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; border: 1px solid #ffcdd2;"></div>' +
          /* Form */
          '<form class="gsx-reg-form" id="gsxRegForm" novalidate>' +
            fieldsHTML +
            '<div class="gsx-reg-actions">' +
              '<button type="submit" class="gsx-reg-submit" id="gsxRegSubmit">' +
                '<span class="gsx-reg-submit-text">Đăng ký ngay</span>' +
                '<span class="gsx-reg-submit-icon">→</span>' +
              '</button>' +
            '</div>' +
          '</form>' +
        '</div>' +
      '</div>'
    );
  }

  function openRegistrationModal() {
    var existing = document.getElementById('gsxRegModal');
    if (existing) {
      existing.parentNode.removeChild(existing);
    }

    // Check registration time
    if (window.gsxCurrentWorkshop) {
      var now = new Date();
      var regStart = window.gsxCurrentWorkshop.registrationStartDate ? new Date(window.gsxCurrentWorkshop.registrationStartDate) : null;
      var regEnd = window.gsxCurrentWorkshop.registrationEndDate ? new Date(window.gsxCurrentWorkshop.registrationEndDate) : null;
      
      if (regStart && now < regStart) {
        toast('Workshop này chưa mở đăng ký!');
        return;
      }
      if (regEnd && now > regEnd) {
        toast('Workshop này đã hết hạn đăng ký!');
        return;
      }
    }

    // Build & inject
    document.body.insertAdjacentHTML('beforeend', buildModalHTML());

    var modal = document.getElementById('gsxRegModal');
    if (!modal) {
      alert('Không thể tạo phiếu đăng ký. Vui lòng thử lại.');
      return;
    }
    
    var closeBtn = modal.querySelector('#gsxModalClose');
    var backdrop = modal.querySelector('.gsx-modal-backdrop');
    var form = modal.querySelector('#gsxRegForm');
    var submitBtn = modal.querySelector('#gsxRegSubmit');

    // Force reflow then open
    void modal.offsetWidth;
    requestAnimationFrame(function() {
      modal.classList.add('is-open');
    });
    document.body.style.overflow = 'hidden';
    
    // Dynamic phone maxlength
    var phoneInput = modal.querySelector('#reg-phone');
    if (phoneInput) {
      phoneInput.addEventListener('input', function() {
        var val = this.value.trim();
        if (val.startsWith('+')) {
          this.setAttribute('maxlength', '11');
        } else {
          this.setAttribute('maxlength', '10');
        }
      });
    }

    // Auto-fill user info if logged in
    var token = localStorage.getItem('gsx_token');
    if (token) {
      fetch('http://localhost:8080/identity/users/my-infor', {
        headers: { 'Authorization': 'Bearer ' + token }
      })
      .then(res => res.json())
      .then(data => {
        var user = data.result || data;
        if (user) {
          if (user.username) modal.querySelector('#reg-name').value = user.username;
          if (user.phone) modal.querySelector('#reg-phone').value = user.phone;
          
          // Address handling
          if (user.addresses && user.addresses.length > 0) {
            modal.querySelector('#reg-address').value = user.addresses[0].detail;
          } else if (user.address) {
            modal.querySelector('#reg-address').value = user.address;
          }
        }
      })
      .catch(e => console.error('Error fetching user info for auto-fill:', e));
    }

    // Set date constraints and subtitle
    if (window.gsxCurrentWorkshop) {
      var dateInput = modal.querySelector('#reg-date');
      var subtitle = modal.querySelector('#gsxModalSubtitle');
      
      var wsStart = new Date(window.gsxCurrentWorkshop.startDate);
      var wsEnd = new Date(window.gsxCurrentWorkshop.endDate);
      
      // Local format YYYY-MM-DD
      var toLocalISO = function(date) {
        var offset = date.getTimezoneOffset();
        var local = new Date(date.getTime() - (offset * 60 * 1000));
        return local.toISOString().split('T')[0];
      };

      var minDate = toLocalISO(wsStart);
      var maxDate = toLocalISO(wsEnd);
      
      dateInput.setAttribute('min', minDate);
      dateInput.setAttribute('max', maxDate);
      dateInput.value = minDate;

      if (subtitle) {
        subtitle.innerHTML = 'Workshop: <strong>' + window.gsxCurrentWorkshop.name + '</strong><br>' +
                             'Thời gian diễn ra: <strong>' + wsStart.toLocaleDateString('vi-VN') + '</strong> đến <strong>' + wsEnd.toLocaleDateString('vi-VN') + '</strong>';
      }
    }

    var isProcessing = false;
    function showModalError(msg) {
      var errBox = modal.querySelector('#gsxModalError');
      if (errBox) {
        errBox.textContent = msg;
        errBox.style.display = 'block';
      }
    }
    function hideModalError() {
      var errBox = modal.querySelector('#gsxModalError');
      if (errBox) errBox.style.display = 'none';
    }

    function setFieldError(id, msg) {
      var field = modal.querySelector(id);
      if (!field) return;
      field.classList.add('is-error');
      
      // Remove existing error msg if any
      var existing = field.parentNode.querySelector('.gsx-field-error');
      if (existing) existing.parentNode.removeChild(existing);
      
      var err = document.createElement('span');
      err.className = 'gsx-field-error';
      err.textContent = msg;
      field.parentNode.appendChild(err);

      // Clear on input
      var onInput = function() {
        clearFieldError(id);
        field.removeEventListener('input', onInput);
      };
      field.addEventListener('input', onInput);
    }

    function clearFieldError(id) {
      var field = modal.querySelector(id);
      if (!field) return;
      field.classList.remove('is-error');
      var err = field.parentNode.querySelector('.gsx-field-error');
      if (err) err.parentNode.removeChild(err);
    }

    // Close handlers
    if (closeBtn) closeBtn.addEventListener('click', closeRegistrationModal);
    if (backdrop) backdrop.addEventListener('click', closeRegistrationModal);

    // ESC to close
    document.addEventListener('keydown', function onEsc(e) {
      if (e.key === 'Escape') {
        closeRegistrationModal();
        document.removeEventListener('keydown', onEsc);
      }
    });

    function handleSubmit(e) {
      if (e) e.preventDefault();
      hideModalError();
      
      if (!submitBtn || submitBtn.disabled || isProcessing) return;
      isProcessing = true;

      try {
        // 1. Validate mandatory fields
        var allValid = true;
        var inputs = form.querySelectorAll('.gsx-reg-input[required]');
        Array.prototype.forEach.call(inputs, function(inp) {
          if (!inp.value.trim()) {
            allValid = false;
            setFieldError('#' + inp.id, 'Trường này không được để trống');
          }
        });

        if (!allValid) {
          isProcessing = false;
          return;
        }

        // 2. Date validation
        if (window.gsxCurrentWorkshop) {
          var selDate = new Date(modal.querySelector('#reg-date').value);
          var wsStart = new Date(window.gsxCurrentWorkshop.startDate);
          var wsEnd = new Date(window.gsxCurrentWorkshop.endDate);
          
          selDate.setHours(0,0,0,0);
          wsStart.setHours(0,0,0,0);
          wsEnd.setHours(0,0,0,0);

          if (selDate < wsStart || selDate > wsEnd) {
            var startStr = wsStart.toLocaleDateString('vi-VN');
            var endStr = wsEnd.toLocaleDateString('vi-VN');
            setFieldError('#reg-date', 'Workshop chỉ diễn ra từ ' + startStr + ' đến ' + endStr);
            isProcessing = false;
            return;
          }
        }

        // 3. Collect Values & Validate Phone/Qty
        var nameVal = modal.querySelector('#reg-name').value;
        var phoneVal = modal.querySelector('#reg-phone').value;
        var qty = modal.querySelector('#reg-qty').value;
        var dateVal = modal.querySelector('#reg-date').value;
        var timeVal = modal.querySelector('#reg-time').value;
        var note = modal.querySelector('#reg-note').value;

        var phoneRegex = /^(0\d{9}|\+84\d{9})$/;
        var cleanPhone = phoneVal.replace(/\s/g, '');
        if (!phoneRegex.test(cleanPhone)) {
          var errorMsg = 'Số điện thoại phải có đúng 10 chữ số (Ví dụ: 0912345678)';
          if (cleanPhone.startsWith('0') && cleanPhone.length !== 10) {
            errorMsg = 'Số điện thoại bắt đầu bằng 0 phải có đúng 10 chữ số';
          }
          setFieldError('#reg-phone', errorMsg);
          isProcessing = false;
          return;
        }

        if (!qty || parseInt(qty) <= 0) {
          setFieldError('#reg-qty', 'Số lượng tham gia phải lớn hơn 0');
          isProcessing = false;
          return;
        }

        // --- ALL VALIDATED, START LOADING ---
        submitBtn.classList.add('is-loading');
        var btnText = submitBtn.querySelector('.gsx-reg-submit-text');
        if (btnText) btnText.textContent = 'Đang gửi…';
        submitBtn.disabled = true;

        var token = localStorage.getItem('gsx_token');
        if (!token) {
          var pendingData = {
            workshopId: window.gsxCurrentWorkshopId || 1,
            qty: qty,
            date: dateVal,
            time: timeVal
          };
          sessionStorage.setItem('gsx_pending_registration', JSON.stringify(pendingData));
          toast('Vui lòng đăng nhập để hoàn tất đăng ký');
          setTimeout(function() { go(pages.login); }, 800);
          return;
        }

        var wsId = window.gsxCurrentWorkshopId || 1;
        if (!wsId || wsId === 'undefined') {
          toast('Lỗi: Không xác định được Workshop. Vui lòng thử lại.');
          submitBtn.disabled = false;
          isProcessing = false;
          return;
        }

        var formData = new URLSearchParams();
        formData.append('workshopId', wsId);
        formData.append('quantity', qty);
        formData.append('participationDate', dateVal);
        formData.append('participationTime', timeVal);
        formData.append('note', note);
        formData.append('name', nameVal);
        formData.append('phone', phoneVal);

        fetch('http://localhost:8080/workshop/regis-workshops/register', {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: formData
        })
        .then(function(res) {
          if (!res.ok) {
            return res.json().then(function(errBody) {
              var msg = errBody.message || 'Mã lỗi ' + res.status;
              if (res.status === 403) msg = 'Hết hạn phiên đăng nhập';
              throw new Error(msg);
            }).catch(function(e) {
              // Re-throw if it's our intentional Error (from the .then above)
              if (e instanceof Error && e.message !== undefined) throw e;
              // Otherwise it's a JSON parse failure — create generic error
              throw new Error('Mã lỗi ' + res.status);
            });
          }
          return res.json();
        })
        .then(function(data) {
          closeRegistrationModal();
          toast('Đăng ký thành công!');
          setTimeout(function() {
            go(pages.history);
          }, 1000);
        })
        .catch(function(err) {
          console.error(err);
          var errorMsg = err.message || 'Lỗi kết nối máy chủ';
          showModalError('Lỗi hệ thống: ' + errorMsg);
          submitBtn.classList.remove('is-loading');
          if (btnText) btnText.textContent = 'Đăng ký ngay';
          submitBtn.disabled = false;
          isProcessing = false;
        });
      } catch (err) {
        console.error('Submit error:', err);
        alert('Lỗi xử lý phiếu: ' + err.message);
        if (submitBtn) {
          submitBtn.classList.remove('is-loading');
          submitBtn.disabled = false;
        }
      }
    }

    if (form) {
      form.addEventListener('submit', handleSubmit);
    }
    if (submitBtn) {
      submitBtn.addEventListener('click', function(e) {
        // Only trigger if click is on the button and not handled by form submit
        if (e.target.tagName !== 'BUTTON' && e.target.closest('button') !== submitBtn) return;
        handleSubmit(e);
      });
    }

    // Focus first input
    setTimeout(function() {
      var first = modal.querySelector('.gsx-reg-input');
      if (first) first.focus();
    }, 400);
  }

  function closeRegistrationModal() {
    var modal = document.getElementById('gsxRegModal');
    if (modal) {
      modal.classList.remove('is-open');
      modal.classList.add('is-closing');
      document.body.style.overflow = '';
      setTimeout(function() {
        if (modal.parentNode) {
          modal.parentNode.removeChild(modal);
        }
      }, 400);
    }
  }

  // Expose globally for any external calls
  window.openRegistrationModal = openRegistrationModal;
  window.closeModal = closeRegistrationModal;

  /* ===== WORKSHOP: Registration Link Hijacking ===== */
  function initWorkshopLinks() {
    // Hijack all registration links across the entire site
    var regSelectors = [
      'a[href*="frame-235"]',
      'a[href*="dang-ky"]',
      '.btn-solid',
      '.btn-outline',
      'a.hotspot'
    ];

    var regLinks = document.querySelectorAll(regSelectors.join(','));

    Array.prototype.forEach.call(regLinks, function(link) {
      var href = link.getAttribute('href') || '';
      var text = (link.textContent || link.getAttribute('aria-label') || link.getAttribute('title') || '').toLowerCase();

      // Direct frame-235 links → always open modal
      if (href.indexOf('frame-235') !== -1) {
        link.addEventListener('click', function(e) {
          e.preventDefault();
          e.stopPropagation();
          window.gsxCurrentWorkshopId = this.getAttribute('data-id') || 1;
          openRegistrationModal();
        });
        return;
      }

      // "Đăng ký" buttons on workshop / product pages
      var isWorkshopContext = file.indexOf('trang-workshop') === 0 ||
                              file.indexOf('ve-tranh-gom') === 0 ||
                              document.querySelector('.workshop-hero') ||
                              document.querySelector('.workshop-list');

      if (isWorkshopContext) {
        var isReg = text.indexOf('đăng ký') !== -1 || text.indexOf('dang ky') !== -1;
        if (isReg) {
          link.addEventListener('click', function(e) {
            // Fix: If it's an anchor link for scrolling, let it be
            if (href.indexOf('#') === 0) return;

            e.preventDefault();
            e.stopPropagation();
            window.gsxCurrentWorkshopId = this.getAttribute('data-id') || 1;
            openRegistrationModal();
          });
        }
      }
    });
  }

  /* ===== KEYBOARD: Enter → submit ===== */
  document.addEventListener('keydown', function (event) {
    if (event.key !== 'Enter') return;
    if (!event.target.classList || !event.target.classList.contains('app-field')) return;
    var primary = screen ? screen.querySelector('.app-primary') : null;
    var fallback = screen ? screen.querySelector('.app-action') : null;
    var target = primary || fallback;
    if (target) {
      event.preventDefault();
      target.click();
    }
  });

  /* ============================================================
     INDEX PAGE ENHANCEMENTS
     ============================================================ */
  var isIndex = !!document.querySelector('.index');

  if (isIndex) {
    /* --- Staggered card reveal --- */
    var cards = document.querySelectorAll('.card');
    Array.prototype.forEach.call(cards, function (card, i) {
      card.style.animationDelay = Math.min(i * 35, 800) + 'ms';
    });

    /* --- Category filter --- */
    var navTags = document.querySelectorAll('.nav-tag');
    var pageCountEl = document.getElementById('pageCount');
    var totalPages = cards.length;

    function filterCards(category) {
      var visibleCount = 0;
      Array.prototype.forEach.call(cards, function (card) {
        var cat = card.getAttribute('data-category');
        var show = category === 'all' || cat === category;
        card.style.display = show ? '' : 'none';
        if (show) visibleCount++;
      });

      if (pageCountEl) {
        pageCountEl.innerHTML = 'Hiển thị <strong>' + visibleCount + '</strong> / ' + totalPages + ' trang';
      }

      // Update active tag
      Array.prototype.forEach.call(navTags, function (tag) {
        tag.classList.toggle('is-active', tag.getAttribute('data-filter') === category);
      });
    }

    Array.prototype.forEach.call(navTags, function (tag) {
      tag.addEventListener('click', function () {
        filterCards(tag.getAttribute('data-filter'));
      });
    });

    /* --- Search --- */
    var searchInput = document.getElementById('searchInput');
    if (searchInput) {
      searchInput.addEventListener('input', function () {
        var query = this.value.toLowerCase().trim();

        // Reset category filter to "all" when searching
        Array.prototype.forEach.call(navTags, function (tag) {
          tag.classList.toggle('is-active', tag.getAttribute('data-filter') === 'all');
        });

        var visibleCount = 0;
        Array.prototype.forEach.call(cards, function (card) {
          var title = (card.querySelector('strong') || {}).textContent || '';
          var show = !query || title.toLowerCase().indexOf(query) !== -1;
          card.style.display = show ? '' : 'none';
          if (show) visibleCount++;
        });

        if (pageCountEl) {
          pageCountEl.innerHTML = 'Hiển thị <strong>' + visibleCount + '</strong> / ' + totalPages + ' trang';
        }
      });

      // Keyboard shortcut: Ctrl+K or / to focus search
      document.addEventListener('keydown', function (e) {
        if ((e.ctrlKey && e.key === 'k') || (e.key === '/' && document.activeElement === document.body)) {
          e.preventDefault();
          searchInput.focus();
          searchInput.select();
        }
        // Escape to clear search
        if (e.key === 'Escape' && document.activeElement === searchInput) {
          searchInput.value = '';
          searchInput.dispatchEvent(new Event('input'));
          searchInput.blur();
        }
      });
    }
  }

  /* ============================================================
     SCROLL PROGRESS BAR
     ============================================================ */
  var scrollProgressEl = document.getElementById('scrollProgress');
  function updateScrollProgress() {
    if (!scrollProgressEl) return;
    var scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (scrollHeight <= 0) {
      scrollProgressEl.style.width = '0%';
      return;
    }
    var progress = (window.scrollY / scrollHeight) * 100;
    scrollProgressEl.style.width = Math.min(progress, 100) + '%';
  }

  /* ============================================================
     BACK TO TOP BUTTON
     ============================================================ */
  var backToTopBtn = document.getElementById('backToTop');
  function updateBackToTop() {
    if (!backToTopBtn) return;
    backToTopBtn.classList.toggle('is-visible', window.scrollY > 400);
  }

  if (backToTopBtn) {
    backToTopBtn.addEventListener('click', function () {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  /* ============================================================
     SCROLL HANDLER (throttled)
     ============================================================ */
  var scrollRAF = null;
  function onScroll() {
    if (scrollRAF) return;
    scrollRAF = requestAnimationFrame(function () {
      updateScrollProgress();
      updateBackToTop();
      scrollRAF = null;
    });
  }

  window.addEventListener('scroll', onScroll, { passive: true });

  /* ============================================================
     PAGE ENTRY ANIMATION (remove transition overlay)
     ============================================================ */
  function clearTransition() {
    var overlay = document.getElementById('pageTransition');
    if (overlay) {
      overlay.classList.remove('is-active');
    }
  }

  /* ============================================================
     TYPOGRAPHY & DESIGN NORMALIZATION (Figma Override)
     ============================================================ */
  function normalizeDesign() {
    var isIndex = !!document.querySelector('.index');
    if (isIndex) return; // Only process Figma pages

    // Batch all reads first, then writes via rAF
    var texts = document.querySelectorAll('.el.text');
    if (!texts.length) return;

    var textUpdates = [];
    Array.prototype.forEach.call(texts, function(el) {
      var fontFam = el.style.fontFamily || '';
      var fontSizeStr = el.style.fontSize || '';
      var fontWeight = el.style.fontWeight || '400';
      var color = el.style.color || '';

      var isHeading = el.classList.contains('heading-1') || 
                      el.classList.contains('heading-2') || 
                      el.classList.contains('heading-3') ||
                      fontFam.indexOf('Anton') !== -1 || 
                      fontFam.indexOf('Bellota') !== -1 || 
                      fontFam.indexOf('Akatab') !== -1 ||
                      (parseInt(fontWeight) >= 700 && parseFloat(fontSizeStr) >= 28);

      textUpdates.push({ el: el, isHeading: isHeading, fontSizeStr: fontSizeStr, color: color });
    });

    var shapes = document.querySelectorAll('.el.shape');
    var shapeUpdates = [];
    Array.prototype.forEach.call(shapes, function(el) {
      var bg = el.style.backgroundColor || el.style.background || '';
      if (bg.indexOf('rgba(85, 139, 47') !== -1 || bg.indexOf('rgb(85, 139, 47)') !== -1) {
        shapeUpdates.push(el);
      }
    });

    // Apply all writes in a single rAF to avoid layout thrashing
    requestAnimationFrame(function() {
      textUpdates.forEach(function(item) {
        var el = item.el;
        if (item.isHeading) {
          if (!el.classList.contains('heading-1') && 
              !el.classList.contains('heading-2') && 
              !el.classList.contains('heading-3')) {
            el.style.setProperty('font-family', 'var(--font-display)', 'important');
          }
          el.style.setProperty('letter-spacing', '-0.02em', 'important');
          if (!el.classList.contains('text-paragraph')) {
            el.style.setProperty('line-height', '1.2', 'important');
          }
        } else {
          el.style.setProperty('font-family', 'var(--font-body)', 'important');
          el.style.setProperty('letter-spacing', '0.01em', 'important');
        }

        // Font size normalization
        if (item.fontSizeStr) {
          var size = parseFloat(item.fontSizeStr);
          var newSize = size;
          if (size >= 14 && size <= 17) newSize = 16;
          else if (size > 17 && size <= 19) newSize = 18;
          else if (size > 19 && size <= 22) newSize = 20;
          else if (size > 22 && size <= 25) newSize = 24;
          else if (size > 25 && size <= 30) newSize = 28;
          else if (size > 30 && size <= 36) newSize = 32;
          else if (size > 36 && size <= 48) newSize = 40;
          
          if (newSize !== size) {
            el.style.setProperty('font-size', newSize + 'px', 'important');
          }
        }

        // Color normalization (replace harsh black)
        if (item.color === 'rgba(0, 0, 0, 1)' || item.color === 'rgb(0, 0, 0)') {
          el.style.setProperty('color', 'var(--clr-forest)', 'important');
        }
      });

      // Fix raw shapes/buttons
      shapeUpdates.forEach(function(el) {
        el.style.setProperty('background', 'linear-gradient(135deg, var(--clr-leaf), var(--clr-teal))', 'important');
        el.style.setProperty('border', 'none', 'important');
        el.style.setProperty('box-shadow', '0 4px 16px rgba(46,125,96,0.25)', 'important');
      });
    });
  }

  /* ===== LAZY LOADING: inject loading="lazy" for below-fold images ===== */
  function enableLazyLoading() {
    var allImages = document.querySelectorAll('img:not([loading])');
    Array.prototype.forEach.call(allImages, function(img, idx) {
      // Skip first 2 images (likely above the fold)
      if (idx > 1) {
        img.setAttribute('loading', 'lazy');
      }
      img.setAttribute('decoding', 'async');
    });
  }

  /* ===== FIX background-attachment: fixed → scroll for scroll perf ===== */
  function fixBackgroundPerf() {
    var gsxBodies = document.querySelectorAll('.gsxpage');
    Array.prototype.forEach.call(gsxBodies, function(el) {
      if (window.getComputedStyle(el).backgroundAttachment === 'fixed') {
        el.style.backgroundAttachment = 'scroll';
      }
    });
  }

  /* ===== INIT ===== */
  // Inject navbar & footer immediately (before paint) for all Figma pages
  injectNavbar();
  syncNavbarAuth();
  injectFooter();

  window.addEventListener('resize', debouncedFitScreen);
  window.addEventListener('orientationchange', fitScreen);
  window.addEventListener('load', function () {
    // Navbar/footer already injected above — no need to repeat
    syncNavbarAuth();
    fitScreen();
    clearTransition();
    updateScrollProgress();
    updateBackToTop();
    normalizeDesign();
    initWorkshopLinks();
    enableLazyLoading();
    fixBackgroundPerf();
  });
  fitScreen();
  clearTransition();
})();
