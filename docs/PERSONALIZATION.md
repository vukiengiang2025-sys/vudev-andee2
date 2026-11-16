# Cá nhân hoá VUDEV ANDEE 2

Mọi thứ trong tài liệu này đổi được **ngay trong ứng dụng**, không cần build lại. Đa số nằm trong bảng **⚙ Cài đặt** (bấm quả cầu → ⚙); một số nằm trên thanh điều khiển của quả cầu.

> Đường dẫn menu có thể khác chút ít theo phiên bản HyperOS/Android và ngôn ngữ hệ thống. Tên mục trong tài liệu là tên tiếng Việt của ứng dụng.

---

## 1. Bảng tổng hợp: đổi gì, ở đâu

| Bạn muốn đổi | Vào đâu | Áp dụng khi nào |
| --- | --- | --- |
| Ngôn ngữ giao diện | ⚙ Cài đặt → nút **VI / 中 / EN** ở góc trên bên phải | Ngay lập tức |
| Nền sáng / tối | ⚙ → **Giao diện** | Ngay lập tức (toàn bộ thẻ, trang, bong bóng, quả cầu) |
| Cỡ chữ | ⚙ → **Giao diện** → **Cỡ chữ** | Sau khi khởi động lại trợ lý (tắt/bật lại hoặc khởi động lại máy) |
| Hình dáng / biểu cảm | ⚙ → **Hình dáng & Biểu cảm** → **Mở** | Ngay khi chạm |
| Nhân vật (quả cầu/thú cưng) | Cùng thẻ trên → **Hình thể** → **Hình dáng** | Ngay khi chạm |
| Nhân cách, cách xưng hô | ⚙ → **Nâng cao** → **Nhân cách trợ lý** | Từ câu tiếp theo (sau khi lưu) |
| Ngữ cảnh, kịch bản | Thanh trên quả cầu → **Ngữ cảnh** | Khi áp dụng/chạy |
| Sổ tay, ký ức | Thanh trên quả cầu → **Sổ tay** | Ngay khi xoá/sửa |
| Giọng nói (nghe/đọc) | ⚙ → **Hệ thống AI** → **Cục bộ / API** → mục **Giọng nói** | Sau khi đóng Cài đặt |
| Đánh thức bằng giọng | ⚙ → **Đánh thức bằng giọng nói** | Ngay khi thu âm xong |
| Chủ động chào | ⚙ → **Chủ động chào** | Ngay |
| Tự chủ động làm việc | ⚙ → **Tự chủ động làm việc** | Ngay |
| Thông báo được đọc | ⚙ → **Thông báo tin nhắn** | Ngay |
| Tự xem lại sau khi im lặng | ⚙ → **Sổ tay** (mục cài đặt) | Ngay |
| Số bước tối đa mỗi việc | ⚙ → **Số bước tối đa mỗi tác vụ** | Từ lượt sau |
| Nhà cung cấp AI | ⚙ → **Hệ thống AI** → **Cục bộ / API** → **Quản lý nhà cung cấp và model** | Khi đóng Cài đặt |

---

## 2. Ngôn ngữ

### Mặc định tiếng Việt

Bản fork đặt **tiếng Việt làm mặc định** (commit `4a66230`): nếu chưa từng chọn gì, giao diện và các mẫu câu (lời chào, ngữ cảnh có sẵn, cách xưng hô, giọng đọc hệ thống) đều bằng tiếng Việt — không phụ thuộc ngôn ngữ hệ thống của máy.

Ứng dụng có ba ngôn ngữ giao diện:

| Mã | Ngôn ngữ | Ghi chú |
| --- | --- | --- |
| `vi` | Tiếng Việt | Mặc định |
| `zh` | Tiếng Trung | Bản gốc |
| `en` | Tiếng Anh | |

### Đổi ngôn ngữ

- Trong ứng dụng: ⚙ **Cài đặt** → góc trên bên phải có ba nút **VI · 中 · EN**.
- Lần chạy đầu tiên: màn hình chào có mục **Ngôn ngữ**.
- Chọn xong, toàn bộ giao diện đổi ngay (không cần bấm Lưu). Các cửa sổ đang mở (thẻ cài đặt, sổ tay, tự kiểm tra) được vẽ lại theo ngôn ngữ mới.

Android 13+ cũng hiển thị Andee trong **Cài đặt hệ thống → Ứng dụng → Andee → Ngôn ngữ** nhờ tệp `res/xml/locales_config.xml` khai báo `vi`, `zh`, `en`. Tuy nhiên, **nên đổi trong ứng dụng** để quả cầu, phụ đề và mọi cửa sổ nổi đổi đồng bộ — bộ chọn của hệ thống không tự vẽ lại các cửa sổ nổi.

### Ngôn ngữ giao diện ≠ ngôn ngữ câu trả lời

- Giao diện chỉ đổi chữ bạn đọc.
- Mô hình được dặn **trả lời theo ngôn ngữ người dùng đang nói**; nếu câu hỏi mơ hồ, nó dùng ngôn ngữ giao diện làm tín hiệu.
- Vì vậy bạn có thể để giao diện tiếng Việt nhưng hỏi bằng tiếng Anh, và câu trả lời sẽ theo tiếng Anh (hoặc ngược lại).

### Giọng nói tiếng Việt

Xem mục 8. Tóm tắt: **đọc to** dùng được giọng hệ thống tiếng Việt (miễn phí, có sẵn) hoặc Google TTS với mã `vi-VN`; **nghe** (nhận dạng) phụ thuộc nhà cung cấp — Google cho đặt mã ngôn ngữ, Volcengine mặc định không được cấu hình cho tiếng Việt.

---

## 3. Giao diện: sáng / tối

⚙ **Cài đặt → Giao diện** có hai lựa chọn:

- **Sáng** — nền trắng, chữ tối.
- **Tối** — nền tối, chữ sáng.

Chạm là đổi ngay toàn bộ: thẻ, trang HTML, bong bóng, quả cầu và thanh điều khiển. Không có bảng màu tuỳ chỉnh khác — hai bảng màu này là toàn bộ giao diện.

---

## 4. Cỡ chữ

⚙ **Cài đặt → Giao diện → Cỡ chữ**:

| Nút | Tỉ lệ | Ghi chú |
| --- | --- | --- |
| Nhỏ | 0.9 | |
| Chuẩn | 1.0 | Mặc định |
| Lớn | 1.15 | |
| Rất lớn | 1.3 | |

Cỡ chữ áp dụng cho **các cửa sổ nổi** (thẻ, sổ tay, cài đặt) khi dịch vụ khởi động lại. Vì vậy sau khi chọn, hãy **thoát trợ lý rồi mở lại** (phím nguồn → Thoát, rồi bấm icon ứng dụng) hoặc khởi động lại máy. Dòng ghi chú ngay dưới nút nhắc đúng điều này.

---

## 5. Hình dáng, nhân vật và biểu cảm

⚙ **Cài đặt → Hình dáng & Biểu cảm → Mở**.

Thẻ này có một quả cầu riêng để xem thử (không phải quả cầu thật đang chạy), cùng các lựa chọn:

### Hình thể

- **Quả cầu** — nhân vật tròn như biểu tượng ứng dụng.
- **Thú cưng** — nhân vật dạng người/động vật có thân hình đầy đủ.

### Hình dáng (6 lựa chọn)

| Tên | Mô tả ngắn |
| --- | --- |
| **Andee** | Nhân vật gốc, mặc định |
| **Mèo con** | Cô mèo dễ thương |
| **Bát Giới** | Nhân vật tinh nghịch |
| **Thỏ lém** | Thỏ lanh lợi |
| **Gấu ngố** | Gấu hiền |
| **Tiểu quỷ** | Nhân vật tinh quái |

Khi chọn **Thú cưng**, có 5 nhân vật: Andee, Kem bơ, Kem bơ · Tóc ngắn, Andee nữ, Mèo con. Mỗi nhân vật có giọng đọc và cách nói riêng.

### Biểu cảm (19)

Thẻ cho xem thử từng biểu cảm: Bình tĩnh, Vui vẻ, Tò mò, Tức giận, Hồi hộp, Lo lắng, Nháy mắt, Vẫy tay, Bảo vệ, Ăn mừng, Nghỉ ngơi, Đang nghe, Chờ bạn chọn, Đang nghĩ, Đang nói, Đang làm việc, Thành công, Thất bại, Đang ngủ.

Chạm vào biểu cảm chỉ đổi **quả cầu xem thử** trong thẻ; không lưu. Biểu cảm thật do mô hình chọn theo cảm xúc câu trả lời (ví dụ nó có thể viết `[vui]` trước câu trả lời) và do trạng thái máy (đang nghe, đang nghĩ, đang nói).

### Đổi nhanh ngoài màn hình chính

**Vuốt quả cầu** để đổi sang nhân vật kế tiếp — cách nhanh nhất khi không mở Cài đặt. Lựa chọn được lưu ngay.

---

## 6. Ngữ cảnh (scene) và kịch bản (flow)

Bấm **Ngữ cảnh** trên thanh điều khiển của quả cầu. Thẻ có hai tab:

- **Ngữ cảnh** — cách làm việc cho một tình huống: mục tiêu, giọng nói, quy tắc, thói quen.
- **Kịch bản** — chuỗi thao tác đã ghi, chạy lại từng bước mà không cần gọi mô hình cho mỗi bước.

### Bốn ngữ cảnh có sẵn

| Tên | Việc nó làm |
| --- | --- |
| **Trực tin nhắn** | Theo dõi thông báo chat, báo ngay việc gấp, giữ yên phần còn lại |
| **Rà soát gói đăng ký** | Tìm các gói trừ tiền hằng tháng, chỉ hủy khi bạn đồng ý |
| **Ghi chú cuộc họp** | Ghi âm cuộc họp, tạo biên bản, đề xuất việc cần làm |
| **Canh đặt chỗ đúng giờ** | Vào sẵn khi mở suất, điền thông tin; thanh toán luôn để bạn |

Ngữ cảnh có sẵn **không tự áp dụng**: chạm **Áp dụng** mới lưu vào sổ tay. Sau đó bạn có thể sửa/xoá như ngữ cảnh của riêng mình.

### Ngữ cảnh tự học

Sau vài lần cùng một kiểu việc, trợ lý có thể đề xuất một ngữ cảnh; **chỉ lưu khi bạn đồng ý**. Vào/ra ngữ cảnh bằng cách nói, bằng thẻ, theo lịch, hoặc tự động khi mở một ứng dụng. Khi đang trong ngữ cảnh, một chip dưới quả cầu hiện tên; dấu ✕ để thoát.

### Kịch bản

- Ghi lại bằng cách nói "Ghi lại chuỗi thao tác vừa rồi" hoặc dùng công cụ ghi.
- Mỗi bước được kiểm tra trước khi chạy; nếu kẹt, kịch bản dừng và báo bước bị kẹt để bạn/trợ lý sửa, không tự đoán.
- Có bước "phần của bạn" (chỉ bạn làm được) và bước "giao cho tôi" (trợ lý xử lý rồi chạy tiếp).
- Kịch bản lưu trong sổ tay (mã hoá), không gửi lên Hub.

---

## 7. Nhân cách trợ lý

⚙ **Cài đặt → Nâng cao → Nhân cách trợ lý**. Bốn ô:

| Ô | Ví dụ | Tác dụng |
| --- | --- | --- |
| **Tên trợ lý** | `An` | Tên dùng khi tự giới thiệu |
| **Cách xưng hô** | `gọi tôi là anh Nam` | Cách gọi bạn |
| **Phong cách trả lời** | `ngắn gọn, thân mật` | Giọng điệu, độ dài |
| **Yêu cầu riêng** | `luôn nhắc tôi uống nước` | Thói quen/việc nhỏ muốn duy trì |

Nhân cách áp dụng cho **mọi cuộc trò chuyện**, được ghép sau phần mô tả nhân vật (Andee/Mèo con/…). Nó **chỉ đổi cách nói**, không mở rộng quyền, không thể ghi đè nguyên tắc bảo vệ hay chốt an toàn của ứng dụng.

Ngoài ra, mỗi nhân vật có sẵn một "persona" riêng (cách nói của Andee, của mèo con…). Nhân cách của bạn được xếp **dưới** phần đó và dưới cả nguyên tắc.

---

## 8. Giọng nói: nghe và đọc

⚙ **Cài đặt → Hệ thống AI → Cục bộ / API** → mục **Giọng nói**.

### Chọn bên nghe (ASR)

| Lựa chọn | Ghi chú |
| --- | --- |
| **Volcengine** (mặc định nếu không có khoá Google) | Nhận dạng streaming; cần API key Volcengine và gói dịch vụ đã kích hoạt (xem cảnh báo bên dưới) |
| **Google** | Dùng Gemini Live; cần Google API key, **Project ID** và **Location**; ô **Ngôn ngữ** cho phép nhập mã như `vi-VN` |

> ⚠️ **Volcengine mặc định được cấu hình cho tiếng Trung/Anh, không phải tiếng Việt.** Muốn nói tiếng Việt, chọn Google và đặt **Ngôn ngữ = `vi-VN`**, hoặc dùng bàn phím để gõ. Nếu bạn có một dịch vụ ASR khác **tương thích giao thức Volcengine** và hỗ trợ tiếng Việt, có thể nhập endpoint riêng trong mục **Địa chỉ API Volcengine**.

Về Volcengine: một khoá đúng vẫn có thể bị từ chối nếu dịch vụ đang ở gói **trả sau** — gói đó chỉ dùng được từ trang demo trên trình duyệt. Trong console Volcengine, vào **Quản lý kích hoạt** và chuyển sang **trả trước**, mua gói tài nguyên streaming ASR.

### Chọn bên đọc (TTS)

| Lựa chọn | Tiếng Việt | Ghi chú |
| --- | --- | --- |
| **Hệ thống** | ✅ (nếu máy có giọng Việt) | Miễn phí; dùng công cụ TTS của Android. Với máy chưa cài giọng Việt, vào **Cài đặt hệ thống → Ngôn ngữ & nhập → Chuyển văn bản thành giọng nói** để cài |
| **Google** | ✅ (đặt `languageCode = vi-VN`) | Dùng Vertex Express key; đọc qua HTTP |
| **Volcengine** | ⚠️ | Giọng có sẵn chủ yếu tiếng Trung/Anh |

Khi chưa nhập khoá giọng nói nào, bản fork mặc định **đọc bằng giọng hệ thống** thay vì im lặng (thay đổi trong `4a66230`). Có thể tắt đọc to bằng nút loa trên thanh quả cầu.

### Tắt micro / từ đánh thức

- Nút **micro** cạnh nút loa trên thanh quả cầu: tắt/bật việc nghe từ đánh thức. Tắt để nhường micro cho ứng dụng khác và đỡ tốn pin; mẫu giọng đã thu vẫn được giữ.
- ⚙ **Đánh thức bằng giọng nói** → **Thu âm**: nói cụm từ (ví dụ "Hey Andee") 3 lần. Có thanh **Độ nhạy** (Chặt chẽ / Tiêu chuẩn / Nhạy / Rất nhạy) và tuỳ chọn **Nghe cả khi khoá màn hình** (tốn pin hơn).

---

## 9. Sổ tay: trí nhớ và việc đang làm

Bấm **Sổ tay** trên thanh quả cầu. Đây là nơi bạn xem **mọi thứ trợ lý được phép nhớ**, và xoá từng mục.

Chín tab:

| Tab | Nội dung |
| --- | --- |
| **Nguyên tắc** | Những điều bạn viết và trợ lý phải tuân thủ; đứng đầu mọi thứ. Chỉ bạn sửa được |
| **Bản thân** | Trợ lý tự mô tả nó là ai (nó tự viết lại khi có chuyện) |
| **Quan tâm** | Những việc/nhóm/mối quan tâm nó đang theo dõi |
| **Nhật ký** | Nhật ký nó viết về ngày hôm nay |
| **Kế hoạch** | Việc hôm nay, việc dài hạn, tạm dừng/tiếp tục/gỡ |
| **Mọi người** | Những người nó đã thực sự làm việc cùng; có thể gộp trùng |
| **Wiki** | Các bài nó viết (kế hoạch, nghiên cứu, đánh giá) dạng Markdown |
| **Ghi nhớ** | Ký ức theo loại; chạm để đọc, xoá được từng mục |
| **Đang làm** | Việc nó đã nhận và chưa xong, rồi các nhắc việc có giờ |

- **Sổ tay được mã hoá** AES/GCM bằng khoá trong Android Keystore; không gửi lên Hub, không có bản sao trên mạng.
- **Xoá là xoá thật** khỏi dữ liệu mã hoá, không chỉ ẩn.
- Nút **Sao chép tất cả** xuất toàn bộ nội dung dạng văn bản — cẩn thận khi dán vào nơi khác.
- Trong ⚙ **Cài đặt → Sổ tay** có cài đặt **tự xem lại sau khi im lặng** (mặc định bật), số phút im lặng, số lần tối đa mỗi ngày, và nút **Xem những gì đã nhớ**. Tắt tự xem lại nếu không muốn tốn token.

---

## 10. Chủ động và thông báo

| Mục | Ở đâu | Tác dụng |
| --- | --- | --- |
| **Thông báo tin nhắn** | ⚙ → mục này | Chọn **Ứng dụng chat** hoặc **Tất cả** để trợ lý phân loại thông báo; tắt để không bị làm phiền |
| **Chủ động chào** | ⚙ → mục này | Bong bóng gợi ý nhỏ ("Uống nước thôi"); **Ít** (1/lần/giờ, 4/ngày) hoặc **Bình thường** (2/giờ, 8/ngày); nhấn giữ bong bóng để tắt loại đó |
| **Tự chủ động làm việc** | ⚙ → mục này | Khoảng 20 phút/lần tự xem có việc bị chậm để xử lý âm thầm (không chạm màn hình, không phát tiếng); tắt thì chỉ làm khi bạn gọi |
| **Hiển thị trong hội thoại** | ⚙ → mục này | Bật/tắt các dòng sự kiện (nhật ký, wiki…) trong lịch sử |
| **Số bước tối đa** | ⚙ → mục này | Giới hạn số lần gọi công cụ mỗi lượt (mặc định 40) để tránh lặp vô hạn tốn token |

---

## 11. Bạn bè và thiết bị khác (tuỳ chọn)

⚙ **Cài đặt → Bạn bè** (hoặc **Bạn bè** trên thanh quả cầu):

- **Quét mã để thêm** — quét QR trên máy kia trong cùng Wi-Fi.
- **Hiện mã QR cho bạn bè** / **Hiện cho thiết bị của tôi** — mã có hiệu lực 2 phút.
- **Thiết bị của tôi** được tin như chính bạn; **Bạn bè** thì mỗi yêu cầu đều hỏi bạn (Cho phép / Luôn cho phép / Từ chối).
- Bạn bè không thấy ký ức, hội thoại hay két của bạn.

---

## 12. Két sắt (mật khẩu)

⚙ **Cài đặt → Két sắt** → **Quản lý**.

- Lưu tài khoản/mật khẩu; mật khẩu **không bao giờ hiện cho mô hình** — chỉ có công cụ điền mật khẩu vào ô đang chọn.
- Mã hoá AES/GCM bằng khoá trong Keystore, không cần mật khẩu chính.
- Két mở được khi máy đã mở khoá; người cầm máy đã mở khoá có thể vào két. Đây là bảo vệ chống trích xuất, không phải chống người mở máy.

---

## 13. Khôi phục cài đặt gốc

⚙ **Cài đặt → Nâng cao → Khôi phục cài đặt gốc**: xoá lịch sử chat, sổ tay, két, nhà cung cấp AI, cài đặt cá nhân. Khoá CodeFlying (nếu có) được giữ; các khoá khác phải nhập lại; từ đánh thức phải thu âm lại.

Đọc kỹ danh sách trước khi xác nhận — thao tác không thể hoàn tác và không có sao lưu.

---

## 14. Liên quan

- [AI-PROVIDERS.md](AI-PROVIDERS.md) — chọn bộ não, khoá, dự phòng, model theo việc.
- [SECURITY.md](SECURITY.md) — dữ liệu nào ở đâu, giới hạn bảo vệ.
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — font tiếng Việt, micro, giọng đọc.
