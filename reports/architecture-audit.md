# Kiểm toán kiến trúc — VUDEV ANDEE 2 (fork của Andee)

Ngày: 2026-11-16. Nguồn: đọc trực tiếp mã nguồn tại ~/vscode/vudev-andee2 (commit 4a66230 + thay đổi VUDEV) và CLAUDE.md của dự án.

## 1. Tổng quan

Andee không phải "ứng dụng có màn hình chính". Nó là một **cơ thể Android** do một bộ não điều khiển:

- **core/** — JVM thuần, dùng chung cho điện thoại và Mac: agent loop (LocalBrain), system prompt (LocalPrompt), schema công cụ (ToolSchemas), hàng đợi/lịch sử, notebook, scene, peer. Không có kiểu dữ liệu Android.
- **app/** — cơ thể Android. Không có Activity làm màn hình chính; mọi thứ người dùng thấy là cửa sổ WindowManager (bóng nổi + các thẻ kính). Điểm vào là BodyCoreService (foreground service) và ScreenBodyService (AccessibilityService).
- **desktop/** — cơ thể macOS (Compose Desktop + Swift bridge), dùng chung core.
- 351 tệp Kotlin, ~101.600 dòng.

## 2. Luồng hoạt động chính

1. ScreenBodyService.onServiceConnected() lắp object graph: FloatingWindowUi -> AudioIO -> Tts/Asr -> ScreenController -> CommandDispatcher, rồi mở 2 đường mạng.
2. Hai đường mạng, một dispatcher:
   - BodyWsServer: lắng nghe 0.0.0.0:9008 cho hub/debug trên LAN hoặc adb forward.
   - BodyWsClient: nối ra hub (ws://...), gửi register kèm tool schema, nhận request.
3. Bộ não:
   - Chế độ local (mặc định): LocalBrain gọi LLM qua HTTP, tự chạy vòng tool-call, gọi CommandDispatcher.
   - Chế độ hub: não ở nơi khác, thiết bị chỉ thi hành.
   - CodeFlying: não chạy tại chỗ nhưng endpoint/key lấy từ BuildConfig.
4. Công cụ: ToolSchemas (tên cho LLM) <-> CommandDispatcher (method nội bộ) qua ToolSchemas.methodOf(). Thêm tool phải sửa cả hai.
5. Bộ nhớ: Notebook (một blob JSON mã hoá AES/GCM trong SharedPreferences, khoá Android Keystore), ChatHistory + ContextArchive (append-only, prune + harvest + topic fold), Scenes, Scheduler (AlarmManager), GearScan (ứng dụng đã cài).
6. Giao diện: toàn bộ View viết tay, không Jetpack Compose (overlay không host Compose được). Chủ đề sáng/tối, nhiều bảng màu.

## 3. Thành phần tái sử dụng được (không cần viết lại)

| Thành phần | Vì sao giữ |
|---|---|
| LocalBrain + TurnGuards + Locator | Vòng agent đã xử lý giới hạn bước, gọi trùng, timeout, ảnh, ngắt giữa chừng. |
| ToolSchemas/CommandDispatcher | Giao thức công cụ ổn định; thêm tool theo đúng 2 chỗ. |
| LlmClient (sau khi mở rộng) | Điểm duy nhất nói HTTP; nay có adapter OpenAI/Gemini/Anthropic + retry/fallback. |
| Notebook/ChatHistory/ContextPruner | Đã có mã hoá, export, prune, archive; không cần động vào. |
| Glass/OverlayA11y/StandAside | Bộ dựng thẻ và các luật an toàn overlay (ẩn khỏi accessibility, nhường chỗ IME). |
| ScreenController + A11yIme | Điều khiển màn hình đã có nhiều guard đo trên máy thật (toạ độ 0-1000, zoom-confirm, chống lặp). |
| Peers/QR | Kênh bạn bè đã hoàn chỉnh. |

## 4. Thành phần cần mở rộng (đã làm trong đợt này)

| Thành phần | Thay đổi |
|---|---|
| Bộ não LLM | Thêm gói core/brain/provider: AiProvider (7 nhà cung cấp), WireAdapters (OpenAI/Gemini/Anthropic), ProviderEndpoint, ModelRole. LlmClient nhận providerId/tham số sinh + retry + fallback. |
| Cấu hình | ProviderStore (danh sách nhà cung cấp, mã hoá key bằng Keystore), brainConfig đọc store trước legacy; migration từ llm_api_key. |
| Giao diện | ProviderUi (thẻ Trung tâm AI: thêm/sửa/xoá/bật-tắt/test/dự phòng/role), lối vào từ Settings; mục nhân cách + cỡ chữ + bảo mật kết nối. |
| Bảo mật | SecretBox (AES/GCM Keystore) cho key nhà cung cấp và key thoại; token xác thực BodyWsServer (loopback miễn); WS auth bật mặc định. |
| Đa ngôn ngữ | vi/en/zh đủ khoá cho các màn hình mới; tiếng Việt là mặc định. |

## 5. Rủi ro kỹ thuật và điểm nghẽn

1. **Phiên bản build lệch**: wrapper Gradle 9.3.1 đi cùng AGP 8.3.1 — cấu hình chạy được nhưng Gradle cảnh báo không tương thích Gradle 10; nên nâng AGP/Kotlin cùng lúc trong một nhánh riêng.
2. **compileSdk 34 trong khi SDK cài sẵn 36**: AGP tự tải platform 34; CI cũng cài. Không phải lỗi nhưng cần mạng.
3. **RAM thấp**: 4 GB, Gradle + Kotlin daemon dễ thrash. Đã cố định heap 1.0–1.6 GB, 2 worker, tắt parallel.
4. **Không có bộ test thật ở app/**: 324 unit test nằm ở core; **16 test đã đỏ ngay tại HEAD** (WorkStepTest, WorkTurnTest, QuietTurnTest, TaskTurnTest...). Đã xác nhận bằng cách stash thay đổi và chạy lại: lỗi có sẵn, không do đợt sửa này. Không được che bằng cách xoá test — ghi nhận và để nguyên.
5. **Chi phí token prompt**: LocalPrompt rất dài (~50k ký tự) + 55 mô tả tool; đã có prune/harvest/archive để bù. Mọi thay đổi thêm vào system prompt nên đi qua persona/guard (được fence) thay vì sửa TEXT.
6. **Giao diện overlay**: mọi card phải gọi hideFromAccessibility() và OwnCard.shown/hidden; quên là bộ não đọc nhầm UI của chính nó. ProviderUi đã theo đúng mẫu.
7. **Hai đường mạng dùng chung CommandDispatcher**: bất kỳ thay đổi dispatcher ảnh hưởng cả hub lẫn local; đã không đổi contract, chỉ thêm nhánh vision role ở screen.tap_target.
8. **Phụ thuộc native/ML Kit**: APK theo ABI, arm64 ~8 MB; ML Kit và play-services stubs có điều khoản riêng.

## 6. Lỗ hổng bảo mật đã biết (và xử lý)

| Vấn đề | Trước | Sau |
|---|---|---|
| API key lưu plaintext trong SharedPreferences | llm_api_key, api_key, google_api_key, codeflying_key | Bọc SecretBox (AES/GCM, khoá Keystore); legacy được migrate và xoá bản rõ. |
| WebSocket 9008 không xác thực | Mọi máy trong Wi-Fi điều khiển được | Token bắt buộc (mặc định), so sánh constant-time, loopback (adb forward) miễn; có nút tạo lại token. |
| Key trong log | Đã chỉ log "set/MISSING" | Giữ nguyên; key không đi vào URL (Gemini dùng header x-goog-api-key). |
| Hub dùng ws:// plaintext | Vẫn còn | Ghi nhận: trên LAN, nên dùng VPN/WireGuard hoặc TLS; chưa triển khai wss. |
| Prompt injection | Có fence trong prompt + guard của người dùng | Giữ nguyên; nội dung từ màn hình/noti vẫn là dữ liệu. |
| Hành động không thể đảo ngược | Guard + xác nhận trong một số tool | Ghi nhận là chưa toàn diện; cần thêm lớp xác nhận chung (roadmap). |

## 7. Kiến trúc mở rộng về lâu dài

- Provider mới: thêm một AiProvider + (nếu khác wire) một adapter; không sửa LocalBrain.
- Tool mới: ToolSchemas + CommandDispatcher + (nếu cần) quyền trong manifest.
- Ngôn ngữ mới: thêm values-xx + locales_config; không sửa logic.
- Màn hình mới: theo mẫu ProviderUi/PeersUi (Glass + OverlayA11y + StandAside).
- Tránh đưa Compose/Hilt/coroutines vào overlay: phá vỡ kiến trúc hiện tại và làm nặng APK.
