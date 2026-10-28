# SubTrack – Subscription Tracker (Android)

Đồ án môn Lập trình thiết bị di động – Nhóm 4. Ứng dụng Android native (Kotlin + Jetpack Compose + Material 3), giao diện 100% tiếng Việt. Ý tưởng và luồng chức năng tham khảo từ Sub Radar (chỉ có trên iOS); tên, giao diện và mã nguồn do nhóm tự xây dựng.

| Thư mục | Nội dung |
|---|---|
| `android/` | Dự án Android Studio |
| `docs/01-kiem-tra-moi-truong.md` | Kết quả kiểm tra môi trường |
| `docs/02-khao-sat-subradar.md` | Khảo sát Sub Radar (đã xác minh / suy luận) |
| `docs/*.pptx`, `docs/*.docx` | Slide giới thiệu đề tài, Use Case, kế hoạch nhóm |

## Chạy bằng Android Studio

1. Mở Android Studio → **File › Open** → chọn thư mục `android` bên trong repo (không chọn thư mục gốc của repo).
2. Đợi Gradle Sync xong (lần đầu cần Internet để tải thư viện). Android Studio tự tạo `local.properties` trỏ tới Android SDK.
3. Chọn thiết bị chạy trên thanh công cụ:
   - **Máy ảo:** chọn `Pixel_8` (đã có sẵn) hoặc tạo mới ở **Tools › Device Manager › Create Virtual Device**. Máy ảo cần khá nhiều RAM; nếu máy yếu nên dùng điện thoại thật.
   - **Điện thoại thật:** bật **Tùy chọn nhà phát triển** (vào Cài đặt › Thông tin điện thoại, chạm 7 lần vào *Số hiệu bản tạo*), bật **Gỡ lỗi USB**, cắm cáp và chấp nhận hộp thoại "Cho phép gỡ lỗi USB". Điện thoại sẽ hiện trong danh sách thiết bị.
4. Bấm nút **Run ▶** (Shift + F10). Ứng dụng tên **SubTrack** được cài và mở tự động.

Yêu cầu: Android 8.0 (API 26) trở lên. Dự án dùng AGP 9.3.3, Gradle 9.5.0 (wrapper), compileSdk/targetSdk 37, JDK 17+ (JBR đi kèm Android Studio).

## Build APK debug bằng dòng lệnh

```bat
cd android
gradlew.bat assembleDebug
```

macOS/Linux: `./gradlew assembleDebug`. Nếu báo không tìm thấy Java, đặt `JAVA_HOME` tới JBR của Android Studio, ví dụ `C:\Program Files\Android\Android Studio\jbr`.

File APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Cài lên điện thoại đang kết nối bằng `adb install -r app-debug.apk`, hoặc chép file vào điện thoại rồi mở để cài (cần cho phép cài từ nguồn không xác định).

Chạy kiểm thử: `gradlew.bat testDebugUnitTest lintDebug`.

## Chức năng đã hoàn thành

- **Giới thiệu lần đầu** (onboarding) khi mở app lần đầu.
- **Gói đăng ký**: thẻ tổng quan (chi phí tháng ước tính, số gói đang hoạt động, số gói đến hạn trong 7 ngày); danh sách có vạch màu báo gia hạn (đỏ ≤ 7 ngày, cam ≤ 30 ngày), chip tag và nền tảng; tìm theo tên, tag, nền tảng, danh mục; lọc theo trạng thái và danh mục; sắp xếp theo ngày, chi phí, tên; trạng thái đang tải / lỗi / trống / không có kết quả.
- **Thêm / sửa / xóa gói**: tên, số tiền, tiền tệ (VND/USD/EUR), chu kỳ (tuần/tháng/quý/năm), ngày thanh toán kế tiếp (DatePicker tiếng Việt), danh mục, trạng thái (đang hoạt động / tạm dừng / đã hủy), nhắc trước (không nhắc, đúng ngày, 1/3/7/30 ngày), tag (tạo tag ngay trong form), nền tảng, liên kết quản lý (mở trình duyệt), ghi chú. Có kiểm tra dữ liệu (tên bắt buộc, tối đa 60 ký tự, số tiền hợp lệ và không âm, URL phải là http/https), hộp thoại xác nhận xóa, thông báo đã lưu / đã xóa.
- **Thêm vào Lịch**: mở ứng dụng Lịch của Android với sự kiện lặp theo chu kỳ.
- **Tự cập nhật ngày**: gói đang hoạt động đã qua ngày thanh toán tự chuyển sang kỳ kế tiếp khi mở app.
- **Tổ chức**: tạo / sửa / xóa tag có màu (chặn trùng tên); thống kê số gói theo nền tảng; chi phí tháng theo danh mục.
- **Báo cáo**: tổng chi phí mỗi tuần / tháng / quý / năm; biểu đồ tròn cơ cấu chi phí (gộp "Các gói khác" khi nhiều hơn 6 gói); chi phí theo danh mục; 3 gói tốn kém nhất; chọn tiền tệ báo cáo (không quy đổi tỷ giá, có ghi chú số gói bị loại).
- **Nhắc hạn**: WorkManager kiểm tra mỗi ngày và ngay sau khi lưu gói, gửi thông báo cục bộ theo số ngày nhắc của từng gói, mỗi kỳ chỉ nhắc một lần; xin quyền thông báo trên Android 13+; nút gửi thông báo thử.
- **Cài đặt**: tiền tệ mặc định, bật/tắt nhắc hạn, xuất CSV (UTF-8, mở được bằng Excel), chia sẻ báo cáo chi phí dạng văn bản, thêm 5 gói mẫu (ghi chú rõ "Gói mẫu để demo"), xóa toàn bộ dữ liệu (có xác nhận), giới thiệu.
- **Lưu trữ**: SQLite trên máy (bảng `subscriptions`, `tags`, `subscription_tags`), truy cập qua Repository; đóng mở app dữ liệu vẫn còn.
- **Việt hóa**: mọi chuỗi nằm trong `res/values/strings.xml` (tiếng Việt là mặc định); ứng dụng ép locale `vi-VN` nên DatePicker, định dạng tiền (260.000 ₫) và ngày (dd/MM/yyyy) đều theo tiếng Việt dù máy đặt ngôn ngữ khác. Chỉ giữ nguyên tên riêng (Android, iPhone, PlayStation…) và mã tiền tệ.
- **Giao diện sáng / tối** theo cài đặt hệ thống.

## Đã kiểm thử

- `assembleDebug`: build thành công.
- `testDebugUnitTest`: 12/12 test qua (quy đổi chi phí theo chu kỳ, tổng chi theo tiền tệ, cuộn ngày thanh toán, cuối tháng, mức báo gia hạn, đọc số tiền VND/USD, kiểm tra URL, điều kiện nhắc hạn, số liệu báo cáo, CSV, định dạng tiền tiếng Việt). Unit test đã phát hiện và giúp sửa một lỗi đọc số tiền (`1.2.3.4` từng được chấp nhận).
- `lintDebug`: không còn lỗi.

## Chưa hoàn thành / giới hạn

- **Chưa chạy thử trên máy ảo hay điện thoại**: theo yêu cầu, nhóm tự mở app vì máy không đủ bộ nhớ chạy emulator cùng lúc. Vì vậy điều hướng, giao diện tiếng Việt trên màn hình nhỏ và lỗi runtime chưa được kiểm tra thực tế; cần chạy thử theo hướng dẫn ở trên.
- **Không đồng bộ đám mây / không có tài khoản**: Sub Radar dùng iCloud. Kế hoạch nhóm xếp đăng nhập vào phần mở rộng; muốn làm thì cần một dự án Firebase (file `google-services.json`, bật Authentication và Firestore).
- **Không quy đổi tỷ giá**: báo cáo chỉ cộng các gói cùng một tiền tệ, vì quy đổi cần API tỷ giá bên ngoài.
- **Nền tảng cố định**: có 12 nền tảng có sẵn, chưa cho tự tạo nền tảng như Sub Radar.
- **Xuất dữ liệu** chỉ hỗ trợ CSV (Sub Radar có thêm JSON và văn bản thuần); chưa có nhập lại dữ liệu từ file.
- **Thông báo hằng ngày** chạy theo lịch của WorkManager nên giờ nhắc không cố định chính xác.

## API, biến môi trường, quyền

- Không cần API key, backend hay biến môi trường nào. Không có khóa bí mật trong mã nguồn.
- Quyền Android: `POST_NOTIFICATIONS` (Android 13+, xin khi bật nhắc hạn). Thêm vào Lịch và mở liên kết dùng Intent của hệ thống, không cần quyền riêng.
