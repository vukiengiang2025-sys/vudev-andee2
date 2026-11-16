# Kiểm toán giấy phép — VUDEV ANDEE 2

Ngày: 2026-11-16
Repo gốc: https://github.com/kuafuai/andee (KUAFUAI)
Fork: https://github.com/vukiengiang2025-sys/vudev-andee2

## 1. Giấy phép của dự án

LICENSE của Andee là **Andee Open Source License**: Apache License 2.0 **cộng thêm các điều kiện trong Part A**. Đây không phải Apache-2.0 thuần. Bản Apache-2.0 đầy đủ nằm ở Part B của cùng tệp.

Bốn điều kiện bổ sung (A.1):

| Điều kiện | Nội dung | Ảnh hưởng tới VUDEV ANDEE 2 |
|---|---|---|
| (a) Không white-label | Không xoá/thay/thu hẹp tên sản phẩm, biểu tượng ứng dụng hoặc thông báo bản quyền mà phần mềm hiển thị cho người dùng. Được thêm thông báo của mình bên cạnh, được nói sản phẩm dựa trên dự án này. Điều kiện này nói về thứ người dùng cuối thấy, không phải tên repo. | **Đã tuân thủ**: app vẫn mang tên Andee và icon gốc; chuỗi giới thiệu VUDEV ANDEE 2 chỉ được thêm dưới dạng ghi chú, tài liệu, không thay tên hiển thị/icon. Nếu muốn đổi tên hiển thị phải có văn bản đồng ý của KUAFUAI. |
| (b) Không multi-tenant hosting | Không vận hành phần mềm như dịch vụ điều khiển/đọc thiết bị của người khác nếu không được phép bằng văn bản. Một người vận hành trên thiết bị của mình thì được. | Dùng cá nhân trên điện thoại của mình: được phép. Cung cấp dịch vụ cho nhiều người dùng thiết bị khác: **không** được nếu chưa xin phép. |
| (c) Giấy phép phải đi kèm | Bản phân phối phải kèm LICENSE; bản sửa đổi phải ghi chú rõ đã thay đổi tệp nào (Apache-2.0 §4(b)). | Giữ nguyên LICENSE và THIRD_PARTY_NOTICES.md trong repo/APK phát hành; CHANGELOG + commit message ghi rõ thay đổi. |
| (d) Nhúng vào sản phẩm khác cần phép bằng văn bản | Phân phối phần mềm (hoặc bản phái sinh) như một thành phần của sản phẩm mình bán cần sự cho phép trước bằng văn bản của KUAFUAI. Tự chạy, chạy trong tổ chức của mình, chia sẻ source cho lập trình viên khác thì được. | Fork công khai + APK để tự cài/chia sẻ cho người thử nghiệm: nằm trong vùng "source/self-use". **Bán APK như sản phẩm đóng gói, hoặc nhúng vào một sản phẩm thương mại, cần xin phép KUAFUAI.** |

Các phần khác:

- **A.2 Đóng góp**: gửi PR vào repo gốc đồng nghĩa cấp phép đóng góp cho KUAFUAI theo giấy phép này và kèm quyền rộng hơn để KUAFUAI dùng đóng góp đó theo điều khoản khác.
- **A.3 Thương hiệu**: Apache-2.0 §6 không cấp quyền dùng tên/logo/thương hiệu. Fork **không** được trình bày như bản phát hành chính thức của Andee/KUAFUAI. Tên "VUDEV ANDEE 2" phải nói rõ là bản dựa trên Andee.
- **A.4 Giấy phép có thể đổi cho phiên bản sau**: bản đã phát hành vẫn theo giấy phép tại thời điểm nhận.

## 2. Giấy phép thành phần (THIRD_PARTY_NOTICES.md)

93 thành phần, gồm:

| Giấy phép | Số lượng | Yêu cầu chính |
|---|---|---|
| Apache-2.0 | 79 | Giữ license/NOTICE, ghi rõ đã sửa gì |
| ML Kit Terms of Service | 6 | **Không phải open source** — SDK quét mã vạch + model tải lúc chạy từ Google. Cần xác nhận chấp nhận được khi phân phối lại. |
| Android SDK License | 4 | **Không phải open source** — các stub play-services-* theo điều khoản Google. |
| MIT | 3 | Giữ thông báo bản quyền |
| BSD-3-Clause | 1 | Giữ thông báo bản quyền; không dùng tên dự án để quảng bá |

Các thành phần này **không bị xoá hay thay thế** trong fork. Việc bổ sung tính năng không thêm thư viện mới: phần AI Provider Center, mã hoá Keystore và giao diện đều dùng API có sẵn của Android/Kotlin/OkHttp/org.json (đã có trong danh sách).

## 3. Nghĩa vụ khi phát hành bản VUDEV ANDEE 2

1. Giữ nguyên LICENSE, THIRD_PARTY_NOTICES.md, thông báo bản quyền KUAFUAI, tên Andee và icon trong app.
2. Nêu rõ trong README/tài liệu: đây là **fork không chính thức** của kuafuai/andee; ghi link upstream.
3. Ghi rõ các tệp đã sửa (CHANGELOG + commit).
4. Không bán/nhúng APK vào sản phẩm thương mại khi chưa có văn bản đồng ý của KUAFUAI; nếu cần, liên hệ KUAFUAI trước khi phát hành.
5. Không dùng tên/logo Andee để quảng bá bản fork như bản chính thức.
6. APK có ML Kit và play-services stubs: kiểm tra lại điều khoản Google nếu phát hành rộng rãi.

## 4. Trạng thái trong bản fork hiện tại

| Hạng mục | Trạng thái |
|---|---|
| LICENSE còn nguyên | Có |
| THIRD_PARTY_NOTICES.md còn nguyên | Có |
| Tên hiển thị/icon Andee | Giữ nguyên (chưa white-label) |
| Ghi nguồn gốc | README.vi.md + docs có nêu nguồn |
| Ghi thay đổi | reports/CHANGELOG.md + commit từng giai đoạn |
| Xin phép KUAFUAI cho phát hành thương mại | **Chưa** — chỉ phát hành dạng fork source + APK thử nghiệm cá nhân |
| Phân phối thương mại/nhúng sản phẩm | **Chưa được phép** — không thực hiện |

## 5. Kết luận

Việc fork, sửa, build APK để tự dùng và chia sẻ source công khai là phù hợp với giấy phép, miễn là giữ nguyên nhận diện Andee và các thông báo bản quyền. Việc **thương mại hoá APK như sản phẩm** hoặc nhúng vào sản phẩm bán ra **cần sự cho phép trước bằng văn bản của KUAFUAI** (A.1.d, A.3). Tài liệu này không phải tư vấn pháp lý.
