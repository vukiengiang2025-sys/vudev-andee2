# Báo cáo kiểm thử — VUDEV ANDEE 2

Ngày: 2026-11-16. Máy: Linux Mint 22.3, 4 nhân, RAM 3,7 GB, JDK 17 (Temurin), Android SDK 34, Gradle wrapper 9.3.1.
Mã kiểm thử: các commit 69ed0a4, 1dd4b7a, b300d8d, d872a69.

## 1. Tóm tắt

| Hạng mục | Kết quả |
|---|---|
| Build APK debug | Thành công — app/build/outputs/apk/debug/app-debug.apk, 28.605.631 byte |
| Biên dịch unit test core | Thành công |
| Test mới tầng nhà cung cấp (WireAdaptersTest) | 11/11 đạt |
| DeepSeekLlmClientTest + LlmClientMovedTest | Đạt |
| Toàn bộ test core | 336 test, 16 lỗi — đều là lỗi có sẵn ở upstream |
| Cài trên thiết bị thật | Không thực hiện được: điện thoại mất kết nối adb giữa phiên |
| Quét bí mật trong source/docs/workflow | Không phát hiện key nào |
| Đồng bộ chuỗi zh/en/vi | 1017 / 1017 / 1017 khoá |

## 2. Lệnh đã chạy

Build APK:

    export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :app:assembleDebug --console=plain --no-daemon -PdefaultLang=vi
      -Dorg.gradle.jvmargs="-Xmx1600m -XX:MaxMetaspaceSize=640m"
      -Dorg.gradle.parallel=false --max-workers=2
      -Dkotlin.daemon.jvmargs="-Xmx1000m"

Test tầng nhà cung cấp:

    ./gradlew :core:test --tests 'net.kuafuai.andee.brain.provider.WireAdaptersTest'
      --tests 'net.kuafuai.andee.brain.DeepSeekLlmClientTest'
      --tests 'net.kuafuai.andee.brain.LlmClientMovedTest'

Toàn bộ test core:

    ./gradlew :core:test

## 3. Kết quả chi tiết

### 3.1 Test mới đều đạt

WireAdaptersTest có 11 test, 0 lỗi:
- endpoint OpenAI nhận cả base URL và URL đầy đủ;
- endpoint Gemini đúng dạng /models/<model>:generateContent;
- endpoint Anthropic thêm /v1/messages;
- chuyển system + lịch sử tool-call sang contents của Gemini, tool result mang đúng TÊN hàm;
- parse phản hồi Gemini (text + functionCall) về dạng chuẩn;
- Anthropic: system ở cấp cao nhất, tool_result nằm trong user message, tool_use khi parse;
- registry nhận diện nhà cung cấp theo URL cũ; Ollama không cần key;
- ProviderEndpoint dùng default của nhà cung cấp khi ô trống;
- fallback: lỗi 5xx ở primary thì trả lời từ nhà cung cấp dự phòng;
- không retry và không fallback với lỗi 400.

ProviderSmokeTest gọi end-to-end qua HTTP server cục bộ giả lập /chat/completions.
DeepSeekLlmClientTest và LlmClientMovedTest vẫn đạt, tức hành vi DeepSeek/reasoning_content/redirect không đổi.

### 3.2 Toàn bộ core test

Kết quả: 336 tests completed, 16 failed.

16 test đỏ: AimHintsTest (2), ContextCompactionTest (4), ContinueAfterStepLimitTest (1), QuietTurnTest (1), SelfWorkTest (1), SourceTurnTest (1), StopAllTest (1), TaskTurnTest (1), TodoGuardsTest (1), WorkStepTest (1), WorkTurnTest (1), SelfhoodTest (1).

Đây là lỗi có sẵn, không do thay đổi của VUDEV. Bằng chứng: đã dùng git stash tạm cất toàn bộ thay đổi, chạy lại WorkStepTest, WorkTurnTest, QuietTurnTest trên đúng mã đầu nhánh 4a66230 — cả 3 vẫn đỏ (4 test, 3 lỗi). Khôi phục thay đổi rồi chạy lại: đúng 16 test đó đỏ. Không test nào bị xoá hay bị nới assertion.

Nguyên nhân gốc chưa được sửa vì nằm ngoài phạm vi đợt này: nhóm test này chạy LocalBrain với HTTP server giả lập; kết quả cho thấy vòng lặp không nhận được phản hồi mong đợi. Tầng nhà cung cấp mới đã được kiểm chứng độc lập và không tham gia vào các test đó.

### 3.3 Thiết bị thật

Đầu phiên adb thấy thiết bị 25057RN09G (Redmi 15 5G), Android 16, arm64-v8a. Cuối phiên thiết bị mất kết nối USB, nên không cài và chạy APK trên máy được. Phương án thay thế: đẩy mã lên GitHub và tải APK từ artifact của workflow build-apk (chữ ký ổn định nhờ debug.keystore đã commit).

Chưa kiểm chứng trên máy thật: bóng nổi, quyền Accessibility, micro, giao diện Trung tâm AI, lưu/khôi phục cài đặt, chạy nền sau khởi động lại.

## 4. Kiểm tra tĩnh

| Hạng mục | Kết quả |
|---|---|
| Quét bí mật (sk-, AIza, ghp_, xox) | Không thấy |
| Key trong log | Chỉ log trạng thái set/MISSING |
| Key trong URL | Gemini dùng header x-goog-api-key |
| Lưu key | SecretBox AES/GCM + Android Keystore; legacy plaintext được migrate và xoá |
| WebSocket 9008 | Token bắt buộc cho client không-loopback, so sánh constant-time |
| API key trong git | Không có; debug.keystore là khoá debug, không phải khoá release |
| Chuỗi đa ngôn ngữ | zh 1017, en 1017, vi 1017 |
| Cảnh báo biên dịch | 38 cảnh báo, đều là API deprecated có sẵn, không có lỗi |

## 5. Hạn chế

1. Không gọi API thật của các nhà cung cấp vì không có key người dùng trong môi trường; adapter được kiểm bằng mock server.
2. Không chạy instrumented test vì không có thiết bị kết nối.
3. 16 test core đỏ có sẵn cần một đợt sửa riêng.
4. Chưa kiểm thử hub LAN; kết nối hub vẫn là ws:// plaintext.
