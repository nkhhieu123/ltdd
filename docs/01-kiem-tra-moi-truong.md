# 1. Kiểm tra môi trường

Ngày kiểm tra: 27/10/2026 · Máy: Windows 11

## Repository

| Mục | Kết quả |
|---|---|
| Repo dự án | `orca/projects/ltdd` (GitHub `nkhhieu123/subtrack`, trước đây tên `ltdd`), trước đó chỉ có thư mục `docs/` – không có dự án Android nào, không lo ghi đè |
| Repo REA | `orca/projects/rea/rea` – bản clone của `morluto/rea` (MCP dịch ngược nhị phân, ứng dụng, quan sát website qua CDP) |
| REA MCP | **Chưa kết nối** với AI agent: không có công cụ `rea` nào trong phiên làm việc. Cần chạy `npx rea-agents setup` rồi khởi động lại agent mới dùng được |
| Thư mục đích | `orca/projects/ltdd/android/` |

## Công cụ Android

| Công cụ | Trạng thái |
|---|---|
| Android Studio | Có (`C:\Program Files\Android\Android Studio`) |
| JDK | JBR 25 đi kèm Android Studio (dùng để build); Java 24 trên PATH |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` – platforms 34, 35, 37.0; build-tools 34/35/36 |
| Gradle | Dùng Gradle Wrapper 9.5.0 (đã có trong cache), AGP 9.3.3 – cùng cấu hình với dự án Android Studio khác trên máy |
| Emulator | Có, AVD `Pixel_8` (Android 37.1, Google Play) |
| Thiết bị thật | Không có thiết bị nào kết nối qua `adb` lúc kiểm tra |

## Thiếu / hạn chế

- REA MCP chưa được cấu hình cho agent → khảo sát SubRadar bằng công cụ tải trang web thay thế.
- Biến môi trường `JAVA_HOME`, `ANDROID_HOME` chưa đặt; dự án dùng `local.properties` (`sdk.dir`) và JBR của Android Studio nên không bắt buộc.
