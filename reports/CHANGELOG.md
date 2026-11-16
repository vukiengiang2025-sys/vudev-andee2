# Nhật ký thay đổi — VUDEV ANDEE 2

Những thay đổi của **bản fork VUDEV ANDEE 2** so với upstream **Andee v0.15.0**.

- **Mốc upstream**: `81589d6` — `chore(release): v0.15.0` (12/11/2026).
- **Đầu nhánh `main` của fork**: `4a66230` — `feat: add Vietnamese language support and update LLM defaults` (16/11/2026).
- **Trạng thái tài liệu này**: viết ngày **16/11/2026**, khi Trung tâm AI và các thay đổi bảo mật còn nằm trong **worktree chưa commit**. Mục nào ghi "đang hoàn thiện" thì hãy đối chiếu `git status` / `git log` trước khi coi là đã phát hành.
- **Chưa có bản phát hành VUDEV ANDEE 2.** `versionName` vẫn là `0.15.0`, `versionCode` vẫn là `30` — build từ `main` báo cùng số phiên bản với upstream. Nhận diện build bằng mã commit, không bằng số phiên bản.

Định dạng tham khảo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/). Chính sách phiên bản gốc nằm ở [CHANGELOG.md](../CHANGELOG.md) của upstream.

---

## Bảng tổng hợp

| Hạng mục | Trạng thái | Bằng chứng |
| --- | --- | --- |
| Giao diện tiếng Việt mặc định | ✅ Đã commit | `4a66230`, `app/src/main/res/values-vi/` |
| Đổi mặc định mô hình / chế độ bộ não | ✅ Đã commit | `4a66230`, `VoiceConfig.kt` |
| Build: ký debug + tắt ABI splits + Gradle 9.3.1 | ✅ Đã commit | `4a66230`, `app/build.gradle`, `gradle/wrapper/` |
| Trung tâm AI (Provider Center) | 🚧 Worktree, chưa commit | `provider/`, `ProviderStore.kt`, `ProviderUi.kt` |
| Khoá mã hoá bằng Keystore (SecretBox) | 🚧 Worktree, chưa commit | `security/SecretBox.kt`, `VoiceConfig.kt` |
| Token WebSocket (mặc định bật) | 🚧 Worktree, chưa commit | `BodyWsServer.kt`, `SettingsUi.kt` |
| Cỡ chữ cho giao diện nổi | 🚧 Worktree, chưa commit | `AppLocale.kt`, `BodyCoreService.kt` |
| Bộ tài liệu tiếng Việt | 🚧 Worktree, chưa commit | các tệp trong mục "Tài liệu" |
| Phát hành chính thức | ❌ Chưa có | chưa có tag/bản release của fork |

---

## [Chưa phát hành] — thay đổi của fork

### Đã commit trên `main` — `4a66230` (16/11/2026)

#### Thêm — Tiếng Việt là ngôn ngữ mặc định

- **Bộ chuỗi tiếng Việt đầy đủ**: `app/src/main/res/values-vi/strings.xml` (gần 980 dòng), phủ giao diện quả cầu, thẻ cài đặt, tự kiểm tra, sổ tay, ngữ cảnh, lỗi và thông báo.
- **Khai báo ngôn ngữ với hệ thống**: `res/xml/locales_config.xml` thêm `vi` bên cạnh `zh`/`en`, để Android 13+ hiện Andee trong mục ngôn ngữ ứng dụng.
- **`VoiceConfig.LANG_VI` và mặc định tiếng Việt**: `uiLanguage()` trả về `vi` khi chưa có lựa chọn và không có `BuildConfig.DEFAULT_LANG` — không còn phụ thuộc ngôn ngữ hệ thống.
- **Bộ chọn ngôn ngữ VI / 中 / EN**: thêm ở đầu bảng Cài đặt và ở màn hình chào; đổi ngay, không cần Lưu.
- **Ngữ cảnh có sẵn bằng tiếng Việt**: bốn mẫu (Trực tin nhắn, Rà soát gói đăng ký, Ghi chú cuộc họp, Canh đặt chỗ đúng giờ) có bản dịch đầy đủ; mẫu mới chọn ngôn ngữ tại thời điểm áp dụng.
- **Câu lệnh thoát bằng tiếng Việt**: "Thoát Andee", "Tắt Andee", "Đóng Andee", "Nghỉ ngơi đi"… (khớp cả câu, chỉ khi người dùng chủ động).
- **Giọng đọc hệ thống tiếng Việt**: `SystemTts` chọn `Locale("vi","VN")` khi giao diện là tiếng Việt.
- **Bản địa hoá thời gian tương đối**: "vừa xong", "5 phút trước", "2 giờ trước", "3 ngày trước" (`Gear.kt`).
- **Bản địa hoá các màn hình**: onboarding, thẻ cảnh, lịch, tự kiểm tra, lịch sử, nhập liệu, nút tắt tiếng/đánh thức, v.v.
- **Ảnh/asset mới**: ảnh chụp màn hình bảo mật MIUI dùng cho tài liệu (`images/…jpg`).

#### Thay đổi — Mặc định AI

- **Model mặc định**: `deepseek-flash` → **`deepseek-chat`** cho chế độ Cục bộ / API.
- **Chế độ bộ não mặc định**: nếu **không** có cấu hình CodeFlying thì mặc định là **Cục bộ / API** thay vì CodeFlying; có cấu hình CodeFlying thì vẫn mặc định CodeFlying.
- **CodeFlying vẫn dùng model riêng** `deepseek-flash` (hằng số tách riêng).
- **Đọc to mặc định**: nếu chưa nhập khoá giọng nói nào, chuyển sang **giọng hệ thống** thay vì im lặng không đọc.

#### Thay đổi — Build

- **Ký bản debug bằng `debug.keystore` cố định** ở thư mục gốc (cấu hình `signingConfigs.debugConfig`) để các lần cài đè giữ được dữ liệu.
- **Tắt ABI splits** (`splits.abi.enable = false`): từ nhiều APK theo kiến trúc về **một APK** `app-debug.apk`. Đây là thay đổi có chủ đích cho tiện cài đặt; đổi lại file APK lớn hơn.
- **Gradle wrapper 8.4 → 9.3.1**, bỏ `jvmToolchain(17)` ở các module, bật `org.gradle.parallel` và `org.gradle.caching`, tăng heap lên `-Xmx3072m`.
- **Bỏ mirror Aliyun** khỏi `settings.gradle` / `build.gradle` (dùng Google Maven + Maven Central + JitPack trực tiếp).
- **`BuildConfig.DEFAULT_LANG=vi`**: `uiLanguage()` công nhận giá trị này (đặt trong `local.properties`) là tiếng Việt cho bản build không có lựa chọn nào khác.

> Lưu ý: vì splits bị tắt, các tài liệu cũ (README upstream, CLAUDE.md) vẫn nói "mỗi ABI một APK" là **không còn đúng** với fork này. Xem [docs/BUILD.md](../docs/BUILD.md#đường-dẫn-apk).

#### Kế thừa từ upstream (không phải thay đổi của fork)

- Commit `6255a4f` — **recorded flows** (kịch bản ghi lại, tự chạy lại, tự sửa bước kẹt, chuyển bước cho bộ não) nằm trên upstream `main` **sau** tag v0.15.0 và đã được fork kéo về. Đây là tính năng của upstream, không phải VUDEV thêm; ghi ở đây để không nhầm khi so sánh.

---

### Đang hoàn thiện — worktree, chưa commit (tại 16/11/2026)

Các mục dưới đây **có mã trong worktree** nhưng chưa nằm trong commit nào. Nếu bạn đang đọc sau khi chúng đã được commit, hãy coi trạng thái là "đã xong"; nếu không thấy mã, đừng coi là có.

#### Trung tâm AI (Provider Center)

- **Danh mục 7 nhà cung cấp** (`core/.../brain/provider/AiProvider.kt`): DeepSeek, OpenAI, Google Gemini, Anthropic Claude, OpenRouter, Ollama, OpenAI-compatible; kèm base URL, model mặc định, danh sách model, khả năng (ảnh/suy luận/streaming/local), gợi ý khoá và link lấy khoá.
- **Ba bộ chuyển đổi định dạng** (`WireAdapters.kt`): OpenAI `/chat/completions`, Gemini `/models/<model>:generateContent` (header `x-goog-api-key`), Anthropic `/v1/messages` (`x-api-key` + `anthropic-version`). Dịch tool-call hai chiều, gộp message cùng role, lọc schema cho Gemini.
- **`ProviderEndpoint`**: giá trị endpoint (provider, URL, model, khoá, thinking, effort, temperature, topP, maxTokens, timeout, retry, headers, fallback), che khoá trong `toString()`.
- **`LlmClient` mở rộng**: nhận `providerId`, gọi đúng bộ chuyển đổi, phân loại lỗi (`LlmErrors`), thử lại trong cùng endpoint, và **fallback một lần** sang endpoint dự phòng; lỗi 400 không thử lại/không fallback.
- **`ProviderStore`**: lưu danh sách provider, provider đang dùng, dự phòng, model theo vai trò; di trú `llm_api_key` cũ; kiểm tra kết nối (ping 16 token, timeout 40 s, không retry).
- **`ProviderUi`**: thẻ Trung tâm AI (thêm/sửa/xoá/bật-tắt/chọn đang dùng/đặt dự phòng; form khoá không in lại khoá đã lưu; nút kiểm tra kết nối; chọn model cho từng việc).
- **Nối vào `SettingsUi`**: tab **Cục bộ / API** hiển thị provider đang dùng thay cho ba ô thô cũ, có nút mở Trung tâm AI.
- **Nối vào bộ não**: `VoiceConfig.brainConfig()` đọc provider đang dùng và fallback; **vai trò Phân tích hình ảnh** dùng cho `tap_target`, **vai trò Tiết kiệm** dùng cho phân loại thông báo.
- **Trạng thái vai trò**: Chính / Dự phòng / Phân tích hình ảnh / Tiết kiệm đã nối; Lập trình / Hội thoại / Tác vụ dài mới lưu cấu hình, chưa dùng ở mọi lời gọi.
- **Test**: `core/src/test/.../provider/WireAdaptersTest.kt` (endpoint, dịch Gemini/Anthropic, registry, fallback, 400 không retry) và `ProviderSmokeTest.kt` (máy chủ OpenAI-shaped cục bộ).

#### Bảo mật khoá

- **`security/SecretBox.kt`**: AES/GCM, khoá trong Android Keystore (`andee_secret_box_v1`), định dạng `v1:base64(iv+ciphertext)`; giải mã thất bại trả `null` thay vì crash.
- **Khoá provider** được seal trong `SharedPreferences andee_ai`; giao diện chỉ hiện gợi ý, không in lại khoá; có nút xoá khoá.
- **Khoá giọng nói / CodeFlying / token WebSocket** được seal trong `voice_prefs` (`*_sealed`), bản văn bản thường bị xoá; có di trú từ giá trị cũ.
- **Token WebSocket**: sinh ngẫu nhiên 18 byte, lưu seal, hiện/copy/tạo mới trong Cài đặt → Nâng cao → Bảo mật kết nối.
- **Chưa thấy**: khoá vẫn nằm trong bộ nhớ khi chạy; két vẫn không ràng buộc mở khoá máy — xem [docs/SECURITY.md](../docs/SECURITY.md).

#### WebSocket có xác thực

- **`BodyWsServer`** nhận `authToken`; kết nối loopback (`adb forward`) được miễn; client LAN phải gửi frame `auth`; sai bị đóng mã 4001.
- **Mặc định bật** (`ws_auth = on`), tắt được trong Cài đặt với cảnh báo; token đổi mới ngắt mọi client cũ.
- **Các lệnh gỡ lỗi** (`debug.say`, `debug.sweep`, `debug.worklog`) chỉ chấp nhận loopback.
- Lưu ý: loopback miễn token cũng cho ứng dụng khác trên máy kết nối `127.0.0.1:9008` — ghi trong [docs/SECURITY.md](../docs/SECURITY.md).

#### Cỡ chữ

- **Thêm lựa chọn Cỡ chữ** (Nhỏ / Chuẩn / Lớn / Rất lớn = 0.9 / 1.0 / 1.15 / 1.3) trong Cài đặt → Giao diện.
- **`AppLocale.withTextScale`** + `attachBaseContext` ở hai service: áp dụng trước khi view được tạo; đổi có hiệu lực lần khởi động dịch vụ tiếp theo.

#### Tài liệu (đợt này)

Đang tạo, chưa commit:

- `README.vi.md` — trang đầu tiếng Việt, liên kết bản tiếng Anh, giấy phép, tính năng, đường dẫn tài liệu.
- `docs/INSTALL.md` — cài APK trên điện thoại thật, HyperOS/Redmi, adb.
- `docs/BUILD.md` — build từ Linux Mint, JDK/SDK, debug.keystore, lỗi Gradle.
- `docs/AI-PROVIDERS.md` — Trung tâm AI, 7 nhà cung cấp, khoá, dự phòng, vai trò.
- `docs/PERSONALIZATION.md` — ngôn ngữ, giao diện, nhân vật, ngữ cảnh, nhân cách, sổ tay, cỡ chữ.
- `docs/SECURITY.md` — khoá, quyền, WebSocket, kill switch, giới hạn.
- `docs/ARCHITECTURE.md` — ba module, hai nửa body, vòng lặp agent, tầng AI.
- `docs/TROUBLESHOOTING.md` — không thấy quả cầu, trợ năng, lỗi khoá, mạng, tiếng Việt, chữ ký APK.
- `reports/CHANGELOG.md` — tệp này.

Các tệp tài liệu trong đợt này chỉ **thêm mới**; không sửa mã nguồn, `strings.xml` hay file Gradle. `README.md` có thay đổi khác trong worktree (bởi công việc khác, không phải đợt tài liệu này).

---

## Chưa làm / giới hạn đã biết

- **Vai trò CODE / CHAT / LONG** đã có UI và lưu cấu hình nhưng chưa được bộ não đọc.
- **Chưa có bản phát hành chính thức** của VUDEV ANDEE 2; chưa đổi `versionName`/`versionCode`.
- **Release chưa có signingConfig**: `assembleRelease` tạo APK **chưa ký**, phải ký thủ công trước khi cài.
- **CI và debug.keystore**: keystore debug được commit trong kho để chữ ký APK ổn định giữa các lần chạy Actions và bản build cục bộ; workflow chỉ tạo dự phòng khi tệp vắng mặt (xem docs/BUILD.md mục 9).
- **ABI splits đang tắt**: chỉ có một APK; script `build-and-install.sh` vẫn tìm tên APK theo ABI (có fallback) — cài thủ công `app-debug.apk` để tránh nhầm.
- **Hub từ xa** vẫn là `ws://` không mã hoá/không xác thực (giới hạn của upstream, chưa sửa trong fork).
- **WebSocket token có thể tắt** và **loopback được miễn** — bảo vệ một phần, không tuyệt đối.
- **Két** chưa ràng buộc sinh trắc học/mở khoá máy.
- **Prompt injection** vẫn chưa có sandbox; phần lớn guard là chỉ dẫn cho mô hình.
- **Chưa có công cụ xuất/nhập danh sách nhà cung cấp**; khoá không xuất được (đúng thiết kế), phải nhập lại khi đổi máy.
- **Desktop (macOS)** chưa được cập nhật cho các thay đổi provider/SecretBox; fork hiện tập trung Android.
- **Chưa có bản dịch tiếng Việt cho** `SECURITY.md` gốc, `DISCLAIMER.md`, `docs/acceptable-use.md`, `docs/permissions.md` — tài liệu tiếng Việt trong `docs/` tham chiếu bản tiếng Anh.

---

## Cách kiểm chứng các thay đổi

```bash
# 1. Commit tiếng Việt + mặc định AI + build
git show --stat 4a66230

# 2. Các thay đổi đang chờ (Trung tâm AI, khoá, token, cỡ chữ)
git status --short

# 3. Danh mục nhà cung cấp
sed -n '1,230p' core/src/main/kotlin/net/kuafuai/andee/brain/provider/AiProvider.kt

# 4. Ba định dạng API + phân loại lỗi/fallback
sed -n '1,120p' core/src/main/kotlin/net/kuafuai/andee/brain/provider/WireAdapters.kt

# 5. Lưu khoá + di trú + test kết nối
sed -n '1,260p' app/src/main/java/net/kuafuai/andee/config/ProviderStore.kt

# 6. Mã hoá khoá
sed -n '1,120p' app/src/main/java/net/kuafuai/andee/security/SecretBox.kt

# 7. Token WebSocket
grep -n 'authToken\|auth' app/src/main/java/net/kuafuai/andee/net/BodyWsServer.kt | head

# 8. Test
./gradlew :core:test --tests '*WireAdapters*' --tests '*ProviderSmoke*' --max-workers=2
```

Kiểm tra bản build đang cài có các thay đổi này: mở ⚙ → **Hệ thống AI → Cục bộ / API** — nếu thấy nút **Quản lý nhà cung cấp và model** thì bản đó có Trung tâm AI.

---

## Ghi công và giấy phép

- **Upstream**: Andee — Copyright 2026 **KUAFUAI**, https://github.com/kuafuai/andee, giấy phép **Apache-2.0 kèm điều kiện bổ sung** ([LICENSE](../LICENSE)).
- **Fork VUDEV ANDEE 2**: duy trì bởi Vũ Huỳnh (`vukiengiang2025-sys`).
- Bản fork **giữ nguyên** tên sản phẩm, icon và thông báo bản quyền của Andee theo điều kiện A.1(a) của giấy phép. Không có ý định white-label.
- Mọi tệp mới trong đợt này đều nằm trong cùng giấy phép; khi phân phối phải kèm [LICENSE](../LICENSE) và ghi rõ các tệp đã thay đổi (điều kiện A.1(c)).
