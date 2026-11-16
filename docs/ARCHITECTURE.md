# Kiến trúc VUDEV ANDEE 2

Tài liệu một-hai trang cho người muốn hiểu **mã nguồn chạy thế nào**, không phải hướng dẫn sử dụng. Nếu bạn chỉ muốn cài và dùng, đọc [INSTALL.md](INSTALL.md) và [AI-PROVIDERS.md](AI-PROVIDERS.md).

---

## 1. Ba module, hai "body"

| Module | Loại | Nội dung |
| --- | --- | --- |
| `core/` | JVM thuần, dùng chung | Vòng lặp agent (`LocalBrain`), prompt hệ thống (`LocalPrompt`), công cụ (`ToolSchemas`), `LlmClient` + bộ chuyển đổi nhà cung cấp, sổ tay (`NotebookBook`), ngữ cảnh/kịch bản, bạn bè (`peer/`), nhân vật (`pet/`, `ui/ball/`), cắt ngữ cảnh/archive |
| `app/` | Android (Kotlin, **không Compose**) | Dịch vụ nền, quả cầu nổi, trợ năng, micro/loa, cài đặt, két, bộ điều khiển thiết bị, WebSocket |
| `desktop/` | macOS (Compose Desktop) | Cùng bộ não, "tay" riêng của Mac (accessibility, chuột/bàn phím, ScreenCaptureKit, terminal) |

Điểm nối giữa bộ não và từng body là interface `BrainHost` trong `core/`. Bộ não không thấy kiểu Android hay macOS; mỗi body cài đặt `BrainHost` một lần (`AndroidBrainHost`, `DesktopBrainHost`).

---

## 2. Sơ đồ một lượt trò chuyện (Android)

```text
                    Người dùng
        chạm quả cầu / nói / gõ / thông báo / hẹn giờ
                              │
                              ▼
        ┌──────────────────────────────────────────────┐
        │ BodyCoreService (foreground service)         │
        │                                              │
        │  FloatingWindowUi  (overlay quả cầu, thẻ)     │
        │  AsrController ← AudioIO → ASR provider      │
        │  TtsController → TTS provider                │
        │                                              │
        │  LocalBrain (core)  ← cấu hình từ            │
        │      │                 VoiceConfig/BrainConfig│
        │      │ gọi mô hình                            │
        │      ▼                                        │
        │  LlmClient → WireAdapters → HTTP → AI provider│
        │      │                                        │
        │      │ tool calls                             │
        │      ▼                                        │
        │  CommandDispatcher ──► ScreenController       │
        │      │                     │                  │
        │      │                     ▼                  │
        │      │             ScreenBodyService          │
        │      │             (AccessibilityService)     │
        │      ├─► DeviceCommsController (SMS, call…)   │
        │      ├─► Vault / Notebook / Scheduler         │
        │      ├─► ScenePresets / Flow / Gear           │
        │      └─► BodyWsServer / BodyWsClient          │
        └──────────────────────────────────────────────┘
                              │
                              ▼
              Trả lời: giọng nói, phụ đề, trang HTML,
              sản phẩm lưu lại, văn bản dài, cử chỉ/quả cầu
```

---

## 3. Hai nửa của body: vì sao tách?

### `BodyCoreService` — phần sống

Là **foreground service** (không phải AccessibilityService). Nó sở hữu:

- Quả cầu, thẻ, phụ đề, lịch sử — tất cả là cửa sổ `TYPE_APPLICATION_OVERLAY` do `WindowManager` thêm vào.
- Micro, ASR, TTS, từ đánh thức.
- Bộ não đang chạy (`LocalBrain` hoặc client Hub) và vòng lặp agent.
- WebSocket server (cổng 9008) và client tới Hub.
- Sổ tay, scheduler, két, cài đặt, Trung tâm AI.

Lý do tách: nếu mọi thứ nằm trong AccessibilityService, mỗi lần ROM thu hồi trợ năng (HyperOS làm rất thường xuyên) là **mất toàn bộ trợ lý**. Tách ra thì mất trợ năng chỉ mất "bàn tay": quả cầu vẫn còn, vẫn nói chuyện được, vẫn đọc lịch sử được, và có nút mở lại trợ năng.

### `ScreenBodyService` — bàn tay

Là `AccessibilityService` mỏng. Khi được bật, nó gọi `BodyCoreService.start()` rồi giao "khả năng màn hình" (`attachScreen`). Khi bị tắt, nó **không** dừng body — chỉ thu hồi khả năng. Đây là nơi duy nhất chạm được vào cửa sổ ứng dụng khác: `dispatchGesture`, `performGlobalAction`, đọc cây UI, `takeScreenshot` (API 30+).

Nếu trợ năng bị thu hồi, còn một đường dự phòng: **MediaProjection** (`screen/ScreenCapture.kt`) để chụp màn hình, nhưng nó **không bấm hộ** được — không có thay thế nào cho `dispatchGesture` khi không root.

---

## 4. Overlay và xung đột với cử chỉ

- Mọi thứ người dùng thấy đều là overlay; không có màn hình chính kiểu Activity. Các Activity chỉ tồn tại cho việc đặc biệt: tự kiểm tra, trang HTML, quét QR, nhìn bằng camera, nhập chữ.
- Trước khi bấm/vuốt bằng `dispatchGesture`, dispatcher tạm chuyển overlay sang **không nhận chạm**, chờ ~50 ms cho cửa sổ cập nhật, rồi gửi cử chỉ và khôi phục. Nếu không, cử chỉ sẽ rơi vào quả cầu thay vì ứng dụng đích.
- Mỗi overlay gọi `hideFromAccessibility()` trước khi hiện, để trợ lý không đọc chính giao diện của mình như giao diện ứng dụng.

---

## 5. Vòng lặp agent: `LocalBrain`

Một lượt bắt đầu từ một trong các nguồn: người dùng nói/gõ, thông báo, hẹn giờ (todo), hoặc yêu cầu qua WebSocket. Tất cả đi vào `LocalBrain.submit()`.

Những điểm cốt lõi (đọc KDoc đầu tệp `core/.../brain/LocalBrain.kt`):

1. **Không streaming.** Gọi một lần, chờ trả lời đầy đủ. Streaming sẽ cần bộ phân tích SSE và ghép `tool_calls` từng mảnh — chưa cần cho bản này.
2. **Gọi công cụ tuần tự.** Không chạy song song: dispatcher có trạng thái cửa sổ (`passthroughForGesture`) không tái nhập được; hai cử chỉ cùng lúc sẽ giẫm lên nhau.
3. **Mỗi lần gọi công cụ có deadline.** `dispatchWithTimeout`; hết giờ thì báo mô hình là có thể việc vẫn đang chạy, không huỷ công việc.
4. **Ảnh bị tách khỏi kết quả công cụ và tiết chế.** Ảnh chụp màn hình được gỡ khỏi JSON, thay bằng dấu `<image attached>`, rồi gắn lại thành message ảnh riêng; chỉ giữ ảnh mới nhất. Tránh 15 bước × 1 ảnh = 15 ảnh trong ngữ cảnh.
5. **Đồng hồ đi kèm message người dùng**, không nằm trong system prompt — để prefix cache của nhà cung cấp còn hiệu lực.
6. **Giới hạn bước** (mặc định 40, đổi trong Cài đặt) và **chặn gọi trùng** cùng tên + tham số quá số lần; cả hai kết thúc bằng một câu trả lời nói ra, không im lặng dừng.
7. **Prompt hệ thống là hằng số**; phần thay đổi (giờ, ngôn ngữ, chỉ mục sổ tay, ngữ cảnh, trang) được chèn vào message người dùng.

Kết quả ra theo sáu kênh: giọng nói, phụ đề/chữ, trang HTML, sản phẩm lưu lại, văn bản dài, và cử chỉ/quả cầu (rung, đổi biểu cảm, thu gọn).

---

## 6. Dispatcher — một cửa cho mọi công cụ

`app/.../net/CommandDispatcher.kt` nhận cặp `(method, params)` và trả về JSON hoặc ném lỗi; lỗi được chuyển thành response lỗi ở tầng transport.

- **Hai bộ tên:** tên mô hình thấy (`tap_screen_element`, `get_screen_element`…) nằm trong `ToolSchemas` (core); tên nội bộ (`screen.tap_id`, `screen.ui_tree`…) nằm trong dispatcher. `ToolSchemas.methodOf()` dịch giữa hai bộ. Thêm công cụ phải sửa **cả hai**.
- **Chốt an toàn nằm trong mã, không chỉ trong prompt:** màn hình khoá chặn nhóm `screen.*`; WeChat trên điện thoại bị chặn thao tác ghi; két không trả mật khẩu; lượt do thông báo không được mở két/tạo ngữ cảnh; một số hành động không thể hoàn tác phải hỏi người dùng. Tuy vậy, **phần lớn an toàn là dặn mô hình** — xem [SECURITY.md](SECURITY.md#8-những-gì-chưa-được-bảo-vệ).
- Các namespace: `screen.*` (màn hình/cử chỉ), `device.*` (thiết bị, SMS, cuộc gọi, lịch…), `ui.*` (thẻ, trang, hỏi người dùng), `note.*` (sổ tay), `scene.*`, `flow.*`, `peer.*`.

---

## 7. Tầng AI: `LlmClient` + bộ chuyển đổi

`core/.../brain/LlmClient.kt` là một HTTP client đồng bộ tới API chat-completions. Toàn bộ hội thoại phía trên giữ **dạng chuẩn OpenAI**; ở rìa gọi, một trong ba bộ chuyển đổi (`core/.../brain/provider/WireAdapters.kt`) dịch sang định dạng của hãng:

| Định dạng | Bộ chuyển đổi | Đường dẫn |
| --- | --- | --- |
| OpenAI | `OpenAiWire` | `<base>/chat/completions` |
| Gemini | `GeminiWire` | `<base>/models/<model>:generateContent` |
| Anthropic | `AnthropicWire` | `<base>/v1/messages` |

Nhờ vậy `LocalBrain`, bộ cắt ngữ cảnh và archive **không biết** hãng nào đang trả lời — đổi nhà cung cấp chỉ là đổi cấu hình.

- `ProviderEndpoint` (core) là giá trị "một endpoint đã cấu hình": nhà cung cấp, URL, model, khoá, tham số, timeout, retry, fallback.
- `ProviderStore` (app) lưu danh sách endpoint, endpoint đang dùng, endpoint dự phòng và model theo vai trò; khoá được mã hoá qua `SecretBox`.
- `ProviderUi` (app) là thẻ **Trung tâm AI**.
- Retry/fallback nằm trong `LlmClient.complete()`: lỗi tạm thời (429/5xx/mạng) thử lại cùng endpoint; lỗi có thể lách (auth/quota/rate limit/5xx) thử **một lần** trên fallback; lỗi 400 trả về ngay.
- Vai trò hiện được đọc: **Chính**, **Dự phòng**, **Phân tích hình ảnh** (`tap_target`), **Tiết kiệm** (phân loại thông báo). Các vai trò khác đã lưu cấu hình nhưng chưa nối — xem [AI-PROVIDERS.md](AI-PROVIDERS.md#6-model-cho-từng-việc-vai-trò).

---

## 8. Trí nhớ, lịch và ngữ cảnh

- **Sổ tay** (`config/Notebook.kt` + `NotebookBook` trong core): một blob JSON mã hoá AES/GCM trong SharedPreferences; khoá Keystore riêng. Chứa ký ức, việc đang làm, người, nhật ký, wiki, nguyên tắc. Các công cụ `note.*` được đánh dấu `localOnly` và bị lọc khỏi gói gửi Hub.
- **Scheduler** (`config/Scheduler.kt`): báo thức Android (AlarmManager) + receiver sau khởi động lại; nhắc việc thật, không phải "hẹn bằng lời".
- **Ngữ cảnh/kịch bản** (`ScenePresets`, `Flow`): dữ liệu trong sổ tay; kịch bản chạy lại trên máy, không gọi mô hình từng bước.
- **Cắt ngữ cảnh** (`ContextPruner`): khi vượt ngân sách ký tự, kết quả công cụ cũ bị thay bằng một dòng stub, message người dùng cũ bị bỏ chỉ mục; lời nói hai bên và lượt mới nhất không bị chạm.
- **Archive** (`ContextArchive`): trước khi bỏ bất cứ thứ gì, nội dung được ghi xuống `filesDir/context` (trừ ảnh và một số công cụ nhạy cảm); mô hình đọc lại bằng `read_earlier`.
- **Tự xem lại** (harvest/sweep): sau khi cuộc trò chuyện im lặng, một lượt "tự nói với mình" đọc lại, ghi điều đáng nhớ, dọn chủ đề; không phát tiếng, không chạm màn hình.

---

## 9. Bạn bè và "hai body"

- `peer/` trong core: ghép bằng QR, ghim khoá công khai của thiết bị kia, kênh WebSocket nhị phân có mã hoá đầu-cuối (`PeerLink`); kể cả khi transport là `ws://`, nội dung được seal và xác thực bằng khoá đã ghim.
- Hai máy chạy cùng bộ não core; phần khác nhau là "tay" của từng nền tảng.
- Bản desktop không build trên Linux; xem [BUILD.md](BUILD.md#11-build-trên-macos-module-desktop--chỉ-khi-cần).

---

## 10. Quy ước lập trình (dành cho người sửa mã)

- **Không Compose trên Android.** Overlay không host được Compose; toàn bộ UI viết tay bằng `View`.
- **Không Hilt/Dagger.** Chỉ có một service; phụ thuộc được tiêm qua constructor bằng tay.
- **Không coroutine trên điện thoại.** Dùng `Handler` + `Executor` riêng; vòng lặp `LocalBrain` chạy trên một thread riêng.
- **Chữ cho mô hình là tiếng Anh; chữ cho người dùng đi qua `AppLocale`.** Đừng trộn hai loại.
- **Prompt là hằng số**; mọi thứ thay đổi theo lượt đi vào message người dùng.
- **Thêm công cụ = sửa `ToolSchemas` + `CommandDispatcher`**, và quyết định xem cử chỉ đó có cần `passthroughForGesture` không.

---

## 11. Đọc tiếp ở đâu

| Muốn hiểu | Đọc tệp |
| --- | --- |
| Body Android, toàn bộ object graph | `app/src/main/java/net/kuafuai/andee/BodyCoreService.kt` |
| Bàn tay trợ năng | `app/src/main/java/net/kuafuai/andee/ScreenBodyService.kt` |
| Vòng lặp agent | `core/src/main/kotlin/net/kuafuai/andee/brain/LocalBrain.kt` |
| Prompt hệ thống | `core/src/main/kotlin/net/kuafuai/andee/brain/LocalPrompt.kt` |
| HTTP + nhà cung cấp | `core/.../brain/LlmClient.kt`, `core/.../brain/provider/` |
| Dispatcher + công cụ | `app/.../net/CommandDispatcher.kt`, `core/.../net/ToolSchemas.kt` |
| Giao diện overlay | `app/src/main/java/net/kuafuai/andee/ui/` |
| Cắt/archive ngữ cảnh | `core/.../brain/ContextPruner.kt`, `ContextArchive.kt` |
| Sơ đồ một trang | [architecture.svg](architecture.svg) |
