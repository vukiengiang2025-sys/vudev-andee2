# VUDEV ANDEE 2

**Trợ lý AI cá nhân sống trong điện thoại Android của bạn — giao diện tiếng Việt.**

VUDEV ANDEE 2 là bản fork tiếng Việt của **[Andee](https://github.com/kuafuai/andee)** (tác giả gốc: **KUAFUAI**, giấy phép Apache-2.0 kèm điều kiện bổ sung). Đây không phải bản phát hành chính thức của Andee: xem [Giấy phép & ghi công](#giấy-phép--ghi-công) trước khi dùng hoặc phát hành lại.

[English README](README.md) · [Cài đặt](docs/INSTALL.md) · [Tự build](docs/BUILD.md) · [Nhà cung cấp AI](docs/AI-PROVIDERS.md) · [Cá nhân hoá](docs/PERSONALIZATION.md) · [Bảo mật](docs/SECURITY.md) · [Kiến trúc](docs/ARCHITECTURE.md) · [Xử lý sự cố](docs/TROUBLESHOOTING.md) · [Nhật ký thay đổi](reports/CHANGELOG.md)

---

## VUDEV ANDEE 2 là gì?

Đây không phải một ứng dụng chat. Sau khi cài, điện thoại có thêm một "cư dân" thường trú:

- **Nói chuyện được** — bấm vào quả cầu để nói, gõ chữ, hoặc đánh thức bằng giọng nói ngoại tuyến ("Hey Andee").
- **Nhìn thấy màn hình** — đọc nội dung các ứng dụng khác, chụp màn hình khi cần.
- **Làm hộ bằng tay** — tự bấm, vuốt, nhập chữ trong ứng dụng khác, không cần ứng dụng đó phải tích hợp gì.
- **Nhớ về bạn** — có một cuốn sổ tay riêng (được mã hoá) để ghi điều cần nhớ, việc đã hứa, người quen, kế hoạch.
- **Có cá tính** — một nhân vật nhỏ trong cửa sổ nổi, có 6 hình dáng, 19 biểu cảm, có thể đổi giọng và cách nói.
- **Có bộ não thay được** — bạn chọn hãng AI nào trả lời (DeepSeek, OpenAI, Gemini, Claude, OpenRouter, Ollama hoặc máy chủ tự dựng), nhập khoá của chính bạn.

Bộ não chạy **trên máy** (chế độ Cục bộ / API) hoặc trên một máy chủ Hub do bạn tự dựng. Mặc định của bản fork: **Cục bộ / API với DeepSeek**, và giao diện **tiếng Việt**.

### Khác gì trợ lý có sẵn trong máy?

- Dữ liệu hội thoại và sổ tay nằm trên máy, không phải trên tài khoản của một hãng.
- Nó điều khiển được mọi ứng dụng bằng quyền trợ năng, không cần danh sách tích hợp.
- Bạn đổi được nhà cung cấp AI bất cứ lúc nào, kể cả dùng máy chủ nội bộ không tốn phí.
- Mã nguồn mở: toàn bộ vòng lặp agent và các chốt an toàn nằm trong kho này.

### Vì sao trên điện thoại vẫn hiện tên "Andee"?

Giấy phép của dự án gốc **cấm xoá hoặc che tên sản phẩm, biểu tượng và thông báo bản quyền mà ứng dụng hiển thị cho người dùng** (điều kiện A.1(a), xem [LICENSE](LICENSE)). Vì vậy bản fork giữ nguyên tên "Andee" trong giao diện, icon và thông báo; tên **VUDEV ANDEE 2** là tên của bản fork/kho mã, không phải nhãn thay thế trong ứng dụng. Nếu bạn định phát hành lại, hãy đọc kỹ phần giấy phép bên dưới.

---

## Tính năng

### Bàn tay — dùng điện thoại như bạn dùng

| Khả năng | Cách hoạt động |
| --- | --- |
| Đọc màn hình | Cây phần tử của Android (mỗi phần tử có mã để bấm), ảnh chụp khi ứng dụng tự vẽ |
| Bấm / vuốt / nhập chữ | Dịch vụ trợ năng của Android; nhập chữ qua kênh bàn phím ảo, bàn phím của bạn vẫn hoạt động |
| Mở ứng dụng, đọc thông báo | Dùng đúng quyền hệ thống, hỏi trước từng việc |
| Chụp màn hình | API chụp màn hình của Android 11+; có thêm đường dự phòng MediaProjection khi trợ năng bị thu hồi |

### Trí nhớ — hiểu bạn hơn theo thời gian

- **Sổ tay** (mã hoá AES/GCM, khoá trong Android Keystore): điều đã nhớ, việc đang làm, người quen, nhật ký, wiki, nguyên tắc bảo vệ do bạn viết.
- **Ngữ cảnh (scene)**: cách làm việc cho từng tình huống, có sẵn vài mẫu hoặc tự học từ thói quen; chỉ lưu khi bạn đồng ý.
- **Kịch bản (flow)**: ghi lại chuỗi thao tác quen thuộc để lần sau chạy lại từng bước, không tốn token cho mỗi bước.
- **Tự xem lại**: sau khi cuộc trò chuyện im lặng, trợ lý tự đọc lại và ghi điều đáng nhớ (tắt được trong Cài đặt).
- **Nhắc việc thật**: dùng báo thức của Android, không phải "hẹn bằng lời".

### Mặt — một nhân vật, không phải bộ giao diện

- 6 hình dáng: Andee, Mèo con, Bát Giới, Thỏ lém, Gấu ngố, Tiểu quỷ; hoặc 5 nhân vật "thú cưng" dạng người.
- 19 biểu cảm; mô hình có thể tự chọn biểu cảm ngay giữa câu trả lời.
- Vuốt quả cầu để đổi nhân vật; mỗi nhân vật có giọng và cách nói riêng.

### Nói, gõ, và những trang kết quả

- Bấm quả cầu để nói, gõ chữ, hoặc gửi tối đa 6 ảnh kèm câu hỏi.
- Câu trả lời dài, bảng so sánh, kế hoạch, biên bản cuộc họp… được viết thành **trang** (HTML) để mở lại, sửa lại, không mất đi.
- Câu trả lời có thể được đọc to bằng giọng hệ thống, Google hoặc Volcengine.

### Bạn bè và thiết bị của bạn (tuỳ chọn)

- Ghép hai máy bằng mã QR (chỉ trong cùng Wi-Fi) để Andee trên máy này nhờ Andee trên máy kia làm việc.
- "Thiết bị của tôi" được tin như chính bạn; "Bạn bè" thì mỗi yêu cầu đều hỏi bạn trước.

### Chó robot (tuỳ chọn)

- Có thể gắn một chú chó robot qua cổng USB OTG để di chuyển và nhìn bằng camera. Đây là tính năng thử nghiệm, cần đọc kỹ hướng dẫn an toàn trong [README gốc](README.md#the-robot-dog-usb-dongle).

---

## Yêu cầu

| Mục | Yêu cầu |
| --- | --- |
| Android | 8.1 (API 27) trở lên để cài và trò chuyện; **Android 11 (API 30) trở lên** để điều khiển màn hình, chụp màn hình, làm mờ cửa sổ |
| Kiến trúc | arm64-v8a (điện thoại phổ thông hiện nay, gồm Redmi 15 5G); bản build cũ có thể có armeabi-v7a / x86_64 |
| Quyền tối thiểu | Cửa sổ nổi (bắt buộc), Trợ năng (để điều khiển màn hình), Micro (để nói) |
| Mạng | Wi-Fi hoặc dữ liệu di động để gọi nhà cung cấp AI; Ollama/chạy nội bộ cần cùng mạng LAN |
| Bộ nhớ trống | Vài trăm MB cho ứng dụng, ảnh chụp, trang đã tạo và nhật ký |
| Tài khoản | Không bắt buộc tài khoản của dự án; cần **API key** của một nhà cung cấp AI (DeepSeek, OpenAI, Gemini, Claude, OpenRouter…) hoặc một máy chủ Ollama |

> Bản debug/build từ mã nguồn không kèm khoá nào. Bạn tự nhập khoá của mình; xem [docs/AI-PROVIDERS.md](docs/AI-PROVIDERS.md).

---

## Cài đặt nhanh

Chi tiết đầy đủ cho điện thoại thật (kể cả Redmi 15 5G / HyperOS) ở **[docs/INSTALL.md](docs/INSTALL.md)**.

1. **Lấy APK** — tải artifact `app-debug` từ GitHub Actions của kho này, hoặc tự build theo [docs/BUILD.md](docs/BUILD.md).
2. **Cài và cấp quyền** — mở ứng dụng, làm theo trang tự kiểm tra: Cửa sổ nổi → Trợ năng → Micro.
3. **Nhập khoá AI** — mở ⚙ trên quả cầu → **Hệ thống AI** → **Cục bộ / API** → **Quản lý nhà cung cấp và model**, thêm DeepSeek (hoặc hãng khác) rồi bấm **Kiểm tra kết nối**.
4. **Bấm quả cầu và nói thử**: "Xin chào, bạn làm được gì?"

Nếu quả cầu không hiện, đừng đoán: mở ứng dụng → **Tự kiểm tra** để xem dòng nào đang đỏ. Xem thêm [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

---

## Cấu hình nhà cung cấp AI

Bản fork có **Trung tâm AI**: một chỗ để thêm nhiều nhà cung cấp, chọn cái đang dùng, đặt cái dự phòng, và gán model cho từng loại việc.

| Nhà cung cấp | Kiểu kết nối | Ghi chú nhanh |
| --- | --- | --- |
| DeepSeek | OpenAI | Mặc định, rẻ, tiếng Việt tốt, không xem được ảnh |
| OpenAI | OpenAI | Xem được ảnh; cần thẻ quốc tế để nạp tiền |
| Google Gemini | Gemini (định dạng riêng) | Có hạn mức miễn phí hằng ngày; xem được ảnh |
| Anthropic Claude | Anthropic (định dạng riêng) | Viết và lập trình tốt; tiếng Việt tự nhiên |
| OpenRouter | OpenAI | Một khoá dùng được nhiều hãng; tự nhập mã model |
| Ollama | OpenAI | Chạy trên máy tính trong LAN; không cần khoá, không tốn phí |
| Tự nhập URL | OpenAI | LM Studio, vLLM, proxy nội bộ… |

Hướng dẫn từng bước, bảng model, cách lưu khoá và cách hoạt động của dự phòng: **[docs/AI-PROVIDERS.md](docs/AI-PROVIDERS.md)**.

---

## Cá nhân hoá

Mọi thứ dưới đây đổi được ngay trong ứng dụng, không cần build lại:

- **Ngôn ngữ**: tiếng Việt (mặc định), tiếng Trung, tiếng Anh.
- **Giao diện**: nền sáng / nền tối.
- **Hình dáng & biểu cảm**: quả cầu hay thú cưng, 6 nhân vật, xem thử 19 biểu cảm.
- **Cỡ chữ**: nhỏ / chuẩn / lớn / rất lớn (áp dụng sau khi khởi động lại trợ lý).
- **Nhân cách**: tên trợ lý, cách xưng hô, phong cách trả lời, yêu cầu riêng.
- **Ngữ cảnh, kịch bản, sổ tay, nguyên tắc bảo vệ**: tự viết hoặc để trợ lý đề xuất.

Chi tiết từng mục nằm ở đâu: **[docs/PERSONALIZATION.md](docs/PERSONALIZATION.md)**.

---

## Trạng thái dự án

- Đây là **bản fork đang phát triển**, chưa có bản phát hành chính thức. Số phiên bản vẫn là **0.15.0** của upstream; build từ nhánh `main` báo cùng số đó. Nhận diện build bằng mã commit.
- **Đã có trong bản fork:** giao diện tiếng Việt mặc định, bốn ngữ cảnh mẫu tiếng Việt, giọng đọc hệ thống tiếng Việt, mặc định DeepSeek `deepseek-chat`, bộ tài liệu này.
- **Đang hoàn thiện (có thể chưa có trong APK bạn tải):** Trung tâm AI nhiều nhà cung cấp, mã hoá khoá bằng Keystore, token WebSocket, cỡ chữ giao diện. Nếu APK không có nút **Quản lý nhà cung cấp và model** thì đó là bản cũ.
- **Chưa có:** bản release ký sẵn, tự cập nhật, bản dịch tiếng Việt cho các tài liệu pháp lý gốc (DISCLAIMER, SECURITY, acceptable-use vẫn là tiếng Anh).
- Danh sách thay đổi chi tiết và việc nào đã xong/đang làm: [reports/CHANGELOG.md](reports/CHANGELOG.md).

---

## Bảo mật & quyền riêng tư

- **Khoá API được mã hoá** bằng Android Keystore (AES/GCM) trước khi ghi xuống bộ nhớ; giao diện không bao giờ hiển thị lại khoá đã lưu.
- **Sổ tay, két mật khẩu** đều mã hoá bằng khoá riêng trong Keystore; khoá không rời khỏi máy.
- **Không có SDK phân tích, không có telemetry nội dung.** Chỉ có một lần kiểm tra phiên bản gửi phiên bản ứng dụng, kiểu máy, hãng, phiên bản Android, ngôn ngữ và một mã cài đặt ngẫu nhiên — chi tiết trong [docs/permissions.md](docs/permissions.md).
- **Cổng WebSocket 9008** cho phép máy tính trong cùng Wi-Fi điều khiển trợ lý. Bản WIP đã thêm **token** (mặc định bật); kết nối qua cáp `adb forward` được miễn token.
- **Nút thoát khẩn**: phím nguồn trên thanh điều khiển (hoặc nói "Thoát Andee") dừng toàn bộ trợ lý ngay trên máy, không cần mạng và không cần mô hình.

Đọc đầy đủ, kể cả **những gì chưa được bảo vệ**: **[docs/SECURITY.md](docs/SECURITY.md)** và [SECURITY.md](SECURITY.md) của dự án gốc.

> Trợ lý có thể đọc màn hình và bấm hộ. Chỉ nên cài trên máy bạn kiểm soát, và đừng coi nó là két an toàn cho tài khoản ngân hàng.

---

## Tài liệu

| Tài liệu | Nội dung |
| --- | --- |
| [docs/INSTALL.md](docs/INSTALL.md) | Cài APK lên điện thoại thật, cấp quyền, HyperOS/Redmi, lệnh adb |
| [docs/BUILD.md](docs/BUILD.md) | Build từ Linux Mint, JDK, Android SDK, lỗi Gradle thường gặp |
| [docs/AI-PROVIDERS.md](docs/AI-PROVIDERS.md) | Trung tâm AI, 7 nhà cung cấp, khoá, kiểm tra kết nối, dự phòng, model theo việc |
| [docs/PERSONALIZATION.md](docs/PERSONALIZATION.md) | Ngôn ngữ, giao diện, nhân vật, ngữ cảnh, nhân cách, sổ tay, cỡ chữ |
| [docs/SECURITY.md](docs/SECURITY.md) | Khoá, quyền, WebSocket, nút thoát, giới hạn còn lại |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Kiến trúc 3 module, BodyCoreService/ScreenBodyService, vòng lặp agent |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Không thấy quả cầu, lỗi khoá, mạng, font tiếng Việt, xung đột chữ ký APK |
| [reports/CHANGELOG.md](reports/CHANGELOG.md) | Những gì bản VUDEV ANDEE 2 thay đổi so với upstream v0.15.0 |
| [docs/getting-started.md](docs/getting-started.md) | Bản hướng dẫn nhanh gốc (tiếng Anh) |
| [docs/permissions.md](docs/permissions.md) | Từng quyền gắn với từng công cụ (tiếng Anh) |
| [docs/architecture.svg](docs/architecture.svg) | Sơ đồ kiến trúc một trang |

## Giấy phép & ghi công

- **Mã gốc**: Andee — Copyright 2026 **KUAFUAI**, https://github.com/kuafuai/andee
- **Giấy phép**: **Apache License 2.0 kèm điều kiện bổ sung** — đọc [LICENSE](LICENSE) (bản đầy đủ, điều kiện nằm ở Phần A). Tóm tắt những điều quan trọng:
  - Được dùng thương mại, sửa mã, bán dịch vụ của bạn — **trừ bốn trường hợp** dưới đây.
  - **Không white-label**: không xoá/thay/che tên sản phẩm, icon, thông báo bản quyền mà ứng dụng hiển thị cho người dùng cuối.
  - **Không chạy đa người thuê**: không vận hành như dịch vụ điều khiển thiết bị của người khác nếu chưa được phép bằng văn bản.
  - **Tệp LICENSE phải đi kèm** mọi bản phân phối; bản sửa đổi phải ghi rõ đã thay đổi tệp nào.
  - **Nhúng vào một sản phẩm khác cần văn bản cho phép của KUAFUAI.**
  - Không được dùng tên/logo của dự án để quảng bá bản fork như bản phát hành chính thức (điều A.3).
- **Ghi công**: dự án gốc KUAFUAI và các cộng tác viên; bản fork VUDEV ANDEE 2 do Vũ Huỳnh (`vukiengiang2025-sys`) duy trì. Xem [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) trước khi phát hành lại: ML Kit barcode và các stub `play-services-*` **không** phải mã nguồn mở.
- **Không phải bản chính thức**: kho này không được KUAFUAI xác nhận là bản phát hành chính thức. Đừng trình bày nó như vậy.

Bằng việc cài đặt hoặc phát hành lại, bạn đồng ý với [LICENSE](LICENSE), [DISCLAIMER.md](DISCLAIMER.md) và [docs/acceptable-use.md](docs/acceptable-use.md).
