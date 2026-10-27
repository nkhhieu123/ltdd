# 2. Khảo sát sản phẩm tham chiếu: Sub Radar

Nguồn khảo sát (27/10/2026):

- REA `capture_browser_scenario` (CLI `rea-agents@6.8.0`, Chromium 156 headless, profile tạm) mở `https://subradar.app/`, chụp DOM, cây accessibility và ảnh màn hình. Evidence ID: `ev_42204ec20c829782d52433c35c535d9db5d70f736e039c9bfc6fa5ba6d0d6bda`.
- 3 ảnh chụp màn hình app được website công khai (`screenshot-1/2/3.png`) và `styles.css`.
- Trang `/help` và trang App Store của ứng dụng.

Nhãn: **[XM]** = đã xác minh trực tiếp từ nguồn trên · **[SL]** = suy luận, chưa xác minh.

## Mục đích sản phẩm

- [XM] Ứng dụng theo dõi gói đăng ký định kỳ ("Take Control of Your Subscriptions"): ghi lại chi phí, chu kỳ, ngày gia hạn; nhắc trước hạn; thống kê chi tiêu.
- [XM] Chỉ có trên hệ sinh thái Apple (iOS/iPadOS 17.6+, macOS, visionOS). **Không có ứng dụng Android chính thức**, nên không có APK hợp pháp để khảo sát.
- [XM] Miễn phí, gói Pro 4,99 USD/năm. Ngôn ngữ duy nhất: tiếng Anh.
- [XM] Website `subradar.app` chỉ là trang giới thiệu, không có bản web của app.

## Màn hình và điều hướng (từ ảnh chụp app)

[XM] Thanh tab dưới cùng gồm 4 mục: **Subscriptions · Organization · Reports · Settings**.

| Màn hình | Nội dung đã thấy |
|---|---|
| Subscriptions (danh sách) | Tiêu đề lớn; nút sắp xếp góc phải; mỗi dòng có tên, chu kỳ, "Next: <ngày>", giá, chip tag màu, biểu tượng nền tảng; vạch màu bên trái báo sắp gia hạn; nút "+" nổi màu xanh lá |
| Edit Subscription | Nhóm "Subscription details": tên, giá, Billing Cycle (chọn), Next Billing Date, "Add reminder to calendar", công tắc Active. Nhóm "Organization": Tags, Platforms (chip). Nhóm "Additional information": Management URL, Notes. Nút Back / Save |
| Reports | 4 thẻ tổng chi phí Weekly / Monthly / Quarterly / Annual (mỗi thẻ một màu nhạt); biểu đồ tròn "Subscription Cost Breakdown" kèm chú thích tên, %, số tiền theo tháng |
| Organization | [SL] Quản lý Tags và Platforms (suy ra từ tên tab và trang Help) |
| Settings | [XM theo Help] iCloud sync, thời điểm nhắc, tích hợp lịch, Export Data (CSV/JSON/Plain text) |

## Tính năng

| Tính năng | Trạng thái | Ghi chú |
|---|---|---|
| Thêm/sửa/xóa gói: tên, giá, chu kỳ, ngày thanh toán kế tiếp, URL quản lý, ghi chú | [XM] | Help + ảnh chụp |
| Trạng thái Active/Inactive; gói inactive không tính vào chi tiêu | [XM] | Help |
| Báo hiệu gia hạn: đỏ ≤ 7 ngày, vàng/cam ≤ 1 tháng | [XM] | Help "Renewal indicators" + vạch màu trên ảnh |
| Category có màu, Tag có màu, Platform (15+ có sẵn, tự tạo được) | [XM] | Help |
| Tìm kiếm theo tên/tag/nền tảng, sắp xếp, lọc | [XM] | App Store + nút sắp xếp |
| Nhắc trước 1 ngày, 7 ngày, 1 tháng; thông báo đẩy | [XM] | Help, bản 1.4.0 |
| Thêm vào lịch (EventKit) | [XM] | Chỉ iOS |
| Báo cáo tuần/tháng/quý/năm, biểu đồ tròn, theo category/tag/platform | [XM] | Ảnh Reports + App Store |
| Xuất dữ liệu CSV/JSON/text | [XM] | Help |
| Đồng bộ iCloud/CloudKit | [XM] | Riêng Apple, không áp dụng cho Android |
| Chu kỳ thanh toán cụ thể | [SL] | Ảnh thấy Weekly, Monthly, Annually; báo cáo có Quarterly → giả định tuần/tháng/quý/năm |
| Đơn vị tiền tệ | Không xác minh được | Ảnh chỉ thấy USD |

## Giao diện

- [XM] Kiểu iOS mặc định: nền xám nhạt `#F2F2F7`, thẻ trắng bo góc, chữ đen đậm, màu nhấn xanh dương hệ thống; nút "+" xanh lá.
- [XM] Website dùng màu thương hiệu cam đỏ (`--primary-coral: #ff6b63`, `--primary-red: #ff5a52`), font hệ thống (`-apple-system`, Segoe UI, Roboto), có chế độ tối.
- [SL] Biểu tượng là SF Symbols của Apple, không dùng lại được trên Android.

## Dữ liệu, tài khoản, backend

- [XM] Dữ liệu lưu trên máy, đồng bộ bằng iCloud (CloudKit riêng tư). Không thấy đăng nhập hay API công khai.
- [XM] Website gửi thống kê truy cập qua TelemetryDeck; không liên quan chức năng app.
- Không có API nào cần tích hợp để tái tạo chức năng cốt lõi.

## Không xác minh được

- Màn hình Organization và Settings thực tế (không có ảnh chụp).
- Luồng thêm mới (chỉ thấy màn hình sửa), quy tắc kiểm tra dữ liệu, cách tự cập nhật ngày gia hạn.
- Tính năng Pro cụ thể.

## Quyết định cho bản Android

- Giữ **cấu trúc và luồng** đã xác minh: 4 tab, danh sách có báo hiệu gia hạn, form chi tiết, báo cáo 4 mốc + biểu đồ tròn, tag/nền tảng, trạng thái hoạt động, xuất CSV.
- Dùng **tên và giao diện riêng** (SubTrack, Material 3), không sao chép logo, ảnh hay thương hiệu Sub Radar, đúng như kế hoạch nhóm đã nêu.
- Thay iCloud bằng lưu trữ cục bộ SQLite (SQLiteOpenHelper + Repository); thay EventKit bằng Intent thêm sự kiện vào ứng dụng Lịch của Android; thông báo cục bộ bằng WorkManager.
- Tiền tệ mặc định VND, có chọn thêm USD/EUR.
