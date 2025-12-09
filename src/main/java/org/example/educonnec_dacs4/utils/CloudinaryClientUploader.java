// File: src/main/java/org/example/educonnec_dacs4/utils/CloudinaryClientUploader.java

package org.example.educonnec_dacs4.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryClientUploader {

    // THÔNG TIN TÀI KHOẢN CLOUDINARY
    private static final String CLOUD_NAME = "dxolfgpgl";
    private static final String API_KEY = "441149122656668";
    // !!! RỦI RO BẢO MẬT: Không nên đặt khóa bí mật trên Client !!!
    private static final String API_SECRET = "WA-7EQZLhj5UgR9eKixDfIouyIA";

    private static final Cloudinary cloudinary;

    static {
        // Khởi tạo Cloudinary client
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key", API_KEY,
                "api_secret", API_SECRET,
                "secure", true // Bắt buộc dùng HTTPS
        ));
    }

    /**
     * Tải lên một tệp tin (ảnh, pdf, docx,...) trực tiếp lên Cloudinary.
     * @param fileToUpload Tệp tin đã chọn.
     * @param publicId Public ID để lưu trữ (username_timestamp_filename).
     * @param fileType Loại tệp ('image'/'file') để xác định folder.
     * @return URL an toàn (HTTPS) của tệp tin.
     * @throws IOException Nếu có lỗi trong quá trình upload.
     */
    public static String upload(File fileToUpload, String publicId, String fileType) throws IOException {
        // Kích thước tối đa cho upload tiêu chuẩn (10MB)
        final long MAX_STANDARD_UPLOAD_SIZE = 10485760;

        // Kiểm tra xem có cần dùng uploadLarge không (Nếu file > 10MB)
        boolean useLargeUpload = fileToUpload.length() > MAX_STANDARD_UPLOAD_SIZE;

        String folder = fileType.equals("image") ? "eduConnect/chat_images" : "eduConnect/chat_files";

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("public_id", publicId);
        params.put("overwrite", true);
        params.put("resource_type", "auto");

        if (fileType.equals("image")) {
            params.put("transformation", new com.cloudinary.Transformation().quality("auto:good"));
        }

        try {
            Map uploadResult;

            // >>> SỬ DỤNG PHƯƠNG THỨC UPLOAD THÍCH HỢP <<<
            if (useLargeUpload) {
                // Dùng uploadLarge cho tệp tin lớn hơn 10MB
                System.out.println("Sử dụng uploadLarge cho tệp tin lớn hơn 10MB.");
                uploadResult = cloudinary.uploader().uploadLarge(fileToUpload, params);
            } else {
                // Dùng upload tiêu chuẩn
                uploadResult = cloudinary.uploader().upload(fileToUpload, params);
            }

            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            System.err.println("Lỗi upload lên Cloudinary: " + e.getMessage());
            throw e;
        }
    }
}