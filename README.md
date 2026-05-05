# 🏺 GỐM SỨ XANH — Nền tảng thương mại nghệ thuật gốm Việt

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)
![JavaScript](https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E)
![HTML5](https://img.shields.io/badge/html5-%23E34F26.svg?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/css3-%231572B6.svg?style=for-the-badge&logo=css3&logoColor=white)
![NodeJS](https://img.shields.io/badge/node.js-6DA55F?style=for-the-badge&logo=node.js&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/Rabbitmq-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)


> [!IMPORTANT]
> ## 📌 Quy tắc & Ràng buộc hệ thống
> 1. **Định danh người dùng**: 
>    - Cho phép trùng **Họ tên (Username)**.
>    - **Email** và **Số điện thoại** bắt buộc phải là duy nhất (Unique).
> 2. **Truy cập công khai**:
>    - Khách có thể xem: Trang chủ, Về chúng tôi, Workshop, Blog, và toàn bộ danh mục Sản phẩm mà không cần đăng nhập.
>    - Chỉ yêu cầu đăng nhập khi: Đặt hàng, Đăng ký workshop, hoặc vào trang Hồ sơ cá nhân.
> 3. **Tài khoản Quản trị (Bypass)**:
>    - Tài khoản: `admin` / Mật khẩu: `admin` (Dùng để truy cập nhanh trang quản trị).
> 4. **Cấu trúc thư mục**: 
>    - Phải giữ đúng cấu trúc `be/`, `fe-admin/`, `fe-user/` để hệ thống hoạt động ổn định.

---

**Gốm Sứ Xanh** là một hệ thống thương mại điện tử hiện đại chuyên cung cấp các sản phẩm gốm sứ thủ công nghệ thuật và các buổi workshop trải nghiệm làm gốm.

---

## 🌟 Tính năng nổi bật

### 🛒 Thương mại điện tử
- **Tìm kiếm & Bộ lọc**: Range Slider lọc giá và tìm kiếm theo tên.
- **Thanh toán**: Hỗ trợ COD và Chuyển khoản ngân hàng với **Mã QR động**.
- **Công khai**: Khách vãng lai có thể xem toàn bộ sản phẩm và thông tin.

### 🎓 Workshop & Trải nghiệm
- **Đăng ký linh hoạt**: Kiểm tra thời gian đăng ký và tự động vô hiệu hóa khi hết hạn.
- **Form thông minh**: Tự động điền thông tin nếu người dùng đã đăng nhập.

### 📊 Quản trị (Admin Dashboard)
- **Doanh thu**: Biểu đồ Chart.js trực quan theo tuần và theo tháng.
- **Quản lý**: Hệ thống quản lý Microservices đồng bộ dữ liệu.

---

## 🛠️ Công nghệ sử dụng

### Frontend (Modern Vanilla JS)
- HTML5, CSS3 (Glassmorphism design), JavaScript.
- Chart.js cho thống kê.

### Backend (Microservices)
- Java 17, Spring Boot, Spring Security, JWT.
- **Message Broker**: RabbitMQ (cho xử lý bất đồng bộ).
- **Dịch vụ**: `identity`, `product`, `order`, `workshop`, `content`.
- **Hạ tầng**: API Gateway, Eureka Discovery.

---

## 📁 Cấu trúc dự án

```text
gom_su_xanh/
├── be/                     # Backend Microservices
├── fe-admin/               # Frontend dành cho Quản trị viên
├── fe-user/                # Frontend dành cho Khách hàng
├── run-all.ps1             # Script khởi động tự động
└── README.md
```

---

## 🚀 Hướng dẫn khởi động nhanh

1. **Backend**:
   - Đảm bảo MySQL đã chạy và tạo đủ các DB cần thiết.
   - Chạy các service thông qua IDE hoặc script.
2. **Frontend**:
   - Sử dụng script `run-all.ps1` để khởi động cả BE và FE cùng lúc.
   - Truy cập `http://localhost:3000` để vào trang người dùng.
   - Truy cập `http://localhost:3000/fe-admin/index.html` để vào trang quản trị.

---

© 2026 **Gốm Sứ Xanh**. Phát triển bởi Nguyễn Thu Nguyệt và Nguyễn Văn Hiệu.
