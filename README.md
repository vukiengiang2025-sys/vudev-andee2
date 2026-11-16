# VUDEV ANDEE 2 — Trợ lý AI cá nhân tiếng Việt

**Đây là bản fork không chính thức của [kuafuai/andee](https://github.com/kuafuai/andee) (KUAFUAI).** Tên, biểu tượng và thông báo bản quyền của Andee được giữ nguyên theo giấy phép của dự án; VUDEV ANDEE 2 chỉ là tên của bản phát triển này, không phải bản phát hành chính thức của Andee.

Andee sống trong điện thoại như một "cơ thể": bóng nổi trên màn hình, dùng AccessibilityService để nhìn và thao tác ứng dụng, nghe bằng micro, nói bằng TTS, ghi nhớ bằng notebook riêng. VUDEV ANDEE 2 phát triển từ nền đó, ưu tiên tiếng Việt và khả năng chọn nhà cung cấp AI.

- Bản tiếng Việt đầy đủ: [README.vi.md](README.vi.md)
- English README: [README.en.md](README.en.md)
- Hướng dẫn cài: [docs/INSTALL.md](docs/INSTALL.md) · Build: [docs/BUILD.md](docs/BUILD.md)
- Nhà cung cấp AI: [docs/AI-PROVIDERS.md](docs/AI-PROVIDERS.md) · Cá nhân hoá: [docs/PERSONALIZATION.md](docs/PERSONALIZATION.md)
- Bảo mật: [docs/SECURITY.md](docs/SECURITY.md) · Kiến trúc: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- 152 ý tưởng phát triển: [docs/IDEAS-120.md](docs/IDEAS-120.md)

## Có gì trong bản này

- **Tiếng Việt là mặc định**: toàn bộ chuỗi giao diện có bản dịch tiếng Việt (cùng tiếng Anh và tiếng Trung), ngày giờ theo locale, font và dấu tiếng Việt, múi giờ Asia/Ho_Chi_Minh.
- **Trung tâm AI (AI Provider Center)**: OpenAI, DeepSeek, Google Gemini, Anthropic Claude, OpenRouter, Ollama và endpoint OpenAI-compatible tự nhập. Mỗi nhà cung cấp có key, Base URL, model riêng; có kiểm tra kết nối, đặt nhà cung cấp dự phòng và chọn model theo từng việc (chính, dự phòng, hình ảnh, tiết kiệm chi phí...).
- **Ba định dạng API thật**: OpenAI-compatible, Gemini generateContent, Anthropic messages — không giả định mọi API giống OpenAI.
- **Bảo vệ API key**: key được mã hoá AES/GCM bằng khoá trong Android Keystore; key cũ dạng plaintext được migrate rồi xoá. Key không vào log, không vào URL.
- **Cổng kết nối có xác thực**: WebSocket 9008 (điều khiển thiết bị từ máy tính) yêu cầu token, so sánh constant-time; kết nối qua cáp/adb forward được miễn.
- **Tạm dừng trợ lý một chạm**, nhân cách trợ lý (tên, cách xưng hô, phong cách, yêu cầu riêng), cỡ chữ, giao diện sáng/tối và các bảng màu.
- **GitHub Actions build APK** để cài từ xa khi điện thoại không nối máy tính: [.github/workflows/build-apk.yml](.github/workflows/build-apk.yml).

## Yêu cầu

- Android 11+ (API 30) cho phần nhìn màn hình; minSdk 27 cho phần cơ bản.
- Quyền Accessibility, hiển thị trên ứng dụng khác, micro, thông báo (xem [docs/INSTALL.md](docs/INSTALL.md)).
- Một API key của nhà cung cấp AI bạn chọn (hoặc Ollama chạy trên máy trong cùng mạng LAN).

## Build từ Linux Mint

    export JAVA_HOME=<JDK 17-20>
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :app:assembleDebug -PdefaultLang=vi

APK: app/build/outputs/apk/debug/app-debug.apk. Chi tiết (SDK, JDK, lỗi thường gặp): [docs/BUILD.md](docs/BUILD.md).

## Kiểm thử

    ./gradlew :core:test

Bộ test core hiện có **16 test đỏ sẵn ở upstream** (các test vòng lặp LocalBrain như WorkStepTest, WorkTurnTest, QuietTurnTest...). Đã xác nhận bằng cách chạy lại trên mã gốc trước khi sửa. Các test mới cho tầng nhà cung cấp (WireAdaptersTest, DeepSeekLlmClientTest) đều xanh. Xem [reports/TEST-REPORT.md](reports/TEST-REPORT.md).

## Giấy phép và ghi công

Mã nguồn gốc: KUAFUAI, giấy phép Andee Open Source License (Apache-2.0 kèm điều kiện bổ sung). Xem [LICENSE](LICENSE) và [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Bản fork này **không white-label**: tên/icon Andee vẫn hiển thị; không dùng tên Andee để quảng bá bản fork như bản chính thức; phát hành thương mại hoặc nhúng vào sản phẩm khác cần sự cho phép trước bằng văn bản của KUAFUAI. Chi tiết: [reports/license-audit.md](reports/license-audit.md).

Các thay đổi của bản fork được ghi trong [reports/CHANGELOG.md](reports/CHANGELOG.md) và lịch sử commit.
