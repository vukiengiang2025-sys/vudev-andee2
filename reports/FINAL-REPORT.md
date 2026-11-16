# Báo cáo bàn giao — VUDEV ANDEE 2

Ngày: 2026-11-16 (Asia/Ho_Chi_Minh). Báo cáo này là bản tóm tắt; bằng chứng chi tiết nằm trong reports/environment-audit.md, reports/architecture-audit.md, reports/license-audit.md, reports/TEST-REPORT.md, reports/BUILD-REPORT.md, reports/CHANGELOG.md.

## 1. Repository fork

- Fork: https://github.com/vukiengiang2025-sys/vudev-andee2
- Upstream: https://github.com/kuafuai/andee (remote upstream được giữ)
- Nhánh: main; commit đầu của fork: 4a66230; các commit VUDEV: 69ed0a4, 1dd4b7a, b300d8d, d872a69, 94835e1, bd8bc23, 6068f93, dbddf13 (và commit tài liệu cuối).
- Bản phát hành có APK tải trực tiếp: https://github.com/vukiengiang2025-sys/vudev-andee2/releases/tag/v0.15.0-vudev2.1

## 2. Đường dẫn mã nguồn

/home/vukgg04/vscode/vudev-andee2 (origin = fork, upstream = kuafuai/andee).

## 3. Công cụ đã cài trong phiên

- GitHub CLI 2.104.0 tại ~/.local/bin/gh (tải release chính thức; sudo cần mật khẩu nên không cài apt).
- JDK 17 Temurin do Gradle tự tải vào ~/.gradle/jdks (máy chỉ có JDK 21 hệ thống, không dùng được với wrapper/AGP này).
- Android platform 34 (AGP tự tải) bổ sung cho SDK sẵn có ở ~/Android/Sdk.
- debug.keystore tạo cục bộ rồi commit (khoá debug, không phải khoá release).

## 4. Công cụ đã tái sử dụng

- git, adb, Android SDK (build-tools 35/36, cmdline-tools, platform-tools), Gradle wrapper 9.3.1, Kotlin 2.1.21, OkHttp/org.json sẵn có, gh auth đã đăng nhập sẵn (vukiengiang2025-sys).
- Không cài Android Studio, không cài emulator.

## 5. Chức năng đã phát triển

- Tiếng Việt là ngôn ngữ mặc định; đủ khoá cho zh/en/vi (1017/1017/1017), gồm toàn bộ chuỗi mới của Trung tâm AI, nhân cách, cỡ chữ, bảo mật.
- AI Provider Center: thêm/sửa/xoá/bật-tắt nhà cung cấp, nhập key/Base URL/model, kiểm tra kết nối (chạy nền, báo mã lỗi dễ hiểu), chọn nhà cung cấp đang dùng, đặt dự phòng, chọn model theo từng việc (chính, dự phòng, lập trình, hội thoại, hình ảnh, tác vụ dài, tiết kiệm).
- Tầng LLM đa nhà cung cấp thật: ba định dạng dây (OpenAI-compatible, Gemini generateContent, Anthropic messages), chuyển đổi tool-call hai chiều, retry có giới hạn, fallback một lần, không retry lỗi 400.
- Bảo mật khoá: SecretBox AES/GCM với khoá Android Keystore; provider key và key thoại được mã hoá, bản plaintext cũ được migrate rồi xoá; key không vào log/URL.
- Xác thực WebSocket 9008 bằng token (constant-time, mặc định bật, loopback/adb forward miễn, tạo lại từ Cài đặt).
- Tạm dừng trợ lý một chạm (chặn turn mới, hiện thông báo không đọc to).
- Nhân cách trợ lý: tên, cách xưng hô, phong cách, yêu cầu riêng (nối vào prompt nhân vật).
- Cỡ chữ giao diện (áp dụng khi service khởi động lại).
- Role VISION nối vào bộ định vị màn hình, CHEAP nối vào phân loại thông báo.
- Workflow GitHub Actions build APK (debug + release unsigned), debug.keystore dùng chung để chữ ký ổn định.
- Tài liệu tiếng Việt đầy đủ, 152 ý tưởng phát triển, báo cáo kiểm toán môi trường/kiến trúc/giấy phép, báo cáo test/build.

## 6. Provider đã tích hợp thực sự

Đã tích hợp ở mức mã nguồn + kiểm thử bằng mock server: DeepSeek (OpenAI wire), OpenAI, OpenRouter, Ollama, endpoint OpenAI-compatible tự nhập (cùng wire OpenAI); Google Gemini (generateContent: systemInstruction, contents/parts, inlineData, functionDeclarations, functionResponse); Anthropic Claude (messages: system cấp cao nhất, tool_use/tool_result, thinking khi không có tools).
Chưa gọi API thật của từng hãng vì môi trường không có key người dùng; do đó "đã tích hợp" nghĩa là request/response đúng định dạng và có test, không phải đã xác thực tài khoản thật.

## 7. Lỗi đã phát hiện và sửa

- gradlew/build-and-install.sh bị commit mất bit thực thi (từ phía Windows) → chmod +x và commit mode.
- debug.keystore bị app/build.gradle yêu cầu nhưng không có trong repo và bị gitignore → tạo + commit khoá debug để build cục bộ và CI dùng cùng chữ ký.
- Không thể build với JDK 21; wrapper/AGP cần 17–20 → dùng JDK 17 Gradle tự tải; ghi lại cấu hình RAM thấp.
- VoiceConfig có lỗi cú pháp do chuỗi nhiều dòng khi thêm persona → viết lại bằng escape đúng; build lại xanh.
- SettingsUi: scope của khối provider làm biến applyThinking ra ngoài phạm vi → sửa cấu trúc if/else; dùng nhầm helper common() → thay bằng lctx.getString.
- Gemini/Anthropic từng bị gửi kèm header Authorization kiểu OpenAI → tách header theo từng wire, Gemini dùng x-goog-api-key.
- Workflow CI đầu tiên chết vì android-actions/setup-android@v3 (gói tools không còn) → dùng SDK có sẵn của runner.
- 3 test golden của app đỏ vì snapshot chưa cập nhật sau upstream 6255a4f → tái tạo snapshot, xem diff chỉ gồm phần recorded flows.
- 16 test core đỏ có sẵn từ upstream: đã xác minh bằng stash rồi chạy lại trên mã gốc; không che, không xoá (xem báo cáo test).

## 8. Kết quả kiểm thử

- :app:assembleDebug: THÀNH CÔNG (APK app-debug.apk, 28.605.631 byte).
- :app:testDebugUnitTest: 90 test, 0 lỗi (sau khi cập nhật golden).
- :core:test: 336 test, 16 lỗi có sẵn từ upstream (không do đợt sửa này); 11 test mới của tầng nhà cung cấp đều đạt.
- GitHub Actions: workflow build-apk xanh (4m41s) trên main và tag v0.15.0-vudev2.1; workflow build của upstream xanh cả debug-apk và release-gate sau khi sửa golden.
- Chưa kiểm thử trên máy thật: điện thoại Redmi 15 5G mất kết nối adb giữa phiên; đã chuẩn bị đường cài từ xa (release + artifact).

## 9. Kết quả build APK

- Cục bộ: app/build/outputs/apk/debug/app-debug.apk, SHA-256 46e45533bd291dde3c800c06c7f22c2306d4c244577b1f1453aaefe21ff95d8c.
- CI: artifact app-debug (26.763.412 byte nén) và app-release-unsigned cho tag.
- Chữ ký: debug (alias androiddebugkey), ổn định nhờ debug.keystore trong kho.

## 10. Đường dẫn APK

- Cục bộ: /home/vukgg04/vscode/vudev-andee2/app/build/outputs/apk/debug/app-debug.apk
- Tải trực tiếp: https://github.com/vukiengiang2025-sys/vudev-andee2/releases/tag/v0.15.0-vudev2.1 (tệp app-debug.apk)
- Artifact CI: tab Actions, workflow build-apk, artifact app-debug.

## 11. Tính năng chưa hoàn thành

- Chưa kiểm thử trên thiết bị thật (bóng nổi, Accessibility, micro, lưu/khôi phục, chạy nền sau khởi động lại).
- Chưa gọi API thật của 7 nhà cung cấp; chưa có đo chi phí token theo thời gian thực trong UI.
- Role CODE/CHAT/LONG đã lưu và chọn được nhưng chưa nối vào nơi dùng cụ thể (mới nối VISION và CHEAP).
- Hub/cloud vẫn dùng ws:// plaintext; chưa có wss/TLS.
- Chưa có màn hình Memory Center xuất/nhập riêng (notebook đã có wipe/export sẵn nhưng chưa gom vào một màn hình mới).
- Chưa có xác nhận chung cho mọi hành động không thể đảo ngược (hiện dựa vào guard + xác nhận cục bộ của từng tool).
- 16 test core đỏ có sẵn chưa sửa.

## 12. Vấn đề giấy phép còn tồn tại

- Giấy phép là Apache-2.0 + điều kiện KUAFUAI: không white-label (đã tuân thủ, app vẫn tên/icon Andee), phải kèm LICENSE/NOTICE và ghi rõ tệp sửa đổi.
- **Chưa được phép phát hành thương mại hoặc nhúng vào sản phẩm bán ra** nếu chưa có văn bản đồng ý của KUAFUAI (A.1.d); fork source + APK dùng cá nhân/chia sẻ thử nghiệm là phù hợp.
- Không được dùng tên/logo Andee để quảng bá fork như bản chính thức (A.3).
- 93 thành phần bên thứ ba, gồm ML Kit Terms of Service (6) và Android SDK License (4) — không phải open source; cần xem lại nếu phân phối rộng.
- Không phải tư vấn pháp lý; nếu thương mại hoá, liên hệ KUAFUAI trước.

## 13. Hướng phát triển tiếp theo

1. Cài APK lên Redmi 15 5G, kiểm thử luồng thật (onboarding, provider center, khôi phục sau reboot), lấy logcat.
2. Sửa 16 test core đỏ (bắt đầu từ LocalBrain + HTTP server giả lập trong test).
3. Nối nốt role CODE/CHAT/LONG; thêm đo chi phí/token từ usage của từng provider.
4. Thêm màn hình Memory Center và lớp xác nhận chung cho hành động không thể đảo ngược.
5. wss/TLS cho hub; khoá hub theo token tương tự WebSocket 9000.
6. Nâng AGP/Kotlin cùng lúc trước Gradle 10; bật ABI split cho release để giảm dung lượng.
7. Chọn và triển khai các ý tưởng ưu tiên trong docs/IDEAS-120.md (lộ trình 3 giai đoạn).
