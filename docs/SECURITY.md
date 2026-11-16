# Bảo mật VUDEV ANDEE 2

Ứng dụng này **đọc màn hình, bấm hộ, đọc thông báo và giữ mật khẩu** — đó là chức năng, không phải lỗi. Vì vậy tài liệu này nói thẳng: cái gì được bảo vệ, cái gì **chưa** được bảo vệ, và bạn nên làm gì.

Đây là bản fork của Andee. Chính sách bảo mật và mô hình đe doạ gốc nằm ở [SECURITY.md](../SECURITY.md) (tiếng Anh). Tài liệu này mô tả **bản fork VUDEV ANDEE 2**, bao gồm các thay đổi đang hoàn thiện (WIP).

> **Không có bảo mật tuyệt đối.** Một thiết bị Android đã root, hoặc đang mở khoá và nằm trong tay người khác, không thể được bảo vệ bởi bất kỳ ứng dụng nào. Hãy đọc mục [8](#8-những-gì-chưa-được-bảo-vệ) trước khi dùng cho việc quan trọng.

---

## 1. Tóm tắt nhanh

| Chủ đề | Trạng thái |
| --- | --- |
| Khoá API AI | ✅ Mã hoá AES/GCM, khoá trong Android Keystore |
| Khoá giọng nói / CodeFlying / token WebSocket | ✅ Mã hoá cùng cơ chế |
| Sổ tay, két mật khẩu | ✅ Mã hoá AES/GCM, khoá riêng trong Keystore |
| Sao lưu dữ liệu | ✅ Tắt hoàn toàn (cloud + chuyển máy) |
| Telemetry nội dung | ✅ Không có SDK phân tích/crash/ads; chỉ kiểm tra phiên bản |
| Cổng WebSocket 9008 | ⚠️ Có token (mặc định bật, WIP); loopback được miễn; có thể tắt |
| Hub từ xa | ⚠️ `ws://` không mã hoá, không xác thực (tài liệu gốc) |
| Prompt injection từ màn hình | ⚠️ Chưa có sandbox; phần lớn là dặn mô hình, không phải chốt cứng |
| Két mật khẩu | ⚠️ Không ràng buộc sinh trắc học/mở khoá máy |
| Khoá khi đang chạy | ⚠️ Nằm trong bộ nhớ tiến trình |
| Bản debug | ⚠️ Ký bằng khoá debug, không dùng để phát hành |

---

## 2. Khoá được lưu thế nào

### 2.1. Lớp mã hoá chung

Tệp `app/src/main/java/net/kuafuai/andee/security/SecretBox.kt`:

- Thuật toán **AES/GCM/NoPadding**, IV 12 byte, tag 128 bit.
- Khoá sinh và giữ trong **Android Keystore**, alias `andee_secret_box_v1`; khoá **không thể xuất khỏi Keystore**.
- Định dạng lưu: `v1:base64(iv + ciphertext)`.
- Nếu không giải mã được (bản sao lưu từ máy khác, Keystore bị đặt lại), trả về `null` thay vì ném lỗi — ứng dụng coi như "chưa có khoá" và mời nhập lại. Đây là chủ ý: một blob cũ không được làm ứng dụng không mở được.

### 2.2. Những gì đã được mã hoá

| Dữ liệu | Nơi lưu | Ghi chú |
| --- | --- | --- |
| Khoá nhà cung cấp AI | `SharedPreferences` `andee_ai` (Trung tâm AI) | Mỗi bản ghi có khoá riêng đã seal |
| Khoá Volcengine (`api_key`), Google (`google_api_key`), CodeFlying | `voice_prefs` | Lưu dạng `*_sealed`; bản văn bản thường bị xoá |
| Token WebSocket | `voice_prefs` (`ws_token_sealed`) | Sinh ngẫu nhiên 18 byte, Base64 URL-safe |
| Sổ tay | `notebook_prefs` | AES/GCM, alias riêng |
| Két mật khẩu | `vault_prefs` | AES/GCM, alias riêng (không phải alias của sổ tay) |

Các alias/prefs **tách riêng có chủ đích**: xoá sổ tay không làm két không đọc được, và ngược lại. Trước đây khoá từng nằm dạng văn bản thường trong `llm_api_key`; khi Trung tâm AI khởi tạo lần đầu, khoá được chuyển sang bản mã hoá và **bản văn bản thường bị xoá**.

### 2.3. Giao diện không lộ khoá

- Ô khoá đã lưu **luôn trống**; chỉ hiện gợi ý "Đã lưu key — nhập key mới để thay".
- Nút **Xoá key đã lưu** xoá cả bản mã hoá.
- Khoá không được ghi log, không đưa vào `toString()`, không hiện trong thông báo lỗi.
- Khi gọi API, khoá chỉ đi trong header tới **Base URL bạn cấu hình**. Riêng Gemini dùng header `x-goog-api-key` thay vì query parameter, để khoá không rơi vào log URL.

---

## 3. Dữ liệu đi đâu?

### 3.1. Không có telemetry nội dung

Trong mã nguồn **không có** SDK phân tích, crash reporting, quảng cáo hay advertising ID (không có Firebase Analytics, Sentry, Crashlytics, v.v.). Không có nội dung hội thoại, màn hình, danh bạ hay vị trí nào được gửi về dự án.

### 3.2. Ngoại lệ duy nhất: kiểm tra phiên bản

Khi mở ứng dụng lần đầu và khi bấm **Kiểm tra lại** trong **Tự kiểm tra**, ứng dụng gọi:

```text
https://api.heyandee.com/api/version
```

Tham số gửi đi (đúng như trong mã nguồn):

| Tham số | Giá trị |
| --- | --- |
| `current` | Phiên bản ứng dụng (`versionName`) |
| `platform` | `android` |
| `model` | Kiểu máy (`Build.MODEL`) |
| `brand` | Hãng (`Build.BRAND`) |
| `os_version` | Phiên bản Android + API |
| `device_id` | Mã cài đặt ngẫu nhiên (`body-xxxxxxxx`, đổi mỗi lần cài; **không phải** ANDROID_ID) |
| `app_package` | `net.kuafuai.andee` |
| `codeflying` | `1` nếu đang dùng CodeFlying và có khoá, ngược lại `0` |
| `lang` | Ngôn ngữ **của hệ thống** (không phải ngôn ngữ giao diện ứng dụng) |

- Địa chỉ này **cứng trong mã**, không có công tắc tắt. Muốn tắt phải sửa mã và build lại (`device/SelfCheck.kt`, hằng `VERSION_API_BASE`).
- Giới hạn 10 lần/IP/ngày; bấm **Kiểm tra lại** tiêu một lượt.
- Nội dung không bao gồm: hội thoại, màn hình, ứng dụng đã cài, danh bạ, vị trí, log crash, advertising ID.

### 3.3. Dữ liệu bạn chủ động gửi tới nhà cung cấp

| Dữ liệu | Đi tới | Ghi chú |
| --- | --- | --- |
| Câu hỏi + lịch sử hội thoại + kết quả công cụ | Nhà cung cấp AI bạn chọn (Base URL trong Trung tâm AI) | Có thể kèm ảnh chụp màn hình khi gọi mô hình thị giác |
| Âm thanh micro | Nhà cung cấp ASR (Volcengine hoặc Google) | Chỉ khi bạn bấm nói/đánh thức |
| Câu trả lời cần đọc | Nhà cung cấp TTS (hệ thống, Google, Volcengine) | Nếu chọn giọng hệ thống, không ra khỏi máy |
| Mã QR/ghép bạn bè | Thiết bị kia trong cùng Wi-Fi | Kênh mã hoá đầu-cuối bằng khoá công khai đã ghim |
| Lệnh khi dùng Hub | Hub bạn cấu hình (`ws://`) | Không mã hoá — xem mục 6 |

> "Chế độ cục bộ" chỉ đổi **nơi đặt bộ não**, không có nghĩa "xử lý hoàn toàn trên máy". Dữ liệu vẫn tới nhà cung cấp AI/giọng nói bạn chọn.

---

## 4. Quyền: vì sao và khi nào

Ứng dụng xin nhiều quyền nhạy cảm. Nguyên tắc thiết kế: **hỏi trước từng việc, không xin hết lúc cài, bị từ chối thì dừng việc đó** (`device/DeviceCommsController.kt` gọi `ensurePermission()` trước mỗi thao tác và trả về `denied("sms")` thay vì tìm cách lách).

| Quyền | Cho phép gì | Nếu từ chối |
| --- | --- | --- |
| **Cửa sổ nổi** (`SYSTEM_ALERT_WINDOW`) | Quả cầu, phụ đề, thẻ cài đặt — toàn bộ giao diện | **Không hiện gì cả**; đây là quyền bắt buộc duy nhất |
| **Trợ năng** (AccessibilityService) | Đọc cây UI, bấm, vuốt, nhập chữ, chụp màn hình | Mất "bàn tay"; quả cầu vẫn còn, trò chuyện vẫn chạy |
| **Micro** (`RECORD_AUDIO`) | Nhận dạng giọng nói, từ đánh thức | Không nói được; vẫn gõ chữ được |
| **Đọc thông báo** (NotificationListenerService) | Phân loại thông báo, nhắc việc | Không tự đọc thông báo |
| **Thông báo** (`POST_NOTIFICATIONS`, Android 13+) | Hiện thông báo nhắc việc | Không có nhắc nhở |
| **SMS / cuộc gọi / danh bạ / lịch** | Chỉ khi bạn yêu cầu một việc cần chúng | Việc đó báo `denied`, dừng lại |
| **Vị trí / nhận diện hoạt động** | Thông tin ngữ cảnh (thời tiết, đi lại…) | Bỏ qua phần đó |
| **Camera** | "Mắt" chụp ảnh khi bạn yêu cầu | Không dùng được camera |
| **Danh sách ứng dụng** (`QUERY_ALL_PACKAGES`) | Trang bị: biết ứng dụng nào đã cài | Trang bị không đầy đủ |
| **Usage access** (`PACKAGE_USAGE_STATS`) | Xếp hạng ứng dụng hay dùng | Chỉ xếp theo ứng dụng Andee tự mở |
| **Bỏ tối ưu pin** | Nhắc việc đúng giờ, chạy nền | Nhắc việc có thể trễ |
| **`WRITE_SECURE_SETTINGS`** | Chỉ cấp được bằng adb; đổi bàn phím tạm thời, thêm dịch vụ trợ năng phụ trên MIUI | Không ảnh hưởng chức năng chính |
| **MediaProjection** | Chụp màn hình dự phòng khi trợ năng bị thu hồi | Mất đường dự phòng |

Danh sách đầy đủ, gắn từng quyền với từng công cụ: [permissions.md](permissions.md) (tiếng Anh).

---

## 5. Cổng WebSocket 9008

### 5.1. Hiện trạng

- `BodyWsServer` lắng nghe trên **`0.0.0.0:9008`** (mọi giao diện mạng) khi trợ năng/body đang chạy.
- Mục đích: cho bộ não trên máy tính hoặc công cụ gỡ lỗi điều khiển thiết bị trong cùng LAN (hoặc qua `adb forward`).
- Không có mã hoá TLS. Xác thực bằng **token** (WIP, mặc định bật):
  - Token sinh ngẫu nhiên 18 byte, lưu đã mã hoá, hiện trong ⚙ **Cài đặt → Nâng cao → Bảo mật kết nối** (có **Sao chép token** / **Tạo token mới**).
  - Client LAN phải gửi frame đăng nhập: `{"type":"auth","token":"<token>"}`; sai thì bị đóng với mã 4001.
  - **Kết nối qua loopback (127.0.0.1, tức `adb forward`) được miễn token.**
  - Tắt token (công tắc **Yêu cầu token → Tắt**): bất kỳ ai trong cùng Wi-Fi đều có thể đọc màn hình và bấm hộ. Chỉ tắt khi bạn hiểu rõ.
- Các lệnh gỡ lỗi nguy hiểm (`debug.say`, `debug.sweep`, `debug.worklog`) **chỉ chấp nhận từ loopback**, kể cả khi token bật.

### 5.2. Rủi ro còn lại

- **Loopback được miễn token** cũng có nghĩa một ứng dụng khác trên chính điện thoại (có quyền Internet) có thể kết nối `127.0.0.1:9008` và điều khiển trợ lý. Đây là đánh đổi để `adb forward` và công cụ gỡ lỗi hoạt động mà không cần token.
- Khi token **tắt**, mức bảo vệ quay về như upstream v0.15.0: **không xác thực**. Xem SECURITY.md gốc, mục "Known security limitations".
- Giảm rủi ro: bật token, chỉ dùng Wi-Fi tin cậy, hoặc dùng `adb forward` và chặn cổng 9008 trên router/tường lửa.

---

## 6. Hub từ xa (chế độ Đám mây)

Nếu bạn chọn bộ não trên Hub thay vì chạy trên máy:

- `BodyWsClient` kết nối tới `hub_url` bạn nhập, gửi `register` (device_id, device_name, danh sách công cụ) rồi nhận lệnh.
- **Không có token/chữ ký cho kết nối hub**, và URL thường là `ws://` **không mã hoá**. Trên mạng không tin cậy, người nghe lén có thể thấy lệnh và nội dung màn hình, hoặc giả làm hub để ra lệnh.
- Cách giảm: chỉ trỏ tới Hub bạn kiểm soát, trên mạng tin cậy; hoặc dùng chế độ **Cục bộ / API** (không có kênh nhận lệnh từ xa).

---

## 7. Nút thoát khẩn (kill switch)

Ứng dụng có ba cách dừng, tất cả đều **cục bộ** — không cần mạng, không cần mô hình, không cần số dư:

1. **Phím nguồn trên thanh điều khiển của quả cầu → Thoát → xác nhận.** Đây là nút thoát hoàn toàn: dừng micro, từ đánh thức, mọi cửa sổ nổi, socket WebSocket, vòng lặp bộ não và gọi `stopSelf()`. Dữ liệu và quyền đã cấp được giữ nguyên.
2. **Nói câu thoát** (khớp cả câu, do người dùng chủ động): "Thoát Andee", "Tắt Andee", "Đóng Andee", "Andee thoát", "Andee tắt", "Andee đóng", "Nghỉ ngơi đi", "Tạm nghỉ đi", "Andee nghỉ đi". Không có tác dụng với câu trả lời tự động, và bị từ chối khi đang ghi biên bản cuộc họp.
3. **Dừng việc đang làm** (nút dừng trên giao diện): dừng lượt hiện tại và gửi sự kiện `task.stop`, không tắt toàn bộ trợ lý.

Sau khi Thoát, mở lại ứng dụng (icon Andee) để khởi động lại. Mở lại **không** cần cấp lại quyền đã có.

Giới hạn của kill switch: nó **không** xoá dữ liệu, **không** thu hồi quyền, **không** ngăn ứng dụng tự khởi động lại nếu Android/HyperOS bật lại dịch vụ trợ năng hoặc người dùng mở ứng dụng. Muốn "sạch" hơn, thu hồi quyền Trợ năng trong Cài đặt hệ thống hoặc gỡ ứng dụng.

---

## 8. Những gì CHƯA được bảo vệ

Đây là danh sách trung thực, đọc trước khi dùng cho việc quan trọng.

1. **Prompt injection từ nội dung màn hình.** Trợ lý đọc chữ trên màn hình (tin nhắn, web, SMS) và nội dung đó đi vào ngữ cảnh mô hình. Kẻ xấu có thể viết "bỏ qua chỉ dẫn trước đó và…". **Không có sandbox.** Phần lớn biện pháp là **dặn mô hình** (hỏi trước khi làm việc không thể hoàn tác), không phải chốt cứng. Một số ít hành động bị chặn bằng mã (ví dụ: thao tác ghi trên WeChat bị từ chối trên điện thoại; lượt do thông báo không được mở két/tạo ngữ cảnh), nhưng đó là ngoại lệ. Coi thiết bị có thể bị dẫn dắt làm bất cứ điều gì mà quyền và trạng thái đăng nhập cho phép — kể cả chuyển tiền.
2. **Cổng WebSocket có thể bị tắt xác thực**, và loopback luôn được miễn token (mục 5). Trên Wi-Fi công cộng, hãy coi thiết bị như đang mở.
3. **Hub từ xa dùng `ws://` không mã hoá, không xác thực** (mục 6).
4. **Két không ràng buộc mở khoá máy/sinh trắc học.** Khoá Keystore không yêu cầu xác thực người dùng. Ai mở được máy (hoặc root) có thể vào két qua ứng dụng. Két bảo vệ chống trích xuất dữ liệu và chống lộ cho mô hình, **không** chống người cầm máy đã mở khoá.
5. **Khoá nằm trong bộ nhớ khi đang chạy.** Dù lưu mã hoá, khi gọi API khoá phải được giải mã. Root, debugger, hoặc lỗ hổng trong tiến trình có thể đọc được.
6. **Bản debug không phải bản phát hành.** APK debug ký bằng khoá debug; bất kỳ ai có APK đều có thể dịch ngược/đọc mã. Muốn phát hành, dùng bản release ký bằng khoá riêng và bảo vệ khoá đó.
7. **Dữ liệu nhạy cảm trên đĩa.** Ảnh chụp màn hình, biên bản cuộc họp, nhật ký chế độ "thu thập dữ liệu thao tác" (mặc định **tắt**) nằm trong `filesDir`. Chúng không được sao lưu, nhưng người mở được máy/root có thể đọc. Bật thu thập thao tác sẽ ghi lại những gì bạn làm.
8. **Archive ngữ cảnh** (`filesDir/context`) là văn bản thường chứa lời hội thoại và kết quả công cụ (trừ ảnh và một số công cụ nhạy cảm). Nó bị xoá khi xoá lịch sử chat.
9. **Không có chống sửa đổi ứng dụng (anti-tamper)** và không có chứng thực hai chiều với nhà cung cấp AI; API key là thứ duy nhất xác thực bạn.
10. **Keystore bị đặt lại hoặc khôi phục backup** làm dữ liệu mã hoá không đọc được (mất dữ liệu, không phải lộ). Ứng dụng sẽ yêu cầu nhập lại khoá.
11. **Andee gốc / fork không chịu trách nhiệm về hậu quả** — xem [DISCLAIMER.md](../DISCLAIMER.md) và [acceptable-use.md](acceptable-use.md).

---

## 9. Khuyến nghị sử dụng

- Cài trên máy **bạn sở hữu**; không đưa máy đã cài cho người lạ dùng chung.
- Bật **Yêu cầu token** (mặc định); không tắt trên Wi-Fi công cộng. Nếu chỉ dùng qua cáp, dùng `adb forward`.
- Không dùng trên Wi-Fi khách sạn/quán cà phê khi bật trợ năng + không có token.
- Bật **Tự khởi chạy** và **Không hạn chế pin** trên HyperOS để trợ lý không bị dọn — nhưng hiểu rằng điều đó cũng làm nó chạy nền thường xuyên hơn.
- Dùng khoá API riêng, có **hạn mức chi tiêu** trên trang của nhà cung cấp; không dùng khoá chung với hệ thống khác.
- Không dùng trợ lý để thanh toán/chuyển tiền khi không có người lớn trông coi. Không có chốt cứng nào cho việc đó.
- Định kỳ xem **Sổ tay** và xoá điều bạn không muốn nó nhớ; xem **Tự kiểm tra** sau mỗi lần cập nhật hệ thống.
- Khi không dùng nữa: tắt quyền Trợ năng, rồi gỡ ứng dụng (dữ liệu sẽ mất — xem [INSTALL.md](INSTALL.md#10-dữ-liệu-và-sao-lưu)).
- Giữ ứng dụng và Android cập nhật; bản fork chỉ được sửa lỗi trên phiên bản mới nhất.

---

## 10. Báo lỗi bảo mật

- **Không mở issue công khai** cho lỗ hổng bảo mật.
- Dự án gốc: báo qua GitHub Security Advisories của `kuafuai/andee` (xem [SECURITY.md](../SECURITY.md)).
- Bản fork: mở **private security advisory** trên kho `vukiengiang2025-sys/vudev-andee2`, hoặc liên hệ người duy trì. Đừng đăng chi tiết ra nơi công cộng trước khi có bản sửa.
- Khi báo, ghi rõ: phiên bản/mã commit, kiểu máy + ROM, trợ năng có bật không, chế độ bộ não (Cục bộ/Hub), bước tái hiện, mức ảnh hưởng.

---

## 11. Liên quan

- [AI-PROVIDERS.md](AI-PROVIDERS.md) — khoá, dự phòng, nhà cung cấp.
- [INSTALL.md](INSTALL.md) — cấp quyền, cập nhật, dữ liệu.
- [permissions.md](permissions.md) — từng quyền gắn với từng công cụ (tiếng Anh).
- [README gốc, phần Security disclosure](../README.md#security-disclosure) — mô hình đe doạ upstream.
