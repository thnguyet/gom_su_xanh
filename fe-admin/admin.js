(function () {
  'use strict';
  var API = 'http://localhost:8080';
  var token = localStorage.getItem('adm_token') || '';

  // Early Auth Check
  if (!token) {
    location.href = '../pages/dang-nhap_839-134.html';
  }

  var currentSection = 'dashboard';
  var pageState = {};

  /* ===== HELPERS ===== */
  function $(id) { return document.getElementById(id); }
  function toast(msg) { var t = $('admToast'); t.textContent = msg; t.classList.add('is-visible'); setTimeout(function () { t.classList.remove('is-visible'); }, 2600); }
  function fmt(n) { return new Intl.NumberFormat('vi-VN').format(n || 0); }
  function fmtDate(d) { if (!d) return '—'; var dt = new Date(d); return dt.toLocaleDateString('vi-VN'); }
  function fmtMoney(n) { return fmt(n) + 'đ'; }
  function authHeaders() { return { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' }; }

  function api(path, opts) {
    opts = opts || {};
    var headers = opts.noAuth ? { 'Content-Type': 'application/json' } : authHeaders();
    if (opts.formData) { delete headers['Content-Type']; }
    return fetch(API + path, { method: opts.method || 'GET', headers: headers, body: opts.body || null })
      .then(function (r) {
        if ((r.status === 401 || r.status === 403) && token !== 'admin_local') {
          toast('Phiên đăng nhập hết hạn');
          logout();
          throw new Error('Unauthorized');
        }
        var ct = r.headers.get('content-type') || '';
        var isJson = ct.indexOf('json') !== -1;

        if (!r.ok) {
          return (isJson ? r.json() : r.text()).then(function (err) {
            // HIỂN THỊ LỖI RA CONSOLE ĐỂ DEBUG
            console.group("%c ❌ API ERROR: " + path, "color: white; background: #e11d48; padding: 4px; border-radius: 4px; font-weight: bold;");
            console.error("Status:", r.status);
            if (typeof err === 'object') {
                console.error("Mã lỗi (Code):", err.code);
                console.error("Thông báo (Message):", err.message);
                console.error("Chi tiết phản hồi:", err);
            } else {
                console.error("Phản hồi lỗi (Text):", err);
            }
            console.groupEnd();
            
            throw err;
          });
        }
        return isJson ? r.json() : r.text();
      });
  }

  /* ===== LOGIN ===== */
  function initLogin() {
    var form = $('admLoginForm');
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var user = $('admUser').value.trim();
      var pass = $('admPass').value.trim();
      var err = $('admLoginError');
      var btn = $('admLoginBtn');
      err.textContent = '';

      if (user === 'admin' && pass === 'admin') {
        btn.classList.add('is-loading'); btn.querySelector('span').textContent = 'Đang đăng nhập...';
        // Try real backend login first
        fetch(API + '/identity/auth/token', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'admin', password: 'admin' }) })
          .then(function (r) { return r.json(); })
          .then(function (data) {
            if (data.token) { token = data.token; localStorage.setItem('adm_token', token); }
            else { token = 'admin_local'; localStorage.setItem('adm_token', token); }
            showDashboard();
          })
          .catch(function () {
            // Backend not running - use local mode
            token = 'admin_local'; localStorage.setItem('adm_token', token);
            showDashboard();
          });
      } else {
        err.textContent = 'Sai tài khoản hoặc mật khẩu!';
        $('admUser').focus();
      }
    });
  }

  function showDashboard() {
    $('admLogin').style.display = 'none';
    $('admApp').style.display = 'flex';
    $('admDate').textContent = new Date().toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    navigate('dashboard');
  }

  function logout() {
    token = ''; localStorage.removeItem('adm_token');
    localStorage.removeItem('gsx_token');
    localStorage.setItem('gsx_logged_in', JSON.stringify(false));
    // Redirect to main login page (unified flow)
    window.location.href = '../fe-user/pages/dang-nhap_839-134.html';
  }

  /* ===== NAVIGATION ===== */
  function navigate(section) {
    currentSection = section;
    var items = document.querySelectorAll('.adm-nav-item[data-section]');
    items.forEach(function (it) { it.classList.toggle('is-active', it.dataset.section === section); });
    var titles = { dashboard: 'Tổng quan', products: 'Sản phẩm', categories: 'Danh mục sản phẩm', orders: 'Đơn hàng', shipping: 'Vận chuyển', payments: 'Thanh toán', workshops: 'Workshop', workshopRegs: 'ĐK Workshop', posts: 'Bài viết', postCategories: 'Danh mục bài viết', reviews: 'Đánh giá', users: 'Người dùng' };
    $('admTopTitle').textContent = titles[section] || section;
    pageState = { page: 0, size: 10, keyword: '' };
    renderSection(section);
  }

  function renderSection(s) {
    var c = $('admContent');
    var fn = { dashboard: renderDashboard, products: renderProducts, categories: renderCategories, orders: renderOrders, shipping: renderShipping, payments: renderPayments, workshops: renderWorkshops, workshopRegs: renderWorkshopRegs, posts: renderPosts, postCategories: renderPostCategories, reviews: renderReviews, users: renderUsers };
    if (fn[s]) fn[s](c); else c.innerHTML = '<div class="adm-empty"><div class="adm-empty-icon">🚧</div><p>Đang phát triển...</p></div>';
  }

  /* ===== DASHBOARD ===== */
  function renderDashboard(c) {
    c.innerHTML = '<div class="adm-stats" id="admStats">' + skeleton(4) + '</div>' +
      '<div class="adm-section"><div class="adm-section-header" style="display:flex;justify-content:space-between;align-items:center"><h3 class="adm-section-title">📈 Thống kê doanh thu</h3><select class="adm-select" id="admChartRange" style="width:auto;margin-left:16px"><option value="7days">7 ngày qua</option><option value="monthly">Theo từng tháng (Năm nay)</option></select></div><div style="background:var(--adm-surface);padding:1.5rem;border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,0.03);margin-bottom:2rem;"><canvas id="admRevenueChart" style="width:100%;height:350px;"></canvas></div></div>' +
      '<div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">📦 Đơn hàng gần đây</h3></div><div id="admRecentOrders">' + skeleton(3) + '</div></div>';

    // Load stats
    Promise.all([
      api('/product/products/all?page=0&size=1').catch(function () { return { totalElements: 0 }; }),
      api('/order/orders/all-orders?page=0&size=1').catch(function () { return { totalElements: 0 }; }),
      api('/workshop/workshops/all?page=0&size=1').catch(function () { return { totalElements: 0 }; }),
      api('/identity/users').catch(function () { return []; })
    ]).then(function (res) {
      var stats = [
        { icon: '📦', value: fmt(res[0].totalElements || 0), label: 'Sản phẩm' },
        { icon: '🛒', value: fmt(res[1].totalElements || 0), label: 'Đơn hàng' },
        { icon: '🎨', value: fmt(res[2].totalElements || 0), label: 'Workshop' },
        { icon: '👥', value: fmt(Array.isArray(res[3]) ? res[3].length : 0), label: 'Người dùng' }
      ];
      $('admStats').innerHTML = stats.map(function (s) {
        return '<div class="adm-stat"><div class="adm-stat-icon">' + s.icon + '</div><div class="adm-stat-value">' + s.value + '</div><div class="adm-stat-label">' + s.label + '</div></div>';
      }).join('');
    });

    // Load Chart Data
    Promise.all([
      api('/order/orders/all-orders?size=2000&sortBy=createdAt&sortDir=desc').catch(() => ({ content: [] })),
      api('/workshop/regis-workshops/all?size=2000&sortBy=registrationDate&sortDir=desc').catch(() => ({ content: [] }))
    ]).then(function (res) {
      var orders = res[0].content || [];
      var workshops = res[1].content || [];

      var ctx = document.getElementById('admRevenueChart');
      if (!ctx || !window.Chart) return;

      var chartInstance = null;

      function updateChart(type) {
        var labels = [];
        var orderData = [];
        var wsData = [];

        if (type === '7days') {
          for (var i = 6; i >= 0; i--) {
            var d = new Date();
            d.setDate(d.getDate() - i);
            var dateStr = d.toISOString().split('T')[0]; // YYYY-MM-DD
            labels.push(d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }));

            var sumOrder = orders.filter(function (o) { return o.status !== 'CANCELLED' && o.createdAt && o.createdAt.startsWith(dateStr); })
              .reduce(function (sum, o) { return sum + (o.totalAmount || 0); }, 0);
            var sumWs = workshops.filter(function (w) { return w.status !== 'CANCELLED' && w.registrationDate && w.registrationDate.startsWith(dateStr); })
              .reduce(function (sum, w) { return sum + (w.totalPrice || 0); }, 0);
            orderData.push(sumOrder);
            wsData.push(sumWs);
          }
        } else if (type === 'monthly') {
          var currentYear = new Date().getFullYear();
          for (var m = 1; m <= 12; m++) {
            var monthStr = currentYear + '-' + (m < 10 ? '0' + m : m); // YYYY-MM
            labels.push('Tháng ' + m);

            var sumOrder = orders.filter(function (o) { return o.status !== 'CANCELLED' && o.createdAt && o.createdAt.startsWith(monthStr); })
              .reduce(function (sum, o) { return sum + (o.totalAmount || 0); }, 0);
            var sumWs = workshops.filter(function (w) { return w.status !== 'CANCELLED' && w.registrationDate && w.registrationDate.startsWith(monthStr); })
              .reduce(function (sum, w) { return sum + (w.totalPrice || 0); }, 0);
            orderData.push(sumOrder);
            wsData.push(sumWs);
          }
        }

        if (chartInstance) chartInstance.destroy();

        chartInstance = new Chart(ctx, {
          type: type === 'monthly' ? 'bar' : 'line',
          data: {
            labels: labels,
            datasets: [
              {
                label: 'Đơn hàng (VNĐ)',
                data: orderData,
                borderColor: '#1e3a8a',
                backgroundColor: type === 'monthly' ? '#1e3a8a' : 'rgba(30, 58, 138, 0.1)',
                borderWidth: 2,
                tension: 0.3,
                fill: true
              },
              {
                label: 'Workshop (VNĐ)',
                data: wsData,
                borderColor: '#059669',
                backgroundColor: type === 'monthly' ? '#059669' : 'rgba(5, 150, 105, 0.1)',
                borderWidth: 2,
                tension: 0.3,
                fill: true
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { position: 'top' },
              tooltip: {
                callbacks: {
                  label: function (context) { return context.dataset.label + ': ' + new Intl.NumberFormat('vi-VN').format(context.parsed.y) + 'đ'; }
                }
              }
            },
            scales: {
              y: { beginAtZero: true, ticks: { callback: function (value) { return new Intl.NumberFormat('vi-VN', { notation: "compact", compactDisplay: "short" }).format(value); } } }
            }
          }
        });
      }

      updateChart('7days');

      var rangeSelect = document.getElementById('admChartRange');
      if (rangeSelect) {
        rangeSelect.addEventListener('change', function (e) {
          updateChart(e.target.value);
        });
      }
    });

    // Recent orders
    api('/order/orders/all-orders?page=0&size=5&sortBy=createdAt&sortDir=desc').then(function (data) {
      var rows = (data.content || []);
      if (!rows.length) { $('admRecentOrders').innerHTML = emptyMsg('Chưa có đơn hàng'); return; }
      $('admRecentOrders').innerHTML = tableWrap(['ID', 'Khách hàng', 'Tổng tiền', 'Trạng thái', 'Ngày tạo'], rows.map(function (o) {
        return '<tr><td>#' + o.id + '</td><td>' + esc(o.customerName || 'KH-' + o.customerId) + '</td><td>' + fmtMoney(o.totalAmount) + '</td><td>' + statusBadge(o.status) + '</td><td>' + fmtDate(o.createdAt) + '</td></tr>';
      }).join(''));
    }).catch(function () { $('admRecentOrders').innerHTML = emptyMsg('Không thể tải đơn hàng'); });
  }

  /* ===== PRODUCTS ===== */
  function renderProducts(c) {
    c.innerHTML = sectionHeader('Sản phẩm', 'products', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddProductModal()">+ Thêm sản phẩm</button>') + '<div id="admProdTable">' + skeleton(5) + '</div><div id="admProdPage"></div>';
    bindSearch('products'); loadProducts();
  }
  function loadProducts() {
    var p = pageState;
    var q = p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '';
    api('/product/products/all?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admProdTable').innerHTML = emptyMsg('Không tìm thấy sản phẩm'); $('admProdPage').innerHTML = ''; return; }
      $('admProdTable').innerHTML = tableWrap(['ID', 'Ảnh', 'Tên sản phẩm', 'Thương hiệu', 'Giá', 'Kho', 'Thao tác'], rows.map(function (p) {
        var img = (p.imageUrls && p.imageUrls.length) ? '<img class="adm-thumb" src="' + p.imageUrls[0] + '" alt="">' : '—';
        var nameLink = `<a href="javascript:void(0)" onclick="ADM.openProductDetailModal(${p.id})" style="font-weight:600;color:var(--adm-accent2);text-decoration:none">${esc(p.name)}</a>`;
        return '<tr><td>#' + p.id + '</td><td>' + img + '</td><td>' + nameLink + '</td><td>' + esc(p.brand || '—') + '</td><td>' + fmtMoney(p.price) + '</td><td>' + p.stockQuantity + '</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delProduct(' + p.id + ')">Xóa</button></td></tr>';
      }).join(''));
      $('admProdPage').innerHTML = paginate(data);
    }).catch(function () { $('admProdTable').innerHTML = emptyMsg('Lỗi tải sản phẩm'); });
  }

  /* ===== PRODUCT CATEGORIES ===== */
  function renderCategories(c) {
    c.innerHTML = sectionHeader('Danh mục sản phẩm', 'categories', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddCategoryModal()">+ Thêm danh mục</button>') + '<div id="admCatTable">' + skeleton(5) + '</div><div id="admCatPage"></div>';
    bindSearch('categories'); loadCategories();
  }
  function loadCategories() {
    var p = pageState;
    var q = p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '';
    api('/product/categories?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admCatTable').innerHTML = emptyMsg('Không tìm thấy danh mục'); $('admCatPage').innerHTML = ''; return; }
      $('admCatTable').innerHTML = tableWrap(['ID', 'Ảnh đại diện', 'Tên danh mục', 'Số sản phẩm', 'Trạng thái', 'Thao tác'], rows.map(function (s) {
        var img = s.imageUrl ? '<img class="adm-thumb" src="' + s.imageUrl + '" alt="">' : '—';
        var badge = s.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
        var editBtn = '<button class="adm-btn adm-btn--info adm-btn--sm" onclick="ADM.openEditCategoryModal(' + s.id + ')">Sửa</button>';
        var delBtn = '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delCategory(' + s.id + ')">Xóa</button>';
        var nameLink = `<a href="javascript:void(0)" onclick="ADM.openCategoryDetailModal(${s.id})" style="font-weight:600;color:var(--adm-accent2);text-decoration:none">${esc(s.name)}</a>`;
        return '<tr><td>#' + s.id + '</td><td>' + img + '</td><td>' + nameLink + '</td><td>' + (s.productCount || 0) + '</td><td>' + badge + '</td><td><div class="adm-table-actions">' + editBtn + ' ' + delBtn + '</div></td></tr>';
      }).join(''));
      $('admCatPage').innerHTML = paginate(data);
    }).catch(function () { $('admCatTable').innerHTML = emptyMsg('Lỗi tải danh mục'); });
  }

  /* ===== ORDERS ===== */
  function renderOrders(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admOrderFilter"><option value="">Tất cả trạng thái</option><option value="PENDING">Chờ xử lý</option><option value="CONFIRMED">Đã xác nhận</option><option value="SHIPPING">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></div>' + sectionHeader('Đơn hàng', 'orders') + '<div id="admOrdTable">' + skeleton(5) + '</div><div id="admOrdPage"></div>';
    $('admOrderFilter').addEventListener('change', function () { pageState.status = this.value; pageState.page = 0; loadOrders(); });
    bindSearch('orders'); loadOrders();
  }
  function loadOrders() {
    var p = pageState;
    var statusQ = p.status ? '&status=' + p.status : '';
    var q = p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '';
    api('/order/orders/all-orders?page=' + p.page + '&size=' + p.size + statusQ + q + '&sortBy=createdAt&sortDir=desc').then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admOrdTable').innerHTML = emptyMsg('Không có đơn hàng'); $('admOrdPage').innerHTML = ''; return; }
      $('admOrdTable').innerHTML = tableWrap(['ID', 'Khách', 'Tổng tiền', 'Thanh toán', 'Trạng thái', 'Ngày', 'Thao tác'], rows.map(function (o) {
        var acts = '<select class="adm-select" onchange="ADM.updateOrder(' + o.id + ',this.value)" style="font-size:12px"><option value="">Cập nhật...</option><option value="CONFIRMED">Xác nhận</option><option value="SHIPPING">Giao hàng</option><option value="DELIVERED">Đã giao</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Hủy</option></select>';
        var idLink = `<a href="javascript:void(0)" onclick="ADM.openOrderDetailModal(${o.id})" style="font-weight:600;color:var(--adm-accent2)">#${o.id}</a>`;
        return '<tr><td>' + idLink + '</td><td>' + esc(o.customerName || 'KH-' + o.customerId) + '</td><td>' + fmtMoney(o.totalAmount) + '</td><td>' + esc(o.paymentMethod || '—') + '</td><td>' + statusBadge(o.status) + '</td><td>' + fmtDate(o.createdAt) + '</td><td>' + acts + '</td></tr>';
      }).join(''));
      $('admOrdPage').innerHTML = paginate(data);
    }).catch(function () { $('admOrdTable').innerHTML = emptyMsg('Lỗi tải đơn hàng'); });
  }

  /* ===== SHIPPING METHODS ===== */
  function renderShipping(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admShipFilter"><option value="">Tất cả trạng thái</option><option value="true">Đang hoạt động</option><option value="false">Tạm dừng</option></select></div>' + sectionHeader('Đơn vị vận chuyển', 'shipping', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddShippingModal()">+ Thêm đơn vị</button>') + '<div id="admShipTable">' + skeleton(5) + '</div><div id="admShipPage"></div>';
    $('admShipFilter').addEventListener('change', function () { pageState.active = this.value; pageState.page = 0; loadShipping(); });
    bindSearch('shipping'); loadShipping();
  }
  function loadShipping() {
    var p = pageState;
    var q = (p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '') + (p.active ? '&active=' + p.active : '');
    api('/order/shipping-methods/search?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admShipTable').innerHTML = emptyMsg('Không tìm thấy đơn vị vận chuyển'); $('admShipPage').innerHTML = ''; return; }
      $('admShipTable').innerHTML = tableWrap(['ID', 'Tên đơn vị', 'Phí vận chuyển', 'Trạng thái', 'Thao tác'], rows.map(function (s) {
        var badge = s.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
        var toggleBtn = s.active ? '<button class="adm-btn adm-btn--warning adm-btn--sm" onclick="ADM.toggleShippingStatus(' + s.id + ',true)">Tạm dừng</button>' : '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.toggleShippingStatus(' + s.id + ',false)">Mở lại</button>';
        var editBtn = '<button class="adm-btn adm-btn--info adm-btn--sm" onclick="ADM.openEditShippingModal(' + s.id + ')">Sửa</button>';
        var delBtn = '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delShipping(' + s.id + ')">Xóa</button>';
        return '<tr><td>#' + s.id + '</td><td style="font-weight:600">' + esc(s.name) + '</td><td>' + fmtMoney(s.shippingFee) + '</td><td>' + badge + '</td><td><div class="adm-table-actions">' + editBtn + ' ' + toggleBtn + ' ' + delBtn + '</div></td></tr>';
      }).join(''));
      $('admShipPage').innerHTML = paginate(data);
    }).catch(function () { $('admShipTable').innerHTML = emptyMsg('Lỗi tải đơn vị vận chuyển'); });
  }

  /* ===== PAYMENT METHODS ===== */
  function renderPayments(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admPayFilter"><option value="">Tất cả trạng thái</option><option value="true">Đang hoạt động</option><option value="false">Tạm dừng</option></select></div>' + sectionHeader('Phương thức thanh toán', 'payments', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddPaymentModal()">+ Thêm phương thức</button>') + '<div id="admPayTable">' + skeleton(5) + '</div><div id="admPayPage"></div>';
    $('admPayFilter').addEventListener('change', function () { pageState.active = this.value; pageState.page = 0; loadPayments(); });
    bindSearch('payments'); loadPayments();
  }
  function loadPayments() {
    var p = pageState;
    var q = (p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '') + (p.active ? '&active=' + p.active : '');
    api('/order/payment-methods/search?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admPayTable').innerHTML = emptyMsg('Không tìm thấy phương thức thanh toán'); $('admPayPage').innerHTML = ''; return; }
      $('admPayTable').innerHTML = tableWrap(['ID', 'Tên phương thức', 'Trạng thái', 'Thao tác'], rows.map(function (p) {
        var badge = p.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
        var toggleBtn = p.active ? '<button class="adm-btn adm-btn--warning adm-btn--sm" onclick="ADM.togglePaymentStatus(' + p.id + ',true)">Tạm dừng</button>' : '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.togglePaymentStatus(' + p.id + ',false)">Mở lại</button>';
        var editBtn = '<button class="adm-btn adm-btn--info adm-btn--sm" onclick="ADM.openEditPaymentModal(' + p.id + ')">Sửa</button>';
        var delBtn = '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delPayment(' + p.id + ')">Xóa</button>';
        return '<tr><td>#' + p.id + '</td><td style="font-weight:600">' + esc(p.name) + '</td><td>' + badge + '</td><td><div class="adm-table-actions">' + editBtn + ' ' + toggleBtn + ' ' + delBtn + '</div></td></tr>';
      }).join(''));
      $('admPayPage').innerHTML = paginate(data);
    }).catch(function () { $('admPayTable').innerHTML = emptyMsg('Lỗi tải phương thức thanh toán'); });
  }

  /* ===== WORKSHOPS ===== */
  function renderWorkshops(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admWsFilter"><option value="">Tất cả trạng thái</option><option value="true">Đang hoạt động</option><option value="false">Đã tạm dừng</option></select></div>' + sectionHeader('Workshop', 'workshops', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddWorkshopModal()">+ Thêm Workshop</button>') + '<div id="admWsTable">' + skeleton(5) + '</div><div id="admWsPage"></div>';
    $('admWsFilter').addEventListener('change', function () { pageState.active = this.value; pageState.page = 0; loadWorkshops(); });
    bindSearch('workshops'); loadWorkshops();
  }
  function loadWorkshops() {
    var p = pageState;
    var q = (p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '') + (p.active ? '&active=' + p.active : '');
    api('/workshop/workshops/all?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admWsTable').innerHTML = emptyMsg('Không có workshop'); $('admWsPage').innerHTML = ''; return; }
      $('admWsTable').innerHTML = tableWrap(['ID', 'Ảnh', 'Tên', 'Địa điểm', 'Giá', 'Số người', 'Trạng thái', 'Thao tác'], rows.map(function (w) {
        var img = w.mainImage ? '<img class="adm-thumb" src="' + w.mainImage + '" alt="">' : '—';
        var badge = w.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
        var toggleBtn = w.active ? '<button class="adm-btn adm-btn--warning adm-btn--sm" onclick="ADM.toggleWorkshopStatus(' + w.id + ',true)">Tạm dừng</button>' : '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.toggleWorkshopStatus(' + w.id + ',false)">Hoạt động</button>';
        var nameLink = `<a href="javascript:void(0)" onclick="ADM.openWorkshopDetailModal(${w.id})" style="font-weight:600;color:var(--adm-accent2);text-decoration:none">${esc(w.name)}</a>`;
        return '<tr><td>#' + w.id + '</td><td>' + img + '</td><td>' + nameLink + '</td><td>' + esc(w.location || '—') + '</td><td>' + fmtMoney(w.price) + '</td><td>' + ((w.currentParticipants || 0) + '/' + (w.maxParticipants || 0)) + '</td><td>' + badge + '</td><td><div class="adm-table-actions">' + toggleBtn + ' <button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delWorkshop(' + w.id + ')">Xóa</button></div></td></tr>';
      }).join(''));
      $('admWsPage').innerHTML = paginate(data);
    }).catch(function () { $('admWsTable').innerHTML = emptyMsg('Lỗi tải workshop'); });
  }

  /* ===== POSTS ===== */
  function renderPosts(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admPostFilter"><option value="">Tất cả trạng thái</option><option value="true">Đã đăng</option><option value="false">Bản nháp</option></select></div>' + sectionHeader('Bài viết', 'posts', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddPostModal()">+ Thêm bài viết</button>') + '<div id="admPostTable">' + skeleton(5) + '</div><div id="admPostPage"></div>';
    $('admPostFilter').addEventListener('change', function () { pageState.published = this.value; pageState.page = 0; loadPosts(); });
    bindSearch('posts'); loadPosts();
  }
  function loadPosts() {
    var p = pageState;
    var q = (p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '') + (p.published ? '&published=' + p.published : '');
    api('/content/posts?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admPostTable').innerHTML = emptyMsg('Không có bài viết'); $('admPostPage').innerHTML = ''; return; }
      $('admPostTable').innerHTML = tableWrap(['ID', 'Ảnh', 'Tiêu đề', 'Danh mục', 'Trạng thái', 'Ngày tạo', 'Thao tác'], rows.map(function (p) {
        var img = p.thumbnail ? '<img class="adm-thumb" src="' + p.thumbnail + '" alt="">' : '—';
        var pub = p.published ? '<span class="adm-badge adm-badge--success">Đã đăng</span>' : '<span class="adm-badge adm-badge--warning">Nháp</span>';
        var toggleBtn = p.published ? '<button class="adm-btn adm-btn--warning adm-btn--sm" onclick="ADM.togglePostStatus(' + p.id + ',true)">Gỡ bài</button>' : '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.togglePostStatus(' + p.id + ',false)">Đăng bài</button>';
        var titleLink = `<a href="javascript:void(0)" onclick="ADM.openPostDetailModal(${p.id})" style="font-weight:600;color:var(--adm-accent2);text-decoration:none;border-bottom:none">${esc(p.title)}</a>`;
        return '<tr><td>#' + p.id + '</td><td>' + img + '</td><td>' + titleLink + '</td><td>' + esc(p.category ? p.category.name : '—') + '</td><td>' + pub + '</td><td>' + fmtDate(p.createdAt) + '</td><td><div class="adm-table-actions">' + toggleBtn + ' <button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delPost(' + p.id + ')">Xóa</button></div></td></tr>';
      }).join(''));
      $('admPostPage').innerHTML = paginate(data);
    }).catch(function () { $('admPostTable').innerHTML = emptyMsg('Lỗi tải bài viết'); });
  }

  /* ===== POST CATEGORIES ===== */
  function renderPostCategories(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admPostCatFilter"><option value="">Tất cả trạng thái</option><option value="true">Đang hoạt động</option><option value="false">Tạm dừng</option></select></div>' + 
                  sectionHeader('Danh mục bài viết', 'postCategories', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddPostCategoryModal()">+ Thêm danh mục</button>') + 
                  '<div id="admPostCatTable">' + skeleton(5) + '</div><div id="admPostCatPage"></div>';
    $('admPostCatFilter').addEventListener('change', function () { pageState.active = this.value; pageState.page = 0; loadPostCategories(); });
    bindSearch('postCategories'); 
    loadPostCategories();
  }
  function loadPostCategories() {
    var p = pageState;
    var q = (p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '') + (p.active ? '&active=' + p.active : '');
    api('/content/categories?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admPostCatTable').innerHTML = emptyMsg('Không tìm thấy danh mục'); $('admPostCatPage').innerHTML = ''; return; }
      $('admPostCatTable').innerHTML = tableWrap(['ID', 'Tên danh mục', 'Mô tả', 'Trạng thái', 'Thao tác'], rows.map(function (s) {
        var badge = s.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
        var toggleBtn = s.active ? '<button class="adm-btn adm-btn--warning adm-btn--sm" onclick="ADM.togglePostCategoryStatus(' + s.id + ',true)">Tạm dừng</button>' : '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.togglePostCategoryStatus(' + s.id + ',false)">Mở lại</button>';
        var editBtn = '<button class="adm-btn adm-btn--info adm-btn--sm" onclick="ADM.openEditPostCategoryModal(' + s.id + ')">Sửa</button>';
        var delBtn = '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delPostCategory(' + s.id + ')">Xóa</button>';
        var nameLink = `<a href="javascript:void(0)" onclick="ADM.openPostCategoryDetailModal(${s.id})" style="font-weight:600;color:var(--adm-accent2);text-decoration:none">${esc(s.name)}</a>`;
        return '<tr><td>#' + s.id + '</td><td>' + nameLink + '</td><td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(s.description || '—') + '</td><td>' + badge + '</td><td><div class="adm-table-actions">' + editBtn + ' ' + toggleBtn + ' ' + delBtn + '</div></td></tr>';
      }).join(''));
      $('admPostCatPage').innerHTML = paginate(data);
    }).catch(function () { $('admPostCatTable').innerHTML = emptyMsg('Lỗi tải danh mục bài viết'); });
  }

  /* ===== REVIEWS ===== */
  function renderReviews(c) {
    c.innerHTML = sectionHeader('⭐ Đánh giá sản phẩm', 'reviews') + '<div id="admRevTable">' + skeleton(5) + '</div><div id="admRevPage"></div>';
    bindSearch('reviews'); loadReviews();
  }
  function loadReviews() {
    var p = pageState;
    var q = p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '';
    api('/content/api/v1/reviews/admin/all?page=' + p.page + '&size=' + p.size + q).then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admRevTable').innerHTML = emptyMsg('Chưa có đánh giá'); $('admRevPage').innerHTML = ''; return; }
      $('admRevTable').innerHTML = tableWrap(['ID', 'Sản phẩm', 'Người dùng', 'Sao', 'Nội dung', 'Ngày', 'Thao tác'], rows.map(function (r) {
        var stars = '⭐'.repeat(r.rating || 0);
        var acts = '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delReview(' + r.id + ')">Xóa</button>';
        return '<tr><td>#' + r.id + '</td><td>' + esc(r.productName || 'SP-' + r.productId) + '</td><td>' + esc(r.username || '—') + '</td><td>' + stars + '</td><td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(r.comment || '—') + '</td><td>' + fmtDate(r.createdAt) + '</td><td>' + acts + '</td></tr>';
      }).join(''));
      $('admRevPage').innerHTML = paginate(data);
    }).catch(function () { $('admRevTable').innerHTML = emptyMsg('Lỗi tải đánh giá'); });
  }

  /* ===== USERS ===== */
  function renderUsers(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admUserFilter"><option value="">Tất cả vai trò</option><option value="ADMIN">Admin</option><option value="USER">User</option></select></div>' + sectionHeader('👥 Danh sách người dùng', 'users') + '<div id="admUserTable">' + skeleton(5) + '</div>';
    $('admUserFilter').addEventListener('change', function () { pageState.role = this.value; loadUsers(); });
    bindSearch('users'); loadUsers();
  }
  function loadUsers() {
    var p = pageState;
    var q = (p.keyword ? '?keyword=' + encodeURIComponent(p.keyword) : '') + (p.role ? (p.keyword ? '&' : '?') + 'role=' + p.role : '');
    api('/identity/users' + q).then(function (data) {
      var rows = Array.isArray(data) ? data : [];
      if (!rows.length) { $('admUserTable').innerHTML = emptyMsg('Không tìm thấy người dùng'); return; }
      $('admUserTable').innerHTML = tableWrap(['ID', 'Tên', 'Email', 'SĐT', 'Vai trò', 'Thao tác'], rows.map(function (u) {
        var badge = u.roles === 'ADMIN' ? '<span class="adm-badge adm-badge--info">Admin</span>' : '<span class="adm-badge adm-badge--neutral">User</span>';
        return '<tr><td>#' + u.id + '</td><td style="font-weight:600;color:var(--adm-text)">' + esc(u.username || '—') + '</td><td>' + esc(u.email || '—') + '</td><td>' + esc(u.phone || '—') + '</td><td>' + badge + '</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delUser(' + u.id + ')">Xóa</button></td></tr>';
      }).join(''));
    }).catch(function () { $('admUserTable').innerHTML = emptyMsg('Lỗi tải người dùng'); });
  }


  /* ===== WORKSHOP REGISTRATIONS ===== */
  function renderWorkshopRegs(c) {
    c.innerHTML = '<div class="adm-quick-actions"><select class="adm-select" id="admRegFilter"><option value="">Tất cả trạng thái</option><option value="CONFIRMED">Đã xác nhận</option><option value="COMPLETED">Đã tham gia</option><option value="CANCELLED">Đã hủy</option></select></div>' + sectionHeader('ĐK Workshop', 'workshopRegs') + '<div id="admRegTable">' + skeleton(5) + '</div><div id="admRegPage"></div>';
    $('admRegFilter').addEventListener('change', function () { pageState.status = this.value; pageState.page = 0; loadWorkshopRegs(); });
    bindSearch('workshopRegs'); loadWorkshopRegs();
  }
  function loadWorkshopRegs() {
    var p = pageState; var statusQ = p.status ? '&status=' + p.status : '';
    var q = p.keyword ? '&keyword=' + encodeURIComponent(p.keyword) : '';
    api('/workshop/regis-workshops/all?page=' + p.page + '&size=' + p.size + statusQ + q + '&sortBy=registrationDate&sortDir=desc').then(function (data) {
      var rows = data.content || [];
      if (!rows.length) { $('admRegTable').innerHTML = emptyMsg('Không có lượt đăng ký nào'); $('admRegPage').innerHTML = ''; return; }
      $('admRegTable').innerHTML = tableWrap(['ID', 'Khách hàng', 'Workshop', 'Số vé', 'Tổng tiền', 'Ngày ĐK', 'Trạng thái', 'Thao tác'], rows.map(function (r) {
        var badge = r.status === 'CONFIRMED' ? '<span class="adm-badge adm-badge--info">Đã xác nhận</span>' :
          r.status === 'COMPLETED' ? '<span class="adm-badge adm-badge--success">Đã tham gia</span>' :
            '<span class="adm-badge adm-badge--danger">Đã hủy</span>';
        var actions = '';
        if (r.status === 'CONFIRMED') {
          actions = '<button class="adm-btn adm-btn--success adm-btn--sm" onclick="ADM.checkInWorkshop(' + r.id + ')">Check-in</button> ' +
            '<button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.cancelWorkshopReg(' + r.id + ')">Hủy</button>';
        }
        var idLink = `<a href="javascript:void(0)" onclick="ADM.openWorkshopRegDetailModal(${r.id})" style="font-weight:600;color:var(--adm-accent2)">#${r.id}</a>`;
        return '<tr><td>' + idLink + '</td><td>' + esc(r.customerName || 'KH-' + r.customerId) + '</td><td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(r.workshopName) + '</td><td>' + r.ticketQuantity + '</td><td>' + fmtMoney(r.totalPrice) + '</td><td>' + fmtDate(r.registrationDate) + '</td><td>' + badge + '</td><td>' + actions + '</td></tr>';
      }).join(''));
      $('admRegPage').innerHTML = paginate(data);
    }).catch(function () { $('admRegTable').innerHTML = emptyMsg('Lỗi tải danh sách đăng ký'); });
  }

  /* ===== ACTIONS (exposed globally) ===== */
  window.ADM = {
    goPage: function (page) { pageState.page = page; renderSection(currentSection); },
    delProduct: function (id) { if (confirm('Xóa sản phẩm #' + id + '?')) api('/product/products/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa sản phẩm'); loadProducts(); }).catch(function () { toast('Lỗi xóa sản phẩm'); }); },
    delWorkshop: function (id) { if (confirm('Xóa workshop #' + id + '?')) api('/workshop/workshops/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa workshop'); loadWorkshops(); }).catch(function () { toast('Lỗi xóa workshop'); }); },
    delPost: function (id) { if (confirm('Xóa bài viết #' + id + '?')) api('/content/posts/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa bài viết'); loadPosts(); }).catch(function () { toast('Lỗi xóa bài viết'); }); },
    delUser: function (id) { if (confirm('Xóa người dùng #' + id + '?')) api('/identity/users/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa người dùng'); renderUsers($('admContent')); }).catch(function () { toast('Lỗi xóa người dùng'); }); },
    delReview: function (id) { if (confirm('Xóa đánh giá #' + id + '?')) api('/content/api/v1/reviews/admin/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa đánh giá'); renderReviews($('admContent')); }).catch(function () { toast('Lỗi xóa đánh giá'); }); },
    delShipping: function (id) { if (confirm('Xóa đơn vị vận chuyển #' + id + '?')) api('/order/shipping-methods/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa đơn vị vận chuyển'); loadShipping(); }).catch(function () { toast('Lỗi xóa'); }); },
    delPayment: function (id) { if (confirm('Xóa phương thức thanh toán #' + id + '?')) api('/order/payment-methods/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa phương thức thanh toán'); loadPayments(); }).catch(function () { toast('Lỗi xóa'); }); },
    delPostCategory: function (id) { if (confirm('Xóa danh mục bài viết #' + id + '?')) api('/content/categories/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa danh mục'); loadPostCategories(); }).catch(function () { toast('Lỗi xóa danh mục'); }); },
    delCategory: function (id) { if (confirm('Xóa danh mục sản phẩm #' + id + '?')) api('/product/categories/' + id, { method: 'DELETE' }).then(function () { toast('Đã xóa danh mục'); loadCategories(); }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Không thể xóa')); }); },
    openAddCategoryModal: function () {
      showModal('Thêm danh mục sản phẩm', `
        <form class="adm-form" id="addCatForm">
          <div class="adm-form-group">
            <label>Tên danh mục</label>
            <input type="text" name="name" placeholder="Ví dụ: Ấm chén Tử Sa">
          </div>
          <div class="adm-form-group">
            <label>Ảnh đại diện (Chọn từ máy tính)</label>
            <input type="file" id="addCatFile" accept="image/*">
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu danh mục</button>
          </div>
        </form>
      `);
      $('addCatForm').onsubmit = function (e) {
        e.preventDefault();
        var fd = new FormData(this);
        var name = fd.get('name');
        if (!name) { toast('Vui lòng nhập tên danh mục'); return; }
        
        var finalFd = new FormData();
        finalFd.append('request', new Blob([JSON.stringify({ name: name })], { type: 'application/json' }));
        
        var fileInput = $('addCatFile');
        if (fileInput && fileInput.files[0]) {
          finalFd.append('image', fileInput.files[0]);
        }
        
        api('/product/categories', { method: 'POST', body: finalFd, formData: true }).then(function () {
          toast('Đã thêm thành công'); ADM.closeModal(); loadCategories();
        }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Không thể lưu')); });
      };
    },
    openEditCategoryModal: function (id) {
      api('/product/categories/' + id).then(function (s) {
        showModal('Sửa danh mục sản phẩm #' + id, `
          <form class="adm-form" id="editCatForm">
            <div class="adm-form-group">
              <label>Tên danh mục</label>
              <input type="text" name="name" value="${esc(s.name)}">
            </div>
            <div class="adm-form-group">
              <label>Trạng thái</label>
              <select name="active">
                <option value="true" ${s.active ? 'selected' : ''}>Hoạt động</option>
                <option value="false" ${!s.active ? 'selected' : ''}>Tạm dừng</option>
              </select>
            </div>
            <div class="adm-form-group">
              <label>Ảnh hiện tại</label>
              <div id="editCatImage" style="margin-top:8px">
                ${s.imageUrl ? `
                  <div class="ws-img-edit-item" style="position:relative;display:inline-block;cursor:pointer" onclick="this.classList.toggle('to-delete')">
                    <img src="${s.imageUrl}" style="width:120px;height:120px;object-fit:cover;border-radius:8px">
                    <div class="ws-img-delete-overlay" style="position:absolute;inset:0;background:rgba(232,93,93,0.6);display:none;align-items:center;justify-content:center;color:#fff;font-size:24px;border-radius:8px">✕</div>
                    <p style="font-size:11px;text-align:center;margin-top:4px;color:var(--adm-danger)">Nhấn để xóa</p>
                  </div>
                ` : '<p style="color:var(--adm-text3)">Chưa có ảnh</p>'}
              </div>
            </div>
            <div class="adm-form-group">
              <label>Thay đổi ảnh mới</label>
              <input type="file" id="editCatFile" accept="image/*">
            </div>
            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Cập nhật</button>
            </div>
          </form>
        `);
        $('editCatForm').onsubmit = function (e) {
          e.preventDefault();
          var fd = new FormData(this);
          var obj = {
            name: fd.get('name'),
            active: fd.get('active') === 'true',
            deleteImage: document.querySelector('#editCatImage .ws-img-edit-item.to-delete') !== null
          };
          
          var finalFd = new FormData();
          finalFd.append('request', new Blob([JSON.stringify(obj)], { type: 'application/json' }));
          
          var fileInput = $('editCatFile');
          if (fileInput && fileInput.files[0]) {
            finalFd.append('image', fileInput.files[0]);
          }
          
          api('/product/categories/' + id, { method: 'PUT', body: finalFd, formData: true }).then(function () {
            toast('Đã cập nhật thành công'); ADM.closeModal(); loadCategories();
          }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Cập nhật thất bại')); });
        };
      }).catch(function () { toast('Lỗi tải thông tin danh mục'); });
    },
    openCategoryDetailModal: function (id) {
      api('/product/categories/' + id).then(function (s) {
        showModal('Chi tiết danh mục sản phẩm #' + id, `
          <div class="adm-detail">
            <div style="display:grid;grid-template-columns:auto 1fr;gap:24px;margin-bottom:24px">
              ${s.imageUrl ? `<img src="${s.imageUrl}" style="width:140px;height:140px;object-fit:cover;border-radius:12px;border:1px solid var(--adm-border)">` : ''}
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📂 Thông tin danh mục</h4>
                <p style="margin-bottom:8px"><strong>ID:</strong> #${s.id}</p>
                <p style="margin-bottom:8px"><strong>Tên danh mục:</strong> ${esc(s.name)}</p>
                <p style="margin-bottom:8px"><strong>Trạng thái:</strong> ${s.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>'}</p>
                <p style="margin-bottom:8px"><strong>Số sản phẩm:</strong> ${s.productCount || 0}</p>
                <p style="margin-bottom:8px"><strong>Ngày tạo:</strong> ${fmtDate(s.createdAt)}</p>
              </div>
            </div>
            <div class="adm-form-actions" style="margin-top:32px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng cửa sổ</button>
              <button class="adm-btn adm-btn--primary" onclick="ADM.closeModal(); ADM.openEditCategoryModal(${s.id})">Chỉnh sửa danh mục</button>
            </div>
          </div>
        `);
      }).catch(function () { toast('Không thể tải thông tin danh mục'); });
    },
    togglePostCategoryStatus: function (id, currentActive) {
      if (confirm(currentActive ? 'Tạm dừng danh mục này?' : 'Mở lại danh mục này?')) {
        api('/content/categories/' + id, { method: 'PUT', body: JSON.stringify({ active: !currentActive }) }).then(function () { toast('Đã cập nhật trạng thái'); loadPostCategories(); }).catch(function () { toast('Lỗi cập nhật'); });
      }
    },
    openAddPostCategoryModal: function () {
      showModal('Thêm danh mục bài viết', `
        <form class="adm-form" id="addPostCatForm">
          <div class="adm-form-group">
            <label>Tên danh mục</label>
            <input type="text" name="name" placeholder="Ví dụ: Tin tức gốm sứ">
          </div>
          <div class="adm-form-group">
            <label>Mô tả</label>
            <textarea name="description" rows="3"></textarea>
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu danh mục</button>
          </div>
        </form>
      `);
      $('addPostCatForm').onsubmit = function (e) {
        e.preventDefault();
        var obj = Object.fromEntries(new FormData(this).entries());
        if (!obj.name) { toast('Vui lòng nhập tên danh mục'); return; }
        api('/content/categories', { method: 'POST', body: JSON.stringify(obj) }).then(function () {
          toast('Đã thêm thành công'); ADM.closeModal(); loadPostCategories();
        }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Không thể lưu')); });
      };
    },
    openEditPostCategoryModal: function (id) {
      api('/content/categories/' + id).then(function (s) {
        showModal('Sửa danh mục bài viết #' + id, `
          <form class="adm-form" id="editPostCatForm">
            <div class="adm-form-group">
              <label>Tên danh mục</label>
              <input type="text" name="name" value="${esc(s.name)}">
            </div>
            <div class="adm-form-group">
              <label>Mô tả</label>
              <textarea name="description" rows="3">${esc(s.description || '')}</textarea>
            </div>
            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Cập nhật</button>
            </div>
          </form>
        `);
        $('editPostCatForm').onsubmit = function (e) {
          e.preventDefault();
          var obj = Object.fromEntries(new FormData(this).entries());
          api('/content/categories/' + id, { method: 'PUT', body: JSON.stringify(obj) }).then(function () {
            toast('Đã cập nhật thành công'); ADM.closeModal(); loadPostCategories();
          }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Cập nhật thất bại')); });
        };
      }).catch(function () { toast('Lỗi tải thông tin danh mục'); });
    },
    openPostCategoryDetailModal: function (id) {
      api('/content/categories/' + id).then(function (s) {
        showModal('Chi tiết danh mục bài viết #' + id, `
          <div class="adm-detail">
            <div style="margin-bottom:24px">
              <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📂 Thông tin danh mục</h4>
              <p style="margin-bottom:8px"><strong>Tên danh mục:</strong> ${esc(s.name)}</p>
              <p style="margin-bottom:8px"><strong>Trạng thái:</strong> ${s.active ? '<span class="adm-badge adm-badge--success">Hoạt động</span>' : '<span class="adm-badge adm-badge--neutral">Tạm dừng</span>'}</p>
              <p style="margin-bottom:8px"><strong>Ngày tạo:</strong> ${fmtDate(s.createdAt)}</p>
            </div>
            <div>
              <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📖 Mô tả</h4>
              <div style="font-size:14px;color:var(--adm-text2);line-height:1.6;white-space:pre-line">
                ${esc(s.description || 'Chưa có mô tả cho danh mục này.')}
              </div>
            </div>
            <div class="adm-form-actions" style="margin-top:32px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng cửa sổ</button>
              <button class="adm-btn adm-btn--primary" onclick="ADM.closeModal(); ADM.openEditPostCategoryModal(${s.id})">Chỉnh sửa</button>
            </div>
          </div>
        `);
      }).catch(function () {
        toast('Không thể tải thông tin danh mục');
      });
    },
    updateOrder: function (id, status) { if (!status) return; if (confirm('Cập nhật đơn #' + id + ' → ' + status + '?')) api('/order/orders/' + id + '/status?status=' + status, { method: 'PATCH' }).then(function () { toast('Đã cập nhật'); loadOrders(); }).catch(function () { toast('Lỗi cập nhật'); }); },
    checkInWorkshop: function (id) { if (confirm('Xác nhận khách hàng đã tham gia workshop?')) api('/workshop/regis-workshops/' + id, { method: 'PUT' }).then(function () { toast('Đã check-in thành công'); loadWorkshopRegs(); }).catch(function (e) { toast('Lỗi check-in: ' + e.message); }); },
    cancelWorkshopReg: function (id) { if (confirm('Hủy đơn đăng ký này?')) api('/workshop/regis-workshops/' + id + '/cancel', { method: 'PATCH' }).then(function () { toast('Đã hủy đăng ký'); loadWorkshopRegs(); }).catch(function (e) { toast('Lỗi hủy đăng ký: ' + e.message); }); },
    toggleWorkshopStatus: function (id, currentActive) {
      if (confirm(currentActive ? 'Tạm dừng workshop này?' : 'Mở lại workshop này?')) {
        var fd = new FormData();
        fd.append("request", new Blob([JSON.stringify({ active: !currentActive })], { type: "application/json" }));
        api('/workshop/workshops/' + id, { method: 'PUT', body: fd, formData: true }).then(function () { toast('Đã cập nhật trạng thái'); loadWorkshops(); }).catch(function () { toast('Lỗi cập nhật'); });
      }
    },
    togglePostStatus: function (id, currentPublished) {
      if (confirm(currentPublished ? 'Gỡ bài viết này xuống bản nháp?' : 'Công khai bài viết này?')) {
        var fd = new FormData();
        fd.append("request", new Blob([JSON.stringify({ published: !currentPublished })], { type: "application/json" }));
        api('/content/posts/' + id, { method: 'PUT', body: fd, formData: true }).then(function () { toast('Đã cập nhật trạng thái bài viết'); loadPosts(); }).catch(function () { toast('Lỗi cập nhật'); });
      }
    },
    toggleShippingStatus: function (id, currentActive) {
      if (confirm(currentActive ? 'Tạm dừng đơn vị vận chuyển này?' : 'Mở lại đơn vị vận chuyển này?')) {
        api('/order/shipping-methods/' + id + '/status?active=' + !currentActive, { method: 'PATCH' }).then(function () { toast('Đã cập nhật trạng thái'); loadShipping(); }).catch(function () { toast('Lỗi cập nhật'); });
      }
    },
    togglePaymentStatus: function (id, currentActive) {
      if (confirm(currentActive ? 'Tạm dừng phương thức thanh toán này?' : 'Mở lại phương thức thanh toán này?')) {
        api('/order/payment-methods/' + id + '/status?active=' + !currentActive, { method: 'PATCH' }).then(function () { toast('Đã cập nhật trạng thái'); loadPayments(); }).catch(function () { toast('Lỗi cập nhật'); });
      }
    },
    openAddShippingModal: function () {
      showModal('Thêm đơn vị vận chuyển', `
        <form class="adm-form" id="addShipForm">
          <div class="adm-form-group">
            <label>Tên đơn vị</label>
            <input type="text" name="name" placeholder="Ví dụ: Giao Hàng Nhanh">
          </div>
          <div class="adm-form-group">
            <label>Phí vận chuyển (VNĐ)</label>
            <input type="number" name="shippingFee" placeholder="Ví dụ: 30000">
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu đơn vị</button>
          </div>
        </form>
      `);
      $('addShipForm').onsubmit = function (e) {
        e.preventDefault();
        var obj = Object.fromEntries(new FormData(this).entries());
        if (!obj.name) { toast('Vui lòng nhập tên'); return; }
        if (obj.shippingFee === "") obj.shippingFee = 0;
        api('/order/shipping-methods', { method: 'POST', body: JSON.stringify(obj) }).then(function () {
          toast('Đã thêm thành công'); ADM.closeModal(); loadShipping();
        }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Không thể lưu')); });
      };
    },
    openEditShippingModal: function (id) {
      api('/order/shipping-methods/search?keyword=' + id).then(function (data) {
        var s = (data.content || []).find(it => it.id === id);
        if (!s) return;
        showModal('Sửa đơn vị vận chuyển #' + id, `
          <form class="adm-form" id="editShipForm">
            <div class="adm-form-group">
              <label>Tên đơn vị</label>
              <input type="text" name="name" value="${esc(s.name)}">
            </div>
            <div class="adm-form-group">
              <label>Phí vận chuyển (VNĐ)</label>
              <input type="number" name="shippingFee" value="${s.shippingFee}">
            </div>
            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Cập nhật</button>
            </div>
          </form>
        `);
        $('editShipForm').onsubmit = function (e) {
          e.preventDefault();
          var obj = Object.fromEntries(new FormData(this).entries());
          api('/order/shipping-methods/' + id, { method: 'PUT', body: JSON.stringify(obj) }).then(function () {
            toast('Đã cập nhật thành công'); ADM.closeModal(); loadShipping();
          }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Cập nhật thất bại')); });
        };
      });
    },
    openAddPaymentModal: function () {
      showModal('Thêm phương thức thanh toán', `
        <form class="adm-form" id="addPayForm">
          <div class="adm-form-group">
            <label>Tên phương thức</label>
            <input type="text" name="name" placeholder="Ví dụ: Ví MoMo">
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu phương thức</button>
          </div>
        </form>
      `);
      $('addPayForm').onsubmit = function (e) {
        e.preventDefault();
        var obj = Object.fromEntries(new FormData(this).entries());
        if (!obj.name) { toast('Vui lòng nhập tên'); return; }
        api('/order/payment-methods', { method: 'POST', body: JSON.stringify(obj) }).then(function () {
          toast('Đã thêm thành công'); ADM.closeModal(); loadPayments();
        }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Không thể lưu')); });
      };
    },
    openEditPaymentModal: function (id) {
      api('/order/payment-methods/search?keyword=' + id).then(function (data) {
        var p = (data.content || []).find(it => it.id === id);
        if (!p) return;
        showModal('Sửa phương thức thanh toán #' + id, `
          <form class="adm-form" id="editPayForm">
            <div class="adm-form-group">
              <label>Tên phương thức</label>
              <input type="text" name="name" value="${esc(p.name)}">
            </div>
            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Cập nhật</button>
            </div>
          </form>
        `);
        $('editPayForm').onsubmit = function (e) {
          e.preventDefault();
          var obj = Object.fromEntries(new FormData(this).entries());
          api('/order/payment-methods/' + id, { method: 'PUT', body: JSON.stringify(obj) }).then(function () {
            toast('Đã cập nhật thành công'); ADM.closeModal(); loadPayments();
          }).catch(function (err) { toast('Lỗi: ' + (err.message || 'Cập nhật thất bại')); });
        };
      });
    },

    openOrderDetailModal: function (id) {
      api('/order/orders/' + id).then(function (o) {
        var itemsHTML = (o.orderDetails || []).map(item => `
          <div style="display:flex;justify-content:space-between;padding:12px 0;border-bottom:1px solid var(--adm-border)">
            <div>
              <p style="font-weight:600">${esc(item.productName)}</p>
              <p style="font-size:12px;color:var(--adm-text3)">Số lượng: ${item.quantity} x ${fmtMoney(item.priceAtPurchase)}</p>
            </div>
            <p style="font-weight:600">${fmtMoney(item.subTotal || (item.priceAtPurchase * item.quantity))}</p>
          </div>
        `).join('');

        showModal('Chi tiết đơn hàng #' + id, `
          <div class="adm-detail">
            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(240px, 1fr));gap:24px;margin-bottom:24px">
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">👤 Thông tin khách hàng</h4>
                <p style="margin-bottom:6px"><strong>Họ tên:</strong> ${esc(o.customerName || '—')}</p>
                <p style="margin-bottom:6px"><strong>Số điện thoại:</strong> ${esc(o.phoneNumber || '—')}</p>
                <p style="margin-bottom:6px"><strong>Địa chỉ giao hàng:</strong> ${esc(o.address || '—')}</p>
              </div>
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📦 Thông tin đơn hàng</h4>
                <p style="margin-bottom:6px"><strong>Ngày đặt:</strong> ${fmtDate(o.createdAt)}</p>
                <p style="margin-bottom:6px"><strong>Trạng thái:</strong> ${statusBadge(o.status)}</p>
                <p style="margin-bottom:6px"><strong>Thanh toán:</strong> ${esc(o.paymentMethod || '—')}</p>
                <p style="margin-bottom:6px"><strong>Vận chuyển:</strong> ${esc(o.shippingMethod || '—')}</p>
              </div>
            </div>

            <div style="background:var(--adm-bg2);padding:16px;border-radius:8px;border:1px dashed var(--adm-border);margin-bottom:24px">
              <h4 style="margin-bottom:8px">📝 Ghi chú từ khách hàng</h4>
              <p style="font-style:italic;color:var(--adm-text2)">${esc(o.note || 'Không có ghi chú.')}</p>
            </div>

            <div>
              <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">🛒 Danh mục sản phẩm</h4>
              <div style="max-height:300px;overflow-y:auto">
                ${itemsHTML || '<p style="color:var(--adm-text3)">Không có dữ liệu sản phẩm.</p>'}
              </div>
              <div style="display:flex;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid var(--adm-border)">
                <p>Tiền hàng:</p>
                <p style="font-weight:600">${fmtMoney(o.totalAmount - (o.shippingFee || 0))}</p>
              </div>
              <div style="display:flex;justify-content:space-between;margin-top:8px">
                <p>Phí vận chuyển:</p>
                <p style="font-weight:600">${fmtMoney(o.shippingFee || 0)}</p>
              </div>
              <div style="display:flex;justify-content:space-between;margin-top:12px;padding-top:12px;border-top:2px solid var(--adm-border)">
                <h3 style="font-size:18px">Tổng giá trị đơn hàng:</h3>
                <h3 style="color:var(--adm-warning);font-size:22px;font-weight:800">${fmtMoney(o.totalAmount)}</h3>
              </div>
            </div>
            <div class="adm-form-actions" style="margin-top:32px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng cửa sổ</button>
            </div>
          </div>
        `);
      }).catch(() => toast('Không thể tải chi tiết đơn hàng'));
    },

    openWorkshopRegDetailModal: function (id) {
      api('/workshop/regis-workshops/' + id).then(function (r) {
        showModal('Chi tiết đăng ký Workshop #' + id, `
          <div class="adm-detail">
            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(280px, 1fr));gap:24px;margin-bottom:24px">
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">👤 Thông tin khách hàng</h4>
                <p style="margin-bottom:6px"><strong>Họ tên:</strong> ${esc(r.customerName)}</p>
                <p style="margin-bottom:6px"><strong>Số điện thoại:</strong> ${esc(r.customerPhone || '—')}</p>
                <p style="margin-bottom:6px"><strong>Email:</strong> ${esc(r.customerEmail || '—')}</p>
                <p style="margin-bottom:6px"><strong>Mã khách hàng:</strong> #${r.customerId}</p>
              </div>
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">🎨 Thông tin Workshop</h4>
                <p style="margin-bottom:6px"><strong>Tên Workshop:</strong> ${esc(r.workshopName)}</p>
                <p style="margin-bottom:6px"><strong>Địa điểm:</strong> ${esc(r.location || '—')}</p>
                <p style="margin-bottom:6px"><strong>Thời gian:</strong> ${fmtDate(r.workshopStartDate)} - ${fmtDate(r.workshopEndDate)}</p>
              </div>
            </div>

            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(280px, 1fr));gap:24px;margin-bottom:24px">
              <div>
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">🎫 Chi tiết đăng ký</h4>
                <p style="margin-bottom:6px"><strong>Ngày đăng ký:</strong> ${fmtDate(r.registrationDate)}</p>
                <p style="margin-bottom:6px;color:var(--adm-warning)"><strong>Ngày tham gia:</strong> ${r.participationDate ? new Date(r.participationDate).toLocaleDateString('vi-VN') : '—'}</p>
                <p style="margin-bottom:6px;color:var(--adm-warning)"><strong>Giờ tham gia:</strong> ${r.participationTime || '—'}</p>
                <p style="margin-bottom:6px"><strong>Số lượng vé:</strong> ${r.ticketQuantity} vé</p>
                <p style="margin-bottom:6px"><strong>Giá mỗi vé:</strong> ${fmtMoney(r.pricePerTicket)}</p>
                <p style="margin-bottom:6px"><strong>Trạng thái:</strong> <span style="font-weight:600">${r.status === 'CONFIRMED' ? 'Đã xác nhận' : r.status === 'COMPLETED' ? 'Đã tham gia' : 'Đã hủy'}</span></p>
              </div>
              <div style="background:var(--adm-bg2);padding:16px;border-radius:8px;border:1px dashed var(--adm-border)">
                <h4 style="margin-bottom:8px">📝 Ghi chú từ khách</h4>
                <p style="font-style:italic;color:var(--adm-text2)">${esc(r.note || 'Không có ghi chú.')}</p>
              </div>
            </div>

            <div style="display:flex;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:2px solid var(--adm-border)">
              <h3 style="font-size:18px">Tổng tiền thanh toán:</h3>
              <h3 style="color:var(--adm-warning);font-size:22px;font-weight:800">${fmtMoney(r.totalPrice)}</h3>
            </div>

            <div class="adm-form-actions" style="margin-top:32px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng cửa sổ</button>
            </div>
          </div>
        `);
      }).catch(() => toast('Không thể tải chi tiết đăng ký'));
    },

    openProductDetailModal: function (id) {
      api('/product/products/' + id).then(function (p) {
        var imagesHTML = (p.imageUrls || []).map(img => `<img src="${img}" style="width:100px;height:100px;object-fit:cover;border-radius:8px;margin-right:8px;margin-bottom:8px;border:1px solid var(--adm-border)">`).join('');

        showModal('Chi tiết sản phẩm #' + id, `
          <div class="adm-detail">
            <div style="display:flex;gap:24px;margin-bottom:24px;flex-wrap:wrap">
              <div style="flex:1;min-width:280px">
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">💎 Thông tin cơ bản</h4>
                <p style="margin-bottom:8px"><strong>Tên sản phẩm:</strong> ${esc(p.name)}</p>
                <p style="margin-bottom:8px"><strong>Thương hiệu:</strong> ${esc(p.brand || '—')}</p>
                <p style="margin-bottom:8px"><strong>Giá bán:</strong> <span style="color:var(--adm-warning);font-weight:700">${fmtMoney(p.price)}</span></p>
                <p style="margin-bottom:8px"><strong>Tồn kho:</strong> ${p.stockQuantity} sản phẩm</p>
                <p style="margin-bottom:8px"><strong>Danh mục:</strong> ${esc(p.categoryName || '—')}</p>
                <p style="margin-bottom:8px"><strong>Đánh giá:</strong> ⭐ ${p.averageRating || 0} (${p.reviewCount || 0} nhận xét)</p>
              </div>
              <div style="flex:1;min-width:280px">
                <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📖 Mô tả</h4>
                <div style="font-size:14px;color:var(--adm-text2);line-height:1.6;white-space:pre-line">
                  ${esc(p.description || 'Chưa có mô tả cho sản phẩm này.')}
                </div>
              </div>
            </div>

            <div>
              <h4 style="color:var(--adm-accent2);margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">🖼️ Hình ảnh sản phẩm</h4>

              <div style="display:flex;flex-wrap:wrap;margin-top:12px">
                ${imagesHTML || '<p style="color:var(--adm-text3)">Sản phẩm này chưa có hình ảnh.</p>'}
              </div>
            </div>

            <div class="adm-form-actions" style="margin-top:32px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng cửa sổ</button>
              <button class="adm-btn adm-btn--primary" onclick="ADM.closeModal(); ADM.openEditProductModal(${p.id})">Chỉnh sửa sản phẩm</button>
            </div>
          </div>
        `);
      }).catch(function () {
        toast('Không thể tải thông tin chi tiết sản phẩm');
      });
    },

    openAddWorkshopModal: function () {
      showModal('Thêm Workshop mới', `
        <form class="adm-form" id="addWorkshopForm">
          <div class="adm-form-group">
            <label>Tên Workshop</label>
            <input type="text" name="name" placeholder="Ví dụ: Lớp học làm gốm cơ bản">
          </div>
          <div class="adm-form-group">
            <label>Mô tả ngắn</label>
            <textarea name="description" rows="2"></textarea>
          </div>
          <div class="adm-form-group">
            <label>Nội dung chi tiết</label>
            <textarea name="content" rows="4"></textarea>
          </div>
          <div class="adm-form-row">
            <div class="adm-form-group">
              <label>Địa điểm</label>
              <input type="text" name="location">
            </div>
            <div class="adm-form-group">
              <label>Giá (VNĐ)</label>
              <input type="number" name="price">
            </div>
          </div>
          <div class="adm-form-row">
            <div class="adm-form-group">
              <label>Số người tối đa</label>
              <input type="number" name="maxParticipants">
            </div>
          </div>
          <div class="adm-form-row">
            <div class="adm-form-group">
              <label>Ngày bắt đầu</label>
              <input type="datetime-local" name="startDate">
            </div>
            <div class="adm-form-group">
              <label>Ngày kết thúc</label>
              <input type="datetime-local" name="endDate">
            </div>
          </div>
          <div class="adm-form-row">
            <div class="adm-form-group">
              <label>Bắt đầu đăng ký</label>
              <input type="datetime-local" name="registrationStartDate">
            </div>
            <div class="adm-form-group">
              <label>Kết thúc đăng ký</label>
              <input type="datetime-local" name="registrationEndDate">
            </div>
          </div>
          <div class="adm-form-group">
            <label>Đối tượng tham gia</label>
            <input type="text" name="targetAudience" placeholder="Ví dụ: Trẻ em từ 6-12 tuổi">
          </div>
          <div class="adm-form-group">
            <label>Dụng cụ chuẩn bị</label>
            <input type="text" name="tools" placeholder="Ví dụ: Tạp dề, khăn lau">
          </div>
          <div class="adm-form-group">
            <label>Quyền lợi</label>
            <input type="text" name="benefits" placeholder="Ví dụ: Miễn phí nước uống">
          </div>
          <div class="adm-form-group">
            <label>Hình ảnh Workshop (Chọn từ máy tính)</label>
            <input type="file" id="addWsFiles" multiple accept="image/*">
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu Workshop</button>
          </div>
        </form>
      `);

      $('addWorkshopForm').onsubmit = function (e) {
        e.preventDefault();
        var fd = new FormData(this);
        var obj = Object.fromEntries(fd.entries());

        // Frontend Validation
        if (!obj.name || !obj.name.trim()) { toast('Tên workshop không được để trống'); return; }
        if (!obj.description || !obj.description.trim()) { toast('Mô tả ngắn không được để trống'); return; }
        if (!obj.content || !obj.content.trim()) { toast('Nội dung không được để trống'); return; }
        if (!obj.location || !obj.location.trim()) { toast('Địa điểm không được để trống'); return; }
        if (!obj.price) { toast('Giá tiền không được để trống'); return; }
        if (!obj.maxParticipants) { toast('Số người không được để trống'); return; }

        obj.price = parseFloat(obj.price);
        obj.maxParticipants = parseInt(obj.maxParticipants);

        // Xử lý ngày tháng: Nếu trống thì gửi null
        if (!obj.startDate) obj.startDate = null;
        if (!obj.endDate) obj.endDate = null;
        if (!obj.registrationStartDate) obj.registrationStartDate = null;
        if (!obj.registrationEndDate) obj.registrationEndDate = null;

        // Xử lý các trường không bắt buộc
        if (!obj.targetAudience) obj.targetAudience = null;
        if (!obj.tools) obj.tools = null;
        if (!obj.benefits) obj.benefits = null;

        var finalFd = new FormData();
        finalFd.append('workshop', new Blob([JSON.stringify(obj)], { type: 'application/json' }));

        var fileInput = $('addWsFiles');
        if (fileInput && fileInput.files.length > 0) {
          for (var i = 0; i < fileInput.files.length; i++) {
            finalFd.append('images', fileInput.files[i]);
          }
        } else {
          toast('Vui lòng chọn ít nhất một hình ảnh!'); return;
        }

        api('/workshop/workshops/create', {
          method: 'POST',
          body: finalFd,
          formData: true
        }).then(() => {
          toast('Đã thêm Workshop thành công!');
          ADM.closeModal();
          loadWorkshops();
        }).catch(err => {
          toast('Lỗi: ' + (err.message || 'Không thể tạo workshop'));
        });
      };
    },

    openWorkshopDetailModal: function (id) {
      api('/workshop/workshops/' + id).then(function (w) {
        var imagesHTML = (w.allImages || []).map(img => `<img src="${img}" class="adm-detail-img" style="width:100px;height:100px;object-fit:cover;border-radius:4px;margin-right:8px;margin-bottom:8px">`).join('');

        showModal('Chi tiết Workshop #' + id, `
          <div class="adm-detail">
            <div class="adm-detail-section">
              <h4 style="margin-bottom:8px;color:var(--adm-primary)">📌 Thông tin chung</h4>
              <p><strong>Tên:</strong> ${esc(w.name)}</p>
              <p><strong>Địa điểm:</strong> ${esc(w.location)}</p>
              <p><strong>Giá vé:</strong> ${fmtMoney(w.price)}</p>
              <p><strong>Sức chứa:</strong> ${w.currentParticipants}/${w.maxParticipants} người</p>
              <p><strong>Trạng thái:</strong> ${w.active ? 'Đang hoạt động' : 'Đã tạm dừng'}</p>
            </div>
            
            <div class="adm-detail-section" style="margin-top:16px">
              <h4 style="margin-bottom:8px;color:var(--adm-primary)">📝 Nội dung & Mô tả</h4>
              <p><strong>Mô tả ngắn:</strong> ${esc(w.description)}</p>
              <p><strong>Nội dung chi tiết:</strong> ${esc(w.content)}</p>
            </div>

            <div class="adm-detail-section" style="margin-top:16px">
              <h4 style="margin-bottom:8px;color:var(--adm-primary)">⏰ Thời gian</h4>
              <p><strong>Tổ chức:</strong> ${fmtDate(w.startDate)} → ${fmtDate(w.endDate)}</p>
              <p><strong>Hạn đăng ký:</strong> ${fmtDate(w.registrationStartDate)} → ${fmtDate(w.registrationEndDate)}</p>
            </div>

            <div class="adm-detail-section" style="margin-top:16px">
              <h4 style="margin-bottom:8px;color:var(--adm-primary)">🎯 Khác</h4>
              <p><strong>Đối tượng:</strong> ${esc(w.targetAudience || '—')}</p>
              <p><strong>Dụng cụ:</strong> ${esc(w.tools || '—')}</p>
              <p><strong>Quyền lợi:</strong> ${esc(w.benefits || '—')}</p>
            </div>

            <div class="adm-detail-section" style="margin-top:16px">
              <h4 style="margin-bottom:8px;color:var(--adm-primary)">🖼️ Hình ảnh</h4>
              <div style="display:flex;flex-wrap:wrap;margin-top:8px">${imagesHTML || 'Chưa có ảnh'}</div>
            </div>

            <div class="adm-form-actions" style="margin-top:24px">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng</button>
              <button class="adm-btn adm-btn--primary" onclick="ADM.closeModal(); ADM.openEditWorkshopModal(${w.id})">Chỉnh sửa Workshop</button>
            </div>
          </div>
        `);
      }).catch(function () {
        toast('Không thể lấy thông tin chi tiết Workshop');
      });
    },

    openEditWorkshopModal: function (id) {
      api('/workshop/workshops/' + id).then(function (w) {
        var deletedImageIds = [];
        // Format ISO date for input datetime-local
        var fd = (d) => d ? d.substring(0, 16) : '';

        showModal('Chỉnh sửa Workshop #' + id, `
          <form class="adm-form" id="editWorkshopForm">
            <div class="adm-form-group">
              <label>Tên Workshop</label>
              <input type="text" name="name" value="${esc(w.name)}">
            </div>
            <div class="adm-form-group">
              <label>Mô tả ngắn</label>
              <textarea name="description" rows="2">${esc(w.description)}</textarea>
            </div>
            <div class="adm-form-group">
              <label>Nội dung chi tiết</label>
              <textarea name="content" rows="4">${esc(w.content)}</textarea>
            </div>
            <div class="adm-form-row">
              <div class="adm-form-group">
                <label>Địa điểm</label>
                <input type="text" name="location" value="${esc(w.location)}">
              </div>
              <div class="adm-form-group">
                <label>Giá (VNĐ)</label>
                <input type="number" name="price" value="${w.price}">
              </div>
            </div>
            <div class="adm-form-row">
              <div class="adm-form-group">
                <label>Số người tối đa</label>
                <input type="number" name="maxParticipants" value="${w.maxParticipants}">
              </div>
            </div>
            <div class="adm-form-row">
              <div class="adm-form-group">
                <label>Ngày bắt đầu</label>
                <input type="datetime-local" name="startDate" value="${fd(w.startDate)}">
              </div>
              <div class="adm-form-group">
                <label>Ngày kết thúc</label>
                <input type="datetime-local" name="endDate" value="${fd(w.endDate)}">
              </div>
            </div>
            <div class="adm-form-row">
              <div class="adm-form-group">
                <label>Bắt đầu đăng ký</label>
                <input type="datetime-local" name="registrationStartDate" value="${fd(w.registrationStartDate)}">
              </div>
              <div class="adm-form-group">
                <label>Kết thúc đăng ký</label>
                <input type="datetime-local" name="registrationEndDate" value="${fd(w.registrationEndDate)}">
              </div>
            </div>
            <div class="adm-form-group">
              <label>Đối tượng tham gia</label>
              <input type="text" name="targetAudience" value="${esc(w.targetAudience || '')}">
            </div>
            <div class="adm-form-group">
              <label>Dụng cụ chuẩn bị</label>
              <input type="text" name="tools" value="${esc(w.tools || '')}">
            </div>
            <div class="adm-form-group">
              <label>Quyền lợi</label>
              <input type="text" name="benefits" value="${esc(w.benefits || '')}">
            </div>
            
            <div class="adm-form-group">
              <label>Ảnh hiện tại (Nhấn vào ảnh để chọn xóa)</label>
              <div id="editWsImages" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px">
                ${(w.imagesInfo || []).map((img) => `
                  <div class="ws-img-edit-item" style="position:relative;cursor:pointer" data-id="${img.id}" onclick="this.classList.toggle('to-delete')">
                    <img src="${img.url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;border:2px solid transparent">
                    <div class="ws-img-delete-overlay" style="position:absolute;inset:0;background:rgba(232,93,93,0.6);display:none;align-items:center;justify-content:center;color:#fff;font-size:20px;border-radius:4px">✕</div>
                  </div>
                `).join('')}
              </div>
              <style>
                .ws-img-edit-item.to-delete img { border-color: var(--adm-danger) !important; opacity: 0.5; }
                .ws-img-edit-item.to-delete .ws-img-delete-overlay { display: flex !important; }
              </style>
            </div>

            <div class="adm-form-group">
              <label>Thêm ảnh mới từ máy tính</label>
              <input type="file" id="editWsFiles" multiple accept="image/*">
            </div>

            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Cập nhật Workshop</button>
            </div>
          </form>
        `);

        $('editWorkshopForm').onsubmit = function (e) {
          e.preventDefault();
          var fd = new FormData(this);
          var obj = Object.fromEntries(fd.entries());

          // Lấy danh sách ID ảnh cần xóa
          var delIds = [];
          document.querySelectorAll('.ws-img-edit-item.to-delete').forEach(el => {
            delIds.push(parseInt(el.dataset.id));
          });
          obj.deletedImageIds = delIds;

          obj.price = parseFloat(obj.price) || 0;
          obj.maxParticipants = parseInt(obj.maxParticipants) || 0;

          if (!obj.startDate) obj.startDate = null;
          if (!obj.endDate) obj.endDate = null;
          if (!obj.registrationStartDate) obj.registrationStartDate = null;
          if (!obj.registrationEndDate) obj.registrationEndDate = null;

          var finalFd = new FormData();
          finalFd.append('request', new Blob([JSON.stringify(obj)], { type: 'application/json' }));

          var fileInput = $('editWsFiles');
          if (fileInput && fileInput.files.length > 0) {
            for (var i = 0; i < fileInput.files.length; i++) {
              finalFd.append('images', fileInput.files[i]);
            }
          }

          api('/workshop/workshops/' + id, {
            method: 'PUT',
            body: finalFd,
            formData: true
          }).then(() => {
            toast('Đã cập nhật Workshop thành công!');
            ADM.closeModal();
            loadWorkshops();
          }).catch(err => {
            toast('Lỗi: ' + (err.message || 'Cập nhật thất bại'));
          });
        };
      }).catch(function () {
        toast('Không thể lấy thông tin Workshop');
      });
    },

    openAddProductModal: function () {
      showModal('Thêm sản phẩm mới', `
      <form class="adm-form" id="addProductForm">
        <div class="adm-form-group">
          <label>Tên sản phẩm</label>
          <input type="text" name="name" placeholder="Ví dụ: Bình gốm men lam">
        </div>
        <div class="adm-form-group">
          <label>Thương hiệu</label>
          <input type="text" name="brand" placeholder="Ví dụ: Gốm Sứ Bát Tràng">
        </div>
        <div class="adm-form-row">
          <div class="adm-form-group">
            <label>Giá (VNĐ)</label>
            <input type="number" name="price">
          </div>
          <div class="adm-form-group">
            <label>Số lượng kho</label>
            <input type="number" name="stockQuantity">
          </div>
        </div>
        <div class="adm-form-group">
          <label>Danh mục</label>
          <select name="categoryId" id="modalCategorySelect"><option value="">Đang tải...</option></select>
        </div>
        <div class="adm-form-group">
          <label>Mô tả</label>
          <textarea name="description" rows="3"></textarea>
        </div>
        <div class="adm-form-group">
          <label>Ảnh sản phẩm (Chọn từ máy tính)</label>
          <input type="file" id="addProdFiles" multiple accept="image/*">
        </div>
        <div class="adm-form-actions">
          <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
          <button type="submit" class="adm-btn adm-btn--primary">Lưu sản phẩm</button>
        </div>
      </form>
    `);

      // Load active categories for product dropdown
      api('/product/categories/active').then(rows => {
        var sel = $('modalCategorySelect');
        if (sel) sel.innerHTML = rows.map(c => `<option value="${c.id}">${c.name}</option>`).join('') || '<option value="">Chưa có danh mục</option>';
      });

      $('addProductForm').onsubmit = function (e) {
        e.preventDefault();
        var fd = new FormData(this);
        var obj = Object.fromEntries(fd.entries());

        // --- VALIDATION TẠI FRONTEND ---
        if (!obj.name || !obj.name.trim()) { toast('Tên sản phẩm không được để trống!'); return; }
        if (!obj.brand || !obj.brand.trim()) { toast('Thương hiệu không được để trống!'); return; }
        if (!obj.price || isNaN(parseFloat(obj.price))) { toast('Giá không được để trống!'); return; }
        if (parseFloat(obj.price) < 0) { toast('Giá sản phẩm phải lớn hơn hoặc bằng 0'); return; }
        if (!obj.stockQuantity || isNaN(parseInt(obj.stockQuantity))) { toast('Số lượng không được để trống!'); return; }
        if (parseInt(obj.stockQuantity) < 0) { toast('Số lượng tồn kho không hợp lệ'); return; }
        if (!obj.categoryId) { toast('Mã danh mục không được để trống!'); return; }

        obj.price = parseFloat(obj.price);
        obj.stockQuantity = parseInt(obj.stockQuantity);
        obj.categoryId = parseInt(obj.categoryId);
        obj.imageUrls = [];

        var finalFd = new FormData();
        finalFd.append('data', new Blob([JSON.stringify(obj)], { type: 'application/json' }));

        // Thêm các file ảnh từ máy tính
        var fileInput = $('addProdFiles');
        if (fileInput && fileInput.files.length > 0) {
          for (var i = 0; i < fileInput.files.length; i++) {
            finalFd.append('images', fileInput.files[i]);
          }
        }

        api('/product/products', {
          method: 'POST',
          body: finalFd,
          formData: true
        }).then(() => {
          toast('Đã thêm sản phẩm thành công!');
          ADM.closeModal();
          loadProducts();
        }).catch(err => {
          toast('Lỗi: ' + (err.message || 'Dữ liệu không hợp lệ'));
        });
      };
    },
    openAddPostModal: function () {
      showModal('Thêm bài viết mới', `
        <form class="adm-form" id="addPostForm">
          <div class="adm-form-group">
            <label>Tiêu đề bài viết</label>
            <input type="text" name="title" placeholder="Nhập tiêu đề hấp dẫn...">
          </div>
          <div class="adm-form-group">
            <label>Danh mục</label>
            <select name="categoryId" id="modalPostCategorySelect"><option value="">Đang tải...</option></select>
          </div>
          <div class="adm-form-group">
            <label>Tóm tắt (Mô tả ngắn)</label>
            <textarea name="summary" rows="2" placeholder="Nếu bỏ trống sẽ tự động lấy từ nội dung..."></textarea>
          </div>
          <div class="adm-form-group">
            <label>Nội dung bài viết</label>
            <textarea name="content" rows="8" placeholder="Nhập nội dung bài viết ở đây..."></textarea>
          </div>
          <div class="adm-form-group">
            <div class="adm-form-check">
              <input type="checkbox" id="addPostPub" name="published" value="true" checked>
              <label for="addPostPub">Đã đăng (Công khai ngay)</label>
            </div>
          </div>
          <div class="adm-form-group">
            <label>Ảnh bài viết (Ảnh đầu tiên sẽ làm thumbnail)</label>
            <input type="file" id="addPostFiles" multiple accept="image/*">
          </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu bài viết</button>
          </div>
        </form>
      `);

      api('/content/categories').then(data => {
        var sel = $('modalPostCategorySelect');
        var rows = data.content || (Array.isArray(data) ? data : []);
        if (sel) sel.innerHTML = rows.map(c => `<option value="${c.id}">${c.name}</option>`).join('') || '<option value="">Chưa có danh mục</option>';
      });

      $('addPostForm').onsubmit = function (e) {
        e.preventDefault();
        var fd = new FormData(this);
        var obj = Object.fromEntries(fd.entries());
        if (!obj.title || obj.title.length < 10) { toast('Tiêu đề phải ít nhất 10 ký tự'); return; }
        if (!obj.content) { toast('Nội dung không được để trống'); return; }

        obj.published = !!obj.published;
        obj.categoryId = parseInt(obj.categoryId);

        var finalFd = new FormData();
        finalFd.append('title', obj.title);
        finalFd.append('content', obj.content);
        finalFd.append('summary', obj.summary || '');
        finalFd.append('categoryId', obj.categoryId);
        finalFd.append('published', obj.published);

        var fileInput = $('addPostFiles');
        if (fileInput && fileInput.files.length > 0) {
          for (var i = 0; i < fileInput.files.length; i++) {
            finalFd.append('images', fileInput.files[i]);
          }
        }

        api('/content/posts', { method: 'POST', body: finalFd, formData: true }).then(() => {
          toast('Đã thêm bài viết thành công!');
          ADM.closeModal(); loadPosts();
        }).catch(err => toast('Lỗi: ' + (err.message || 'Không thể tạo bài viết')));
      };
    },

    openEditPostModal: function (id) {
      api('/content/posts/' + id).then(function (p) {
        showModal('Chỉnh sửa bài viết #' + id, `
          <form class="adm-form" id="editPostForm">
            <div class="adm-form-group">
              <label>Tiêu đề bài viết</label>
              <input type="text" name="title" value="${esc(p.title)}">
            </div>
            <div class="adm-form-group">
              <label>Danh mục</label>
              <select name="categoryId" id="modalPostCategoryEdit" data-sel="${p.category ? p.category.id : ''}"><option value="">Đang tải...</option></select>
            </div>
            <div class="adm-form-group">
              <label>Tóm tắt (Mô tả ngắn)</label>
              <textarea name="summary" rows="2">${esc(p.summary || '')}</textarea>
            </div>
            <div class="adm-form-group">
              <label>Nội dung bài viết</label>
              <textarea name="content" rows="8">${esc(p.content)}</textarea>
            </div>
            <div class="adm-form-group">
              <div class="adm-form-check">
                <input type="checkbox" id="editPostPub" name="published" value="true" ${p.published ? 'checked' : ''}>
                <label for="editPostPub">Đã đăng</label>
              </div>
            </div>
            <div class="adm-form-group">
              <label>Ảnh hiện tại (Nhấn vào ảnh để chọn xóa)</label>
              <div id="editPostImages" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px">
                ${(p.imagesInfo || []).map((img) => `
                  <div class="ws-img-edit-item" style="position:relative;cursor:pointer" data-id="${img.id}" onclick="this.classList.toggle('to-delete')">
                    <img src="${img.url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;border:2px solid transparent">
                    <div class="ws-img-delete-overlay" style="position:absolute;inset:0;background:rgba(232,93,93,0.6);display:none;align-items:center;justify-content:center;color:#fff;font-size:20px;border-radius:4px">✕</div>
                  </div>
                `).join('')}
              </div>
            </div>
            <div class="adm-form-group">
              <label>Thêm ảnh mới</label>
              <input type="file" id="editPostFiles" multiple accept="image/*">
            </div>
            <div class="adm-form-actions">
              <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
              <button type="submit" class="adm-btn adm-btn--primary">Lưu thay đổi</button>
            </div>
          </form>
        `);

        api('/content/categories').then(data => {
          var sel = $('modalPostCategoryEdit');
          var rows = data.content || (Array.isArray(data) ? data : []);
          var selId = parseInt(sel.getAttribute('data-sel'));
          if (sel) sel.innerHTML = rows.map(c => `<option value="${c.id}" ${c.id === selId ? 'selected' : ''}>${c.name}</option>`).join('') || '<option value="">Chưa có danh mục</option>';
        });

        $('editPostForm').onsubmit = function (e) {
          e.preventDefault();
          var fd = new FormData(this);
          var obj = Object.fromEntries(fd.entries());
          obj.published = !!obj.published;
          obj.categoryId = parseInt(obj.categoryId);

          var finalFd = new FormData();

          // Lấy danh sách ID ảnh cần xóa
          var delIds = [];
          document.querySelectorAll('#editPostImages .ws-img-edit-item.to-delete').forEach(el => {
            delIds.push(parseInt(el.dataset.id));
          });

          finalFd.append('request', new Blob([JSON.stringify({
            title: obj.title,
            content: obj.content,
            summary: obj.summary,
            categoryId: obj.categoryId,
            published: obj.published,
            deletedImageIds: delIds
          })], { type: 'application/json' }));

          var fileInput = $('editPostFiles');
          if (fileInput && fileInput.files.length > 0) {
            for (var i = 0; i < fileInput.files.length; i++) {
              finalFd.append('newImages', fileInput.files[i]);
            }
          }

          api('/content/posts/' + id, { method: 'PUT', body: finalFd, formData: true }).then(() => {
            toast('Đã cập nhật bài viết!');
            ADM.closeModal(); loadPosts();
          }).catch(err => toast('Lỗi: ' + (err.message || 'Cập nhật thất bại')));
        };
      }).catch(() => toast('Không thể tải thông tin bài viết'));
    },

    openPostDetailModal: function (id) {
      api('/content/posts/' + id).then(function (p) {
        var imagesHTML = (p.images || []).map(img => `<img src="${img}" style="width:120px;height:120px;object-fit:cover;border-radius:8px;border:1px solid var(--adm-border)">`).join('');

        showModal('Chi tiết bài viết #' + id, `
          <div class="adm-detail">
            <div style="margin-bottom:24px">
              <h2 style="font-size:24px;margin-bottom:8px;color:var(--adm-text)">${esc(p.title)}</h2>
              <div style="display:flex;gap:12px;align-items:center;margin-bottom:16px">
                <span class="adm-badge adm-badge--info">${esc(p.category ? p.category.name : '—')}</span>
                <span style="font-size:13px;color:var(--adm-text3)">📅 ${fmtDate(p.createdAt)}</span>
                ${p.published ? '<span class="adm-badge adm-badge--success">Đã đăng</span>' : '<span class="adm-badge adm-badge--warning">Nháp</span>'}
              </div>
            </div>

            <div style="background:var(--adm-bg2);padding:16px;border-radius:8px;border:1px dashed var(--adm-border);margin-bottom:24px">
              <h4 style="margin-bottom:8px;font-size:14px;color:var(--adm-accent2)">📝 Tóm tắt</h4>
              <p style="font-style:italic;color:var(--adm-text2);line-height:1.6">${esc(p.summary || 'Không có tóm tắt.')}</p>
            </div>

            <div style="margin-bottom:24px">
              <h4 style="margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">📖 Nội dung</h4>
              <div style="color:var(--adm-text2);line-height:1.8;white-space:pre-line;max-height:400px;overflow-y:auto;padding-right:8px">
                ${esc(p.content)}
              </div>
            </div>

            <div style="margin-bottom:24px">
              <h4 style="margin-bottom:12px;border-bottom:1px solid var(--adm-border);padding-bottom:8px">🖼️ Hình ảnh</h4>
              <div style="display:flex;flex-wrap:wrap;gap:12px;margin-top:12px">
                ${imagesHTML || '<p style="color:var(--adm-text3)">Bài viết này không có hình ảnh.</p>'}
              </div>
            </div>

            <div class="adm-form-actions" style="margin-top:32px;padding-top:24px;border-top:1px solid var(--adm-border)">
              <button class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Đóng</button>
              <button class="adm-btn adm-btn--primary" onclick="ADM.closeModal(); ADM.openEditPostModal(${p.id})">Sửa bài viết</button>
            </div>
          </div>
        `);
      }).catch(() => toast('Không thể tải chi tiết bài viết'));
    },

    openEditProductModal: function (id) {
      // Fetch current product
      api('/product/products/' + id).then(function (p) {
        showModal('Sửa sản phẩm #' + id, `
        <form class="adm-form" id="editProductForm">
          <div class="adm-form-group">
            <label>Tên sản phẩm</label>
            <input type="text" name="name" value="${p.name || ''}">
          </div>
          <div class="adm-form-group">
            <label>Thương hiệu</label>
            <input type="text" name="brand" value="${p.brand || ''}">
          </div>
          <div class="adm-form-row">
            <div class="adm-form-group">
              <label>Giá (VNĐ)</label>
              <input type="number" name="price" value="${p.price || 0}">
            </div>
            <div class="adm-form-group">
              <label>Số lượng kho</label>
              <input type="number" name="stockQuantity" value="${p.stockQuantity || 0}">
            </div>
          </div>
          <div class="adm-form-group">
            <label>Danh mục</label>
            <select name="categoryId" id="modalCategorySelectEdit" data-sel="${p.categoryId || ''}"><option value="">Đang tải...</option></select>
          </div>
          <div class="adm-form-group">
            <label>Mô tả</label>
            <textarea name="description" rows="3">${p.description || ''}</textarea>
          </div>
            <div class="adm-form-group">
              <label>Ảnh hiện tại (Nhấn vào ảnh để chọn xóa)</label>
              <div id="editProdImages" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px">
                ${(p.imagesInfo || []).map((img) => `
                  <div class="ws-img-edit-item" style="position:relative;cursor:pointer" data-id="${img.id}" onclick="this.classList.toggle('to-delete')">
                    <img src="${img.url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;border:2px solid transparent">
                    <div class="ws-img-delete-overlay" style="position:absolute;inset:0;background:rgba(232,93,93,0.6);display:none;align-items:center;justify-content:center;color:#fff;font-size:20px;border-radius:4px">✕</div>
                  </div>
                `).join('')}
              </div>
              <style>
                .ws-img-edit-item.to-delete img { border-color: var(--adm-danger) !important; opacity: 0.5; }
                .ws-img-edit-item.to-delete .ws-img-delete-overlay { display: flex !important; }
              </style>
            </div>
            <div class="adm-form-group">
              <label>Thêm ảnh mới từ máy tính</label>
              <input type="file" id="editProdFiles" multiple accept="image/*">
            </div>
          <div class="adm-form-actions">
            <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
            <button type="submit" class="adm-btn adm-btn--primary">Lưu thay đổi</button>
          </div>
        </form>
      `);

        // Load active categories for product dropdown
        api('/product/categories/active').then(rows => {
          var sel = $('modalCategorySelectEdit');
          var selId = parseInt(sel.getAttribute('data-sel'));
          if (sel) sel.innerHTML = rows.map(c => `<option value="${c.id}" ${c.id === selId ? 'selected' : ''}>${c.name}</option>`).join('') || '<option value="">Chưa có danh mục</option>';
        });

        $('editProductForm').onsubmit = function (e) {
          e.preventDefault();
          var fd = new FormData(this);
          var obj = Object.fromEntries(fd.entries());

          // --- VALIDATION TẠI FRONTEND ---
          if (!obj.name || !obj.name.trim()) { toast('Tên sản phẩm không được để trống!'); return; }
          if (!obj.brand || !obj.brand.trim()) { toast('Thương hiệu không được để trống!'); return; }
          if (!obj.price || isNaN(parseFloat(obj.price))) { toast('Giá không được để trống!'); return; }
          if (parseFloat(obj.price) < 0) { toast('Giá sản phẩm phải lớn hơn hoặc bằng 0'); return; }
          if (!obj.stockQuantity || isNaN(parseInt(obj.stockQuantity))) { toast('Số lượng không được để trống!'); return; }
          if (parseInt(obj.stockQuantity) < 0) { toast('Số lượng tồn kho không hợp lệ'); return; }

          // Lấy danh sách ID ảnh cần xóa
          var delIds = [];
          document.querySelectorAll('.ws-img-edit-item.to-delete').forEach(el => {
            delIds.push(parseInt(el.dataset.id));
          });
          obj.deletedImageIds = delIds;

          obj.price = parseFloat(obj.price) || 0;
          obj.stockQuantity = parseInt(obj.stockQuantity) || 0;
          obj.categoryId = parseInt(obj.categoryId);

          var finalFd = new FormData();
          finalFd.append('data', new Blob([JSON.stringify(obj)], { type: 'application/json' }));

          // Thêm các file ảnh mới từ máy tính
          var fileInput = $('editProdFiles');
          if (fileInput && fileInput.files.length > 0) {
            for (var i = 0; i < fileInput.files.length; i++) {
              finalFd.append('images', fileInput.files[i]);
            }
          }

          api('/product/products/' + id, {
            method: 'PUT',
            body: finalFd,
            formData: true
          }).then(() => {
            toast('Đã cập nhật sản phẩm thành công!');
            ADM.closeModal();
            loadProducts();
          }).catch(err => {
            toast('Lỗi: ' + (err.message || 'Cập nhật thất bại'));
          });
        };
      }).catch(function () {
        toast('Không thể lấy thông tin sản phẩm');
      });
    },

    closeModal: function () {
      var m = $('admModalOverlay');
      if (m) m.classList.remove('is-open');
    }
  };

  /* ===== UI BUILDERS ===== */
  function esc(s) { if (!s) return ''; var d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
  function skeleton(n) { var h = ''; for (var i = 0; i < n; i++)h += '<div class="adm-skeleton" style="width:' + (60 + Math.random() * 40) + '%;height:18px"></div>'; return h; }
  function emptyMsg(t) { return '<div class="adm-empty"><div class="adm-empty-icon">📭</div><p class="adm-empty-text">' + t + '</p></div>'; }

  function statusBadge(s) {
    var map = { PENDING: ['warning', 'Chờ xử lý'], CONFIRMED: ['info', 'Đã xác nhận'], SHIPPING: ['info', 'Đang giao'], DELIVERED: ['success', 'Đã giao'], COMPLETED: ['success', 'Hoàn thành'], CANCELLED: ['danger', 'Đã hủy'] };
    var m = map[s] || ['neutral', s || '—'];
    return '<span class="adm-badge adm-badge--' + m[0] + '">' + m[1] + '</span>';
  }

  function tableWrap(heads, bodyHTML) {
    return '<div class="adm-table-wrap"><table class="adm-table"><thead><tr>' + heads.map(function (h) { return '<th>' + h + '</th>'; }).join('') + '</tr></thead><tbody>' + bodyHTML + '</tbody></table></div>';
  }

  function paginate(data) {
    if (!data || data.totalPages <= 1) return '';
    var html = '<div class="adm-pagination">';
    html += '<button class="adm-page-btn" onclick="ADM.goPage(' + (data.number - 1) + ')" ' + (data.first ? 'disabled' : '') + '>‹</button>';
    for (var i = 0; i < Math.min(data.totalPages, 7); i++) {
      html += '<button class="adm-page-btn' + (i === data.number ? ' is-active' : '') + '" onclick="ADM.goPage(' + i + ')">' + (i + 1) + '</button>';
    }
    html += '<button class="adm-page-btn" onclick="ADM.goPage(' + (data.number + 1) + ')" ' + (data.last ? 'disabled' : '') + '>›</button>';
    return html + '</div>';
  }

  function sectionHeader(title, section, actionBtn) {
    var currentKeyword = pageState.keyword || '';
    return '<div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">' + title + '</h3><div class="adm-section-actions">' + (actionBtn || '') + '<div class="adm-search"><input type="text" id="admSearch_' + section + '" value="' + esc(currentKeyword) + '" placeholder="Tìm theo tên hoặc ID..."></div></div></div></div>';
  }

  function showModal(title, contentHTML) {
    var m = $('admModalOverlay');
    if (!m) {
      m = document.createElement('div');
      m.id = 'admModalOverlay';
      m.className = 'adm-modal-overlay';
      document.body.appendChild(m);
    }
    m.innerHTML = `<div class="adm-modal">
    <div class="adm-modal-head">
      <h3 class="adm-modal-title">${title}</h3>
      <button class="adm-modal-close" onclick="ADM.closeModal()">×</button>
    </div>
    <div class="adm-modal-body">${contentHTML}</div>
  </div>`;
    setTimeout(() => m.classList.add('is-open'), 10);
  }

  var searchTimer;
  function bindSearch(section) {
    setTimeout(function () {
      var inp = $('admSearch_' + section);
      if (!inp) return;
      inp.addEventListener('input', function () {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(function () { pageState.keyword = inp.value.trim(); pageState.page = 0; renderSection(currentSection); }, 400);
      });
    }, 50);
  }

  /* ===== INIT ===== */
  document.addEventListener('DOMContentLoaded', function () {
    initLogin();
    // Check if already logged in
    if (token) { showDashboard(); }
    // Nav clicks
    document.querySelectorAll('.adm-nav-item[data-section]').forEach(function (btn) {
      btn.addEventListener('click', function () { navigate(this.dataset.section); });
    });
    $('admLogout').addEventListener('click', logout);
    // Mobile menu
    $('admMenuToggle').addEventListener('click', function () { $('admSidebar').classList.toggle('is-open'); });
    // Close sidebar on content click (mobile)
    $('admMain').addEventListener('click', function () { $('admSidebar').classList.remove('is-open'); });
  });
})();
