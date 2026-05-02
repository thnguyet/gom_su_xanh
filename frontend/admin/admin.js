(function(){
'use strict';
var API='http://localhost:8080';
var token=localStorage.getItem('adm_token')||'';
var currentSection='dashboard';
var pageState={};

/* ===== HELPERS ===== */
function $(id){return document.getElementById(id);}
function toast(msg){var t=$('admToast');t.textContent=msg;t.classList.add('is-visible');setTimeout(function(){t.classList.remove('is-visible');},2600);}
function fmt(n){return new Intl.NumberFormat('vi-VN').format(n||0);}
function fmtDate(d){if(!d)return'—';var dt=new Date(d);return dt.toLocaleDateString('vi-VN');}
function fmtMoney(n){return fmt(n)+'đ';}
function authHeaders(){return{'Authorization':'Bearer '+token,'Content-Type':'application/json'};}

function api(path,opts){
  opts=opts||{};
  var headers=opts.noAuth?{'Content-Type':'application/json'}:authHeaders();
  if(opts.formData){delete headers['Content-Type'];} 
  return fetch(API+path,{method:opts.method||'GET',headers:headers,body:opts.body||null})
    .then(function(r){
      // Chỉnh sửa: Nếu là token giả 'admin_local' thì không đá ra ngoài khi gặp lỗi 401/403
      if((r.status===401||r.status===403) && token !== 'admin_local'){
        toast('Phiên đăng nhập hết hạn');
        logout();
        throw new Error('Unauthorized');
      }
      if(!r.ok) throw new Error('HTTP '+r.status);
      var ct=r.headers.get('content-type')||'';
      return ct.indexOf('json')!==-1?r.json():r.text();
    });
}

/* ===== LOGIN ===== */
function initLogin(){
  var form=$('admLoginForm');
  form.addEventListener('submit',function(e){
    e.preventDefault();
    var user=$('admUser').value.trim();
    var pass=$('admPass').value.trim();
    var err=$('admLoginError');
    var btn=$('admLoginBtn');
    err.textContent='';

    if(user==='admin'&&pass==='admin'){
      btn.classList.add('is-loading');btn.querySelector('span').textContent='Đang đăng nhập...';
      // Try real backend login first
      fetch(API+'/identity/auth/token',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:'admin',password:'admin'})})
        .then(function(r){return r.json();})
        .then(function(data){
          if(data.token){token=data.token;localStorage.setItem('adm_token',token);}
          else{token='admin_local';localStorage.setItem('adm_token',token);}
          showDashboard();
        })
        .catch(function(){
          // Backend not running - use local mode
          token='admin_local';localStorage.setItem('adm_token',token);
          showDashboard();
        });
    } else {
      err.textContent='Sai tài khoản hoặc mật khẩu!';
      $('admUser').focus();
    }
  });
}

function showDashboard(){
  $('admLogin').style.display='none';
  $('admApp').style.display='flex';
  $('admDate').textContent=new Date().toLocaleDateString('vi-VN',{weekday:'long',year:'numeric',month:'long',day:'numeric'});
  navigate('dashboard');
}

function logout(){
  token='';localStorage.removeItem('adm_token');
  localStorage.removeItem('gsx_token');
  localStorage.setItem('gsx_logged_in', JSON.stringify(false));
  // Redirect to main login page (unified flow)
  window.location.href = '../pages/dang-nhap_839-134.html';
}

/* ===== NAVIGATION ===== */
function navigate(section){
  currentSection=section;
  var items=document.querySelectorAll('.adm-nav-item[data-section]');
  items.forEach(function(it){it.classList.toggle('is-active',it.dataset.section===section);});
  var titles={dashboard:'Tổng quan',products:'Sản phẩm',orders:'Đơn hàng',workshops:'Workshop',posts:'Bài viết',reviews:'Đánh giá',users:'Người dùng',promotions:'Khuyến mãi'};
  $('admTopTitle').textContent=titles[section]||section;
  pageState={page:0,size:10,keyword:''};
  renderSection(section);
}

function renderSection(s){
  var c=$('admContent');
  var fn={dashboard:renderDashboard,products:renderProducts,orders:renderOrders,workshops:renderWorkshops,posts:renderPosts,reviews:renderReviews,users:renderUsers,promotions:renderPromotions};
  if(fn[s])fn[s](c);else c.innerHTML='<div class="adm-empty"><div class="adm-empty-icon">🚧</div><p>Đang phát triển...</p></div>';
}

/* ===== DASHBOARD ===== */
function renderDashboard(c){
  c.innerHTML='<div class="adm-stats" id="admStats">'+skeleton(4)+'</div><div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">📦 Đơn hàng gần đây</h3></div><div id="admRecentOrders">'+skeleton(3)+'</div></div>';
  // Load stats
  Promise.all([
    api('/product/products/all?page=0&size=1').catch(function(){return{totalElements:0};}),
    api('/order/orders/all-orders?page=0&size=1').catch(function(){return{totalElements:0};}),
    api('/workshop/workshops/all?page=0&size=1').catch(function(){return{totalElements:0};}),
    api('/identity/users').catch(function(){return[];})
  ]).then(function(res){
    var stats=[
      {icon:'📦',value:fmt(res[0].totalElements||0),label:'Sản phẩm'},
      {icon:'🛒',value:fmt(res[1].totalElements||0),label:'Đơn hàng'},
      {icon:'🎨',value:fmt(res[2].totalElements||0),label:'Workshop'},
      {icon:'👥',value:fmt(Array.isArray(res[3])?res[3].length:0),label:'Người dùng'}
    ];
    $('admStats').innerHTML=stats.map(function(s){
      return '<div class="adm-stat"><div class="adm-stat-icon">'+s.icon+'</div><div class="adm-stat-value">'+s.value+'</div><div class="adm-stat-label">'+s.label+'</div></div>';
    }).join('');
  });
  // Recent orders
  api('/order/orders/all-orders?page=0&size=5&sortBy=createdAt&sortDir=desc').then(function(data){
    var rows=(data.content||[]);
    if(!rows.length){$('admRecentOrders').innerHTML=emptyMsg('Chưa có đơn hàng');return;}
    $('admRecentOrders').innerHTML=tableWrap(['ID','Khách hàng','Tổng tiền','Trạng thái','Ngày tạo'],rows.map(function(o){
      return '<tr><td>#'+o.id+'</td><td>KH-'+o.customerId+'</td><td>'+fmtMoney(o.totalAmount)+'</td><td>'+statusBadge(o.status)+'</td><td>'+fmtDate(o.createdAt)+'</td></tr>';
    }).join(''));
  }).catch(function(){$('admRecentOrders').innerHTML=emptyMsg('Không thể tải đơn hàng');});
}

/* ===== PRODUCTS ===== */
function renderProducts(c){
  c.innerHTML=sectionHeader('Sản phẩm','products', '<button class="adm-btn adm-btn--primary" onclick="ADM.openAddProductModal()">+ Thêm sản phẩm</button>')+'<div id="admProdTable">'+skeleton(5)+'</div><div id="admProdPage"></div>';
  bindSearch('products');loadProducts();
}
function loadProducts(){
  var p=pageState;
  var q=p.keyword?'&keyword='+encodeURIComponent(p.keyword):'';
  api('/product/products/all?page='+p.page+'&size='+p.size+q).then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admProdTable').innerHTML=emptyMsg('Không tìm thấy sản phẩm');$('admProdPage').innerHTML='';return;}
    $('admProdTable').innerHTML=tableWrap(['Ảnh','Tên','Giá','Kho','Danh mục','Ngày tạo',''],rows.map(function(p){
      var img=p.imageUrls&&p.imageUrls[0]?'<img class="adm-thumb" src="'+p.imageUrls[0]+'" alt="">':'—';
      return '<tr><td>'+img+'</td><td style="font-weight:600;color:var(--adm-text)">'+esc(p.name)+'</td><td>'+fmtMoney(p.price)+'</td><td>'+(p.stockQuantity||0)+'</td><td>'+esc(p.categoryName||'—')+'</td><td>'+fmtDate(p.createdAt)+'</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delProduct('+p.id+')">Xóa</button></td></tr>';
    }).join(''));
    $('admProdPage').innerHTML=paginate(data);
  }).catch(function(){$('admProdTable').innerHTML=emptyMsg('Lỗi tải sản phẩm');});
}

/* ===== ORDERS ===== */
function renderOrders(c){
  c.innerHTML='<div class="adm-quick-actions"><select class="adm-select" id="admOrderFilter"><option value="">Tất cả trạng thái</option><option value="PENDING">Chờ xử lý</option><option value="CONFIRMED">Đã xác nhận</option><option value="SHIPPING">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></div>'+sectionHeader('Đơn hàng','orders')+'<div id="admOrdTable">'+skeleton(5)+'</div><div id="admOrdPage"></div>';
  $('admOrderFilter').addEventListener('change',function(){pageState.status=this.value;pageState.page=0;loadOrders();});
  bindSearch('orders');loadOrders();
}
function loadOrders(){
  var p=pageState;var statusQ=p.status?'&status='+p.status:'';
  api('/order/orders/all-orders?page='+p.page+'&size='+p.size+statusQ+'&sortBy=createdAt&sortDir=desc').then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admOrdTable').innerHTML=emptyMsg('Không có đơn hàng');$('admOrdPage').innerHTML='';return;}
    $('admOrdTable').innerHTML=tableWrap(['ID','Khách','Tổng tiền','Thanh toán','Trạng thái','Ngày','Thao tác'],rows.map(function(o){
      var acts='<select class="adm-select" onchange="ADM.updateOrder('+o.id+',this.value)" style="font-size:12px"><option value="">Cập nhật...</option><option value="CONFIRMED">Xác nhận</option><option value="SHIPPING">Giao hàng</option><option value="DELIVERED">Đã giao</option><option value="COMPLETED">Hoàn thành</option><option value="CANCELLED">Hủy</option></select>';
      return '<tr><td>#'+o.id+'</td><td>KH-'+o.customerId+'</td><td>'+fmtMoney(o.totalAmount)+'</td><td>'+esc(o.paymentMethod||'—')+'</td><td>'+statusBadge(o.status)+'</td><td>'+fmtDate(o.createdAt)+'</td><td>'+acts+'</td></tr>';
    }).join(''));
    $('admOrdPage').innerHTML=paginate(data);
  }).catch(function(){$('admOrdTable').innerHTML=emptyMsg('Lỗi tải đơn hàng');});
}

/* ===== WORKSHOPS ===== */
function renderWorkshops(c){
  c.innerHTML=sectionHeader('Workshop','workshops')+'<div id="admWsTable">'+skeleton(5)+'</div><div id="admWsPage"></div>';
  bindSearch('workshops');loadWorkshops();
}
function loadWorkshops(){
  var p=pageState;var q=p.keyword?'&keyword='+encodeURIComponent(p.keyword):'';
  api('/workshop/workshops/all?page='+p.page+'&size='+p.size+q).then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admWsTable').innerHTML=emptyMsg('Không có workshop');$('admWsPage').innerHTML='';return;}
    $('admWsTable').innerHTML=tableWrap(['Ảnh','Tên','Địa điểm','Giá','Số người','Trạng thái','Thao tác'],rows.map(function(w){
      var img=w.mainImage?'<img class="adm-thumb" src="'+w.mainImage+'" alt="">':'—';
      var badge=w.active?'<span class="adm-badge adm-badge--success">Hoạt động</span>':'<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
      return '<tr><td>'+img+'</td><td style="font-weight:600;color:var(--adm-text)">'+esc(w.name)+'</td><td>'+esc(w.location||'—')+'</td><td>'+fmtMoney(w.price)+'</td><td>'+((w.currentParticipants||0)+'/'+(w.maxParticipants||0))+'</td><td>'+badge+'</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delWorkshop('+w.id+')">Xóa</button></td></tr>';
    }).join(''));
    $('admWsPage').innerHTML=paginate(data);
  }).catch(function(){$('admWsTable').innerHTML=emptyMsg('Lỗi tải workshop');});
}

/* ===== POSTS ===== */
function renderPosts(c){
  c.innerHTML=sectionHeader('Bài viết','posts')+'<div id="admPostTable">'+skeleton(5)+'</div><div id="admPostPage"></div>';
  bindSearch('posts');loadPosts();
}
function loadPosts(){
  var p=pageState;var q=p.keyword?'&keyword='+encodeURIComponent(p.keyword):'';
  api('/content/posts?page='+p.page+'&size='+p.size+q).then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admPostTable').innerHTML=emptyMsg('Không có bài viết');$('admPostPage').innerHTML='';return;}
    $('admPostTable').innerHTML=tableWrap(['Ảnh','Tiêu đề','Danh mục','Trạng thái','Ngày tạo','Thao tác'],rows.map(function(p){
      var img=p.thumbnail?'<img class="adm-thumb" src="'+p.thumbnail+'" alt="">':'—';
      var pub=p.published?'<span class="adm-badge adm-badge--success">Đã đăng</span>':'<span class="adm-badge adm-badge--warning">Nháp</span>';
      return '<tr><td>'+img+'</td><td style="font-weight:600;color:var(--adm-text)">'+esc(p.title)+'</td><td>'+esc(p.category?p.category.name:'—')+'</td><td>'+pub+'</td><td>'+fmtDate(p.createdAt)+'</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delPost('+p.id+')">Xóa</button></td></tr>';
    }).join(''));
    $('admPostPage').innerHTML=paginate(data);
  }).catch(function(){$('admPostTable').innerHTML=emptyMsg('Lỗi tải bài viết');});
}

/* ===== REVIEWS ===== */
function renderReviews(c){
  c.innerHTML='<div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">⭐ Đánh giá sản phẩm</h3></div><div id="admRevTable">'+skeleton(5)+'</div><div id="admRevPage"></div></div>';
  api('/content/api/v1/reviews/admin/all?page=0&size=20').then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admRevTable').innerHTML=emptyMsg('Chưa có đánh giá');return;}
    $('admRevTable').innerHTML=tableWrap(['Sản phẩm','Người dùng','Sao','Nội dung','Trạng thái','Ngày'],rows.map(function(r){
      var stars='⭐'.repeat(r.rating||0);
      var badge=r.isApproved?'<span class="adm-badge adm-badge--success">Duyệt</span>':'<span class="adm-badge adm-badge--warning">Chờ</span>';
      return '<tr><td>'+esc(r.productName||'SP-'+r.productId)+'</td><td>'+esc(r.username||'—')+'</td><td>'+stars+'</td><td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">'+esc(r.comment||'—')+'</td><td>'+badge+'</td><td>'+fmtDate(r.createdAt)+'</td></tr>';
    }).join(''));
  }).catch(function(){$('admRevTable').innerHTML=emptyMsg('Lỗi tải đánh giá');});
}

/* ===== USERS ===== */
function renderUsers(c){
  c.innerHTML='<div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">👥 Danh sách người dùng</h3></div><div id="admUserTable">'+skeleton(5)+'</div></div>';
  api('/identity/users').then(function(data){
    var rows=Array.isArray(data)?data:[];
    if(!rows.length){$('admUserTable').innerHTML=emptyMsg('Chưa có người dùng');return;}
    $('admUserTable').innerHTML=tableWrap(['ID','Tên','Email','SĐT','Vai trò','Thao tác'],rows.map(function(u){
      var badge=u.roles==='ADMIN'?'<span class="adm-badge adm-badge--info">Admin</span>':'<span class="adm-badge adm-badge--neutral">User</span>';
      return '<tr><td>#'+u.id+'</td><td style="font-weight:600;color:var(--adm-text)">'+esc(u.username||'—')+'</td><td>'+esc(u.email||'—')+'</td><td>'+esc(u.phone||'—')+'</td><td>'+badge+'</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delUser('+u.id+')">Xóa</button></td></tr>';
    }).join(''));
  }).catch(function(){$('admUserTable').innerHTML=emptyMsg('Lỗi tải người dùng');});
}

/* ===== PROMOTIONS ===== */
function renderPromotions(c){
  c.innerHTML=sectionHeader('Khuyến mãi','promotions')+'<div id="admPromoTable">'+skeleton(5)+'</div><div id="admPromoPage"></div>';
  bindSearch('promotions');loadPromotions();
}
function loadPromotions(){
  var p=pageState;var q=p.keyword?'&keyword='+encodeURIComponent(p.keyword):'';
  api('/product/promotions?page='+p.page+'&size='+p.size+q).then(function(data){
    var rows=data.content||[];
    if(!rows.length){$('admPromoTable').innerHTML=emptyMsg('Chưa có khuyến mãi');$('admPromoPage').innerHTML='';return;}
    $('admPromoTable').innerHTML=tableWrap(['Tên','Bắt đầu','Kết thúc','Trạng thái','SP áp dụng','Thao tác'],rows.map(function(pr){
      var badge=pr.active||pr.isActive?'<span class="adm-badge adm-badge--success">Đang chạy</span>':'<span class="adm-badge adm-badge--neutral">Tạm dừng</span>';
      var items=pr.items?pr.items.length:0;
      return '<tr><td style="font-weight:600;color:var(--adm-text)">'+esc(pr.name)+'</td><td>'+fmtDate(pr.startDate)+'</td><td>'+fmtDate(pr.endDate)+'</td><td>'+badge+'</td><td>'+items+' sản phẩm</td><td><button class="adm-btn adm-btn--danger adm-btn--sm" onclick="ADM.delPromo('+pr.id+')">Xóa</button> <button class="adm-btn adm-btn--outline adm-btn--sm" onclick="ADM.stopPromo('+pr.id+')">Dừng</button></td></tr>';
    }).join(''));
    $('admPromoPage').innerHTML=paginate(data);
  }).catch(function(){$('admPromoTable').innerHTML=emptyMsg('Lỗi tải khuyến mãi');});
}

/* ===== ACTIONS (exposed globally) ===== */
window.ADM={
  goPage:function(page){pageState.page=page;renderSection(currentSection);},
  delProduct:function(id){if(confirm('Xóa sản phẩm #'+id+'?'))api('/product/products/'+id,{method:'DELETE'}).then(function(){toast('Đã xóa sản phẩm');loadProducts();}).catch(function(){toast('Lỗi xóa sản phẩm');});},
  delWorkshop:function(id){if(confirm('Xóa workshop #'+id+'?'))api('/workshop/workshops/'+id,{method:'DELETE'}).then(function(){toast('Đã xóa workshop');loadWorkshops();}).catch(function(){toast('Lỗi xóa workshop');});},
  delPost:function(id){if(confirm('Xóa bài viết #'+id+'?'))api('/content/posts/'+id,{method:'DELETE'}).then(function(){toast('Đã xóa bài viết');loadPosts();}).catch(function(){toast('Lỗi xóa bài viết');});},
  delUser:function(id){if(confirm('Xóa người dùng #'+id+'?'))api('/identity/users/'+id,{method:'DELETE'}).then(function(){toast('Đã xóa người dùng');renderUsers($('admContent'));}).catch(function(){toast('Lỗi xóa người dùng');});},
  delPromo:function(id){if(confirm('Xóa khuyến mãi #'+id+'?'))api('/product/promotions/'+id,{method:'DELETE'}).then(function(){toast('Đã xóa');loadPromotions();}).catch(function(){toast('Lỗi xóa');});},
  stopPromo:function(id){if(confirm('Dừng khuyến mãi #'+id+'?'))api('/product/promotions/'+id+'/stop',{method:'PATCH'}).then(function(){toast('Đã dừng');loadPromotions();}).catch(function(){toast('Lỗi');});},
  updateOrder:function(id,status){if(!status)return;if(confirm('Cập nhật đơn #'+id+' → '+status+'?'))api('/order/orders/'+id+'/status?status='+status,{method:'PATCH'}).then(function(){toast('Đã cập nhật');loadOrders();}).catch(function(){toast('Lỗi cập nhật');});},
  
  openAddProductModal: function() {
    showModal('Thêm sản phẩm mới', `
      <form class="adm-form" id="addProductForm">
        <div class="adm-form-group">
          <label>Tên sản phẩm</label>
          <input type="text" name="name" required placeholder="Ví dụ: Bình gốm men lam">
        </div>
        <div class="adm-form-row">
          <div class="adm-form-group">
            <label>Giá (VNĐ)</label>
            <input type="number" name="price" required>
          </div>
          <div class="adm-form-group">
            <label>Số lượng kho</label>
            <input type="number" name="stockQuantity" required>
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
          <label>URL Hình ảnh</label>
          <input type="text" name="imageUrls" placeholder="Dán link ảnh vào đây">
        </div>
        <div class="adm-form-actions">
          <button type="button" class="adm-btn adm-btn--outline" onclick="ADM.closeModal()">Hủy</button>
          <button type="submit" class="adm-btn adm-btn--primary">Lưu sản phẩm</button>
        </div>
      </form>
    `);
    
    // Load categories
    api('/product/categories').then(data => {
      var sel = $('modalCategorySelect');
      if(sel) sel.innerHTML = (data || []).map(c => `<option value="${c.id}">${c.name}</option>`).join('') || '<option value="">Chưa có danh mục</option>';
    });

    $('addProductForm').onsubmit = function(e) {
      e.preventDefault();
      var fd = new FormData(this);
      var obj = Object.fromEntries(fd.entries());
      obj.price = parseFloat(obj.price);
      obj.stockQuantity = parseInt(obj.stockQuantity);
      obj.categoryId = parseInt(obj.categoryId);
      obj.imageUrls = obj.imageUrls ? [obj.imageUrls] : [];

      api('/product/products', {
        method: 'POST',
        body: JSON.stringify(obj)
      }).then(() => {
        toast('Đã thêm sản phẩm thành công!');
        ADM.closeModal();
        loadProducts();
      }).catch(err => {
        toast('Lỗi: Không có quyền (401) hoặc dữ liệu sai');
      });
    };
  },
  closeModal: function() {
    var m = $('admModalOverlay');
    if(m) m.classList.remove('is-open');
  }
};

/* ===== UI BUILDERS ===== */
function esc(s){if(!s)return'';var d=document.createElement('div');d.textContent=s;return d.innerHTML;}
function skeleton(n){var h='';for(var i=0;i<n;i++)h+='<div class="adm-skeleton" style="width:'+(60+Math.random()*40)+'%;height:18px"></div>';return h;}
function emptyMsg(t){return '<div class="adm-empty"><div class="adm-empty-icon">📭</div><p class="adm-empty-text">'+t+'</p></div>';}

function statusBadge(s){
  var map={PENDING:['warning','Chờ xử lý'],CONFIRMED:['info','Đã xác nhận'],SHIPPING:['info','Đang giao'],DELIVERED:['success','Đã giao'],COMPLETED:['success','Hoàn thành'],CANCELLED:['danger','Đã hủy']};
  var m=map[s]||['neutral',s||'—'];
  return '<span class="adm-badge adm-badge--'+m[0]+'">'+m[1]+'</span>';
}

function tableWrap(heads,bodyHTML){
  return '<div class="adm-table-wrap"><table class="adm-table"><thead><tr>'+heads.map(function(h){return '<th>'+h+'</th>';}).join('')+'</tr></thead><tbody>'+bodyHTML+'</tbody></table></div>';
}

function paginate(data){
  if(!data||data.totalPages<=1)return'';
  var html='<div class="adm-pagination">';
  html+='<button class="adm-page-btn" onclick="ADM.goPage('+(data.number-1)+')" '+(data.first?'disabled':'')+'>‹</button>';
  for(var i=0;i<Math.min(data.totalPages,7);i++){
    html+='<button class="adm-page-btn'+(i===data.number?' is-active':'')+'" onclick="ADM.goPage('+i+')">'+(i+1)+'</button>';
  }
  html+='<button class="adm-page-btn" onclick="ADM.goPage('+(data.number+1)+')" '+(data.last?'disabled':'')+'>›</button>';
  return html+'</div>';
}

function sectionHeader(title,section,actionBtn){
  return '<div class="adm-section"><div class="adm-section-header"><h3 class="adm-section-title">'+title+'</h3><div class="adm-section-actions">'+(actionBtn||'')+'<div class="adm-search"><input type="text" id="admSearch_'+section+'" placeholder="Tìm kiếm..."></div></div></div></div>';
}

function showModal(title, contentHTML) {
  var m = $('admModalOverlay');
  if(!m) {
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
function bindSearch(section){
  setTimeout(function(){
    var inp=$('admSearch_'+section);
    if(!inp)return;
    inp.addEventListener('input',function(){
      clearTimeout(searchTimer);
      searchTimer=setTimeout(function(){pageState.keyword=inp.value.trim();pageState.page=0;renderSection(currentSection);},400);
    });
  },50);
}

/* ===== INIT ===== */
document.addEventListener('DOMContentLoaded',function(){
  initLogin();
  // Check if already logged in
  if(token){showDashboard();}
  // Nav clicks
  document.querySelectorAll('.adm-nav-item[data-section]').forEach(function(btn){
    btn.addEventListener('click',function(){navigate(this.dataset.section);});
  });
  $('admLogout').addEventListener('click',logout);
  // Mobile menu
  $('admMenuToggle').addEventListener('click',function(){$('admSidebar').classList.toggle('is-open');});
  // Close sidebar on content click (mobile)
  $('admMain').addEventListener('click',function(){$('admSidebar').classList.remove('is-open');});
});
})();
