package org.gomsu.productservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Hàm này nhận vào 1 file, đẩy lên mây và trả về cái Link URL
    public String uploadImage(MultipartFile file) throws IOException {
        // Upload file lên Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        // Lấy đường dẫn URL an toàn (https) trả về
        return uploadResult.get("secure_url").toString();
    }

    public void deleteImage(String imageUrl) throws IOException {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                // 1. Tách lấy phần sau chữ /upload/
                // Ví dụ: https://res.cloudinary.com/thunguyet/image/upload/v12345/gomsu/posts/anh_dep.jpg
                String temp = imageUrl.substring(imageUrl.lastIndexOf("/upload/") + 8);

                // 2. Bỏ phần version (v12345/) nếu có
                if (temp.contains("/v")) {
                    temp = temp.substring(temp.indexOf("/") + 1);
                }

                // 3. Bỏ phần định dạng file (.jpg, .png) để lấy publicId
                String publicId = temp.substring(0, temp.lastIndexOf("."));

                // 4. Thực hiện xóa
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                // Log lỗi nhưng không làm sập luồng xóa trong DB của Nguyệt
                System.err.println("Cảnh báo: Lỗi xóa ảnh Cloudinary " + e.getMessage());
            }
        }
    }
}