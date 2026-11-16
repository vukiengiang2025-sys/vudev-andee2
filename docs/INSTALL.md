# Cài đặt VUDEV ANDEE 2 lên điện thoại Android

Tài liệu này dành cho người **không lập trình**: cài một tệp APK đã build sẵn lên điện thoại thật, cấp đúng quyền, và kiểm tra lần chạy đầu tiên. Trọng tâm là **Redmi 15 5G chạy HyperOS (Android 15)**, nhưng phần lớn nội dung dùng được cho điện thoại Android khác.

- Muốn tự build APK: xem [BUILD.md](BUILD.md).
- Muốn cấu hình nhà cung cấp AI: xem [AI-PROVIDERS.md](AI-PROVIDERS.md).
- Gặp lỗi: xem [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

> **Trước khi cài:** VUDEV ANDEE 2 là bản fork của Andee, có thể đọc màn hình và bấm hộ. Chỉ cài trên máy bạn sở hữu. Đọc [DISCLAIMER.md](../DISCLAIMER.md) và [SECURITY.md](SECURITY.md) trước khi dùng thường ngày.

---

## 1. Bạn cần gì

| Mục | Ghi chú |
| --- | --- |
| Điện thoại | Android 8.1 trở lên; **Android 11 trở lên để điều khiển màn hình**. Redmi 15 5G (Android 15) là mục tiêu chính |
| Tệp APK | Bản debug build từ kho này (không kèm khoá AI) hoặc bản bạn tự build |
| Tài khoản AI | Một API key (DeepSeek, OpenAI, Gemini, Claude, OpenRouter…) hoặc Ollama trong cùng Wi-Fi |
| Cáp USB (khuyến nghị) | Để cài bằng `adb` và xem log khi cần |
| Máy tính (tuỳ chọn) | Có `adb` nếu muốn cấp quyền nhanh hoặc kết nối cổng 9008 |

Dung lượng trống nên có ít nhất vài trăm MB. Không cần root.

---

## 2. Lấy APK

### Cách A — tải từ GitHub Actions (không cần build)

1. Mở trang Actions của kho: `https://github.com/vukiengiang2025-sys/vudev-andee2/actions/workflows/build-apk.yml`
2. Chọn lần chạy mới nhất có dấu tích xanh (workflow **build-apk**).
3. Kéo xuống mục **Artifacts**, tải **app-debug** (một tệp .zip).
4. Giải nén, lấy tệp **app-debug.apk**.

> Cấu hình hiện tại **tắt ABI splits** (xem [BUILD.md](BUILD.md#đường-dẫn-apk)), nên có **một** APK duy nhất tên `app-debug.apk`. Nếu bản build của bạn bật splits, chọn tệp theo kiến trúc máy: Redmi 15 5G dùng **arm64-v8a** (`app-arm64-v8a-debug.apk`).
>
> Nếu mục **Actions** chưa có workflow **build-apk**, tệp workflow chưa được đẩy lên GitHub — dùng **Cách B** (tự build) hoặc hỏi người duy trì.
>
> ✅ **Chữ ký ổn định:** debug.keystore nằm trong kho, nên bản tải từ Actions và bản build trên máy cùng chữ ký — cập nhật đè (adb install -r) giữ được dữ liệu.

### Cách B — tự build

Làm theo [BUILD.md](BUILD.md), sau đó lấy tệp trong:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Cách C — bản phát hành (nếu có)

Mục **Releases** của kho có thể có APK ký sẵn. Bản release không kèm khoá; bạn vẫn phải nhập API key của mình.

> APK debug được ký bằng khoá debug của kho mã. Nếu máy đang có bản cài từ khoá khác, Android sẽ từ chối cập nhật — xem [mục 8](#8-cập-nhật-và-xung-đột-chữ-ký-apk).

---

## 3. Chép APK sang điện thoại

Chọn một cách:

- **Cáp USB:** bật "Truyền tệp" (MTP) trên điện thoại, chép APK vào thư mục `Download`.
- **adb (khuyến nghị):**
  ```bash
  adb push app-debug.apk /sdcard/Download/
  ```
- **Gửi qua chat/cloud:** tải lại trên điện thoại (đơn giản nhưng dễ bị đổi tên tệp).

---

## 4. Cài APK trên Redmi 15 5G (HyperOS)

HyperOS chặn cài ứng dụng ngoài Play Store cho đến khi bạn cho phép ứng dụng quản lý tệp cài đặt.

1. Mở **Trình quản lý tệp** (Files) → **Download** → chạm `app-debug.apk`.
2. Nếu hiện "Vì an toàn, điện thoại không được phép cài đặt ứng dụng không rõ nguồn gốc":
   - chạm **Cài đặt** → bật **Cho phép từ nguồn này** cho ứng dụng đang mở tệp (Files/Chrome…).
   - Đường dẫn thủ công: **Cài đặt → Bảo mật → Cài đặt bảo mật → Cài đặt ứng dụng không rõ nguồn gốc**.
3. Nếu Play Protect cảnh báo "Ứng dụng không xác định": chọn **Vẫn cài đặt**. Đây là APK tự build, không phải ứng dụng từ Play Store.
4. Chờ cài xong → **Mở**.

Bằng adb:

```bash
adb devices                 # phải thấy serial + "device"
adb install -r app-debug.apk
```

Nếu adb báo `INSTALL_FAILED_UPDATE_INCOMPATIBLE` hoặc `signatures do not match`, **đừng gỡ ứng dụng** — xem [mục 8](#8-cập-nhật-và-xung-đột-chữ-ký-apk).

---

## 5. Lần chạy đầu tiên

Mở ứng dụng **Andee** (tên hiển thị vẫn là Andee vì điều kiện giấy phép; xem [README.vi.md](../README.vi.md)). Bạn sẽ đi qua:

1. **Màn hình chào:** chọn ngôn ngữ (mặc định **VI**) và bật/tắt giọng nói.
2. **Giới thiệu:** vài trang ngắn giải thích trợ lý làm được gì.
3. **Cấp quyền:** ứng dụng sẽ mời bạn cấp **Cửa sổ nổi**, **Trợ năng**, **Micro**.
4. **Trang Tự kiểm tra:** danh sách các mục xanh/đỏ. Đây là nơi quay lại mỗi khi có gì không chạy.

Nếu bỏ lỡ màn hình chào, mở lại ứng dụng → **Tự kiểm tra**; mọi nút cấp quyền đều ở đó.

---

## 6. Cấp quyền — thứ tự và đường dẫn trên HyperOS

Thứ tự quan trọng: **Cửa sổ nổi trước**, rồi **Trợ năng**, rồi **Micro**.

### 6.1. Cửa sổ nổi (bắt buộc)

Không có quyền này thì **không có gì hiện lên**: quả cầu, thẻ cài đặt, phụ đề đều là cửa sổ nổi.

- Nhanh nhất: mở ứng dụng → **Tự kiểm tra** → dòng **Quyền cửa sổ nổi** → **Mở cài đặt** → bật **Hiển thị trên ứng dụng khác**.
- Thủ công: **Cài đặt → Ứng dụng → Quản lý ứng dụng → Andee → Hiển thị trên ứng dụng khác** (hoặc **Cài đặt → Ứng dụng → Quyền đặc biệt → Hiển thị trên ứng dụng khác**).
- Kiểm tra: sau khi bật, quay lại màn hình chính — quả cầu nhỏ phải xuất hiện.

### 6.2. Dịch vụ Trợ năng (để điều khiển màn hình)

Đây là quyền cho phép đọc màn hình và bấm hộ. Android sẽ hiện cảnh báo đỏ; đọc và xác nhận.

- **Cài đặt → Trợ năng → Ứng dụng đã tải xuống → Andee → Bật**.
- Trên HyperOS, đường dẫn có thể là **Cài đặt → Cài đặt bổ sung → Trợ năng → Ứng dụng đã tải xuống**.
- Sau khi bật, thanh trạng thái có thể hiện biểu tượng trợ năng.

> HyperOS có quyền **tự tắt** dịch vụ trợ năng sau khi cập nhật hệ thống, sau khi "force stop", hoặc đôi khi sau khi khởi động lại. Nếu quả cầu vẫn còn nhưng trợ lý không bấm được gì, mở **Tự kiểm tra** — dòng Trợ năng sẽ đỏ.

### 6.3. Micro (để nói)

- Lần đầu bấm quả cầu, Android sẽ hỏi. Chọn **Trong khi dùng ứng dụng** hoặc **Luôn cho phép**.
- Nếu đã từ chối: **Cài đặt → Ứng dụng → Andee → Quyền → Micro → Cho phép**.

### 6.4. Đọc thông báo (tuỳ chọn, nhưng nên bật)

Cho phép trợ lý đọc thông báo để phân loại và nhắc bạn.

- **Cài đặt → Thông báo & thanh trạng thái → Truy cập thông báo → Andee Đọc thông báo → Bật**.
- HyperOS đôi khi cần **tắt rồi bật lại** sau khi cập nhật (Tự kiểm tra có nút **Tắt rồi bật lại**).

### 6.5. Pin và tự khởi chạy (bắt buộc trên Xiaomi/Redmi)

Đây là hai cài đặt quyết định trợ lý có "sống" qua đêm hay không.

- **Pin:** **Cài đặt → Pin → Tiết kiệm pin → Ứng dụng → Andee → Không hạn chế**. Hoặc dùng nút trong **Tự kiểm tra → Chạy nền** (hộp thoại "Bỏ qua tối ưu hoá pin?" → chọn **Cho phép/Ignore**).
- **Tự khởi chạy:** **Cài đặt → Ứng dụng → Tự khởi động → Andee → Bật**. Nếu máy không có mục này, dùng nút trong **Tự kiểm tra → Tự khởi chạy**; nút sẽ mở đúng màn hình ẩn của MIUI/HyperOS hoặc trang thông tin ứng dụng.

Vì sao cần: trên ROM Xiaomi, tiến trình nền bị hệ thống "dọn" khoảng **40–60 phút một lần** kể cả khi đang chạy foreground service. Nếu **Tự khởi chạy** tắt, sau khi bị dọn ứng dụng không thể tự sống lại — triệu chứng là *"một lúc sau nó tự tắt"*. Đây là hành vi của ROM, không phải lỗi ứng dụng.

### 6.6. Quyền đặc biệt chỉ cấp được bằng adb (tuỳ chọn)

Một số tính năng nâng cao cần quyền mà Android không cho bật bằng tay:

```bash
# Đổi bàn phím tạm thời / thêm dịch vụ trợ năng phụ trên MIUI-WeChat
adb shell pm grant net.kuafuai.andee android.permission.WRITE_SECURE_SETTINGS
```

Không cấp cũng không sao: ứng dụng sẽ dùng đường khác hoặc báo rõ tính năng nào bị thiếu.

### 6.7. Chụp màn hình dự phòng (MediaProjection)

Khi trợ năng bị thu hồi, trợ lý có thể vẫn "nhìn" bằng cách chụp màn hình. Android sẽ hỏi một lần:

- Hộp thoại **"Bắt đầu ghi hoặc chia sẻ màn hình của bạn?"** → chọn **Bắt đầu ngay** (hoặc tên tương tự tuỳ ROM).

Đây là quyền tạm thời: Android có thể hỏi lại ở lần chụp sau hoặc sau khi khởi động lại máy. Không cấp quyền này cũng không ảnh hưởng chức năng chính khi trợ năng còn bật.

---

## 7. Kiểm tra sau khi cài

Làm lần lượt, đừng bỏ qua mục đỏ:

| Việc | Kết quả đúng |
| --- | --- |
| Mở ứng dụng | Thấy trang **Tự kiểm tra**, không có dòng đỏ về Cửa sổ nổi / Trợ năng / Micro |
| Quay về màn hình chính | Quả cầu nhỏ nằm ở cạnh màn hình |
| Bấm quả cầu | Có phản hồi; lần đầu Android hỏi micro |
| Nói "Xin chào" | Nếu chưa có khoá AI, trợ lý báo cần nhập API key — đó là bình thường |
| Mở ⚙ → **Hệ thống AI** | Thấy tab **Cục bộ / API**, có nút **Quản lý nhà cung cấp và model** |
| Thêm DeepSeek + **Kiểm tra kết nối** | Hiện "Kết nối OK sau … ms" |
| Hỏi "Bạn là ai?" | Trả lời bằng tiếng Việt |

Nếu quả cầu không hiện: xem [TROUBLESHOOTING.md](TROUBLESHOOTING.md#1-không-thấy-quả-cầu).

---

## 8. Cập nhật và xung đột chữ ký APK

Android chỉ cho cập nhật một ứng dụng nếu **cùng chữ ký** với bản đang cài. Vì vậy:

- Cập nhật bình thường: `adb install -r app-debug.apk` hoặc chạm vào APK mới trên điện thoại.
- `-r` = cài đè, **giữ nguyên dữ liệu** (lịch sử chat, sổ tay, két, danh sách nhà cung cấp).
- Nếu báo lỗi chữ ký: **đừng gỡ ứng dụng**. Gỡ sẽ xoá sạch dữ liệu và không thể khôi phục (ứng dụng tắt backup).
- Nếu tải APK từ GitHub Actions: debug.keystore trong kho làm chữ ký ổn định giữa các lần chạy, nên cài đè giữ được dữ liệu.
  1. Chắc chắn bạn đang cài APK được build từ **cùng một `debug.keystore`** với bản đang có. Nếu tự build, giữ tệp `debug.keystore` ở thư mục gốc kho và dùng lại, đừng tạo mới mỗi lần (xem [BUILD.md](BUILD.md#5-debugkeystore)).
  2. Nếu vẫn không được: build **gói song song** để cài cạnh bản cũ mà không mất dữ liệu bản cũ:
     ```bash
     ./gradlew :app:assembleDebug -PdbgSuffix=.dbg
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
     (Gói mới có tên gói `net.kuafuai.andee.dbg`, chạy song song.)
  3. Chỉ khi bạn chấp nhận mất toàn bộ dữ liệu mới gỡ bản cũ:
     ```bash
     adb uninstall net.kuafuai.andee
     adb install app-debug.apk
     ```

Xem thêm [TROUBLESHOOTING.md](TROUBLESHOOTING.md#7-xung-đột-chữ-ký-apk).

---

## 9. Lệnh adb hữu ích

```bash
# Cài / cập nhật
adb install -r app-debug.apk

# Quyền runtime
adb shell pm grant net.kuafuai.andee android.permission.RECORD_AUDIO
adb shell pm grant net.kuafuai.andee android.permission.POST_NOTIFICATIONS
adb shell pm grant net.kuafuai.andee android.permission.WRITE_SECURE_SETTINGS

# Cửa sổ nổi (thay cho thao tác tay)
adb shell appops set net.kuafuai.andee SYSTEM_ALERT_WINDOW allow

# Bật trợ năng — chỉ dùng khi bạn biết mình đang làm gì;
# lệnh này GHI ĐÈ danh sách dịch vụ trợ năng hiện có trên máy.
adb shell settings put secure enabled_accessibility_services net.kuafuai.andee/net.kuafuai.andee.ScreenBodyService
adb shell settings put secure accessibility_enabled 1

# Bỏ tối ưu hoá pin
adb shell dumpsys deviceidle whitelist +net.kuafuai.andee

# Mở trang Tự kiểm tra
adb shell am start -n net.kuafuai.andee/.ui.SelfCheckActivity

# Xem log của ứng dụng
adb logcat -s Body:V BodyWs:V BodyWsClient:V Asr:V Tts:V

# Chuyển tiếp cổng WebSocket của máy ra máy tính (không cần token vì là loopback)
adb forward tcp:9008 tcp:9008
```

> **Không chạy lệnh trợ năng nếu máy đang bật TalkBack hoặc dịch vụ trợ năng khác** — nó sẽ thay thế danh sách. Cách an toàn nhất vẫn là bật bằng giao diện.

Nếu bật **token** cho WebSocket (Cài đặt → **Nâng cao** → **Bảo mật kết nối**), máy tính kết nối qua Wi-Fi phải gửi token. Kết nối qua `adb forward` (127.0.0.1) được miễn.

---

## 10. Dữ liệu và sao lưu

- Ứng dụng đặt `android:allowBackup="false"` và loại trừ mọi miền backup: **không có sao lưu tự động**.
- Gỡ ứng dụng hoặc khôi phục cài đặt gốc sẽ mất: lịch sử chat, sổ tay, két mật khẩu, danh sách nhà cung cấp, trang đã tạo.
- Khoá mã hoá nằm trong Android Keystore của **máy này**; chép dữ liệu sang máy khác sẽ không đọc được. Nếu Keystore bị đặt lại, ứng dụng coi như "chưa có khoá" và yêu cầu nhập lại — đây là hành vi đúng, không phải lỗi.
- Muốn xoá sạch dữ liệu nhưng giữ ứng dụng: ⚙ → **Nâng cao** → **Khôi phục cài đặt gốc** (đọc kỹ cảnh báo).

---

## 11. Gỡ cài đặt

```bash
adb uninstall net.kuafuai.andee
```

Hoặc trên máy: giữ biểu tượng Andee → **Gỡ cài đặt**. Thao tác này xoá toàn bộ dữ liệu ứng dụng (xem mục 10). Nếu chỉ muốn tắt trợ lý tạm thời, dùng phím nguồn trên thanh điều khiển của quả cầu → **Thoát** — dữ liệu được giữ nguyên.
