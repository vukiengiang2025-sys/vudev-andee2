# Xử lý sự cố VUDEV ANDEE 2

Trang này đi theo triệu chứng. Nếu không thấy mục của mình, mở **Tự kiểm tra** trong ứng dụng trước — nó đọc đúng trạng thái quyền, mạng, bộ não, micro, pin và phiên bản, rồi có nút dẫn thẳng tới màn hình cần sửa.

**Bộ lệnh chẩn đoán nhanh** (cắm cáp USB, bật gỡ lỗi USB):

```bash
adb devices
adb logcat -s Body:V BodyWs:V BodyWsClient:V Asr:V Tts:V
adb shell dumpsys activity services net.kuafuai.andee | head -40
adb shell appops get net.kuafuai.andee SYSTEM_ALERT_WINDOW
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys deviceidle whitelist | grep kuafuai
```

---

## 1. Không thấy quả cầu

Triệu chứng: mở ứng dụng thấy trang **Tự kiểm tra**, không có quả cầu nổi; hoặc quả cầu biến mất sau một thời gian.

### 1.1. Kiểm tra theo thứ tự

1. **Quyền Cửa sổ nổi** — nguyên nhân số một. Mở ứng dụng → **Tự kiểm tra** → dòng **Quyền cửa sổ nổi**:
   - Nếu đỏ: bấm **Mở cài đặt** → bật **Hiển thị trên ứng dụng khác**.
   - HyperOS: **Cài đặt → Ứng dụng → Quản lý ứng dụng → Andee → Hiển thị trên ứng dụng khác**.
   - Kiểm tra bằng adb: `adb shell appops get net.kuafuai.andee SYSTEM_ALERT_WINDOW` phải thấy `allow`.
2. **Dịch vụ nền có đang chạy?** Quả cầu do `BodyCoreService` (foreground service) vẽ. Nếu tiến trình bị dừng, không có gì hiện. Mở lại ứng dụng hoặc bấm thông báo thường trực của Andee.
3. **Pin / tự khởi chạy** — nếu quả cầu sống được vài chục phút rồi mất, xem mục [8](#8-ứng-dụng-tự-tắt-sau-một-lúc-hyperos).
4. **Khởi động lại máy** — sau khi HyperOS cập nhật, quyền trợ năng và đôi khi cửa sổ nổi bị đặt lại.
5. **Gỡ và cài lại chỉ là phương án cuối** — mất dữ liệu (xem [INSTALL.md](INSTALL.md#10-dữ-liệu-và-sao-lưu)).

### 1.2. Quả cầu có nhưng bấm không thấy gì

Đây thường là **trợ năng** chứ không phải cửa sổ nổi — xem mục 2. Nếu bấm quả cầu mà không có phản hồi nào (không nghe, không hiện thẻ), kiểm tra thêm **Micro** (mục 6) và xem log:

```bash
adb logcat -s Body:V BodyWs:V | grep -i -E "overlay|start|crash|exception"
```

### 1.3. Trên HyperOS còn một lớp quyền riêng

HyperOS có thêm **"Hiển thị cửa sổ nổi"** (pop-up window) tách khỏi "Hiển thị trên ứng dụng khác" trên một số bản ROM. Nếu đã bật overlay mà vẫn không thấy, tìm trong **Cài đặt → Ứng dụng → Andee → Quyền khác** hoặc **Quyền đặc biệt** và bật mọi mục liên quan đến cửa sổ nổi/pop-up.

---

## 2. Quả cầu hiện nhưng không điều khiển được màn hình

Triệu chứng: trợ lý trả lời được, nhưng khi nhờ mở ứng dụng/bấm thì báo lỗi; hoặc **Tự kiểm tra** có dòng **Dịch vụ Hỗ trợ tiếp cận** màu đỏ.

### 2.1. Bật lại trợ năng

- **Cài đặt → Trợ năng → Ứng dụng đã tải xuống → Andee → Bật**.
- Trên HyperOS: **Cài đặt → Cài đặt bổ sung → Trợ năng → Ứng dụng đã tải xuống → Andee**.
- Android sẽ hiện cảnh báo đỏ; đọc và xác nhận.

### 2.2. Trợ năng tự tắt

Đây là hành vi của ROM, không phải lỗi ứng dụng:

- HyperOS/MIUI **tắt trợ năng sau khi cập nhật hệ thống**, sau khi "force stop", đôi khi sau khi khởi động lại.
- Nếu bạn dùng **WeChat** trên MIUI/HyperOS: WeChat có thể che cây UI; ứng dụng có cơ chế thêm dịch vụ phụ `MiuiEnhanceTBService` **chỉ khi** đã cấp `WRITE_SECURE_SETTINGS` bằng adb:
  ```bash
  adb shell pm grant net.kuafuai.andee android.permission.WRITE_SECURE_SETTINGS
  ```
  Không có quyền này, ứng dụng vẫn làm việc bằng ảnh chụp màn hình nhưng chậm và kém chính xác hơn.

### 2.3. Kiểm tra bằng adb

```bash
adb shell settings get secure enabled_accessibility_services
# phải thấy: net.kuafuai.andee/net.kuafuai.andee.ScreenBodyService
adb shell settings get secure accessibility_enabled
# phải là: 1
```

Nếu thiếu, bật bằng giao diện là an toàn nhất. Lệnh `settings put secure enabled_accessibility_services` sẽ **ghi đè toàn bộ** danh sách dịch vụ trợ năng hiện có (có thể tắt TalkBack hoặc dịch vụ khác). Nếu buộc phải dùng, hãy **nối thêm** vào giá trị hiện tại, đừng ghi đè.

### 2.4. Màn hình khoá

Khi màn hình khoá, nhóm lệnh `screen.*` bị chặn. Đây là chốt an toàn, không phải lỗi.

### 2.5. WeChat trên điện thoại là chỉ-đọc

Trên điện thoại, ứng dụng **từ chối** bấm/vuốt/nhập vào WeChat vì một cú chạm tự động có thể khiến WeChat chặn chụp màn hình của chính bạn. Đây là chủ ý (chi tiết trong [SECURITY.md](SECURITY.md)). Đọc và chụp màn hình WeChat vẫn được; muốn thao tác, dùng bản desktop hoặc nhờ thiết bị khác.

---

## 3. Lỗi xác thực nhà cung cấp AI

Triệu chứng: chat báo "chưa có API Key", "API key bị từ chối", hoặc **Kiểm tra kết nối** đỏ.

### 3.1. Trước tiên: đã đóng Cài đặt chưa?

Thay đổi nhà cung cấp/khoá chỉ áp dụng khi **đóng bảng Cài đặt**. Đóng rồi hỏi lại.

### 3.2. Đọc đúng thông báo

| Thông báo trong Trung tâm AI | Nghĩa | Cách sửa |
| --- | --- | --- |
| "Hãy nhập API key trước" | Chưa có khoá và nhà cung cấp yêu cầu khoá | Dán khoá rồi Lưu |
| "API key bị từ chối (HTTP 401/403)" | Khoá sai, bị thu hồi, hoặc không có quyền | Tạo khoá mới trên trang nhà cung cấp; kiểm tra đã bật API/model chưa |
| "Tài khoản hết số dư (HTTP 402)" | Hết tiền/hạn mức | Nạp tiền hoặc đổi nhà cung cấp/dự phòng |
| "Không tìm thấy model hoặc địa chỉ (HTTP 404)" | Sai **Model** hoặc **Base URL** | Xem mục 3.3 |
| "Bị giới hạn tần suất (HTTP 429)" | Gọi quá nhanh/quá nhiều | Xem mục 4 |
| "Nhà cung cấp đang lỗi (HTTP 5xx)" | Máy chủ hãng lỗi | Thử lại sau; đặt dự phòng |

### 3.3. Sai Base URL / định dạng

- **OpenAI, DeepSeek, OpenRouter, Ollama, máy chủ tương thích:** Base URL là phần gốc; ứng dụng tự thêm `/chat/completions`. Ví dụ DeepSeek: `https://api.deepseek.com`.
- **Gemini:** Base URL `https://generativelanguage.googleapis.com/v1beta`; ứng dụng tự thêm `/models/<model>:generateContent`. **Không** dán URL đầy đủ có `?key=`.
- **Anthropic:** Base URL `https://api.anthropic.com`; ứng dụng tự thêm `/v1/messages`.
- Nếu bạn dán URL đã có sẵn `/chat/completions`, ứng dụng dùng nguyên URL — hữu ích khi tài liệu hãng đưa URL đầy đủ.

### 3.4. Khoá dán sai

- Sao chép lại từ đầu đến cuối, không có dấu cách/xuống dòng.
- DeepSeek bắt đầu bằng `sk-`; Gemini bằng `AIza`; Claude bằng `sk-ant-`; OpenRouter bằng `sk-or-`.
- Nếu dán khoá mới nhưng ô khoá trống: ứng dụng đang dùng **khoá đã lưu**. Gõ khoá mới vào ô rồi Lưu.

### 3.5. Nhà cung cấp trả lời nhưng trợ lý không làm được gì

- Mô hình không hỗ trợ **function calling / tools**. Chọn model có tools (DeepSeek Chat, GPT-4o, Gemini Flash, Claude Sonnet…).
- Mô hình quá nhỏ (Ollama trên máy yếu) trả lời sai định dạng công cụ. Thử model lớn hơn cho vai trò **Chính**.
- "Kết nối OK" chỉ chứng minh URL + khoá + model hợp lệ, **không** chứng minh model biết gọi công cụ.

### 3.6. Khoá biến mất sau khi đổi máy / khôi phục backup

Khoá được mã hoá bằng Android Keystore của **máy này**. Backup/khôi phục sang máy khác hoặc Keystore bị đặt lại → khoá cũ không đọc được. Nhập lại khoá; đây là hành vi đúng.

---

## 4. Bị giới hạn tần suất (429) / tốn token

- **429** là giới hạn của nhà cung cấp, không phải của ứng dụng. Chờ vài giây rồi thử lại; ứng dụng đã tự thử lại 1 lần với 429/5xx/lỗi mạng.
- Đặt **Dự phòng** ở Trung tâm AI để tự chuyển khi nhà cung cấp chính bị giới hạn/hết số dư.
- Với gói miễn phí (Gemini), hạn mức tính theo phút/ngày; giảm tần suất:
  - Tắt **Thông báo tin nhắn** nếu không cần phân loại từng thông báo.
  - Tắt **Tự xem lại sau khi im lặng** (⚙ → Sổ tay) hoặc tăng số phút im lặng, giảm số lần/ngày.
  - Gán vai trò **Tiết kiệm** cho một model rẻ: phân loại thông báo không cần model mạnh.
- Xem mức dùng: ⚙ → **Sổ tay** hiện số lần tổng kết và token ước tính; nhà cung cấp cũng có trang usage.
- Nếu bị 429 liên tục dù chờ: kiểm tra tài khoản có bị khoá/hết hạn mức tháng không.

---

## 5. Không có mạng / không kết nối được

Triệu chứng: **Tự kiểm tra** dòng **Mạng Internet** đỏ; chat báo lỗi mạng; **Kiểm tra kết nối** timeout.

1. Kiểm tra Wi-Fi/dữ liệu di động; mở trình duyệt thử một trang.
2. Tắt VPN/proxy tạm thời; một số VPN chặn API của hãng.
3. Mạng có trang đăng nhập (captive portal): mở trình duyệt đăng nhập rồi thử lại.
4. DNS: đổi sang DNS công cộng (1.1.1.1 / 8.8.8.8) nếu mạng nhà chặn.
5. Kiểm tra nhà cung cấp có bị chặn ở Việt Nam không (một số dịch vụ cần mạng quốc tế).
6. **Ollama/máy chủ nội bộ:** điện thoại và máy tính phải cùng Wi-Fi; dùng IP LAN của máy tính (không dùng 127.0.0.1); mở cổng 11434 trên tường lửa.
7. **Hub:** `ws://` tới máy tính; kiểm tra Hub đang chạy, đúng IP/cổng, và máy tính nhìn thấy điện thoại.
8. **Kiểm tra phiên bản** (api.heyandee.com) có thể đỏ dù chat vẫn chạy; đây là kênh riêng.

Lệnh kiểm tra từ điện thoại (nếu có `curl`, thường không có trên HyperOS):

```bash
adb shell ping -c 3 api.deepseek.com
adb shell ping -c 3 8.8.8.8
```

Ping có thể bị chặn ICMP dù mạng vẫn tốt; kết quả "không ping được" chưa chắc là mất mạng.

---

## 6. Tiếng Việt: font, dấu, giọng đọc, nhận dạng

### 6.1. Chữ hiện sai dấu / ô vuông / mất dấu

- Ứng dụng **không nhúng font riêng**; mọi chữ dùng font hệ thống Android (Roboto), vốn hỗ trợ tiếng Việt. Nếu chữ trong ứng dụng bị lỗi, kiểm tra **Cài đặt hệ thống → Hiển thị → Cỡ chữ/Phông chữ** đã đổi sang font lạ chưa; đổi về mặc định.
- **Trang HTML** (kết quả dài) do Android System WebView vẽ. Nếu trang bị ô vuông/mất dấu: cập nhật **Android System WebView** và **Chrome** trong CH Play.
- Nếu chỉ **một số chữ** bị lỗi (thường là ký tự trong khối mã), đó có thể là font monospace của máy; đổi nội dung sang chữ thường hoặc dùng bản desktop.

### 6.2. Mô hình trả lời lẫn tiếng Anh / không dấu

- Kiểm tra ngôn ngữ giao diện (⚙ → VI/中/EN) và **hỏi lại bằng tiếng Việt đầy đủ dấu**. Mô hình được dặn trả lời theo ngôn ngữ người dùng.
- Model quá nhỏ thường trả lời kém tự nhiên; thử DeepSeek Chat / Claude / Gemini Flash.
- Nếu câu trả lời hiện đúng nhưng **đọc to** bị sai dấu: đó là vấn đề của giọng TTS, không phải font (xem 6.3).

### 6.3. Đọc to tiếng Việt

- ⚙ → **Hệ thống AI → Cục bộ / API → Giọng nói → Đọc to**:
  - Chọn **Hệ thống** và cài giọng tiếng Việt: **Cài đặt hệ thống → Ngôn ngữ & nhập → Chuyển văn bản thành giọng nói → Công cụ TTS → Ngôn ngữ → Tiếng Việt** (tải dữ liệu giọng nếu cần).
  - Hoặc **Google TTS** với **Ngôn ngữ = `vi-VN`**.
  - **Volcengine** có giọng chủ yếu tiếng Trung/Anh; đọc tiếng Việt sẽ sai hoặc lẫn.
- Nếu không nghe gì: kiểm tra nút loa trên quả cầu (đang tắt tiếng?), âm lượng media, và tai nghe Bluetooth đang kết nối.

### 6.4. Nhận dạng giọng nói tiếng Việt

- **Volcengine** mặc định được cấu hình cho tiếng Trung/Anh, **không nhận tiếng Việt**.
- Chọn **Google** cho phần **Nghe**, nhập **Ngôn ngữ = `vi-VN`**, và cần Google API key + Project ID + Location.
- Hoặc **gõ chữ** — không cần micro.
- Nếu bấm quả cầu mà micro không mở: cấp quyền Micro; kiểm tra micro đang bị ứng dụng khác (cuộc gọi, ghi âm) chiếm; tắt/bật lại từ đánh thức bằng nút micro trên thanh.

### 6.5. Từ đánh thức không nhận

⚙ → **Đánh thức bằng giọng nói**:

- Thu âm lại 3 lần, nói rõ và đều.
- Tăng **Độ nhạy** lên **Nhạy** hoặc **Rất nhạy** (dễ bị TV/tiếng ồn đánh thức nhầm).
- Bật **Nghe cả khi khoá màn hình** nếu muốn dùng khi tắt màn hình (tốn pin).
- Đảm bảo nút micro trên thanh quả cầu **đang bật** (không bị tắt).

---

## 7. Xung đột chữ ký APK

Triệu chứng khi cập nhật:

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE
Package net.kuafuai.andee signatures do not match previously installed version
```

Nguyên nhân: APK mới được ký bằng **khoá khác** với bản đang cài. Android không cho ghi đè.

### 7.1. Nguyên tắc: cài đè bằng -r, tuyệt đối không gỡ để "sửa"

- `adb install -r` cài đè, **giữ dữ liệu** (chat, sổ tay, két, nhà cung cấp).
- Gỡ ứng dụng sẽ **xoá sạch** dữ liệu, và ứng dụng **không có backup** (`allowBackup=false`). Không có cách khôi phục.
- Đừng làm theo lời khuyên "uninstall rồi cài lại" trên mạng.

### 7.2. Cách xử lý đúng

1. **Dùng đúng APK cùng nguồn ký.** Nếu bạn tự build, giữ **một** `debug.keystore` ở thư mục gốc kho và dùng lại cho mọi lần build (xem [BUILD.md](BUILD.md#5-debugkeystore)). Nếu tải từ GitHub Actions: debug.keystore nằm trong kho nên chữ ký ổn định giữa các lần chạy.
2. **Cài gói song song** nếu chỉ muốn dùng bản mới mà không mất bản cũ:
   ```bash
   ./gradlew :app:assembleDebug -PdbgSuffix=.dbg
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Gói mới có tên `net.kuafuai.andee.dbg`, cài cạnh bản cũ. Lưu ý: hai gói có dữ liệu riêng, không chia sẻ sổ tay/khoá.
3. **Nếu buộc phải gỡ** (chỉ khi bạn chấp nhận mất dữ liệu): xem [INSTALL.md](INSTALL.md#8-cập-nhật-và-xung-đột-chữ-ký-apk). Trước khi gỡ, thử **Sao chép tất cả** trong Sổ tay và ghi lại danh sách nhà cung cấp/khoá (khoá không xuất được; bạn phải có lại khoá từ trang hãng).

### 7.3. Sau khi cài đè

- Quyền đã cấp (trợ năng, micro, overlay) **thường được giữ**, nhưng HyperOS có thể tắt trợ năng sau khi cài lại. Mở **Tự kiểm tra** để chắc chắn.
- APK debug **không cập nhật được** lên bản release ký bằng khoá khác — chọn một loại và giữ.

---

## 8. Ứng dụng tự tắt sau một lúc (HyperOS)

Triệu chứng: dùng bình thường, khoảng **40–60 phút** sau tự mất quả cầu/không trả lời; mở lại thì được.

Đây là **chính sách năng lượng của Xiaomi** (AutoPowerKill), không phải crash:

1. Bật **Tự khởi chạy**: **Cài đặt → Ứng dụng → Tự khởi động → Andee → Bật** (hoặc dùng nút trong **Tự kiểm tra → Tự khởi chạy**).
2. Bật **Không hạn chế pin**: **Cài đặt → Pin → Tiết kiệm pin → Andee → Không hạn chế**.
3. Trong **Tự kiểm tra**, dòng **Chạy nền** và **Tự khởi chạy** phải xanh.
4. Khoá ứng dụng trong đa nhiệm (mở **Ứng dụng gần đây** → giữ thẻ Andee → biểu tượng khoá) nếu ROM của bạn có.
5. Không dùng "dọn RAM" hoặc "tối ưu" các ứng dụng dọn nền.

Nếu vẫn bị: ghi log `adb logcat -s Body:V` quanh thời điểm mất để xác định bị hệ thống kill hay crash thật.

---

## 9. Những lỗi nhỏ thường gặp

| Triệu chứng | Cách xử lý |
| --- | --- |
| "Chưa nhập API Key" dù đã nhập | Đóng bảng Cài đặt để áp dụng; kiểm tra dòng nhà cung cấp có nhãn **Đang dùng** và **Đã lưu API key** |
| Thẻ cài đặt không mở | Quả cầu còn nhưng trợ năng/overlay lỗi; mở ứng dụng → Tự kiểm tra |
| Đổi ngôn ngữ nhưng quả cầu vẫn tiếng cũ | Đóng/mở lại thẻ; nếu vẫn vậy, tắt/bật lại trợ lý để vẽ lại overlay |
| Đổi cỡ chữ không thấy đổi | Cỡ chữ chỉ áp dụng sau khi dịch vụ khởi động lại; thoát và mở lại trợ lý |
| Không nhận thông báo | Bật **Truy cập thông báo**; HyperOS cần tắt/bật lại sau cập nhật; kiểm tra **Thông báo tin nhắn** không bị Tắt |
| Nhắc việc trễ | Bật **Không hạn chế pin**; báo thức Android có thể trễ vài phút khi máy ngủ sâu |
| Máy tính không kết nối cổng 9008 | Kiểm tra token (Cài đặt → Nâng cao → Bảo mật kết nối), cùng Wi-Fi; hoặc dùng `adb forward tcp:9008 tcp:9008` |
| "Unauthorized" khi kết nối WebSocket | Client chưa gửi frame `{"type":"auth","token":"…"}`; token sai; hoặc token vừa được tạo mới (mọi client cũ bị ngắt) |
| Ứng dụng báo "không có quyền ghi secure settings" | Cấp bằng adb: `adb shell pm grant net.kuafuai.andee android.permission.WRITE_SECURE_SETTINGS` (không bắt buộc) |
| Dữ liệu biến mất sau khi đổi máy | Không có backup; Keystore không di chuyển. Đây là thiết kế, không phải lỗi |

---

## 10. Khi nào cần báo lỗi

Báo lỗi kèm:

- Mã commit/phiên bản (⚙ → Tự kiểm tra → dòng **Phiên bản**, hoặc `adb shell dumpsys package net.kuafuai.andee | grep versionName`).
- Kiểu máy, phiên bản HyperOS/Android.
- Trợ năng có bật không, overlay có bật không.
- Chế độ bộ não: Cục bộ / API hay Hub.
- 20–50 dòng log quanh lúc lỗi (`adb logcat -s Body:V BodyWs:V Asr:V Tts:V`), **che khoá API** trước khi gửi.

Lỗi bảo mật: xem [SECURITY.md](SECURITY.md#10-báo-lỗi-bảo-mật) — **không** mở issue công khai.

---

## 11. Liên quan

- [INSTALL.md](INSTALL.md) — cấp quyền, HyperOS, adb.
- [BUILD.md](BUILD.md) — lỗi build/Gradle, keystore.
- [AI-PROVIDERS.md](AI-PROVIDERS.md) — lỗi khoá, model, dự phòng.
- [PERSONALIZATION.md](PERSONALIZATION.md) — giọng nói, ngôn ngữ, cỡ chữ.
- [SECURITY.md](SECURITY.md) — WebSocket, token, giới hạn.
