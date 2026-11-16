# Nhà cung cấp AI — Trung tâm AI

VUDEV ANDEE 2 gọi mô hình AI qua API của hãng bạn chọn. Bạn tự nhập khoá, tự chọn model, và có thể đổi bất cứ lúc nào — không cần build lại ứng dụng.

Tài liệu này giải thích **Trung tâm AI**: thêm nhà cung cấp, kiểm tra kết nối, đặt dự phòng, và gán model cho từng loại việc.

> Nếu bản APK của bạn **không** thấy nút **Quản lý nhà cung cấp và model**, đó là bản cũ chưa có Trung tâm AI. Tải bản mới hoặc tự build theo [BUILD.md](BUILD.md).

---

## 1. Mở Trung tâm AI ở đâu?

1. Bấm quả cầu → biểu tượng **⚙** (Cài đặt).
2. Chọn mục **Hệ thống AI**.
3. Chọn tab **Cục bộ / API**.
4. Bấm **Quản lý nhà cung cấp và model**.

Thẻ **Trung tâm AI** mở ra với ba phần:

- **Danh sách nhà cung cấp** — mỗi dòng có nhãn, model, trạng thái (Đang dùng / Dự phòng / Đang tắt), và các nút **Dùng cái này**, **Đặt dự phòng**, **Sửa**, **Tắt/Bật**.
- **Model cho từng việc** — gán nhà cung cấp/model cho từng loại việc (xem mục 6).
- **+ Thêm nhà cung cấp** — mở biểu mẫu thêm mới.

Thay đổi được lưu **ngay khi bấm nút**, không cần bấm "Lưu" cho cả thẻ. Để áp dụng vào bộ não đang chạy, **đóng bảng Cài đặt**: ứng dụng sẽ tự khởi động lại bộ não nếu "ai trả lời" thay đổi.

---

## 2. Bảng nhà cung cấp có sẵn

Dữ liệu lấy từ `AiProviders.kt` (`core/src/main/kotlin/net/kuafuai/andee/brain/provider/`).

| Nhà cung cấp | Định dạng API | Base URL mặc định | Model mặc định | Cần khoá | Nhìn ảnh | Suy luận | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **DeepSeek** | OpenAI | `https://api.deepseek.com` | `deepseek-chat` | Có | Không | Có | Rẻ, tiếng Việt tốt; mặc định của bản fork |
| **OpenAI** | OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini` | Có | Có | Có | Nhiều model; cần thẻ quốc tế để nạp tiền |
| **Google Gemini** | **Gemini** (riêng) | `https://generativelanguage.googleapis.com/v1beta` | `gemini-2.5-flash` | Có | Có | Có | Có hạn mức miễn phí hằng ngày |
| **Anthropic Claude** | **Anthropic** (riêng) | `https://api.anthropic.com` | `claude-sonnet-4-5` | Có | Có | Có | Mạnh về viết/lập trình; tiếng Việt tự nhiên |
| **OpenRouter** | OpenAI | `https://openrouter.ai/api/v1` | (tự nhập) | Có | Có | Có | Một khoá dùng nhiều hãng; tự nhập mã model |
| **Ollama** | OpenAI | `http://127.0.0.1:11434/v1` | `qwen3` | Không | Tuỳ model | Tuỳ model | Chạy trên máy tính trong LAN; miễn phí |
| **OpenAI-compatible (tự nhập URL)** | OpenAI | (tự nhập) | (tự nhập) | Có (nhập chuỗi bất kỳ nếu máy chủ bỏ qua) | Có | Có | LM Studio, vLLM, proxy nội bộ… |

Model gợi ý trong ứng dụng:

| Nhà cung cấp | Danh sách model |
| --- | --- |
| DeepSeek | `deepseek-chat`, `deepseek-reasoner` (suy luận) |
| OpenAI | `gpt-4o`, `gpt-4o-mini`, `gpt-4.1`, `gpt-4.1-mini`, `o3-mini` |
| Gemini | `gemini-2.5-pro`, `gemini-2.5-flash`, `gemini-2.5-flash-lite` |
| Anthropic | `claude-sonnet-4-5`, `claude-haiku-4-5`, `claude-opus-4-1` |
| Ollama | `qwen3`, `llama3.3`, `deepseek-r1`, `gemma3` |

Danh sách này chỉ là gợi ý cho ô chọn. Với OpenRouter, Ollama và "tự nhập URL", bạn gõ được **bất kỳ mã model nào** mà máy chủ đó phục vụ.

---

## 3. Thêm một nhà cung cấp

1. Trung tâm AI → **+ Thêm nhà cung cấp**.
2. **Loại nhà cung cấp**: chọn DeepSeek / OpenAI / Google Gemini / Anthropic Claude / OpenRouter / Ollama / OpenAI-compatible.
3. **Tên hiển thị**: ví dụ `DeepSeek của tôi` (để trống sẽ dùng tên hãng).
4. **Base URL**: để trống dùng mặc định của hãng (khuyến nghị); chỉ nhập khi dùng proxy/máy chủ nội bộ. Nếu nhập, phải bắt đầu bằng `http://` hoặc `https://`.
5. **Model**: để trống dùng model mặc định; hoặc chọn trong danh sách gợi ý.
6. **API key**: dán khoá. Với Ollama, để trống. Với "OpenAI-compatible (tự nhập URL)", giao diện coi khoá là bắt buộc kể cả khi máy chủ bỏ qua — nhập một chuỗi bất kỳ (ví dụ `local`).
7. **Tuỳ chọn nâng cao** (chỉ hiện khi nhà cung cấp hỗ trợ):
   - **Suy luận (thinking)** và **Mức suy luận**: dùng cho DeepSeek/OpenAI/o-series/Gemini 2.5/Claude.
   - **Temperature**, **Top P**, **Max output tokens**: để trống = mặc định của hãng.
   - **Timeout (giây)**: mặc định 180.
   - **Số lần thử lại**: mặc định 1 (chỉ cho lỗi tạm thời).
8. Bấm **Kiểm tra kết nối** để thử trước, rồi **Lưu**.

Sau khi lưu, bấm **Dùng cái này** để đặt làm nhà cung cấp chính. Bấm **Đặt dự phòng** ở một nhà cung cấp khác để đặt phương án dự phòng (xem mục 5).

### Lấy khoá ở đâu

| Nhà cung cấp | Trang lấy khoá |
| --- | --- |
| DeepSeek | https://platform.deepseek.com/api_keys |
| OpenAI | https://platform.openai.com/api-keys |
| Google Gemini | https://aistudio.google.com/apikey |
| Anthropic Claude | https://console.anthropic.com/settings/keys |
| OpenRouter | https://openrouter.ai/keys |
| Ollama | Không cần khoá — https://ollama.com/download |

Khoá DeepSeek thường bắt đầu bằng `sk-`; Gemini bằng `AIza`; Claude bằng `sk-ant-`; OpenRouter bằng `sk-or-`. Ứng dụng chỉ dùng các gợi ý này để bạn nhận ra mình dán đúng loại khoá.

---

## 4. Định dạng API: vì sao Gemini và Claude không "giống OpenAI"

Bên trong, ứng dụng giữ **một dạng hội thoại chuẩn** (giống OpenAI) để vòng lặp agent, sổ tay và kho lưu trữ không phải biết hãng nào đang trả lời. Ở rìa HTTP, có ba bộ chuyển đổi trong `WireAdapters.kt`:

| Định dạng | Dùng cho | Đường dẫn thực tế | Xác thực | Khác biệt chính |
| --- | --- | --- | --- | --- |
| **OpenAI** | OpenAI, DeepSeek, OpenRouter, Ollama, máy chủ tương thích | `<base>/chat/completions` | `Authorization: Bearer <key>` | Chuẩn phổ biến nhất |
| **Gemini** | Google Gemini | `<base>/models/<model>:generateContent` | Header `x-goog-api-key` (không đưa key lên URL) | Nội dung dạng `contents/parts`, công cụ là `functionDeclarations`, không có tool-call id; tham số schema không hợp lệ bị lọc |
| **Anthropic** | Claude | `<base>/v1/messages` | `x-api-key` + `anthropic-version: 2023-06-01` | `system` là trường riêng; kết quả công cụ là block trong message `user`; không có `tool_choice` theo kiểu OpenAI |

Hai quy tắc dịch quan trọng (đã xử lý tự động, bạn không phải làm gì):

- Kết quả công cụ của Gemini/Anthropic cần **tên hàm**, trong khi dạng chuẩn chỉ có `tool_call_id`. Bộ chuyển đổi ghi nhớ `id → tên` từ message trước đó.
- Gemini/Anthropic **từ chối hai message cùng role liền nhau**; bộ chuyển đổi gộp chúng lại (ví dụ ảnh được gắn thành message `user` ngay sau một message `user` khác).

Vì vậy: đổi từ DeepSeek sang Claude/Gemini chỉ là đổi nhà cung cấp trong Trung tâm AI, không phải cấu hình lại công cụ.

---

## 5. Kiểm tra kết nối, thử lại và dự phòng

### 5.1. Nút "Kiểm tra kết nối"

- Gửi **một** yêu cầu rất ngắn ("ping"), không bật suy luận, tối đa 16 token, timeout 40 giây, **không thử lại**.
- Thành công nghĩa là **URL + khoá + model cùng hợp lệ** và máy chủ trả lời được; nội dung trả lời hiện ngay dưới nút.
- Nếu ô khoá đang trống nhưng đã lưu khoá trước đó, ứng dụng dùng khoá đã lưu để thử.
- Lỗi được dịch thành câu dễ hiểu:

| Mã | Ý nghĩa | Việc nên làm |
| --- | --- | --- |
| 401 / 403 | Khoá bị từ chối | Kiểm tra khoá, quyền của khoá, đã nạp tiền chưa |
| 402 | Hết số dư | Nạp tiền hoặc đổi nhà cung cấp |
| 404 | Không thấy model hoặc địa chỉ | Kiểm tra **Model** và **Base URL** (đừng thêm `/chat/completions` cho Gemini/Claude) |
| 429 | Bị giới hạn tần suất | Chờ một lát, hoặc đặt dự phòng |
| 5xx | Nhà cung cấp đang lỗi | Thử lại sau, hoặc dùng dự phòng |
| Khác | Lỗi mạng / chứng chỉ / JSON | Kiểm tra Wi-Fi, VPN, tường lửa; xem log `adb logcat -s Body:V` |

### 5.2. Thử lại trên cùng nhà cung cấp

Mặc định `Số lần thử lại = 1`. Khi gặp:

- **429** (giới hạn tần suất), **5xx** (máy chủ lỗi), hoặc **lỗi mạng** → thử lại cùng endpoint với thời gian chờ tăng dần.

Các lỗi còn lại **không** thử lại. Riêng **400** (yêu cầu sai) không bao giờ thử lại và cũng **không** đẩy sang dự phòng: gửi cùng một yêu cầu sai sang hãng khác chỉ tốn thêm tiền.

### 5.3. Dự phòng

- Đặt một nhà cung cấp khác làm **Dự phòng** (chỉ một cái).
- Khi nhà cung cấp chính lỗi ở nhóm **xác thực (401/403), hết số dư (402), giới hạn tần suất (429), lỗi mạng, hoặc 5xx**, ứng dụng thử **một lần** trên dự phòng.
- Dự phòng **không có dự phòng của chính nó** — không có vòng lặp.
- Nếu chính dự phòng lỗi, lỗi của dự phòng là lỗi bạn thấy.

### 5.4. Khi nào thay đổi có hiệu lực?

- Đóng bảng **Cài đặt** sau khi sửa. Dịch vụ sẽ so sánh cấu hình trước/sau:
  - Đổi **nhà cung cấp, khoá, Base URL hoặc dự phòng** → bộ não được **khởi động lại**; hội thoại đang dở bị mất ngữ cảnh (đúng như khi đổi "ai trả lời").
  - Chỉ đổi **model / thinking / mức suy luận** → áp dụng tại chỗ, hội thoại được giữ.
- Nếu đang có câu trả lời dở, nên đợi xong rồi mới đổi.

---

## 6. Model cho từng việc (vai trò)

Trung tâm AI cho phép gán một nhà cung cấp/model riêng cho từng loại việc. Chưa gán thì việc đó dùng nhà cung cấp đang dùng.

| Vai trò | Mã | Dùng cho | Trạng thái nối vào bộ não |
| --- | --- | --- | --- |
| **Chính** | `main` | Mọi lượt trò chuyện và gọi công cụ | ✅ Đang dùng |
| **Dự phòng** | `fallback` | Khi nhà cung cấp chính lỗi như mục 5.3 | ✅ Đang dùng |
| **Phân tích hình ảnh** | `vision` | `tap_target`: mô hình nhìn ảnh để tìm phần tử cần chạm | ✅ Đang dùng (nếu chưa gán, dùng Chính) |
| **Tiết kiệm chi phí** | `cheap` | Phân loại thông báo (một lượt gọi rất ngắn, chạy mỗi khi có thông báo) | ✅ Đang dùng (nếu chưa gán, dùng Chính) |
| **Lập trình** | `code` | Việc viết mã | 🚧 Đã lưu cấu hình; chưa phải mọi lời gọi đều dùng |
| **Hội thoại** | `chat` | Hội thoại thường | 🚧 Đã lưu cấu hình; chưa nối |
| **Tác vụ dài** | `long` | Việc cần ngữ cảnh lớn | 🚧 Đã lưu cấu hình; chưa nối |

Cách đặt: Trung tâm AI → **Model cho từng việc** → chạm vào một vai trò → chạm vào nhà cung cấp muốn giao (có thể chọn model cụ thể). **Bỏ chọn** để quay về "Theo nhà cung cấp đang dùng".

> 🔴 **Đừng coi các vai trò 🚧 là đã hoạt động.** Hiện tại chỉ **Chính, Dự phòng, Phân tích hình ảnh và Tiết kiệm** được bộ não đọc. Các vai trò còn lại là cấu hình đã lưu để dùng ở bản sau; nếu bạn cần chúng ngay, hãy gán bằng nhà cung cấp Chính.

---

## 7. Khoá được lưu thế nào?

Đây là phần quan trọng nhất về an toàn.

- Khoá trong Trung tâm AI được mã hoá bằng **AES/GCM** với khoá sinh ra trong **Android Keystore** (lớp `security/SecretBox.kt`, alias `andee_secret_box_v1`). Khoá giải mã **không rời khỏi máy**.
- Dữ liệu lưu có dạng `v1:base64(iv + ciphertext)` trong tệp SharedPreferences riêng của Trung tâm AI (`andee_ai`).
- Khoá **không bao giờ được in lại vào ô nhập**. Ô khoá luôn trống với gợi ý "Đã lưu key — nhập key mới để thay". Để trống khi lưu = giữ khoá cũ; nhập khoá mới = thay; nút **Xoá key đã lưu** = xoá.
- Khoá **không được ghi vào log**, không đưa vào `toString()`, không hiện trong thông báo lỗi.
- Cài đặt cũ dùng khoá dạng văn bản thường (`llm_api_key`) được **chuyển sang bản mã hoá một lần** khi nâng cấp; bản văn bản thường bị xoá ngay sau đó.
- Khoá giọng nói (Volcengine, Google) và khoá CodeFlying cũng được mã hoá cùng cơ chế. Token WebSocket cũng vậy.
- Nếu bạn **khôi phục bản sao lưu sang máy khác** hoặc **đặt lại Android Keystore**, dữ liệu mã hoá cũ không đọc được. Ứng dụng coi như "chưa có khoá" và yêu cầu nhập lại — không crash, không lộ ciphertext.
- Khoá **vẫn nằm trong bộ nhớ** khi đang dùng để gọi API. Một thiết bị bị chiếm quyền root/người mở khoá máy vẫn có thể đọc được qua bộ nhớ hoặc qua chính ứng dụng. Xem [SECURITY.md](SECURITY.md).

---

## 8. Ollama: chạy AI trên máy tính của bạn

Ollama chạy mô hình trên máy tính trong cùng mạng Wi-Fi, không cần khoá, không tốn phí.

1. Trên máy tính, cài Ollama: https://ollama.com/download
2. Tải một mô hình, ví dụ:
   ```bash
   ollama pull qwen3
   ```
3. Cho Ollama nghe trên mạng LAN (mặc định chỉ nghe 127.0.0.1):
   ```bash
   OLLAMA_HOST=0.0.0.0:11434 ollama serve
   ```
4. Trên điện thoại, thêm nhà cung cấp **Ollama**, sửa **Base URL** thành địa chỉ máy tính, ví dụ:
   ```text
   http://192.168.1.10:11434/v1
   ```
   (Không dùng `127.0.0.1` — trên điện thoại, `127.0.0.1` là chính điện thoại.)
5. **Model** gõ đúng tên đã tải (ví dụ `qwen3`), bấm **Kiểm tra kết nối**.

Lưu ý:

- Điện thoại và máy tính phải cùng mạng; tường lửa trên máy tính phải mở cổng 11434.
- Mô hình nhỏ chạy trên máy tính có thể **không gọi công cụ tốt**; nếu trợ lý không bấm được gì, thử mô hình lớn hơn hoặc dùng nhà cung cấp đám mây cho vai trò **Chính**.
- Ollama không nhìn ảnh trừ khi bạn dùng mô hình có khả năng thị giác (ví dụ `gemma3`).

---

## 9. Máy chủ tự dựng / OpenAI-compatible

Dùng khi bạn có LM Studio, vLLM, proxy nội bộ, hoặc một cổng API khác theo chuẩn OpenAI:

1. Thêm nhà cung cấp **OpenAI-compatible (tự nhập URL)**.
2. **Base URL**: địa chỉ gốc của máy chủ, ví dụ:
   - LM Studio: `http://192.168.1.10:1234/v1`
   - vLLM: `http://192.168.1.10:8000/v1`
   - Proxy nội bộ: `https://ai.noibo.example/v1`
3. **Model**: mã model máy chủ phục vụ (xem tài liệu máy chủ; LM Studio hiện danh sách model trong ứng dụng của nó).
4. **API key**: nếu máy chủ yêu cầu thì dán khoá; nếu không yêu cầu, nhập một chuỗi bất kỳ (ví dụ `local`) — giao diện bắt buộc có khoá cho loại nhà cung cấp này.
5. Nếu máy chủ đã có sẵn đường dẫn đầy đủ `…/chat/completions`, dán nguyên URL cũng được — ứng dụng nhận ra và không ghép đôi đường dẫn.
6. **Kiểm tra kết nối**.

Mẹo: nếu máy chủ không hỗ trợ công cụ (function calling), trợ lý vẫn trò chuyện được nhưng **không điều khiển được điện thoại**. Hãy chọn mô hình có hỗ trợ tools.

---

## 10. Câu hỏi thường gặp

**Tôi có cần nhiều khoá không?**
Không. Một khoá DeepSeek là đủ để bắt đầu. Chỉ thêm nhà cung cấp thứ hai nếu muốn dự phòng hoặc dùng model khác cho từng việc.

**Khoá của tôi có bị gửi lên máy chủ nào khác không?**
Khoá chỉ được gửi tới **Base URL bạn cấu hình** (qua HTTPS, trừ khi bạn tự nhập `http://`). Ứng dụng không có máy chủ trung gian cho khoá AI. Tuy nhiên, mọi cuộc gọi mô hình đều đi tới hãng bạn chọn — xem [SECURITY.md](SECURITY.md).

**Đổi nhà cung cấp có mất lịch sử chat không?**
Không mất lịch sử đã lưu, nhưng **ngữ cảnh của lượt đang dở** bị mất khi bộ não khởi động lại (đổi khoá/URL/nhà cung cấp/dự phòng). Đổi model thì giữ nguyên.

**Vì sao bấm "Dùng cái này" mà trợ lý vẫn báo thiếu khoá?**
Kiểm tra bạn đã **đóng bảng Cài đặt** chưa; thay đổi áp dụng khi đóng. Sau đó hỏi lại. Nếu vẫn lỗi, mở Trung tâm AI xem dòng đó có nhãn **Đang dùng** và **Đã lưu API key** không.

**Tôi muốn xoá hẳn một khoá.**
Mở nhà cung cấp → **Xoá key đã lưu** → **Lưu**. Hoặc xoá cả nhà cung cấp; các vai trò/dự phòng trỏ tới nó cũng được gỡ.

**Model nào trả lời tiếng Việt tốt?**
DeepSeek Chat, Claude Sonnet, Gemini Flash và GPT-4o-mini đều dùng được. Mô hình quá nhỏ (chạy Ollama trên máy yếu) thường trả lời lẫn tiếng Anh hoặc gọi công cụ sai.

---

## 11. Liên quan

- [PERSONALIZATION.md](PERSONALIZATION.md) — đổi ngôn ngữ, nhân cách, hình dáng.
- [SECURITY.md](SECURITY.md) — khoá, quyền, WebSocket, giới hạn.
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md#3-lỗi-xác-thực-nhà-cung-cấp-ai) — lỗi 401/429/không mạng.
