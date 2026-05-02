$env:DB_PASSWORD = "Nguyetdth892005"
$env:JWT_SIGNER_KEY = "Nguyetdth89200512345678910111213" # Dự phòng luôn cho JWT_SIGNER_KEY
$env:CLOUDINARY_API_KEY = "316921521754154" # Cấu hình giả để service khởi động được
$env:CLOUDINARY_API_SECRET = "_UmYMjvVBmizhELcVrSaUrYMPjw" # Bạn có thể thay bằng key thật sau


$services = @(
    "discovery-service",
    "api-gateway",
    "identity-service",
    "content-service",
    "order-service",
    "product-service",
    "workshop-service"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     KHOI DONG HE THONG GOM SU XANH     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Chạy discovery-service (Eureka) trước tiên để các service khác đăng ký
Write-Host "-> Dang khoi dong discovery-service (Eureka)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit -Command `"cd backend/discovery-service; mvn spring-boot:run`""

# Chờ 15 giây để Eureka Server khởi động lên hẳn
Write-Host "-> Cho 15 giay de Eureka Server san sang..." -ForegroundColor Magenta
Start-Sleep -Seconds 15

# 2. Chạy các service backend còn lại
foreach ($service in $services) {
    if ($service -ne "discovery-service") {
        Write-Host "-> Dang khoi dong $service..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-NoExit -Command `"cd backend/$service; mvn spring-boot:run`""
        # Dừng 3 giây giữa các lần bật để máy không bị đơ do quá tải CPU
        Start-Sleep -Seconds 3 
    }
}

# 3. Chạy Frontend
Write-Host "-> Dang khoi dong Frontend tai cong 3000..." -ForegroundColor Cyan
# Yêu cầu máy cài Node.js (npx http-server). Tham số -c-1 để tắt cache, cờ -o để tự mở trình duyệt.
Start-Process powershell -ArgumentList "-NoExit -Command `"cd frontend; npx http-server -p 3000 -c-1 -o /pages/trang-chu_417-354.html`""

Write-Host "========================================" -ForegroundColor Green
Write-Host "Hoan tat! Cac dich vu dang chay o cac cua so moi." -ForegroundColor Green
Write-Host "Frontend chay tai: http://localhost:3000" -ForegroundColor Green
Write-Host "De tat, ban chi can dong cac cua so terminal do lai." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
