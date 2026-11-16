# Ý tưởng phát triển VUDEV ANDEE 2 — 152 đề xuất

**Tài liệu nội bộ.** Viết ngày 2026-11-16, trên nhánh `main` của `vudev-andee2` (fork của `kuafuai/andee`).

**Mục tiêu.** Một danh sách việc *làm được*, cụ thể tới từng lớp và API, để trải nghiệm của một trợ lý AI cá nhân tiếng Việt tốt hơn — không phải danh sách mong ước chung chung. Mỗi ý tưởng trả lời bốn câu: làm ở đâu trong repo này, vì sao người dùng Việt muốn nó, tốn bao nhiêu, và hỏng thì hỏng thế nào.

## Cách đọc

- **Công sức:** `S` ≤ 3 ngày công; `M` 1–2 tuần; `L` ≥ 3 tuần, hoặc cần nghiên cứu / quyền hệ thống / app bên thứ ba / thoả thuận.
- **Rủi ro / ghi chú:** chỉ ghi cái *thật sự* chặn hoặc làm hỏng, không ghi "cần test".
- **Tên lớp** viết dạng `code` là lớp/hàm **đang có** trong repo. Chỗ nào phải dựng mới, tài liệu nói rõ "chưa có, cần thêm".
- Ý tưởng đánh dấu **◇** là **chỉ ở mức mockup**: làm được nhưng chưa thể làm thật hôm nay vì phụ thuộc app bên thứ ba, API không tồn tại, hoặc rủi ro pháp lý/phạm vi sản phẩm. Danh sách gom ở cuối.
- Mọi ý tưởng phải sống trong khuôn khổ **LICENSE** của repo (Apache-2.0 + điều kiện bổ sung: không white-label, file LICENSE phải đi cùng, muốn nhúng vào sản phẩm khác phải có văn bản cho phép của tác giả gốc KUAFUAI). Ý tưởng nào đụng tới "bán lại", "đổi thương hiệu", "nhúng vào app khác" đều không nằm trong tài liệu này.

## Bối cảnh kỹ thuật đang có

| Mảnh | Hiện trạng trong repo |
|---|---|
| Thân Android | `ScreenBodyService` dựng cả graph: `FloatingWindowUi` → `AudioIO` → `TtsController`/`AsrController` → `ScreenController` → `CommandDispatcher`; `BodyCoreService` giữ service, hai brain và scheduler. |
| Hai đường mạng | `BodyWsServer` (cổng 9008, trong LAN hoặc qua `adb forward`) và `BodyWsClient` (dial ra hub, gửi `register` kèm `ToolSchemas.all()`). Chung một envelope `{"type":"request","id":…,"method":…,"params":{…}}`. |
| Bộ não | `LocalBrain` (mặc định từ 2026-09-29), hoặc hub, hoặc CodeFlying — chọn qua `BrainConfig` (`BRAIN_LOCAL`/`BRAIN_HUB`/`BRAIN_CODEFLYING`); gọi DeepSeek qua `LlmClient`. `LocalPrompt` là system prompt; `ContextPruner`/`ContextArchive`/`HistoryTrim` giữ context trong ngân sách. |
| Công cụ | **112** schema trong `ToolSchemas` ↔ **99** method trong `CommandDispatcher` (tên nội bộ kiểu `screen.tap`, `device.vault.fill`, `note.remember`, `scene.enter`, `flow.run`, `dog.move`…). Thêm một tool nghĩa là sửa **cả hai** file, lệch một bên thì tool hoặc vô hình hoặc báo `unknown method`. |
| Sổ tay | `Notebook`/`NotebookBook`/`NotebookStore` — **một blob JSON đã mã hoá AES/GCM** (khoá trong Android Keystore) gồm `memories`, `todos`, `scenes`, `flows` và các section `self`, `plan`, `gear`, `guard`, `people`, `tasks`, `notify_rule`, `wiki`, `drive`. `Vault` là kho bí mật riêng (cùng kiểu mã hoá nhưng prefs và key alias riêng), bí mật **không bao giờ** đi qua WebSocket. **Chưa có đường xuất** cho sổ tay. |
| Bối cảnh & luồng | `Scene`/`ScenesUi`/`ScenePresets` (đã có bản tiếng Việt), `Flow`/`FlowRecorder`/`FlowPlayer`/`FlowRuntime`/`FlowStep`. |
| Đồng hồ | `Scheduler` + `AlarmReceiver` + `CronExpr`; hai alarm đặt lịch là `todo` (việc tới hạn) và `sweep` (rà soát ban đêm), cộng receiver `BOOT` để arm lại sau khi khởi động. |
| Bạn bè | `PeerQr` (ghép bằng QR, ghim khoá), `PeerProtocol` (ECDH + chữ ký), `PeerLink`, `PeerDiscovery` (mDNS + nhớ địa chỉ cuối), `PeerStore.Trust` (`OWN`/`FRIEND`), `PeerTasks`, `GuestHost`. |
| Bộ nhớ dài hạn | `Selfhood` (self/cares/diary), `People` + `Source`, `WikiBook`, `Plans` (directions + day plan), `Tasks` + `NotifyTasks`, `NudgeBook`. |
| Giọng nói | `WakeWord`/`WakeMic`/`WakeTemplates` (MFCC + DTW tự viết), `AsrEngine`/`GoogleAsr`/`HuoshanAsr`, `TtsEngine`/`SystemTts`/`HuoshanTts`/`GoogleTts`; `SystemTts` đã map `Locale("vi","VN")`. |
| Tiếng Việt | `values-vi/strings.xml` 940/941 chuỗi, `locales_config.xml` đã khai `vi`, `AppLocale`, `ScenePresets` và `VoiceExitCommand` đã có câu tiếng Việt. Phần còn lại của tài liệu là những chỗ *chưa* có. |
| Thân Mac | `desktop/`: `DesktopBrainHost`, `MacScreen`, `MacVoice`, `MacDevice`, `MacGear`, `ShellTools` + `ShellPolicy` + `SandboxProfile` + `RiskClassifier` + `ApprovalCard`, `WorkLog`, `TimelineEvents`. |

**Cập nhật 2026-11-16:** `ProviderStore` **đã được thêm** trong đợt phát triển này (app/src/main/java/net/kuafuai/andee/config/ProviderStore.kt), cùng `AiProviders`/`WireAdapters`/`ProviderEndpoint` (core/src/main/kotlin/net/kuafuai/andee/brain/provider/) và `ProviderUi` (giao diện Trung tâm AI). Trước đó chỉ có `BrainConfig`, `VoiceSettings` và ba mode brain. Nhóm 5 vì vậy mô tả cả phần đã có và phần còn thiếu.

---

## 1. Giao diện & trải nghiệm

**1. Dọn nốt chuỗi hiển thị còn viết cứng trong Kotlin**
- **Mô tả:** Còn vài chỗ chữ người dùng thấy được nằm thẳng trong code chứ không qua `strings.xml`, ví dụ nhãn nút "中" trong `LangToggle`/`SettingsUi`, các câu mẫu tiếng Trung trong `PhoneWorkLog`, chú thích tiếng Trung lọt vào câu `device event` trong `BodyCoreService`.
- **Vì sao:** Người dùng Việt gặp một chữ Hán giữa giao diện tiếng Việt sẽ mất niềm tin vào "bản Việt hoá"; đây là loại lỗi rẻ nhất để sửa và dễ thấy nhất.
- **Kỹ thuật:** Rà `app/src/main/java` tìm literal có ký tự CJK nằm ngoài comment; đưa vào `values-vi`/`values`, đọc qua `AppLocale`. Riêng các chuỗi **model đọc** (kết quả tool) thì giữ tiếng Anh theo quy ước repo, đừng dịch.
- **Công sức:** S
- **Rủi ro / ghi chú:** Comment tiếng Trung trong code **không** phải lỗi, đừng đụng. Chuỗi so sánh với text của app khác (ví dụ `CN_CALL_KEYWORDS`) cũng không được dịch — dịch là hỏng tính năng.

**2. Mặc định tiếng Việt ở lần chạy đầu, và một bước "chọn giọng" trong onboarding**
- **Mô tả:** `OnboardingUi` đang cho chọn ngôn ngữ; đổi thứ tự để tiếng Việt là lựa chọn đầu và thêm bước nghe thử 3 giọng đọc tiếng Việt trước khi vào app.
- **Vì sao:** Người dùng Việt mua một sản phẩm Việt sẽ không đi tìm nút đổi ngôn ngữ; lần đầu nghe sai giọng là lần đầu mất thiện cảm.
- **Kỹ thuật:** `OnboardingUi` + `OnboardingState` + `AppCompatDelegate.setApplicationLocales` qua `AppLocale`; phát thử bằng `TtsController`/`SystemTts` với `Locale("vi","VN")`. Không đụng `VoiceConfig` defaults nếu lead chưa cho phép.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng hứa "giọng tự nhiên" nếu máy chỉ có TTS hệ thống; câu chọn giọng phải nói rõ nguồn.

**3. Thanh trượt "bóng to / bóng nhỏ / bóng trong suốt" ngay trong bảng điều khiển nổi**
- **Mô tả:** Cho người dùng kéo kích thước và độ mờ của bóng trực tiếp, thay vì phải vào `SettingsUi` tìm.
- **Vì sao:** Bóng nổi là thứ duy nhất luôn hiện; trên màn hình nhỏ nó che nội dung, trên tablet nó lại quá bé.
- **Kỹ thuật:** `FloatingWindowUi` (WindowManager params) + `BallLook`/`BallLooks.byName`; lưu vào `VoiceConfig`-style prefs hoặc section `gear`. Kéo bằng `onTouch` sẵn có của bóng, không cần activity mới.
- **Công sức:** M
- **Rủi ro / ghi chú:** Bóng càng trong càng dễ lỡ chạm; phải giữ vùng chạm tối thiểu 48dp. Đụng `FloatingWindowUi` là đụng file nhiều người sửa — cần phối hợp.

**4. Chế độ một tay: dồn nút và bong bóng xuống nửa dưới màn hình**
- **Mô tả:** Một công tắc "dùng một tay" làm bong bóng phụ đề, nút mic và nút dừng dồn vào vùng ngón cái với được, trên máy cao.
- **Vì sao:** Điện thoại Việt Nam phổ biến là máy 6.5–6.8 inch; một tay với lên góc trên là không tới.
- **Kỹ thuật:** `FloatingWindowUi` + `WindowMetricsCompat` (đã có lớp tương thích số đo cửa sổ) + `HomeSwipeRegion` để tránh vùng vuốt home.
- **Công sức:** M
- **Rủi ro / ghi chú:** Trên máy có cử chỉ hệ thống, vùng đáy là của hệ thống — phải chừa inset, nếu không người dùng vuốt là thoát app.

**5. Phụ đề to, viền tương phản, đọc được ngoài trời**
- **Mô tả:** Bong bóng phụ đề có ba cỡ chữ, viền đen mảnh và nền bán trong suốt để đọc được dưới nắng.
- **Vì sao:** Người dùng hay dùng khi đang đi đường, ngoài trời; phụ đề hiện tại chìm vào nền sáng.
- **Kỹ thuật:** Đường `onBrainProgress`/`onBrainFinal` trong `BodyCoreService` đẩy chữ xuống `FloatingWindowUi`; style trong `Glass`/`Theme`. `ScrollbackLimits` giữ số dòng.
- **Công sức:** S
- **Rủi ro / ghi chú:** Tăng cỡ chữ làm dài dòng và che màn hình — cần tự rút ngắn sau 2 dòng.

**6. Bảng lệnh nhanh (quick panel) mở từ bóng**
- **Mô tả:** Nhấn giữ bóng mở một lưới 6–8 lệnh hay dùng nhất ("đọc màn hình", "tóm tắt", "ghi chú", "dừng", "im lặng", "gọi người thân"), mỗi lệnh chạy không cần nói.
- **Vì sao:** Phần lớn việc lặp lại hằng ngày không đáng nói một câu; nói cũng phải chờ ASR.
- **Kỹ thuật:** `FloatingWindowUi` + một view lưới mới (chưa có) gọi thẳng `CommandDispatcher.dispatch` với method nội bộ (`screen.ui_tree`, `note.todo`…), không qua model. `Earcon` báo đã nhận.
- **Công sức:** M
- **Rủi ro / ghi chú:** Lệnh không qua model thì không có "suy nghĩ" — chỉ dùng cho hành động đã chắc nghĩa. Ba chạm liên tiếp trên bóng đã là `task.stop`, đừng chiếm cử chỉ đó.

**7. "Đang làm gì" hiện ngay trên bóng bằng một dòng chữ nhỏ**
- **Mô tả:** Khi `LocalBrain` chạy nhiều bước, bóng hiện dòng ngắn "đang mở Zalo…", "đang đọc màn hình…", lấy từ tên tool đang gọi.
- **Vì sao:** Người dùng không biết nó đang bận hay đang treo; đây là câu hỏi phổ biến nhất với agent chạm màn hình.
- **Kỹ thuật:** `LocalBrain` đã có `onProgress`; `WorkCardView`/`PhoneWorkLog`/`BrainTrace` đã có dữ liệu. Chỉ cần map tên tool → câu tiếng Việt trong một bảng dịch nhỏ.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng hiện tên tool thô (`get_screen_element`) cho người dùng thường.

**8. Xem lại hội thoại theo ngày, tìm được bằng tiếng Việt không dấu**
- **Mô tả:** Màn hình lịch sử có nhóm theo ngày và ô tìm kiếm hiểu "zalo" khớp "Zalo", "zâlo", "zalo".
- **Vì sao:** Người dùng Việt gõ không dấu rất nhiều, nhất là trên bàn phím nhanh; không tìm được là tính năng chết.
- **Kỹ thuật:** `HistoryListView` + `ChatHistory` (rows/topics đã có), thêm hàm chuẩn hoá bỏ dấu (dùng `java.text.Normalizer` NFD + bỏ dấu kết hợp + `đ→d`). `MarkdownBlocks` để render kết quả.
- **Công sức:** M
- **Rủi ro / ghi chú:** `đ` không tách được bằng NFD, phải map tay. Đừng đụng định dạng lưu trên đĩa.

**9. Thẻ "việc đang chạy" có nút Dừng và Tiếp tục đặt cạnh nhau**
- **Mô tả:** Một thẻ nhỏ luôn hiện khi có tác vụ dài, hai nút "Dừng" và "Tiếp tục" cỡ ngón tay.
- **Vì sao:** `LocalBrain` có `Unfinished`/`stopUnfinished` (dừng rồi nói "tiếp tục" là tìm lại được), nhưng UI chưa chỉ ra điều đó; người dùng tưởng dừng là mất.
- **Kỹ thuật:** `WorkCardView` + `PhoneWorkLog`; nút Dừng gọi `task.stop` qua `BodyCoreService`, nút Tiếp tục gọi `LocalBrain.submit` với câu "tiếp tục".
- **Công sức:** S
- **Rủi ro / ghi chú:** Hiện nút Tiếp tục khi thật sự còn `Unfinished`, nếu không sẽ hứa suông.

**10. Bóng tự né vùng có phím, né cạnh màn hình, và nhớ chỗ đứng theo từng app**
- **Mô tả:** Bóng nhớ vị trí từng app (Zalo, Facebook, Chrome) và không đậu lên thanh phím hay nút điều hướng.
- **Vì sao:** Bóng nằm đúng chỗ cần bấm là lý do người dùng gỡ app.
- **Kỹ thuật:** `FloatingWindowUi` + `HomeNavigationTracker` + `WindowMetricsCompat` (đã có để đọc inset); lưu map `pkg → (x,y)` trong prefs.
- **Công sức:** M
- **Rủi ro / ghi chú:** Xung đột với `passthroughForGesture` khi đang thao tác — chỉ đổi vị trí khi rảnh.

**11. Màn hình chờ kiểu "đồng hồ + lời nhắc" khi cắm sạc**
- **Mô tả:** Cắm sạc qua đêm thì app hiện một màn hình tối giản: giờ, thời tiết, việc sáng mai, và khuôn mặt bóng đang "ngủ".
- **Vì sao:** Điện thoại cắm sạc qua đêm là lúc duy nhất nó rảnh; người dùng Việt hay để máy cạnh giường.
- **Kỹ thuật:** `LaunchPadActivity`/`StandAside` (đang có màn hình phụ) + `GlBackend` cho mặt bóng + dữ liệu từ `Plans`. Cần một `BroadcastReceiver` cho `ACTION_POWER_CONNECTED`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Màn hình sáng cả đêm tốn pin và dễ cháy màn hình AMOLED; phải tắt sau N phút và giảm sáng.

**12. Chủ đề theo giờ Việt Nam và theo ảnh nền người dùng chọn**
- **Mô tả:** Sáng/chiều/tối đổi màu bóng và nền thẻ; người dùng chọn ảnh nền thì app lấy màu chủ đạo làm màu thẻ.
- **Vì sao:** Giao diện đổi theo ngày làm app có cảm giác "sống", rẻ, và không ảnh hưởng logic.
- **Kỹ thuật:** `Theme` + `Backdrop` + `Glass`; lấy màu chủ đạo bằng `androidx.palette` (nếu chưa có dependency thì tự downsample bitmap).
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng thêm dependency mới cho việc này; repo đang giữ dependency tối thiểu.

**13. Cử chỉ trên bóng có "menu cử chỉ" để người dùng tự chọn**
- **Mô tả:** Cho người dùng gán: chạm = nói, chạm hai = bật/tắt nghe, vuốt = ẩn, nhấn giữ = bảng lệnh nhanh — và xem trước bằng hoạt cảnh.
- **Vì sao:** Mỗi người một thói quen; cử chỉ cố định làm người thuận tay trái hoặc người lớn tuổi bối rối.
- **Kỹ thuật:** `FloatingWindowUi` (đang xử lý `onTouch`) + `SettingsUi.segmented`; lưu map cử chỉ trong prefs. Ba chạm = `task.stop` phải giữ nguyên.
- **Công sức:** M
- **Rủi ro / ghi chú:** Cử chỉ trùng nhau là bug khó tái hiện; cần một hằng số bảng cử chỉ ở một chỗ duy nhất.

**14. Ảnh chụp màn hình có đánh dấu điểm vừa chạm, để người dùng kiểm tra lại**
- **Mô tả:** Sau mỗi tác vụ, người dùng mở được ảnh màn hình kèm dấu chấm ở nơi nó đã chạm và nhãn phần tử nó nghĩ là đúng.
- **Vì sao:** "Nó bấm sai chỗ" là phàn nàn số một với agent điều khiển màn hình; ảnh có dấu giúp người dùng chỉ đúng chỗ sai thay vì tranh cãi.
- **Kỹ thuật:** `TapMarkerUi` + `ScreenCapture` + `GroundingLog` (đã ghi lại điểm chạm và cách tìm ra). Chỉ cần một màn xem ảnh + overlay.
- **Công sức:** S
- **Rủi ro / ghi chú:** `GroundingLog` có thể chứa text nhạy cảm trên màn hình — phải xoá theo `ChatHistory.wipe`.

**15. Một công tắc "im lặng hoàn toàn": chỉ rung và chấm đỏ, không đọc to**
- **Mô tả:** Bật là tắt TTS và âm báo, chỉ còn rung + `RedDot` khi có kết quả; bật lại bằng một chạm.
- **Vì sao:** Đang họp, đang trong lớp, đang ở nơi công cộng — người dùng cần im *ngay*, không phải mở Settings ba lớp.
- **Kỹ thuật:** `TtsController` (đã có logic tắt khi meeting/call qua `AudioFocusGate`/`CallState`) + `Earcon`; thêm cờ người-dùng-chọn vào `VoiceConfig`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng để "im lặng" làm mất luôn phụ đề — im tiếng chứ không im chữ.

**16. Nhãn trạng thái bóng bằng tiếng Việt ("đang nghe", "đang nghĩ", "đang chạy", "đang chờ bạn")**
- **Mô tả:** Bốn trạng thái có câu tiếng Việt ngắn đi kèm hoạt cảnh, thay vì chỉ đổi màu.
- **Vì sao:** Người dùng không đọc được "màu xanh là gì"; một câu chữ rõ ràng giảm cảm giác "nó treo".
- **Kỹ thuật:** `SpeechMood` (đã quyết định biểu cảm), `EmotionState`/`Mood`/`BallLookLabel`, chuỗi trong `values-vi`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Trạng thái phải khớp sự thật — `SpeechMood` có sẵn quy tắc "đừng nói cảm xúc khi không có"; đừng thêm nhãn "đang nghĩ" khi nó thật ra đang chờ mạng.

---

## 2. Trợ lý & hội thoại

**17. Trả lời ngắn hay dài theo ngữ cảnh, không theo một cỡ**
- **Mô tả:** Câu hỏi một dòng thì trả lời một dòng; việc cần hướng dẫn thì có bước. Không có chỗ nào trong giao diện buộc người dùng chọn "ngắn/dài".
- **Vì sao:** Câu trả lời ba đoạn cho câu hỏi "mấy giờ rồi" là lý do người ta bỏ trợ lý giọng nói; còn việc phức tạp mà trả lời cụt thì vô dụng.
- **Kỹ thuật:** `LocalPrompt` (một quy tắc mới trong §giọng điệu) + `SpeechMood.strip` để bỏ nhãn cảm xúc trước khi đọc. Có thể cho model tự chọn độ dài qua `show_html` khi cần dài.
- **Công sức:** S
- **Rủi ro / ghi chú:** System prompt là prefix được cache — thêm chữ là mất cache, cân nhắc gộp vào lần sửa prompt khác.

**18. Khi mơ hồ thì hỏi lại bằng nút tiếng Việt, thay vì đoán**
- **Mô tả:** Gặp câu như "chuyển cho nó đi", trợ lý dùng `ask_user` với 2–3 lựa chọn tiếng Việt ("chuyển cho chị Lan", "chuyển cho anh Hùng", "để sau").
- **Vì sao:** Đoán sai trong tác vụ có tác động thật (gửi tiền, gửi tin) đắt hơn nhiều so với hỏi một câu.
- **Kỹ thuật:** `CommandDispatcher` nhánh `ui.ask` (đã có, có label nút qua `AppLocale`) + một quy tắc trong `LocalPrompt` về ngưỡng mơ hồ; dữ liệu người để chọn lấy từ `People`.
- **Công sức:** S
- **Rủi ro / ghi chú:** `ui.ask` chặn tới timeout — đừng hỏi ở turn nền (SWEEP/harvest) làm treo cả vòng.

**19. Chủ đề tự đặt tên tiếng Việt, và đọc lại được**
- **Mô tả:** Mỗi đoạn hội thoại dài được "cất" thành một chủ đề có tiêu đề tiếng Việt ("Đặt vé máy bay Đà Nẵng"), mở lại được từ lịch sử.
- **Vì sao:** `TopicSplitter` + `get_topic` đã có cơ chế, nhưng tiêu đề do model viết trong prompt tiếng Anh/Trung dễ ra tên nửa vời.
- **Kỹ thuật:** `TopicSplitter`/`LocalBrain` (`note.get_topic`) + `ChatHistory`; thêm luật "tiêu đề theo ngôn ngữ người dùng" trong `LocalPrompt`. Hiển thị trong `HistoryListView`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đổi cách đặt tên là đổi prompt — chỉ làm khi có đợt sửa prompt chung.

**20. Câu trả lời có trích nguồn từ sổ tay**
- **Mô tả:** Khi trả lời dựa vào điều đã ghi ("sinh nhật chị Lan 12/3"), câu trả lời kèm một dòng nhỏ "từ sổ tay · 12/02".
- **Vì sao:** Người dùng cần biết nó nhớ đúng hay nhớ nhầm; một trợ lý nhớ sai mà không nói nguồn là nguy hiểm.
- **Kỹ thuật:** `LocalBrain` đã dán "index block" sổ tay vào mỗi user message; thêm một tool `recall` trả kèm timestamp/section (đã có `note.recall`) và yêu cầu model nêu nguồn trong `LocalPrompt`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Trích nguồn là prompt-level, model có thể bỏ qua; UI chỉ nên hiện khi tool `note.recall` thật sự được gọi.

**21. Chế độ "cùng làm": nó chỉ dẫn từng bước, người dùng tự bấm**
- **Mô tả:** Với app nhạy cảm (ngân hàng, VNeID, Zalo), trợ lý không tự chạm mà tô sáng chỗ cần bấm và chờ người dùng bấm.
- **Vì sao:** Nhiều người dùng *muốn* được hướng dẫn chứ không muốn bị thao tác hộ — và với app tài chính thì đây là cách duy nhất an toàn.
- **Kỹ thuật:** `CommandDispatcher` (`screen.ui_tree` để biết phần tử, `TapMarkerUi`/`EdgeRippleUi` để tô sáng) + `FlowStep` kind `human` (đã có: chờ người dùng làm, timeout 60s) + `ask_user`. Không cần tool mới.
- **Công sức:** M
- **Rủi ro / ghi chú:** Không được để turn treo vô hạn; `FlowStep.humanTimeoutSec` và `WaitBudget` là chỗ đặt trần.

**22. Câu trả lời dài thành thẻ tóm tắt, bấm "đọc tiếp" mới mở trang**
- **Mô tả:** Quá 5 dòng thì phần còn lại gom vào một trang `show_html`, phụ đề chỉ giữ 5 dòng đầu.
- **Vì sao:** Bong bóng phụ đề che màn hình; câu trả lời dài trên overlay là trải nghiệm tệ nhất của app.
- **Kỹ thuật:** `FloatingWindowUi` + `MarkdownView`/`MarkdownBlocks` + `ArtifactsUi` + `HtmlActivity`. Chỉ là quyết định hiển thị ở `onBrainFinal`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Tự tạo trang là tạo `Entry` trong lịch sử — phải tính vào `MAX_PAGES` (100), đừng để đầy.

**23. Hiểu tiếng Việt không dấu và viết tắt trong câu người dùng**
- **Mô tả:** "mai nhac toi uong thuoc luc 8h" vẫn ra đúng việc; "ok", "uk", "dc", "ko" được hiểu là đồng ý/không.
- **Vì sao:** Người Việt gõ không dấu trên bàn phím nhanh và trong tin nhắn thoại chuyển văn bản rất thường; ASR cũng hay mất dấu.
- **Kỹ thuật:** Một hàm chuẩn hoá ở `LocalPrompt`/`LocalBrain` trước khi gửi (hoặc luật trong prompt), dùng `Normalizer` NFD; các từ đồng ý/không nên xử lý bằng bảng từ, không bằng model.
- **Công sức:** S
- **Rủi ro / ghi chú:** Bỏ dấu có thể đổi nghĩa ("má" / "ma", "cô" / "cơ") — chỉ dùng để *so khớp*, không viết lại câu người dùng.

**24. Câu trả lời biết người dùng đang ở app nào**
- **Mô tả:** Đang mở Zalo mà hỏi "trả lời nó đi" thì hiểu là trả lời tin trong Zalo; đang mở Chrome thì hiểu là trang đang xem.
- **Vì sao:** Người dùng nói tắt dựa vào ngữ cảnh; trợ lý không biết ngữ cảnh thì hỏi lại vô ích.
- **Kỹ thuật:** `screen.ui_tree`/`get_screen_element` đã trả package; thêm dòng "app đang mở" vào index của user message (chỗ đã dán index sổ tay/gear) trong `LocalBrain`. `AndroidGear` biết tên app.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đọc màn hình liên tục là quyền riêng tư — chỉ lấy khi người dùng vừa nói, và ghi rõ trong docs.

**25. Chế độ "giải thích cho người mới"**
- **Mô tả:** Một công tắc trong Settings làm mọi câu trả lời có thêm một câu giải thích thuật ngữ và một bước tiếp theo.
- **Vì sao:** Phần lớn người dùng Việt mới làm quen trợ lý AI; thuật ngữ kỹ thuật làm họ bỏ cuộc.
- **Kỹ thuật:** `SettingsUi.segmented` + một biến trong `VoiceConfig` + nhánh prompt trong `LocalPrompt` (đọc từ `BrainHost`).
- **Công sức:** S
- **Rủi ro / ghi chú:** Thêm biến vào prompt là mất cache mỗi lần đổi — chấp nhận được vì đổi rất ít.

**26. Chữ và giọng khớp nhau: đọc tới đâu, phụ đề sáng tới đó**
- **Mô tả:** Khi TTS đọc, phụ đề tô đậm đúng câu đang đọc và cuộn theo.
- **Vì sao:** Người dùng vừa nghe vừa liếc màn hình; lệch nhịp làm khó theo dõi, nhất là với người khiếm thính.
- **Kỹ thuật:** `TtsController` (`onStart`/`onRangeStart` của engine nếu có) + `FloatingWindowUi`/`MarkdownView`. Nếu engine không báo range, chia câu theo dấu chấm và ước lượng thời lượng.
- **Công sức:** M
- **Rủi ro / ghi chú:** `SystemTts` và `HuoshanTts` khác nhau về callback; phải có đường dự phòng "không tô được thì thôi".

**27. Banner "đã khôi phục cuộc trò chuyện trước" sau khi app bị hệ thống kill**
- **Mô tả:** `LocalBrain.restoreConversation` đã khôi phục ngữ cảnh; chỉ cần nói cho người dùng biết điều đó, kèm nút "bắt đầu mới".
- **Vì sao:** Nếu không nói, người dùng thấy nó "nhớ lung tung" và mất tin; nếu nói, họ hiểu vì sao nó nhắc lại việc cũ.
- **Kỹ thuật:** `ChatHistory` có dòng mở đầu do `restoreConversation` tạo; thêm một thẻ thông báo trong `FloatingWindowUi` đọc từ `session.json`. Nút "bắt đầu mới" gọi `ChatHistory.wipe` (chỉ phiên, đừng xoá sổ tay).
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng để nút đó xoá cả sổ tay/scene — chỉ xoá scrollback.

**28. Nói chen ngang khi nó đang chạy: trợ lý dừng đúng cách và trả lời câu mới**
- **Mô tả:** Người dùng nói câu khác giữa lúc tác vụ đang chạy thì nó dừng tác vụ, trả lời câu mới, và nói rõ "việc cũ chưa xong, muốn làm tiếp không".
- **Vì sao:** Chen ngang là hành vi tự nhiên; nếu nó lờ đi hoặc treo, người dùng phải chờ vô ích.
- **Kỹ thuật:** `LocalBrain` đã có `generation`/`submit`/`cancel` và `Unfinished`; `LocalBrain.answerDangling` xử lý tool call treo. Việc còn lại là một dòng hỏi trong câu trả lời + cập nhật `WorkCardView`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Chỉ `TurnKind.USER` được bump generation (đã có quy ước) — đừng vô tình làm turn nền huỷ câu người dùng.

**29. "Vì sao nó làm vậy" — nhật ký suy nghĩ đơn giản cho người thường**
- **Mô tả:** Một nút nhỏ trong thẻ việc mở ra bản tóm tắt: nó hiểu yêu cầu là gì, đã thử bước nào, hỏng ở đâu.
- **Vì sao:** Khi tác vụ sai, người dùng cần một lời giải thích để sửa câu lệnh, không cần log kỹ thuật.
- **Kỹ thuật:** `BrainTrace`/`TracePage` đã có dữ liệu; thêm một bước "dịch trace sang tiếng Việt" bằng chính model (một turn riêng, chỉ khi người dùng bấm) — hoặc map tay các loại bước.
- **Công sức:** M
- **Rủi ro / ghi chú:** Nếu dịch bằng model thì tốn tiền mỗi lần bấm; nên map tay trước, chỉ gọi model khi trace có lỗi.

**30. Học từ chỗ người dùng sửa lại câu trả lời**
- **Mô tả:** Khi người dùng nói "không phải, ý tôi là…", nó ghi lại một `self_note` ngắn về sở thích đó, có hỏi xác nhận.
- **Vì sao:** Mỗi lần sửa lại là một lần dạy miễn phí; không học thì tuần sau sai lại đúng chỗ đó.
- **Kỹ thuật:** `LocalBrain` nhận ra mẫu "không/ý tôi là/sai rồi" trong câu người dùng (so khớp, không cần model), rồi để model chủ động gọi `self_note` (đã có trong `LifeTools`). Cần `ask_user` xác nhận trước khi ghi.
- **Công sức:** L
- **Rủi ro / ghi chú:** Ghi bừa vào self là đầu độc bộ nhớ; phải có xác nhận và xem lại được trong `NotebookUi`.

---

## 3. Tự động hoá & tác vụ

**31. Bộ luồng dựng sẵn cho người Việt: đặt cơm, đặt xe, chấm công, đóng tiền**
- **Mô tả:** Thêm các `ScenePresets`/luồng mẫu tiếng Việt cho những việc lặp lại hằng ngày, mỗi mẫu có luồng ghi sẵn hoặc hướng dẫn từng bước.
- **Vì sao:** Người dùng không tự nghĩ ra "tôi có thể bảo nó làm gì"; mẫu dựng sẵn là cách dạy tính năng nhanh nhất.
- **Kỹ thuật:** `ScenePresets` (đã có bản tiếng Việt) + `Flow`/`FlowStep` trong `Notebook` section `flows`; giao ở `ScenesUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Luồng ghi sẵn dễ hỏng khi app đổi giao diện; phải chỉ rõ "mẫu này chỉ là gợi ý, lần đầu sẽ học lại". **◇** với các app bên thứ ba không có giao diện ổn định.

**32. Sửa luồng bằng lời, có xem trước thay đổi**
- **Mô tả:** "Lần sau đừng bấm quảng cáo, chờ 2 giây rồi hãy bấm" — luồng được sửa và hiện diff từng bước trước khi lưu.
- **Vì sao:** Luồng hỏng dần theo thời gian; nếu sửa phải ghi lại từ đầu thì người dùng bỏ luôn tính năng.
- **Kỹ thuật:** `flow.edit`/`flow.refit` đã có trong `CommandDispatcher` (tool `edit_flow`/`refit_flow`); thêm UI diff trước/sau ở `CardUi`/`FlowRuntime`.
- **Công sức:** M
- **Rủi ro / ghi chú:** `refit_flow` hiện là hành động của model; UI phải cho người dùng từ chối trước khi ghi đè.

**33. Luồng có điều kiện "nếu … thì …"**
- **Mô tả:** Một bước có thể rẽ nhánh theo câu trả lời của model ("nếu có nút Đặt thì bấm, không thì thoát và báo tôi").
- **Vì sao:** Thực tế app thay đổi (có/không khuyến mãi, có/không còn chỗ); luồng tuyến tính gãy ngay.
- **Kỹ thuật:** `FlowStep.kind` đã có `ai` (giao lại bước cho brain); thêm bước `branch` với điều kiện chữ và hai nhánh con trong `FlowRuntime`.
- **Công sức:** L
- **Rủi ro / ghi chú:** Rẽ nhánh làm luồng khó kiểm tra; phải giới hạn độ sâu và số bước, và luôn hiện "đang ở nhánh nào" cho người dùng.

**34. Chạy luồng theo lịch**
- **Mô tả:** "8h sáng mỗi ngày mở app chấm công và bấm vào ca" — luồng có thể được `Scheduler` gọi như một todo.
- **Vì sao:** Việc lặp theo giờ là loại tự động hoá có giá trị nhất và ít rủi ro nhất (không phải quyết định gì).
- **Kỹ thuật:** `Scheduler`/`AlarmReceiver` hiện chỉ có alarm `todo`; thêm kiểu todo "chạy flow id X" và nhánh trong `BodyCoreService` gọi `FlowRuntime`. `CronExpr` đã hỗ trợ biểu thức lặp.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chạy không người trông là rủi ro cao — chỉ cho luồng đã đánh dấu "an toàn khi tự chạy".

**35. Chia sẻ luồng cho người khác bằng QR**
- **Mô tả:** Xuất một luồng thành QR, người khác quét là có; kèm cảnh báo "luồng do người lạ tạo sẽ hỏi bạn từng bước".
- **Vì sao:** Chia sẻ mẹo dùng app là cách lan truyền tự nhiên của người dùng Việt (nhóm Zalo, Facebook).
- **Kỹ thuật:** `PeerQr` (đã có QR + ghim khoá cho ghép bạn) tái dùng cho payload luồng; hoặc `ScanActivity` + JSON luồng. Chạy bằng `FlowRuntime`, bọc `TurnGuards` như turn không tin cậy.
- **Công sức:** M
- **Rủi ro / ghi chú:** Luồng lạ có thể chứa bước nguy hiểm; phải hiện từng bước và bắt xác nhận. Không chia sẻ được bí mật trong `Vault`.

**36. Kịch bản theo nơi: đến công ty thì im thông báo, về nhà thì đọc to**
- **Mô tả:** Một bối cảnh có thể có điều kiện vị trí ("ở nhà", "ở công ty") thay vì chỉ theo app đang mở.
- **Vì sao:** `Scene.triggerApps` chỉ bắt được app; phần lớn ngữ cảnh thật là nơi chốn.
- **Kỹ thuật:** tool `get_location` đã có (`device.location`), cộng thêm geofence đơn giản bằng khoảng cách tới một toạ độ đã lưu; nhánh vào scene ở `ScenesUi`/`Notebook`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Quyền vị trí nền rất nhạy cảm; chỉ kiểm tra khi có sự kiện (mở app, tới giờ) chứ không theo dõi liên tục.

**37. Lịch âm, ngày rằm, giỗ và ngày lễ Việt Nam trong kế hoạch**
- **Mô tả:** Nhắc giỗ, rằm, Tết, ngày lễ; câu "mai rằm" được hiểu đúng.
- **Vì sao:** Đây là loại ngày mà trợ lý nước ngoài không bao giờ có, và người Việt thật sự cần nhắc trước.
- **Kỹ thuật:** `Plans`/`Scheduler` + `CronExpr`; cần một bảng chuyển âm lịch (thuật toán, không cần API) đặt trong `config/`. `day_plan` đọc bảng này khi lập kế hoạch.
- **Công sức:** M
- **Rủi ro / ghi chú:** Thuật toán âm lịch Việt Nam khác Trung Quốc ở một số năm (múi giờ + lịch); phải dùng bảng của Việt Nam, đừng copy công thức TQ.

**38. Việc đang dở sống qua lần khởi động lại**
- **Mô tả:** Tác vụ nhiều bước bị kill giữa đường thì lần sau mở app vẫn thấy "việc này chưa xong", kèm nút làm tiếp.
- **Vì sao:** Android kill app nền liên tục trên máy ít RAM; mất tác vụ giữa đường là trải nghiệm tệ nhất.
- **Kỹ thuật:** `Tasks` (config/Tasks) + `restoreConversation` (đã khôi phục hội thoại nhưng **không** khôi phục bước của task — CLAUDE.md nói rõ điều này). Cần lưu `WorkStep`/`Tasks` xuống `Notebook` sau mỗi bước, và UI ở `WorkCardView`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Khôi phục bước trên màn hình khác là nguy hiểm — phải bắt đầu lại từ `screen.ui_tree`, đừng tin bước cũ.

**39. Tác vụ dài có ảnh từng bước để xem lại**
- **Mô tả:** Mỗi bước lưu ảnh chụp nhỏ + nó nghĩ gì, gom vào một trang "nhật ký việc" xem lại được.
- **Vì sao:** Khi tác vụ sai, người dùng cần biết sai ở bước nào chứ không phải nghe "xin lỗi, tôi không làm được".
- **Kỹ thuật:** `ContextArchive` đã lưu *chữ* (không ảnh, để tránh rò rỉ); nên làm riêng một thư mục ảnh bước trong `filesDir`, xoá theo `ChatHistory.wipe`. UI bằng `TracePage`/`HtmlActivity`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ảnh màn hình có thể chứa OTP, số tài khoản — **không** lưu ảnh của màn hình ngân hàng/OTP; dùng `GearCategories` (`finance`) để nhận diện.

**40. Bước nhạy cảm thì trả quyền lại cho người dùng**
- **Mô tả:** Trước khi bấm "Xác nhận chuyển tiền", nó dừng, đưa màn hình về tay người dùng và nói rõ đang chờ gì.
- **Vì sao:** Không có API nào đảm bảo an toàn bằng việc để người thật bấm nút cuối.
- **Kỹ thuật:** `TurnGuards` (chặn theo nguồn turn) + `FlowStep.human` + `ask_user`; `ToolSchemas` mô tả `fill_secret` đã cảnh báo không dùng được với bàn phím native của app ngân hàng.
- **Công sức:** S
- **Rủi ro / ghi chú:** Phải là **guard trong code**, không chỉ là câu trong prompt — cùng lý do `Guard` ghi rõ: lời trong prompt không bảo đảm gì.

**41. Mẫu "mỗi sáng làm hộ tôi"**
- **Mô tả:** Một câu lệnh tạo ra chuỗi việc hằng ngày: đọc thời tiết, nhắc lịch, nhắc uống thuốc, tóm tắt tin nhắn quan trọng.
- **Vì sao:** Người dùng không tự lập trình được `Plans`; một mẫu sẵn có biến "trợ lý" thành thói quen.
- **Kỹ thuật:** `Plans` (`Plans.Item` kind `remind`/`ask`) + `Scheduler` (bubble todo không gọi model) + `NudgeUi`/`NudgeBook`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Mỗi mục thêm vào là một lần đánh thức máy; tổng số bubble/ngày phải có trần (đã có ngân sách trong doc `proactive-nudge`).

**42. Học từ thao tác người dùng tự làm**
- **Mô tả:** Người dùng bấm tay một chuỗi thao tác và bấm "ghi lại"; lần sau có thể bảo nó làm hộ.
- **Vì sao:** Cách dạy tự nhiên nhất là "làm mẫu", không phải viết mô tả.
- **Kỹ thuật:** `FlowRecorder`/`FlowPlayer` đã có `record_flow_start`/`record_flow_stop`; `RecordChipUi` đã là chip ghi. Thiếu là một đường vào từ cử chỉ người dùng (không qua model) và màn duyệt bước.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ghi lại mọi chạm trên máy là giám sát — chỉ bật khi người dùng chủ động bấm và hiện rõ đang ghi.

**43. Hàng đợi việc: xếp nhiều việc, làm lần lượt, xem được thứ tự**
- **Mô tả:** "Nhớ mua sữa, rồi nhắc tôi gọi mẹ, xong thì tắt đèn" — ba việc xếp hàng, hiện danh sách và cho kéo thứ tự.
- **Vì sao:** Người dùng nói một hơi nhiều việc; hiện tại chỉ việc đầu được hiểu, phần sau dễ mất.
- **Kỹ thuật:** `Tasks` + `Todo`/`todo` (đã có trong `ToolSchemas`) + `Tasks` UI trong `WorkCardView`. **Không** chạy song song: `CommandDispatcher` và `passthroughForGesture` không reentrant, `LocalBrain` cố ý chạy tool tuần tự.
- **Công sức:** M
- **Rủi ro / ghi chú:** Song song hai tác vụ chạm màn hình là không thể trong kiến trúc này — nói thẳng trong UI ("một việc một lúc").

**44. Nhắc việc theo ngữ cảnh app**
- **Mô tả:** "Khi nào mở Zalo thì nhắc tôi trả lời chị Lan" — nhắc đúng lúc mở app, không phải theo giờ.
- **Vì sao:** Nhắc theo giờ thường đến lúc không làm được; theo ngữ cảnh thì hữu ích hơn nhiều.
- **Kỹ thuật:** `NotifyRule` (`notify_rule`) đã có luật thông báo; `NotificationTriage` nhận diện app; thêm điều kiện "app vào foreground" (dùng `UsageStatsManager` như `AndroidGear.hasUsageAccess`).
- **Công sức:** M
- **Rủi ro / ghi chú:** Theo dõi foreground liên tục là pin + quyền riêng tư; chỉ kiểm tra khi có sự kiện hệ thống.

---

## 4. Bộ nhớ & cá nhân hoá

**45. Sổ tay có mục lục và tìm kiếm tiếng Việt không dấu**
- **Mô tả:** `NotebookUi` hiện là các section; thêm mục lục theo chủ đề (người, việc, sở thích) và ô tìm kiếm bỏ dấu.
- **Vì sao:** Sổ tay càng nhiều càng khó tìm; người dùng không nhớ nó lưu ở section nào.
- **Kỹ thuật:** `NotebookUi` + `Notebook.indexForPrompt`; tìm kiếm trên JSON của `NotebookStore` bằng cùng hàm normalizer như ý 8. Không đổi schema lưu.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đừng dựng index tìm kiếm riêng trên đĩa — thêm một nguồn sự thật là thêm chỗ lệch.

**46. "Tôi đã nói gì với nó" — đọc lại phần hội thoại đã bị cất**
- **Mô tả:** Người dùng mở được các đoạn `read_earlier` (context đã nén) ngay trong app, không phải gõ lệnh.
- **Vì sao:** `ContextArchive` đã lưu nhưng chỉ model đọc được; người dùng có quyền xem ký ức của chính mình.
- **Kỹ thuật:** `ContextArchive` (`filesDir/context`) + `note.earlier` (`read_earlier`) + một màn trong `HistoryListView`/`TextPage`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Archive cố ý không chứa kết quả `vault`/`sms`/`notification`; đừng "cải thiện" bằng cách thêm vào.

**47. Hồ sơ người thân: sinh nhật, sở thích, lần cuối gặp**
- **Mô tả:** Mỗi người một hồ sơ gọn (từ `People`), có nhắc trước sinh nhật và gợi ý quà dựa trên điều đã ghi.
- **Vì sao:** Đây là loại việc trợ lý cá nhân làm tốt hơn lịch điện thoại; người Việt coi trọng quan hệ gia đình và ngày giỗ.
- **Kỹ thuật:** `People` + `Source` (biết ai nói) + `Plans` (`remind` trước N ngày) + `Scheduler`; UI ở `NotebookUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Dữ liệu về người khác là dữ liệu nhạy cảm — phải xuất/xoá được từng người và không đưa sang peer.

**48. Wiki cá nhân tiếng Việt, xuất ra Markdown**
- **Mô tả:** Nó tự viết các trang kiến thức nhỏ ("cách huỷ gói cước Viettel") và người dùng xuất được thành file `.md`.
- **Vì sao:** Kiến thức nó học được nên dùng lại được ngoài app, và người dùng cần cảm giác "đây là của tôi".
- **Kỹ thuật:** `WikiBook`/`Wiki` + `wiki_*` tools + `Markdown`/`MarkdownBlocks`; chia sẻ qua `open_url` + FileProvider, hoặc `HtmlActivity` xuất HTML.
- **Công sức:** M
- **Rủi ro / ghi chú:** Wiki có thể chứa thông tin nội bộ (số tài khoản…) — xuất file phải có bước rà tương đương `Redactor` (Mac đã có `Redactor`, phone chưa).

**49. Nhật ký theo ngày, tự viết, người dùng đọc lại**
- **Mô tả:** Cuối ngày nó viết một đoạn ngắn "hôm nay bạn làm gì" từ những gì đã xảy ra, người dùng mở xem như một cuốn nhật ký.
- **Vì sao:** `diary` đã có nhưng chưa có chỗ đọc cho người dùng; đây là tính năng giữ chân mạnh với người muốn "có người đồng hành".
- **Kỹ thuật:** `Selfhood` (diary) + turn `SWEEP` trong `LocalBrain` + `ChatHistory`/`TimelineEvents` (Mac); UI mới trong `NotebookUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Nhật ký do model viết có thể "bịa" cảm xúc; luật "chỉ viết điều đã xảy ra" phải nằm trong `LocalPrompt` và có nguồn cho từng dòng.

**50. Bí mật có hạn sử dụng: OTP không sống quá lượt đọc**
- **Mô tả:** Nếu mã OTP vào được kết quả tool, nó bị thay bằng `<redacted>` và xoá khỏi lịch sử ngay cuối lượt.
- **Vì sao:** Mã OTP lọt vào file text thuần là lỗi bảo mật thật; repo đã cố ý loại `sms`/`vault` khỏi `ContextArchive` nhưng đường khác vẫn còn.
- **Kỹ thuật:** `ContextArchive` (danh sách loại trừ) + `LocalBrain` hậu xử lý tool result + `ChatHistory` (không ghi row chứa mẫu OTP). Mẫu: 4–8 chữ số đứng một mình trong kết quả `get_notifications`/`read_sms`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Regex bắt nhầm số tiền/số điện thoại — chỉ áp cho kết quả từ `device.sms*` và notification.

**51. Hỏi xác nhận trước khi ghi vào bộ nhớ dài hạn**
- **Mô tả:** "Tôi hiểu bạn thích cà phê ít đường — ghi lại nhé?" [Ghi] [Không].
- **Vì sao:** Người dùng cần kiểm soát ký ức của mình; ghi sai mà không có đường sửa thì mất tin.
- **Kỹ thuật:** `remember`/`note.remember` + `ask_user` + `NudgeUi` cho câu hỏi không chặn; xem lại bằng `NotebookUi`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Hỏi quá nhiều làm phiền; chỉ hỏi với thông tin *về người* (sở thích, thói quen), không hỏi với sự kiện trong ngày.

**52. Xoá theo chủ đề, không phải xoá tất cả**
- **Mô tả:** "Xoá mọi thứ về công việc cũ" — nó liệt kê cái sẽ xoá, người dùng duyệt từng mục rồi xoá.
- **Vì sao:** Người dùng hay đổi việc, đổi mối quan hệ; chỉ có nút "xoá sạch" là quá thô.
- **Kỹ thuật:** `Notebook` section-level + `forget` + `ChatHistory.wipe`/`Scrollback.wipe`; UI danh sách có checkbox trong `NotebookUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Xoá là không hoàn tác — phải xem trước (dry run) và không xoá `guard` (chỉ người dùng sửa được).

**53. Xuất toàn bộ dữ liệu của tôi thành một file**
- **Mô tả:** Một nút xuất sổ tay + wiki + plans + lịch sử hội thoại ra file JSON/Markdown để người dùng giữ.
- **Vì sao:** "Dữ liệu của tôi" là lời hứa cơ bản của một trợ lý cá nhân; cũng là điều giúp người dùng dám dùng lâu dài.
- **Kỹ thuật:** `Notebook`/`NotebookBook` + `Vault` (không xuất bí mật, chỉ xuất nhãn) + `ContextArchive` + `ChatHistory`; ghi ra `filesDir` rồi chia sẻ bằng FileProvider.
- **Công sức:** M
- **Rủi ro / ghi chú:** Sổ tay hiện **cố ý không có đường xuất** (xem KDoc `Notebook`) — nên phần lớn công việc ở đây là dựng đường xuất có kiểm soát, không phải gọi API sẵn có. Có `FactoryReset` để học cách xoá; xuất phải loại secret trong `Vault` và ảnh màn hình.

**54. Nhập từ Google Keep / Google Notes**
- **Mô tả:** Người dùng chọn file xuất của Google Keep, nó chia thành các mục `remember` có ngày.
- **Vì sao:** Người dùng đã có sẵn ghi chú ở nơi khác; nhập được thì không phải nhập lại.
- **Kỹ thuật:** Màn nhập trong `NotebookUi` + parser JSON/HTML Takeout (offline, không cần API); ghi qua `NotebookStore`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ghi chú của người khác có thể rất dài; giới hạn số mục mỗi lần và hiện trước khi ghi.

**55. Sổ tay có ảnh (hoá đơn, giấy hẹn khám, giấy tờ)**
- **Mô tả:** Ghi chú có thể đính một ảnh; model chỉ thấy phần mô tả, không thấy ảnh gốc.
- **Vì sao:** Nhiều thứ người Việt cần nhớ là giấy tờ: hoá đơn, biên lai, giấy hẹn khám.
- **Kỹ thuật:** `NotebookStore` (thêm trường file path) + `take_screenshot`/`camera_turn` + `LlmClient` (vision nếu model hỗ trợ). Ảnh nằm trong `filesDir`, xoá theo `ChatHistory.wipe`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ảnh giấy tờ là dữ liệu nhạy cảm nhất; nên mã hoá cùng `Vault` hoặc ít nhất không đưa vào ngữ cảnh model.

**56. Cho người dùng xem "nó đang quan tâm điều gì"**
- **Mô tả:** Một mục trong Settings (đúng như doc `proactive-nudge` đã định: "những dự định của nó") liệt kê `cares`/`Plans.Direction`, cho xoá và tạm dừng.
- **Vì sao:** Một trợ lý tự nói trước mà người dùng không biết nó đang theo đuổi gì thì sẽ thấy đáng ngại.
- **Kỹ thuật:** `Plans` (`Direction`) + `Selfhood.cares` + `SettingsUi`; xoá/tạm dừng chỉ ghi tombstone (đã có `byUser`).
- **Công sức:** S
- **Rủi ro / ghi chú:** Doc thiết kế đã chốt "chỉ xoá và tạm dừng, còn lại nói với nó một câu" — đừng mở thành form sửa tự do.

**57. Bộ nhớ theo nơi chốn: "ở nhà", "ở công ty"**
- **Mô tả:** Sổ tay có thể gắn một mục với một nơi ("mật khẩu wifi ở nhà"), và câu trả lời ưu tiên đúng nơi.
- **Vì sao:** Người Việt hay có nhiều nơi sinh hoạt (nhà, quê, trọ, công ty); cùng một câu hỏi có đáp án khác nhau.
- **Kỹ thuật:** `Notebook` thêm trường `place` (một chuỗi, không cần geofence) + `get_location` khi cần; index block ở `LocalBrain`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Nếu gắn với toạ độ thì thành theo dõi vị trí — chỉ nên là nhãn người dùng tự đặt.

---

## 5. Đa nhà cung cấp AI & chi phí

**58. Lưu nhiều nhà cung cấp, chọn bằng một bảng, không sửa file**
- **Mô tả:** Người dùng thêm được OpenAI-compatible / Gemini / Qwen / một endpoint tự dựng, mỗi cái có tên riêng, và chuyển qua lại trong Settings.
- **Vì sao:** Định hướng sản phẩm là "đa nhà cung cấp AI"; trước đợt này `BrainConfig` chỉ có một bộ apiKey/baseUrl/model và `LlmClient` viết theo hình dạng DeepSeek.
- **Kỹ thuật:** **Đã làm một phần (2026-11-16):** `ProviderStore` (danh sách provider, provider đang dùng, dự phòng, role theo việc), `OpenAiWire`/`GeminiWire`/`AnthropicWire` thay cho một `LlmClient` chỉ biết DeepSeek, `ProviderUi` để quản lý. Còn thiếu: kiểm tra key định kỳ, chi phí theo token, đồng bộ nhiều thiết bị.
- **Công sức:** M
- **Rủi ro / ghi chú:** `LlmClient` đang xử lý riêng `reasoning_content` của DeepSeek (không được gửi lại) — tách interface mà làm mất chi tiết này là 400 ngay. `BrainConfig.sameBackend` quyết định có dựng lại brain (mất hội thoại) — rất dễ sai ở đây.

**59. Khoá API lưu trong Vault, không nằm trong file text thuần**
- **Mô tả:** Khoá của từng provider được mã hoá như bí mật, không ghi dạng text. **Đã làm (2026-11-16):** `SecretBox` (AES/GCM, khoá trong Android Keystore) bọc key provider và key thoại; bản plaintext cũ được migrate rồi xoá.
- **Vì sao:** Hiện `BrainConfig.apiKey` và `VoiceConfig` đi qua file prefs phẳng; khoá bị lộ là mất tiền thật.
- **Kỹ thuật:** `Vault` (AES/GCM + Keystore, API đã có cho entry `secrets`) + `BrainConfig` chỉ giữ *tên tham chiếu*; `Vault` được đọc ở `ScreenBodyService` khi dựng brain.
- **Công sức:** M
- **Rủi ro / ghi chú:** `Vault` hiện thiết kế cho bí mật *của người dùng để điền vào form*; dùng cho khoá API là mở rộng mục đích — phải kiểm tra `list_vault`/`get_vault` không làm lộ khoá cho model.

**60. Định tuyến theo việc: model nhỏ cho việc nhỏ**
- **Mô tả:** Việc phân loại thông báo, đặt tên chủ đề, tóm tắt một câu dùng model rẻ; tác vụ nhiều bước mới dùng model mạnh.
- **Vì sao:** Phần lớn lượt gọi model là việc nhỏ; dùng model mạnh cho tất cả là lý do chi phí không kiểm soát được.
- **Kỹ thuật:** `LlmClient` nhận thêm tham số "mức" (hoặc provider riêng); các call site đã tách biệt: `NotificationTriage`, `TopicSplitter`, `Heartbeat`, `PlanMind`, `ContextPruner` (không gọi model).
- **Công sức:** M
- **Rủi ro / ghi chú:** Model rẻ hiểu tiếng Việt kém hơn — phải đo trên chính câu tiếng Việt trước khi mặc định.

**61. Hiện token và chi phí mỗi ngày cho người dùng**
- **Mô tả:** Một dòng trong Settings: "hôm nay: 42 lượt, ~31k token vào, ~4k ra, ước tính 1.200đ".
- **Vì sao:** Chi phí API là nỗi lo thật của người dùng Việt; không thấy thì họ tắt tính năng.
- **Kỹ thuật:** `LlmClient` đã đọc usage trong response (kể cả `prompt_cache_hit_tokens`); cộng dồn theo ngày, lưu trong prefs, hiện ở `SettingsUi`. Đơn giá để trong một hằng số cấu hình.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng hiện con số "chính xác" khi chỉ là ước tính; đơn giá thay đổi thì con số cũ sai — ghi rõ "ước tính".

**62. Hạn mức chi tiêu tháng và chặn mềm**
- **Mô tả:** Người dùng đặt trần (ví dụ 50.000đ/tháng); gần trần thì cảnh báo, chạm trần thì chỉ cho việc nhỏ hoặc hỏi.
- **Vì sao:** Một agent tự chạy nhiều bước có thể đốt tiền nhanh; người dùng cần một cái phanh.
- **Kỹ thuật:** `LlmClient` (đếm usage) + `LocalBrain` (`max_steps`, mặc định `VoiceConfig.DEFAULT_MAX_STEPS` = 40) + `TurnGuards` để chặn turn nền trước.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chặn giữa turn làm model mất mạch; nên chặn ở **bắt đầu** turn và nói rõ "đã hết hạn mức tháng này".

**63. So hai model cho một câu khó**
- **Mô tả:** Với câu quan trọng (sức khoẻ, tiền), chạy hai provider và nếu khác nhau nhiều thì nói "hai nguồn không thống nhất, đây là điểm khác".
- **Vì sao:** Người dùng hỏi những câu mà sai thì đắt; một lần đối chiếu rẻ hơn một lần sai.
- **Kỹ thuật:** **Cần thêm** một đường "fan-out" quanh `LlmClient`; `LocalBrain` hiện giả định một client. Chỉ dùng cho lượt người dùng chủ động bấm "kiểm tra chéo".
- **Công sức:** L
- **Rủi ro / ghi chú:** Tốn gấp đôi và làm chậm; không được bật mặc định. **◇** ở mức sản phẩm.

**64. Tự chuyển provider khi provider chính lỗi hoặc timeout**
- **Mô tả:** Provider chính 5xx/timeout 3 lần thì lượt đó chuyển sang provider dự phòng và nói cho người dùng biết.
- **Vì sao:** Mạng Việt Nam tới API nước ngoài hay chập chờn; một lượt hỏng làm người dùng tưởng app hỏng.
- **Kỹ thuật:** `LlmClient` (retry/backoff hiện có) + danh sách provider dự phòng (cùng ý 58).
- **Công sức:** M
- **Rủi ro / ghi chú:** Đổi provider giữa lượt là đổi hình dạng message — chỉ đổi ở **đầu** lượt, khi chưa có tool call nào.

**65. Chế độ "rẻ nhất": trả lời không cần model cho câu đã biết**
- **Mô tả:** Các câu như "mấy giờ", "pin còn bao nhiêu", "hôm nay thứ mấy" trả lời bằng code, không gọi model.
- **Vì sao:** Đây là những câu hỏi nhiều nhất và model không cần thiết; bỏ được là giảm cả chi phí lẫn độ trễ.
- **Kỹ thuật:** `LocalBrain` đã có cơ chế intercept cho `note.fold`/`note.earlier`; thêm bảng câu → hàm cho các câu tất định. Dữ liệu từ `device.battery`, `System.currentTimeMillis`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Câu tự nhiên rất đa dạng; chỉ bắt khi khớp *toàn bộ* câu (giống `VoiceExitCommand`), khớp một phần là trả lời sai.

**66. Cấu hình zero-config cho người mới: dùng khoá dùng chung**
- **Mô tả:** Người dùng mới không phải dán API key; app dùng một khoá dùng chung kèm hạn mức, sau đó mời nâng cấp sang khoá riêng.
- **Vì sao:** Bước "dán API key" là chỗ rơi người dùng nhiều nhất; nhưng phải nói rõ hạn mức.
- **Kỹ thuật:** `VoiceConfig.codeFlyingKey`/`codeFlyingBuiltInKey` (đọc `assets/andee_key.txt`, đã có sẵn) + UI trong `OnboardingUi`; khoá riêng lưu qua `Vault` (ý 59).
- **Công sức:** S
- **Rủi ro / ghi chú:** Khoá nhúng trong APK là khoá bị lộ — chỉ dùng với quota chặt; đây là quyết định kinh doanh, không chỉ kỹ thuật.

**67. Ước lượng chi phí trước khi chạy việc dài**
- **Mô tả:** Trước một luồng 30 bước, nó nói "việc này khoảng 30 bước, ~15k token — chạy không?".
- **Vì sao:** Người dùng cần biết mình đang trả tiền cho cái gì; một câu hỏi rẻ hơn một lần chạy sai 30 bước.
- **Kỹ thuật:** `WorkStep`/`max_steps` + số bước của `Flow` + `ask_user`; ước lượng bằng số bước × token trung bình đo được từ `BrainTrace`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng hỏi với việc ngắn (dưới ~8 bước) — hỏi nhiều thành phiền.

**68. Cảnh báo khi context phình to, kèm nút "nén ngay"**
- **Mô tả:** Khi hội thoại chạm ngưỡng, hiện một dòng "cuộc trò chuyện dài — nén lại cho rẻ hơn?" và người dùng bấm nén.
- **Vì sao:** `ContextPruner` tự chạy nhưng người dùng không biết; tự động nén giữa lúc đang làm việc có thể làm mất mạch.
- **Kỹ thuật:** `ContextPruner`/`maybeHarvest` + `ChatHistory`; thêm cờ "chờ người dùng quyết" trước khi nén trong turn người dùng.
- **Công sức:** S
- **Rủi ro / ghi chú:** Nén thủ công cũng làm mất prefix cache — nói rõ trong câu hỏi.

**69. Nhật ký yêu cầu để tự kiểm toán, xuất được**
- **Mô tả:** Một file log các lượt gọi model (thời điểm, model, token, thời lượng, thành công/thất bại) xuất ra được cho người dùng hoặc dev.
- **Vì sao:** Khi chi phí tăng bất thường, cần biết cái gì gọi; `BrainTrace` đã có nhưng hướng dev.
- **Kỹ thuật:** `BrainTrace` + `LlmClient` usage + `Log`; xuất qua FileProvider. Không ghi nội dung hội thoại, chỉ metadata.
- **Công sức:** M
- **Rủi ro / ghi chú:** Log là nơi dễ rò rỉ — phải đi qua lớp lọc như `Redactor` bên Mac (`Redactor` chưa có bản phone).

---

## 6. An toàn & quyền riêng tư

**70. Bảng "quyền nào đang bật" kèm lối tắt tới đúng màn hình hệ thống**
- **Mô tả:** Một trang liệt kê trợ năng, ghi màn hình, thông báo, vị trí, danh bạ, SMS: đang bật hay tắt, và bấm là nhảy tới đúng chỗ trong Settings.
- **Vì sao:** Trợ năng bị hệ thống thu hồi là lỗi số một trên nhiều ROM; người dùng không biết vì sao nó "tự nhiên không làm gì".
- **Kỹ thuật:** `PermissionsController` + `SelfCheckUi`/`SelfCheckActivity` (đã có tự kiểm tra) + `PermissionRequestActivity`; nguồn câu giải thích là `docs/permissions.md`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng đòi quyền khi không cần — mỗi quyền thêm làm tỉ lệ gỡ app tăng.

**71. Danh sách "app không bao giờ tự bấm" mở rộng ra ngân hàng, ví, VNeID**
- **Mô tả:** Nó không tự thao tác trong app tài chính/hành chính; chỉ hướng dẫn hoặc điền form khi người dùng yêu cầu rõ.
- **Vì sao:** Repo đã có tiền lệ mạnh: v0.15.0 "điện thoại không bao giờ tự thao tác WeChat", vì một cú chạm tự động khoá người dùng khỏi tài khoản của chính họ. Ngân hàng còn tệ hơn.
- **Kỹ thuật:** `GearCategories` (đã có `FINANCE`/`ADULT`/`HEALTH`…) + `ScreenController`/`CommandDispatcher` chặn ở mức code (không phải prompt) + `TurnGuards`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chặn quá tay làm tính năng vô dụng ("mở app ngân hàng xem số dư" cũng bị chặn) — phải phân biệt *đọc* và *bấm*.

**72. OTP chỉ để điền, không bao giờ vào ngữ cảnh model**
- **Mô tả:** Mã OTP đọc từ thông báo/tin nhắn được điền thẳng vào ô đang focus; model chỉ nhận "đã điền 6 ký tự".
- **Vì sao:** Đây là mẫu thiết kế đã có cho `Vault` ("trả về độ dài, không trả về giá trị") và nên áp cho OTP.
- **Kỹ thuật:** `Vault`/`fill_secret` (nguyên tắc "bí mật không qua WebSocket") + `read_sms` + `get_notifications` + `A11yIme`/`ScreenController` để gõ.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đọc OTP là quyền rất nhạy cảm; chỉ đọc khi có ô nhập đang focus và người dùng vừa yêu cầu.

**73. "Điểm mù": không chụp màn hình và không mở camera khi máy đang khoá hoặc đang gọi**
- **Mô tả:** Khi màn hình khoá, khi đang gọi điện, hoặc khi người dùng bật chế độ riêng tư, mọi lệnh chụp/nghe đều bị từ chối kèm lý do.
- **Vì sao:** Chụp màn hình lúc khoá có thể lộ OTP, ảnh riêng tư; đang gọi mà nghe là nghe lén.
- **Kỹ thuật:** `ScreenCapture`/`ScreenController` + `CallState` (đã có dấu hiệu đang gọi) + `DeviceActionsController` (camera) + `AudioFocusGate`.
- **Công sức:** M
- **Rủi ro / ghi chú:** `CallState` hiện nhận diện cuộc gọi bằng từ khoá tiếng Trung (`CN_CALL_KEYWORDS`) và chỉ cho WeChat/QQ — trên máy Việt Nam gần như **không bao giờ bật**. Phải sửa trước (xem ý 86).

**74. Ghi âm cuộc họp có cảnh báo đồng thuận rõ ràng**
- **Mô tả:** Bắt đầu ghi âm thì có âm báo + dòng chữ "đang ghi âm", và nhắc người dùng phải thông báo cho người tham gia.
- **Vì sao:** Ghi âm người khác mà không báo là vấn đề pháp lý và đạo đức; trợ lý cá nhân phải có trách nhiệm.
- **Kỹ thuật:** `MeetingRecorder` + `Earcon` + `RecordChipUi`; câu cảnh báo trong `values-vi`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đây là ngưỡng pháp lý, không phải tính năng — nên có trong `docs/acceptable-use.md` và bản tiếng Việt của nó.

**75. "Chế độ khách": đưa máy cho người khác mà không lộ sổ tay**
- **Mô tả:** Một công tắc tạm thời làm trợ lý quên sổ tay, wiki, lịch sử; chỉ trả lời như một trợ lý mới.
- **Vì sao:** Người dùng hay đưa máy cho con/người thân; một trợ lý nhớ hết mọi thứ là rủi ro.
- **Kỹ thuật:** `GuestHost` (đã có cho bạn bè: lịch sử rỗng, không notebook/scene/page/topic) tái dùng; cờ trong `VoiceConfig` + UI ở `SettingsUi`/`FloatingWindowUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Phải chặn cả các đường khác (peer, scheduler, nudge) — `GuestHost` chỉ chặn turn, không chặn nền.

**76. Khoá một hành động bằng mã PIN**
- **Mô tả:** Người dùng đặt PIN cho nhóm hành động (chuyển tiền, xoá dữ liệu, đổi provider); trước khi làm, nó hỏi PIN.
- **Vì sao:** Một agent có quyền chạm màn hình cần một lớp "chỉ chủ máy làm được".
- **Kỹ thuật:** `Vault` (lưu hash PIN, không lưu PIN) + `SecretInput` (đã có ô nhập che) + `ask_user`/một sheet mới.
- **Công sức:** M
- **Rủi ro / ghi chú:** PIN quên là mất tính năng; phải có đường reset qua xoá dữ liệu, và không được khoá luôn đường thoát.

**77. Nhật ký thao tác chỉ ghi thêm, người dùng đọc được**
- **Mô tả:** Mỗi hành động thật (chạm, gõ, gửi) ghi vào một cuốn log chỉ-thêm, có ảnh hưởng gì và ai yêu cầu.
- **Vì sao:** Khi có sự cố ("nó tự gửi tin"), cần bằng chứng; cũng là nền cho việc gỡ lỗi.
- **Kỹ thuật:** `GroundingLog` (đã ghi điểm chạm + cách tìm) + `BrainTrace` + `ChatHistory`; UI `TracePage` hoặc `WorkCardView`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Log chứa text màn hình → phải xoá theo wipe và không đưa vào `ContextArchive` (đã loại notification/vault/sms/contacts).

**78. Cảnh báo "câu này có thể là lừa đảo" trước khi làm theo tin nhắn**
- **Mô tả:** Khi nội dung đến từ người lạ (thông báo, tin nhắn) và yêu cầu chuyển tiền/bấm link/đọc mã, trợ lý dừng và cảnh báo.
- **Vì sao:** Lừa đảo trực tuyến qua Zalo/tin nhắn là vấn đề rất thật ở Việt Nam; trợ lý đọc hộ tin nhắn là chỗ chắn tốt.
- **Kỹ thuật:** `Source` (turn biết ai nói) + `TurnGuards` (`untrusted`) + `NotificationTriage`; một bộ mẫu cảnh báo tiếng Việt; hiện bằng `NudgeUi` chứ không chặn cứng.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chỉ cảnh báo, đừng tự quyết — chặn nhầm tin nhắn người thân còn tệ hơn. Cảnh báo không phải bảo đảm.

**79. Tự xoá dữ liệu cũ sau N ngày**
- **Mô tả:** Người dùng chọn "giữ hội thoại 30 ngày"; phần cũ hơn bị xoá cùng archive, trừ sổ tay/wiki.
- **Vì sao:** Người dùng không muốn tự tay xoá từng thứ; một chính sách đơn giản là đủ an tâm.
- **Kỹ thuật:** `ChatHistory`/`Scrollback` + `ContextArchive` + alarm `sweep` trong `Scheduler` (đã có) làm việc dọn dẹp theo ngày.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng xoá `Notebook`/`guard` theo chính sách này — những thứ đó là "của người dùng", không phải log.

**80. Tuỳ chọn mã hoá toàn bộ lịch sử trên đĩa**
- **Mô tả:** Sổ tay và `Vault` đã được mã hoá AES/GCM; còn `chat_history.json`, `topics/`, `pages/` và `filesDir/context/` vẫn là text thuần. Cho người dùng bật mã hoá cho cả nhóm này.
- **Vì sao:** Hội thoại, các trang nó tạo ra và bản cất của context chứa nhiều thứ riêng tư hơn cả sổ tay; máy mất hoặc bị root là lộ hết.
- **Kỹ thuật:** Bọc `ChatHistory` (một file `chat_history.json`) bằng lớp mã hoá đúng mẫu `Notebook`/`Vault` (AES/GCM + Keystore, key alias riêng), rồi cho `ContextArchive` ghi qua cùng lớp. KDoc của `ChatHistory` giải thích vì sao topics cố ý không mã hoá riêng (chúng chỉ là bản sao của rows trong cùng file) — nên mã hoá **file gốc**, không mã hoá lẻ từng phần.
- **Công sức:** L
- **Rủi ro / ghi chú:** Mất khoá Keystore (đổi máy, wipe dữ liệu) là mất toàn bộ lịch sử **vĩnh viễn** — phải có đường xuất (ý 53) trước, và mặc định nên tắt.

**81. Cho xem trước mọi thứ nó định làm ("chạy thử")**
- **Mô tả:** Với tác vụ dài, người dùng bấm "chạy thử" để xem danh sách bước dự định, không thực thi.
- **Vì sao:** Tin tưởng đến từ việc nhìn thấy kế hoạch trước khi nó chạm vào máy.
- **Kỹ thuật:** `Flow` (`preview_flow` đã có trong `ToolSchemas`) + `WorkStep`; UI `CardUi`/`FlowPlayer`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Với turn không phải flow (model chưa biết app) thì kế hoạch có thể vô nghĩa — nói rõ giới hạn.

**82. Không bao giờ tự bấm nút xác nhận cuối của thanh toán**
- **Mô tả:** Trợ lý điền tới trước nút "Thanh toán"/"Xác nhận" rồi dừng, trả máy cho người dùng.
- **Vì sao:** `Vault` đã ghi rõ nó không điền được vào bàn phím native của app ngân hàng; nút cuối là ranh giới trách nhiệm.
- **Kỹ thuật:** `ToolSchemas` mô tả `fill_secret` đã có cảnh báo; cần biến nó thành **guard** trong `CommandDispatcher` (chặn `screen.tap_target` khi text phần tử khớp "Thanh toán"/"Xác nhận"/"Chuyển tiền") + `ask_user`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Khớp chữ để chặn là heuristic, có thể chặn nhầm — chặn nhầm còn hơn bấm nhầm; phải cho người dùng override có ghi log.

---

## 7. Giọng nói & đa phương tiện

**83. Wake word tiếng Việt: "An-đi ơi"**
- **Mô tả:** Thêm mẫu đánh thức tiếng Việt ("An-đi ơi", "Ê An-đi", "Andee ơi") vào bộ mẫu và cho người dùng tự thu mẫu giọng mình.
- **Vì sao:** `WakeWord` dùng MFCC + DTW, hoạt động tốt với mẫu đã thu; nhưng người Việt gọi "An-đi" theo âm tiết khác hẳn "Andee" tiếng Anh.
- **Kỹ thuật:** `WakeTemplates` + `WakeEnrollUi` (đã có màn thu mẫu) + `Mfcc`/`Dtw`/`VoiceSegmenter`. Chỉ cần thêm mẫu và cải thiện bước chuẩn hoá âm lượng.
- **Công sức:** M
- **Rủi ro / ghi chú:** Mẫu càng nhiều càng dễ báo nhầm; ngưỡng đã được đo thực nghiệm trong `WakeTemplates`, đừng hạ ngưỡng để "dễ nghe hơn".

**84. ASR tiếng Việt có dấu ổn định hơn**
- **Mô tả:** Chọn engine/locale phù hợp và thêm bước "khôi phục dấu" cho câu tiếng Việt bị ASR trả về không dấu.
- **Vì sao:** ASR tiếng Việt hay mất dấu ở tên riêng và từ hiếm; câu không dấu làm model đoán sai.
- **Kỹ thuật:** `AsrController` + `AsrEngine`/`GoogleAsr`/`HuoshanAsr`; thêm bước hậu xử lý (từ điển + luật) trước khi đưa vào `LocalBrain`. Không đổi giao thức WS.
- **Công sức:** M
- **Rủi ro / ghi chú:** Khôi phục dấu sai còn tệ hơn không dấu — chỉ sửa khi độ tin cậy cao; nếu không, giữ nguyên câu người dùng nói (đừng viết lại lời người dùng).

**85. TTS tiếng Việt tự nhiên hơn, có chọn giọng nam/nữ và tốc độ**
- **Mô tả:** Ngoài `SystemTts` (đã map `vi-VN`), thêm giọng tiếng Việt của `HuoshanTts` (hoặc provider khác) và cho chọn giọng + tốc độ ngay trong Settings.
- **Vì sao:** Giọng hệ thống trên nhiều máy đọc tiếng Việt rất máy móc; giọng đọc là thứ người dùng nghe mỗi ngày.
- **Kỹ thuật:** `TtsController`/`TtsEngine` + `VoiceConfig` (`speak_provider`/`voice_provider` đã có segmented trong `SettingsUi`) + `SystemTts` đã map `vi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Provider ngoài = câu trả lời rời khỏi máy (mất riêng tư) — phải nói rõ trong UI và mặc định vẫn là hệ thống.

**86. Nhận diện cuộc gọi thoại/video của app Việt Nam (Zalo, Messenger), không chỉ WeChat/QQ**
- **Mô tả:** Khi có cuộc gọi Zalo/Messenger, trợ lý tự tắt micro và không đọc to; hiện tại điều này chỉ đúng với WeChat/QQ và chỉ khi chữ trên thông báo là tiếng Trung.
- **Vì sao:** Trên máy Việt Nam `CallState` gần như **không bao giờ bật**: `CN_VOICE_APPS` chỉ có WeChat/QQ, `CN_CALL_KEYWORDS` chỉ có "语音通话中"/"来电". Hậu quả: trợ lý chen vào cuộc gọi, mic bị chiếm, TTS đọc to giữa cuộc gọi.
- **Kỹ thuật:** `NotificationRelayService` — thêm package `com.zing.zalo` (và app gọi khác) + bộ từ khoá tiếng Việt ("Đang gọi", "Cuộc gọi đến", "đang gọi thoại", "Cuộc gọi video"), đồng thời ưu tiên `Notification.CATEGORY_CALL` (đường đã có). `CallState`/`AudioFocusGate`/`TtsController` dùng kết quả này.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đây là **sửa lỗi**, không phải tính năng mới — ưu tiên cao. Từ khoá không được dịch theo `AppLocale` (đúng như KDoc của `CN_CALL_KEYWORDS` đã giải thích).

**87. Nói chuyện liên tục và ngắt lời bằng giọng**
- **Mô tả:** Sau khi trả lời, nó tiếp tục nghe trong vài giây để người dùng nói tiếp; nói "thôi" hoặc "khoan" là dừng ngay.
- **Vì sao:** Hội thoại thật không có chuyện bấm nút mỗi câu; phải chạm lại mỗi lần là mệt.
- **Kỹ thuật:** `VoiceSegmenter`/`WakeMic` + `AsrController` + `LocalBrain.cancel`; "thôi/khoan/dừng lại" thêm vào đường `VoiceExitCommand`-nhưng-không-thoát (một lớp mới nhỏ, chỉ dừng lượt).
- **Công sức:** M
- **Rủi ro / ghi chú:** Nghe liên tục là pin + riêng tư; phải có giới hạn thời gian và tắt tiếng rõ ràng (đèn/rung).

**88. Chế độ lái xe: đọc to thông báo quan trọng, không hiện overlay**
- **Mô tả:** Khi phát hiện đang di chuyển, trợ lý chỉ đọc những tin quan trọng (cuộc gọi nhỡ, tin từ người thân) và không mở bong bóng phụ đề.
- **Vì sao:** Người dùng Việt đi xe máy rất nhiều; nhìn màn hình khi lái là nguy hiểm thật.
- **Kỹ thuật:** `NotificationTriage` (đã tự quyết act/tell/ignore) + `TtsController` + cảm biến chuyển động qua `read_sensors`/`get_step_count` + `CallState`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đừng tự bật chỉ vì thấy rung — người ngồi sau xe cũng rung; phải cho người dùng bật tay hoặc hỏi.

**89. Chế độ rảnh tay trong bếp: đọc công thức theo bước, chuyển bước bằng giọng**
- **Mô tả:** Nó mở một trang công thức và đọc từng bước; nói "bước tiếp" để sang bước sau, tay còn dính bột.
- **Vì sao:** Đây là tình huống mà trợ lý giọng nói thắng điện thoại thường rõ nhất.
- **Kỹ thuật:** `show_html` (`HtmlActivity`) + `TtsController` + `AsrController` ngắn (chỉ cần nhận 2–3 lệnh); nội dung từ `wiki_*` hoặc web.
- **Công sức:** M
- **Rủi ro / ghi chú:** Lệnh rất ngắn dễ nhầm với tiếng ồn; dùng mẫu cử chỉ tay (vuốt trên bóng) làm dự phòng.

**90. Biên bản họp tiếng Việt có mốc thời gian**
- **Mô tả:** Ghi âm cuộc họp, rồi trả về biên bản có mốc thời gian cho từng ý và danh sách việc cần làm.
- **Vì sao:** `MeetingRecorder` đã có; người dùng công sở Việt Nam họp rất nhiều, và biên bản là thứ họ phải làm tay.
- **Kỹ thuật:** `MeetingRecorder` + `asr.final` (đã dùng cho meeting) + `LlmClient` tóm tắt + `Tasks` cho việc cần làm; xuất qua `show_html`/`HtmlActivity`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ghi âm dài + model tóm tắt = tốn token; phải giới hạn độ dài và nói rõ "bản tóm tắt có thể sai, hãy kiểm tra".

**91. Chụp hoá đơn → ghi chi tiêu**
- **Mô tả:** Chụp ảnh hoá đơn, nó đọc số tiền/ngày/nơi bán và ghi vào một mục chi tiêu; người dùng sửa được trước khi lưu.
- **Vì sao:** Người dùng Việt hay giữ hoá đơn để đối chiếu; nhập tay thì không ai làm.
- **Kỹ thuật:** `camera_turn`/`take_screenshot` + `LlmClient` vision (nếu model hỗ trợ) + `Notebook` (section mới hoặc `remember` có cấu trúc) + `ask_user` xác nhận.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ảnh hoá đơn có thể có số thẻ — không lưu ảnh gốc lâu, chỉ lưu số liệu đã xác nhận.

**92. Dịch nhanh chữ trên màn hình**
- **Mô tả:** Chụp màn hình đang có chữ nước ngoài, nó dịch sang tiếng Việt và hiện dưới dạng phụ đề.
- **Vì sao:** Người dùng gặp tiếng Anh/Trung trong app, game, tài liệu; chuyển app để dịch là mất ngữ cảnh.
- **Kỹ thuật:** `ScreenCapture` (ảnh đã có) + `LlmClient` vision hoặc OCR hệ thống (ML Kit nếu thêm dependency) + `FloatingWindowUi` phụ đề. Ảnh đi qua đúng đường ảnh đã có trong `LocalBrain` (tối đa 2 ảnh).
- **Công sức:** M
- **Rủi ro / ghi chú:** Gửi ảnh màn hình lên cloud là rò rỉ — phải hỏi trước mỗi lần hoặc chỉ dùng khi người dùng chủ động.

**93. "Đọc màn hình này cho tôi" bằng giọng**
- **Mô tả:** Nó đọc nội dung chính của màn hình đang mở (tin nhắn, bài báo, thông báo) thành giọng nói, tóm tắt nếu dài.
- **Vì sao:** Rất hữu ích khi mắt đang bận (nấu ăn, lái xe, mỏi mắt) và là nền cho chế độ khiếm thị ở nhóm 10.
- **Kỹ thuật:** `screen.ui_tree` (`get_screen_element`) cho text + `TtsController`; nếu chỉ có ảnh thì `take_screenshot` + model vision. Dùng `ScreenController` để lấy cây phần tử.
- **Công sức:** M
- **Rủi ro / ghi chú:** Cây phần tử có thể chứa mọi thứ trên màn hình, kể cả mật khẩu hiện; phải lọc trường mật khẩu (`CommandDispatcher` đã có khái niệm secret field).

**94. Âm phản hồi (earcon) cho từng trạng thái**
- **Mô tả:** Tiếng "tách" khi bắt đầu nghe, tiếng trầm khi xong, tiếng khác khi lỗi — không cần nhìn màn hình.
- **Vì sao:** Người dùng cần biết nó đã nghe hay chưa mà không phải nhìn; cũng giúp người khiếm thị.
- **Kỹ thuật:** `Earcon` (đã có) + `AudioFocusGate` (để không đè nhạc/cuộc gọi) + `EmotionState` để chọn âm theo trạng thái.
- **Công sức:** S
- **Rủi ro / ghi chú:** Âm phải ngắn (<200ms) và không the thé; tắt được trong Settings.

**95. Tin nhắn thoại Zalo → việc cần làm**
- **Mô tả:** Người dùng tự chuyển tiếp một tin nhắn thoại cho trợ lý; nó nghe và tạo việc/nhắc hẹn.
- **Vì sao:** Tin nhắn thoại là cách người Việt trao đổi công việc phổ biến; gõ lại rất mất thời gian.
- **Kỹ thuật:** Chỉ nhận file người dùng chủ động chia sẻ (Intent share) → `AsrController`/engine + `LocalBrain` + `Tasks`. **◇** nếu muốn tự đọc tin trong Zalo: repo đã có tiền lệ cấm tự thao tác WeChat, Zalo cũng cùng loại rủi ro.
- **Công sức:** M
- **Rủi ro / ghi chú:** Không tự động hoá Zalo. Đây là ranh giới sản phẩm, không phải giới hạn kỹ thuật.

---

## 8. Kết nối thiết bị & chia sẻ

**96. Chuyển phiên giữa điện thoại và máy Mac/PC**
- **Mô tả:** Đang nói trên điện thoại, về nhà mở máy tính thì cuộc trò chuyện tiếp tục ở đó với cùng ngữ cảnh.
- **Vì sao:** Repo đã có hai thân (Android + Mac) chạy cùng một brain; nhưng "chuyển phiên" chưa có UI.
- **Kỹ thuật:** `BodyWsClient`/`BodyWsServer` (envelope chung) + `DesktopBrainHost` + `ContextArchive`/`session.json` (đã lưu bản tóm tắt hội thoại). Cần một kênh "chuyển ngữ cảnh" — hiện chưa có.
- **Công sức:** L
- **Rủi ro / ghi chú:** Không được chạy hai brain cùng lúc cho một người ("hai giọng nói chồng nhau" — chính CLAUDE.md nói lý do hai brain loại trừ nhau).

**97. Hub tự dựng cho gia đình: một máy tính làm não, nhiều điện thoại làm thân**
- **Mô tả:** Một hướng dẫn + màn hình trong app để dựng hub trên máy tính trong nhà, các điện thoại trỏ `hub_url` về đó.
- **Vì sao:** `BodyWsClient` đã dial ra hub và gửi `register` kèm `ToolSchemas.all()`; thiếu là tài liệu và UI. Đây là cách dùng tiết kiệm nhất cho gia đình nhiều máy.
- **Kỹ thuật:** `BodyWsServer` (9008) + `BodyWsClient` (`hub_url`) + `SettingsUi`; tài liệu trong `docs/getting-started.md`. Không cần code mới ở tầng giao thức.
- **Công sức:** M
- **Rủi ro / ghi chú:** Hub trong LAN không có xác thực mạnh — phải nói rõ "chỉ dùng trong mạng nhà", và không mở cổng ra Internet.

**98. Nhờ máy của bạn làm hộ khi máy mình thiếu khả năng**
- **Mô tả:** Điện thoại không có app X, nhưng máy Mac của chính người dùng có — nhờ nó mở và làm bước đó.
- **Vì sao:** Hai thân có khả năng khác nhau (Mac chạy shell, Android có app ngân hàng); phối hợp thì mạnh hơn cộng lại.
- **Kỹ thuật:** `PeerTasks` + `PeerLink` + `PeerProtocol` (đã có chuyển việc giữa các Andee) + `ShellTools` bên Mac. Tool `ask_friend` trả về ngay khi việc được *nhận*, kết quả về sau.
- **Công sức:** L
- **Rủi ro / ghi chú:** `TurnGuards` cố ý chặn `peer.ask` trên turn untrusted và chặn trên turn `PEER` (không chuyền tay nhau) — giữ nguyên các chặn này.

**99. Nhờ bạn bè giúp (dùng đúng thiết kế FRIEND)**
- **Mô tả:** Gửi một việc cho Andee của bạn (ví dụ "nhờ máy bạn chụp giúp ảnh màn hình đó"), chủ máy bên kia duyệt bằng thẻ.
- **Vì sao:** Cơ chế đã có nhưng người dùng không biết nó tồn tại; `PeerStore.Trust.FRIEND` + `GuestHost` là thiết kế rất riêng của repo này.
- **Kỹ thuật:** `PeerQr` (ghép) + `PeerTasks` + `GuestHost` (allow-list `screen.*`, `ui.ask/alert/show_html`, `device.launch/open_url`) + `NudgeUi`/thẻ duyệt; UI ở `PeersUi`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đây là bề mặt tấn công xã hội: một người bạn bị lừa có thể thành bàn đạp. Thẻ duyệt phải nói rõ *ai* và *làm gì*, không chỉ "cho phép".

**100. Chuyển tiếp thông báo giữa các thiết bị**
- **Mô tả:** Thông báo quan trọng trên điện thoại hiện trên máy tính đang mở, và ngược lại.
- **Vì sao:** Người dùng làm việc trên máy tính nhưng điện thoại là nơi có tin nhắn; chuyển tiếp giảm số lần với máy.
- **Kỹ thuật:** Sự kiện `notification` (đã có trong giao thức event, gửi qua `BodyWsClient`) + `MacNotifications` bên Mac + `NotificationRelayService`. Nhớ luật: event chỉ đi qua hub, local brain không thấy — phải gọi thẳng cả hai đường.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chuyển tiếp thông báo ngân hàng/OTP sang máy khác là rủi ro — phải lọc theo `GearCategories` và bỏ qua notification chứa mã.

**101. Đồng bộ sổ tay giữa điện thoại và Mac**
- **Mô tả:** Sổ tay, wiki, plans giống nhau trên mọi thiết bị của cùng người dùng.
- **Vì sao:** Hiện mỗi thân có bản `Notebook` riêng (theo KDoc của `Plans`: "mỗi bên chỉ arm/cancel alarm của mình"), nên người dùng hai máy thấy hai bộ nhớ khác nhau.
- **Kỹ thuật:** `NotebookStore` + `PeerLink` (kênh đã có, có mã hoá `PeerCrypto`) + giải quyết xung đột theo mục (last-writer-wins từng section, không theo file). Tuyệt đối không sync `Vault`.
- **Công sức:** L
- **Rủi ro / ghi chú:** Xung đột đồng bộ là nơi mất dữ liệu; phải có bản sao và "khôi phục về trước khi sync". Chưa nên làm trước khi có export (ý 53).

**102. Robot dog (dog.*) mở rộng: đi theo, canh nhà, báo động**
- **Mô tả:** Dùng các tool `dog_move`/`dog_sequence`/`dog_sense` để dạy nó đi theo người, tuần tra một vòng, và báo khi có tiếng động lạ.
- **Vì sao:** Repo đã có nguyên bộ `dog.*` với `DogLink`/`DogMotion`/`DogSense`; đây là phần "thân thứ ba" độc đáo, rất hợp demo.
- **Kỹ thuật:** `DogLink` (cáp một chiều) + `DogMotion` + `DogSense` + `Flow` (một chuỗi `dog_move` là một flow tự nhiên) + `read_sensors` trên điện thoại.
- **Công sức:** L
- **Rủi ro / ghi chú:** Robot thật di chuyển trong nhà có người; mọi flow phải có nút dừng vật lý và không bao giờ chạy khi không có người lớn.

**103. Ghép bạn bè bằng QR có tên và ảnh đại diện**
- **Mô tả:** Màn hình QR ghép bạn hiện tên, ảnh, và hỏi "đây có đúng là người bạn muốn ghép?" trước khi ghim khoá.
- **Vì sao:** `PeerQr` ghim khoá là thiết kế tốt nhưng người dùng không đọc được fingerprint; một cái tên + ảnh là thứ họ kiểm tra được.
- **Kỹ thuật:** `PeerQr` + `PeersUi` + `PeerStore` (đã lưu tên và `Trust`); ảnh chỉ là ảnh do người dùng chọn, không tự lấy.
- **Công sức:** S
- **Rủi ro / ghi chú:** Tên/ảnh trong QR là do người tạo tự khai — không phải bằng chứng danh tính, đừng để UI ngụ ý như vậy.

**104. Gửi nhật ký lỗi cho người phát triển bằng một nút**
- **Mô tả:** Khi có sự cố, người dùng bấm "báo lỗi", app gom `SelfCheck` + đoạn `BrainTrace` gần nhất (đã lọc bí mật) thành một file gửi đi.
- **Vì sao:** Người dùng Việt không quen đọc logcat; không có đường báo lỗi thì mọi lỗi thành "app không chạy".
- **Kỹ thuật:** `SelfCheck`/`SelfCheckUi` + `BrainTrace` + `Log` + `open_url`/FileProvider; lọc bằng danh sách loại trừ giống `ContextArchive`.
- **Công sức:** S
- **Rủi ro / ghi chú:** Gửi log là gửi dữ liệu người dùng — phải hiện trước khi gửi và cho bỏ từng mục.

**105. Chạy trợ lý trên tablet/TV Android cũ làm "bảng điều khiển nhà"**
- **Mô tả:** Một tablet cũ treo tường chạy chế độ chỉ-hub, hiện lịch, việc, camera, và nghe lệnh.
- **Vì sao:** Nhiều gia đình Việt có tablet cũ; biến nó thành bảng điều khiển là giá trị lớn với chi phí thấp.
- **Kỹ thuật:** `BodyWsServer` + `BodyWsClient` + `LaunchPadActivity`/màn hình chờ (ý 11) + `TtsController`/`AsrController`; không cần thân mới.
- **Công sức:** M
- **Rủi ro / ghi chú:** Tablet cũ yếu pin và Android cũ; phải thử trên Android 8–10 trước (repo đã có `WindowMetricsCompat` và `compat` cho việc này).

**106. Tích hợp app khác qua intent/shortcut thay vì chỉ chạm màn hình**
- **Mô tả:** Ưu tiên mở app bằng intent/shortcut đúng màn hình thay vì mở app rồi chạm 3 lần.
- **Vì sao:** Mỗi cú chạm là một chỗ có thể sai; `launch_app` hiện chỉ mở app, không mở thẳng màn hình con.
- **Kỹ thuật:** `launch_app`/`CommandDispatcher` `device.launch` + `PackageManager` query intent; Android 11+ cần khai báo package trong manifest (không được sửa gradle/manifest theo quy ước — đây là việc của lead).
- **Công sức:** M
- **Rủi ro / ghi chú:** Shortcut của app bên thứ ba thay đổi theo phiên bản; phải có đường lùi về chạm màn hình.

---

## 9. Người dùng Việt Nam đặc thù

**107. Đọc biến động số dư từ SMS/thông báo ngân hàng, tóm tắt chi tiêu**
- **Mô tả:** Nó đọc tin nhắn/thông báo của ngân hàng (miễn phí, không cần API), ghi lại các khoản và cuối tuần tóm tắt "tuần này bạn chi bao nhiêu, nhiều nhất là gì".
- **Vì sao:** Ngân hàng Việt Nam không có API mở cho cá nhân; SMS/thông báo là đường duy nhất và người dùng nào cũng có.
- **Kỹ thuật:** `read_sms` + `get_notifications` + `NotificationTriage` + `Notebook` (section mới cho chi tiêu) + `LlmClient` (parse mẫu tin). Mẫu tin của từng ngân hàng cần một bảng regex, không phó cho model đoán.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đây là dữ liệu tài chính: không gửi lên cloud trừ khi người dùng đồng ý; không lưu số tài khoản đầy đủ; và **không tự bấm gì** trong app ngân hàng.

**108. Nhắc hoá đơn định kỳ: điện, nước, internet, học phí**
- **Mô tả:** Người dùng nói "mùng 5 đóng tiền điện"; nó nhắc trước 1–2 ngày và có thể mở sẵn trang thanh toán (người dùng bấm nút cuối).
- **Vì sao:** Hoá đơn định kỳ là loại việc quên là bị cắt; đây là ứng dụng "trợ lý" rõ ràng nhất với gia đình Việt.
- **Kỹ thuật:** `Scheduler`/`CronExpr` + `Plans` item `remind`/`ask` + `Tasks` + `open_url`; nhắc bằng `NudgeUi` (bubble, không cần model — rẻ).
- **Công sức:** M
- **Rủi ro / ghi chú:** Không tự thanh toán. Số tiền/thời hạn phải do người dùng nhập, không tự suy ra.

**109. Thủ tục hành chính: mở đúng trang dịch vụ công và điền sẵn**
- **Mô tả:** "Tôi muốn gia hạn hộ chiếu" → nó mở đúng trang, chỉ ra giấy tờ cần, và điền những trường đã biết từ sổ tay.
- **Vì sao:** Người Việt rất ngại thủ tục hành chính; mở đúng trang đã tiết kiệm rất nhiều thời gian.
- **Kỹ thuật:** `open_url`/`CommandDispatcher` `device.open_url` + `show_html`/`HtmlActivity` (trang hướng dẫn) + `Vault` cho thông tin cá nhân (CCCD, hộ chiếu) + `Flow` nếu người dùng ghi lại đường đi. **◇** phần "điền tự động trên cổng thông tin" — trang đổi liên tục và cần tài khoản.
- **Công sức:** L
- **Rủi ro / ghi chú:** Không có API chính thức nào để dựa vào; đừng hứa "làm hộ", chỉ hứa "mở đúng chỗ và chuẩn bị giấy tờ".

**110. Đặt lịch khám bệnh viện công**
- **Mô tả:** Nó canh giờ mở đặt lịch, điền sẵn thông tin, dừng trước bước xác nhận để người dùng bấm.
- **Vì sao:** Đặt lịch bệnh viện công ở Việt Nam là cuộc đua về giờ; canh đúng giây có giá trị thật.
- **Kỹ thuật:** `Flow` + `Scheduler` (alarm) + `FlowRuntime` + `Vault` + `ask_user`. **◇** vì phụ thuộc app/website bệnh viện cụ thể và thay đổi liên tục.
- **Công sức:** L
- **Rủi ro / ghi chú:** Sai một bước là đặt nhầm người/nhầm khoa; luôn dừng trước "Xác nhận" và hiện đầy đủ thông tin cho người dùng đọc.

**111. Zalo: chỉ đọc thông báo và gợi ý trả lời, tuyệt đối không tự bấm**
- **Mô tả:** Nó tóm tắt tin nhắn Zalo chưa đọc, gợi ý câu trả lời, và để người dùng tự chuyển sang app bấm gửi.
- **Vì sao:** Zalo là app liên lạc chính của người Việt; nhưng tự thao tác vào app chat có tiền lệ xấu (WeChat trong v0.15.0 khiến người dùng bị khoá tài khoản).
- **Kỹ thuật:** `NotificationRelayService` + `NotificationTriage` + `get_notifications` + `NudgeUi`/thẻ gợi ý. Không dùng `ScreenController` để bấm.
- **Công sức:** M
- **Rủi ro / ghi chú:** Lấy nội dung tin nhắn là dữ liệu riêng tư của cả người gửi; không được đưa vào `ContextArchive` và không đi sang peer.

**112. Học tiếng Anh mỗi ngày, có giọng đọc và nhắc**
- **Mô tả:** Mỗi tối nó gửi một thẻ từ vựng ngắn dựa trên điều người dùng đã hỏi trong ngày, đọc to bằng TTS, và nhắc lại theo lịch.
- **Vì sao:** Học tiếng Anh là nhu cầu phổ biến; trợ lý đã có TTS, wiki và bộ nhớ nên gần như miễn phí để làm.
- **Kỹ thuật:** `Plans` (`remind`) + `Scheduler` + `TtsController` + `wiki_*` để lưu bộ từ; nội dung do model sinh (có kiểm tra).
- **Công sức:** M
- **Rủi ro / ghi chú:** Model có thể dạy sai từ; nên lấy từ điển offline hoặc chỉ dùng câu model sinh khi có người duyệt.

**113. Ôn thi: tạo câu hỏi từ ghi chú của người dùng**
- **Mô tả:** Người dùng đưa ghi chú/bài giảng, nó tạo bộ câu hỏi ôn tập và hỏi lại từng câu, chấm đúng/sai.
- **Vì sao:** Học sinh/sinh viên Việt Nam học bằng ghi chú và đề cương; biến ghi chú thành đề là việc AI làm tốt.
- **Kỹ thuật:** `show_html`/`HtmlActivity` cho giao diện hỏi đáp + `LlmClient` + `Notebook`/`wiki_*` để lưu bộ đề + `ask_user` cho từng câu.
- **Công sức:** M
- **Rủi ro / ghi chú:** Chi phí token cao cho tài liệu dài — phải chia nhỏ và chỉ tạo khi người dùng yêu cầu.

**114. Thời khoá biểu và deadline trường học**
- **Mô tả:** Nhập thời khoá biểu một lần, nó nhắc trước mỗi tiết và nhắc deadline bài tập; đổi tiết thì nói một câu là cập nhật.
- **Vì sao:** Học sinh/sinh viên sống theo thời khoá biểu; đây là loại nhắc mà app ghi chú thường không làm tốt.
- **Kỹ thuật:** `Tasks`/`todo` + `Scheduler` + `get_calendar`/`add_calendar_event` (đã có) + `Notebook` lưu thời khoá biểu. Có thể đẩy sang Google Calendar qua `add_calendar_event`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Nhắc quá nhiều làm người dùng tắt thông báo; phải có ngân sách nhắc/ngày.

**115. Theo dõi sức khoẻ: thuốc, huyết áp, chu kỳ**
- **Mô tả:** Người dùng nói "sáng nay huyết áp 130/85" hoặc "uống thuốc lúc 8h"; nó ghi lại và nhắc đúng giờ.
- **Vì sao:** Người Việt có người thân lớn tuổi uống thuốc hằng ngày; nhắc thuốc là tính năng được dùng thật, mỗi ngày.
- **Kỹ thuật:** `Scheduler` (alarm chính xác nếu có quyền, đã có ghi chú về việc không đòi `SCHEDULE_EXACT_ALARM`) + `diary`/`self_note` + `Plans` + `TtsController`; xuất cho bác sĩ bằng `show_html`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đây là dữ liệu y tế: tuyệt đối không chẩn đoán, không khuyên dùng thuốc; câu trả lời phải nói "hỏi bác sĩ" khi vượt quá ghi chép.

**116. Danh sách đi chợ bằng giọng, dùng lại được**
- **Mô tả:** "Thêm sữa, trứng, rau muống" khi đang đi; danh sách hiện trên bong bóng và xoá được bằng một chạm khi đã mua.
- **Vì sao:** Đây là tác vụ nhỏ, lặp lại, đúng sở trường giọng nói, và không cần model mạnh.
- **Kỹ thuật:** `todo`/`note.todo` + `Notebook` + `NudgeUi`/thẻ + `TtsController`; có thể dùng đường "khớp toàn câu" như ý 65 để không gọi model.
- **Công sức:** S
- **Rủi ro / ghi chú:** Tiếng ồn chợ làm ASR sai; cần hiện lại danh sách để người dùng thấy ngay cái vừa thêm.

**117. Giá xăng, thời tiết, giao thông — hỏi nhanh, trả lời ngắn**
- **Mô tả:** "Xăng A95 hôm nay bao nhiêu", "chiều nay có mưa không", "đường về nhà có kẹt không" — trả lời một câu kèm nguồn.
- **Vì sao:** Đây là những câu người Việt hỏi hằng ngày; trả lời ngắn đúng là toàn bộ giá trị.
- **Kỹ thuật:** `get_location` + web qua model (nếu provider hỗ trợ) hoặc `MarketTools` (`market_search`/`market_call` — chỉ bật ở chế độ CodeFlying) + `open_url` cho nguồn.
- **Công sức:** M
- **Rủi ro / ghi chú:** Giá xăng/thời tiết mà model tự "nhớ" là bịa; phải luôn kèm nguồn hoặc nói "tôi không tra được".

**118. Vé tàu xe về quê dịp Tết**
- **Mô tả:** Canh giờ mở bán, mở đúng trang, điền sẵn thông tin, dừng trước bước thanh toán.
- **Vì sao:** Vé Tết là nỗi ám ảnh hằng năm của người Việt xa quê; đây là ví dụ thuyết phục nhất về "agent làm hộ".
- **Kỹ thuật:** `Scheduler` + `Flow` + `FlowRuntime` + `Vault` + `ask_user`. **◇** phụ thuộc website bán vé.
- **Công sức:** L
- **Rủi ro / ghi chú:** Không bao giờ tự thanh toán; và phải nói rõ "có thể không đặt được" — tỉ lệ thành công thấp là bản chất.

**119. Hiểu từ địa phương và cách nói vùng miền**
- **Mô tả:** "bỏ bịch" (miền Nam) hiểu là "bỏ vào túi"; "mô", "tê", "răng" của miền Trung được hiểu đúng là "đâu", "kia", "sao".
- **Vì sao:** ASR + model thường chuẩn hoá về giọng Bắc; người miền Trung/Nam bị hiểu sai nhiều hơn.
- **Kỹ thuật:** Một bảng từ địa phương trong prompt/phụ lục của `LocalPrompt` + bước chuẩn hoá trước model; thêm câu mẫu vào `WakeTemplates` nếu cần.
- **Công sức:** S
- **Rủi ro / ghi chú:** Bảng từ phải nhỏ và có người kiểm; đừng dịch lại câu người dùng trong lịch sử.

**120. Việc trước Tết: dọn dẹp, quà, lì xì, lịch nghỉ**
- **Mô tả:** Vào khoảng tháng Chạp, nó chủ động nhắc: đổi tiền lẻ, mua quà, đặt vé, lịch nghỉ của cơ quan, ngày giỗ.
- **Vì sao:** Tết là sự kiện mà một trợ lý "có kế hoạch" đúng nghĩa có thể giúp; `Plans` sinh ra chính cho việc này.
- **Kỹ thuật:** `Plans` (`Plans.Direction` + `Plans.Day`) + `Scheduler` + `nudge` (bubble) + lịch âm ở ý 37.
- **Công sức:** M
- **Rủi ro / ghi chú:** Nhắc nhiều ngày liền dễ thành spam; phải gộp thành một kế hoạch có "lý do bây giờ" như doc `proactive-nudge` yêu cầu.

---

## 10. Người khiếm thị & người lớn tuổi

**121. Chế độ "đọc màn hình" cho người khiếm thị, sống chung với TalkBack**
- **Mô tả:** Một chế độ trong đó trợ lý đọc nội dung màn hình, mô tả nút nào ở đâu, và người dùng ra lệnh bằng giọng thay vì chạm.
- **Vì sao:** TalkBack đọc được chữ nhưng không *hành động hộ*; trợ lý có `ScreenController` thì làm được cả hai — đây là khác biệt thật, không phải marketing.
- **Kỹ thuật:** `ScreenController` + `TreeHelper` (`screen.ui_tree`) + `TtsController` + `AsrController`; cờ chế độ trong `VoiceConfig`; UI ở `SettingsUi`.
- **Công sức:** L
- **Rủi ro / ghi chú:** Phải làm **với** người khiếm thị thật, không tự đoán; hai dịch vụ trợ năng cùng lúc có thể tranh nhau nếu không cẩn thận (xem ý 130).

**122. Chữ to, tương phản cao, khoảng cách lớn — một công tắc**
- **Mô tả:** Một công tắc trong Settings phóng to mọi chữ của app, tăng tương phản thẻ, và tăng vùng chạm.
- **Vì sao:** Người dùng lớn tuổi chiếm phần lớn người cần trợ lý giọng nói; chữ mặc định 12–14sp là không đọc được.
- **Kỹ thuật:** `Theme` + `Glass` + `CardUi` + `FloatingWindowUi`; đọc từ một cờ trong `VoiceConfig`. Nên dùng `sp` đúng chuẩn (đã dùng) và kiểm tra với phóng to hệ thống.
- **Công sức:** M
- **Rủi ro / ghi chú:** Overlay có nhiều chỗ đặt kích thước cứng bằng px; phải rà hết nếu không sẽ có thẻ vỡ bố cục.

**123. Gọi người thân bằng một chạm**
- **Mô tả:** Trên bong bóng có nút gọi 2–3 người thân đã chọn; bấm là mở dialer, hoặc gọi luôn nếu người dùng đã bật.
- **Vì sao:** Người lớn tuổi và người khiếm thị cần gọi trong 2 giây; tìm danh bạ là rào cản thật.
- **Kỹ thuật:** `device.dial`/`dial` (có tham số `direct`, mặc định mở dialer để người dùng tự bấm) + `People` (chọn người thân) + `FloatingWindowUi` nút phụ.
- **Công sức:** S
- **Rủi ro / ghi chú:** `direct=true` phải là lựa chọn rõ ràng của người dùng, không mặc định — repo đã cố ý đặt mặc định là an toàn.

**124. Nhắc thuốc lặp lại đến khi có xác nhận**
- **Mô tả:** Đến giờ uống thuốc thì nói to + rung; nếu 5 phút không có xác nhận thì nhắc lại (tối đa 3 lần), sau đó báo cho người thân nếu đã cấu hình.
- **Vì sao:** Nhắc một lần rồi im là vô dụng với người lớn tuổi; đây là tính năng được dùng mỗi ngày.
- **Kỹ thuật:** `Scheduler` (alarm `todo`) + `TtsController` + `Earcon` + `NudgeUi` (nút "đã uống") + `Plans` item `ask` + `send_sms` cho người thân (tuỳ chọn).
- **Công sức:** M
- **Rủi ro / ghi chú:** Gửi SMS cho người thân là dữ liệu sức khoẻ ra ngoài — phải là lựa chọn rõ ràng, có thể tắt, và nói trước.

**125. Chống lừa đảo hai lớp cho người lớn tuổi**
- **Mô tả:** Trước khi chuyển tiền, nó hỏi lại "ai nhắn bạn?", đọc to nội dung, và nếu người nhận không nằm trong danh sách quen thì cảnh báo đỏ.
- **Vì sao:** Người lớn tuổi ở Việt Nam là mục tiêu chính của lừa đảo qua điện thoại/Zalo; trợ lý ở giữa là chốt chặn tự nhiên.
- **Kỹ thuật:** `People` (người quen) + `TurnGuards` + `ask_user` hai bước + `TtsController`; có thể dùng `read_sms`/`get_notifications` để biết ngữ cảnh.
- **Công sức:** M
- **Rủi ro / ghi chú:** Không được chặn cứng (người dùng có quyền chuyển tiền cho người mới), chỉ cảnh báo + hỏi lại. Sai mà chặn là mất quyền tự quyết.

**126. Điều khiển hoàn toàn bằng giọng, không cần chạm**
- **Mô tả:** Mọi việc trong app làm được bằng lời, kể cả mở Settings, đổi ngôn ngữ, tắt micro; bong bóng chỉ để báo trạng thái.
- **Vì sao:** Người khiếm thị hoặc tay bận không chạm được vào các nút nhỏ trên overlay.
- **Kỹ thuật:** `WakeWord` + `AsrController` + `CommandDispatcher` qua `LocalBrain`; cần một lớp "lệnh giao diện" (mở card X, đổi Y) — hiện chưa có, có thể làm bằng tool mới + `ToolSchemas`.
- **Công sức:** L
- **Rủi ro / ghi chú:** Lệnh giọng cho thao tác phá huỷ (xoá dữ liệu) phải có xác nhận bằng giọng rõ ràng, không chỉ "ừ".

**127. Trả lời cực ngắn, đọc chậm**
- **Mô tả:** Một chế độ làm câu trả lời tối đa 2 câu, tốc độ đọc chậm, ngắt nghỉ rõ, và tự lặp lại nếu người dùng nói "nói lại".
- **Vì sao:** Người lớn tuổi và người khiếm thính xử lý câu dài kém hơn; đọc nhanh là vô ích.
- **Kỹ thuật:** `SystemTts` (thuộc tính tốc độ, `setSpeechRate`) + `VoiceConfig` + `LocalPrompt` (luật độ dài) + `TtsController` nhớ câu vừa đọc để đọc lại.
- **Công sức:** S
- **Rủi ro / ghi chú:** Nhớ câu vừa đọc là thêm trạng thái vào `TtsController`; đừng để nó nhớ cả hội thoại (bộ nhớ + riêng tư).

**128. Rung và âm báo khác nhau cho từng kết quả**
- **Mô tả:** Xong việc = hai rung ngắn; lỗi = ba rung dài; đang chờ người dùng = rung đều nhẹ.
- **Vì sao:** Người dùng không nhìn màn hình vẫn biết chuyện gì xảy ra; cũng giảm nhu cầu đọc to.
- **Kỹ thuật:** `Earcon` + `Vibrator` (qua context) + `EmotionState` để chọn mẫu; hằng số mẫu ở một chỗ.
- **Công sức:** S
- **Rủi ro / ghi chú:** Đừng thêm quyền mới nếu `VIBRATE` đã có sẵn (kiểm tra manifest — không sửa manifest nếu chưa được phép).

**129. "Tôi đang ở đâu?" bằng giọng**
- **Mô tả:** Nó trả lời địa chỉ gần đúng kèm mốc quen (nếu đã lưu) và thời tiết tại đó.
- **Vì sao:** Người lớn tuổi đi lạc hoặc quên đường; một câu trả lời bằng giọng là trợ giúp thật.
- **Kỹ thuật:** `get_location`/`device.location` + `TtsController` + `Notebook` (địa chỉ nhà/cơ quan người dùng tự lưu).
- **Công sức:** S
- **Rủi ro / ghi chú:** Vị trí là dữ liệu rất nhạy cảm; chỉ lấy khi người dùng hỏi, không lưu lịch sử vị trí.

**130. Overlay không chiếm focus, không phá TalkBack**
- **Mô tả:** Các cửa sổ nổi khai báo cờ không focus và không chặn trợ năng, để TalkBack vẫn đọc được nội dung bên dưới.
- **Vì sao:** Một overlay chiếm focus làm TalkBack câm hoặc đọc sai — đây là lỗi rất phổ biến của app overlay.
- **Kỹ thuật:** `FloatingWindowUi` (`FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCH_MODAL`…) + `OverlayA11y` (đã có lớp riêng cho trợ năng) + `A11yIme` khi cần bàn phím.
- **Công sức:** M
- **Rủi ro / ghi chú:** Có cửa sổ **phải** focus (ô nhập). Phải phân loại cửa sổ nào focus và kiểm tra với TalkBack bật.

**131. Chia sẻ trạng thái/vị trí khẩn cấp cho người nhà**
- **Mô tả:** Một lệnh "báo cho con tôi" gửi vị trí + tình trạng (pin, đang ở đâu) qua SMS cho người đã chọn.
- **Vì sao:** Người lớn tuổi sống một mình cần một đường liên lạc nhanh mà không phải mở app.
- **Kỹ thuật:** `send_sms` (`device.sms.send`, đã có) + `get_location` + `People` + `ask_user` xác nhận ngay trước khi gửi.
- **Công sức:** M
- **Rủi ro / ghi chú:** Gửi vị trí là hành động không rút lại được; phải xác nhận, và phải nói rõ đang gửi cho ai.

---

## 11. Năng suất & công việc

**132. "Bản tin sáng": lịch, thời tiết, việc hôm nay, một câu tóm tắt**
- **Mô tả:** Mỗi sáng một thẻ gọn 4 dòng, không đọc to trừ khi người dùng bật.
- **Vì sao:** Đây là thói quen hàng ngày rẻ nhất để người dùng mở app; cũng là cách chứng minh "nó có kế hoạch".
- **Kỹ thuật:** `Plans` (`Plans.Day`/`Plans.Item`) + `get_calendar` + `Scheduler` + `NudgeUi`/thẻ; thời tiết qua model/web với nguồn.
- **Công sức:** M
- **Rủi ro / ghi chú:** Không gọi model nếu chỉ là dữ liệu đã có (giờ, lịch) — đúng tinh thần "bubble không cần model" của doc `proactive-nudge`.

**133. Biên bản họp → việc cần làm + email nháp**
- **Mô tả:** Sau cuộc họp, nó tạo danh sách việc có người phụ trách và hạn, và soạn sẵn email tóm tắt để người dùng copy/sửa.
- **Vì sao:** Việc sau họp (follow-up) là phần tốn thời gian nhất và hay bị quên.
- **Kỹ thuật:** `MeetingRecorder` + `LlmClient` + `Tasks` (`task`/`note.task`) + `show_html` (email nháp trong `HtmlActivity`); gửi mail cần app mail — repo **không có** API email.
- **Công sức:** M
- **Rủi ro / ghi chú:** **◇** phần "gửi email": chỉ soạn nháp và để người dùng gửi. Đừng hứa gửi được.

**134. Tóm tắt đoạn chat/nhóm dài**
- **Mô tả:** Người dùng chọn "tóm tắt nhóm này từ sáng" và nhận 5 dòng: ai nói gì, việc gì cần làm, có gì gấp.
- **Vì sao:** Nhóm chat công việc ở Việt Nam (Zalo) dày đặc; đọc hết là mất thời gian.
- **Kỹ thuật:** `get_notifications` + `NotificationRelayService` (chỉ nội dung thông báo, không mở app chat) + `LlmClient` + `NudgeUi`/trang. **◇** với nội dung đầy đủ trong app chat: cần tự thao tác app — không làm (tiền lệ WeChat).
- **Công sức:** M
- **Rủi ro / ghi chú:** Tóm tắt từ thông báo là thiếu ngữ cảnh và có thể sai; phải nói rõ nguồn ("từ 12 thông báo").

**135. Soạn văn bản dài trong một trang rồi xuất file**
- **Mô tả:** Nó soạn công văn/bài viết trong `show_html`, người dùng sửa trên trang, rồi xuất .docx/.pdf/.md.
- **Vì sao:** Người dùng cần bản nháp dùng được ngay, không phải copy từ bong bóng chat.
- **Kỹ thuật:** `show_html`/`HtmlActivity` + `ArtifactsUi` (phiên bản hoá đã có: `groupId`/`version`) + xuất file (chưa có — cần thêm bước lưu/print). Trên Mac có `ShellTools` để convert.
- **Công sức:** M
- **Rủi ro / ghi chú:** Xuất PDF/DOCX trên Android cần thêm dependency hoặc in qua `PrintManager` — cân nhắc chi phí trước.

**136. Quản lý dự án nhỏ bằng wiki**
- **Mô tả:** Mỗi dự án là một trang wiki có mục tiêu, người, việc, quyết định; trợ lý cập nhật khi người dùng kể.
- **Vì sao:** Người làm tự do/kinh doanh nhỏ ở Việt Nam không dùng Jira; wiki + việc là đủ.
- **Kỹ thuật:** `WikiBook`/`wiki_*` + `Tasks` + `People` + `wiki_link` (liên kết giữa trang) + `show_html` để xem.
- **Công sức:** M
- **Rủi ro / ghi chú:** Wiki do model viết có thể trùng lặp/mâu thuẫn; cần luật "một dự án một trang" trong `LocalPrompt`.

**137. Báo cáo hằng ngày tự động gửi cho quản lý**
- **Mô tả:** Cuối ngày nó tổng hợp việc đã làm và soạn báo cáo; người dùng bấm gửi (hoặc để nó gửi qua app chat nếu đã cấu hình).
- **Vì sao:** Viết báo cáo cuối ngày là việc ai cũng ghét và ai cũng phải làm.
- **Kỹ thuật:** `Tasks` + `WorkLog`/`PhoneWorkLog` (đã có dữ liệu bước) + `show_html` + `Scheduler`; gửi qua mail/chat thì như ý 133 (**◇**).
- **Công sức:** M
- **Rủi ro / ghi chú:** Báo cáo phải dựa trên việc *thật đã làm*, không được làm đẹp số liệu; cùng nguyên tắc "nói phải thật" của doc `proactive-nudge`.

**138. Pomodoro và chặn app gây xao nhãng**
- **Mô tả:** Người dùng nói "làm 25 phút"; nó đếm giờ, nhắc nghỉ, và gợi ý đóng app mạng xã hội đang mở.
- **Vì sao:** Tự quản lý tập trung là nhu cầu lớn; trợ lý có quyền đọc app đang mở nên làm được.
- **Kỹ thuật:** `Scheduler`/`Earcon` + `AndroidGear` (`UsageStatsManager`, cần quyền usage access) + `LaunchPadActivity`/màn hình chờ; chặn app thật thì cần quyền đặc biệt — chỉ gợi ý, không chặn.
- **Công sức:** M
- **Rủi ro / ghi chú:** "Chặn app" trên Android cần device admin/accessibility hacky; đừng đi con đường đó. Gợi ý + nhắc là đủ.

**139. Ghi chú nhanh ngay từ màn hình khoá**
- **Mô tả:** Không cần mở app: nói "ghi chú: mai mua sữa" và nó lưu, kèm thông báo nhỏ xác nhận.
- **Vì sao:** Ý tưởng đến lúc máy đang khoá; mở app rồi gõ là mất ý.
- **Kỹ thuật:** `TextInputActivity` (đã có activity nhập) + `NotificationRelayService`/quick tile hoặc shortcut màn hình khoá; `note.remember` qua `LocalBrain`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Trên màn hình khoá, nội dung hiện ra có thể bị người khác thấy — chỉ hiện "đã ghi".

**140. "Bắt tay vào việc": mở đúng app, đúng tài liệu, nhắc bước đầu**
- **Mô tả:** Người dùng nói "bắt đầu làm báo cáo"; nó mở app tài liệu, mở trang wiki dự án, và nhắc bước đầu tiên.
- **Vì sao:** Khởi động là phần khó nhất; một cú hích đúng lúc có giá trị thật.
- **Kỹ thuật:** `Stage`/`StageView` (Stage/StageShell/StageStore — đã có khái niệm stage) + `launch_app`/`device.launch` + `open_url` + `Plans`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Mở nhiều thứ cùng lúc trên Android dễ loạn task stack; phải kiểm tra trên HyperOS/MIUI (repo đã có ghi chú về việc Stage launch app).

**141. Chia sẻ nhật ký làm việc cho khách hàng/sếp**
- **Mô tả:** Xuất một bản tóm tắt "tuần này tôi đã làm gì" từ `WorkLog`/`Tasks`, có thể gửi bằng một nút.
- **Vì sao:** Người làm tự do Việt Nam phải báo cáo cho khách; báo cáo tự động tiết kiệm hàng giờ mỗi tuần.
- **Kỹ thuật:** `WorkLog`/`TimelineEvents` (Mac) + `Tasks` + `show_html`/`HtmlActivity` + chia sẻ file qua FileProvider.
- **Công sức:** S
- **Rủi ro / ghi chú:** WorkLog có thể chứa thông tin nội bộ của khách khác; phải cho chọn khoảng thời gian và rà trước khi xuất.

**142. Dịch tài liệu và email dài, giữ định dạng**
- **Mô tả:** Dán hoặc chia sẻ một tài liệu, nó dịch sang tiếng Việt/tiếng Anh và giữ tiêu đề, danh sách, bảng.
- **Vì sao:** Người Việt làm việc với đối tác nước ngoài cần dịch hằng ngày; dịch giữ định dạng là khác biệt so với copy-paste.
- **Kỹ thuật:** `Markdown`/`MarkdownBlocks` (đã có parser) + `LlmClient` + `show_html`; đầu vào qua Intent share hoặc dán vào `TextInputActivity`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Tài liệu dài = nhiều token; chia đoạn và cho người dùng thấy chi phí ước tính (ý 67).

---

## 12. Nhà thông minh & IoT

**143. Điều khiển đèn/quạt/ổ cắm qua HTTP trong mạng nhà**
- **Mô tả:** Thêm một tool gọi HTTP tới thiết bị trong nhà (đèn WiFi giá rẻ, công tắc thông minh) để bật/tắt bằng lời.
- **Vì sao:** Nhà thông minh giá rẻ ở Việt Nam (Tuya, Broadlink, công tắc nội địa) đều có HTTP/local API; đây là bước rẻ nhất để "điều khiển nhà".
- **Kỹ thuật:** **Cần thêm** một tool HTTP có allow-list host trong `ToolSchemas` + nhánh trong `CommandDispatcher` (mẫu đã có: `MarketTools` gọi HTTP qua OkHttp). Cấu hình thiết bị lưu trong `Notebook`.
- **Công sức:** L
- **Rủi ro / ghi chú:** Một tool HTTP tự do là lỗ hổng SSRF; phải allow-list host, chỉ trong LAN, và không cho model tự thêm host.

**144. Tích hợp Home Assistant**
- **Mô tả:** Người dùng đã có Home Assistant thì trợ lý gọi REST API của nó để đọc trạng thái và điều khiển thiết bị.
- **Vì sao:** Home Assistant là trung tâm nhà thông minh phổ biến nhất trong giới DIY Việt Nam; tích hợp một lần là mở cả trăm thiết bị.
- **Kỹ thuật:** Tool mới `ha_states`/`ha_call_service` (chưa có) trong `ToolSchemas` + `CommandDispatcher`; token trong `Vault`; danh sách entity lưu trong `Notebook`. **◇**
- **Công sức:** L
- **Rủi ro / ghi chú:** Home Assistant API không có chuẩn ổn định cho mọi bản; phải giới hạn entity mà model được thấy để không tràn context.

**145. Kịch bản "về nhà" và "đi làm"**
- **Mô tả:** Về tới nhà (hoặc tới giờ) thì bật đèn, mở nhạc, nhắc việc tối; đi làm thì tắt đèn, chuyển điện thoại sang chế độ im.
- **Vì sao:** Đây là kịch bản nhà thông minh mà người dùng hiểu ngay và kể cho người khác.
- **Kỹ thuật:** `ScenesUi`/`Scene` (trigger theo app/giờ) + IoT tools của ý 143/144 + `Plans` + `Scheduler`; **◇** phụ thuộc 143/144 có trước.
- **Công sức:** L
- **Rủi ro / ghi chú:** Điều kiện "đã về nhà" cần vị trí nền — pin và riêng tư; nên dùng geofence của hệ thống nếu có, hoặc chỉ dùng giờ.

**146. Điện thoại làm cảm biến: ánh sáng, tiếng ồn, chuyển động**
- **Mô tả:** Dùng cảm biến sẵn có để trả lời "phòng này tối không", "có tiếng động lạ không", và ghi lại mức ồn khi ngủ.
- **Vì sao:** Không cần mua thiết bị; điện thoại cũ đặt trong phòng đã là một cảm biến.
- **Kỹ thuật:** `read_sensors`/`device.sensors` (đã có) + `get_step_count` + `AudioIO.startStreaming` và tính RMS trong code đo + `Scheduler` để lấy mẫu theo giờ + `Notebook` lưu kết quả.
- **Công sức:** M
- **Rủi ro / ghi chú:** Ghi âm môi trường là nghe lén nếu không nói rõ; chỉ bật khi người dùng chủ động và có đèn báo.

**147. Robot dog tuần tra và báo tiếng động lạ**
- **Mô tả:** Về đêm, robot đi một vòng theo lịch; phát hiện tiếng động bất thường thì gửi bubble + ảnh chụp.
- **Vì sao:** Đã có `dog_*` + `DogSense`; đây là ứng dụng "canh nhà" dễ hiểu và ấn tượng.
- **Kỹ thuật:** `DogLink`/`DogMotion`/`DogSense` + `Flow` (một `dog_sequence` là một flow) + `NudgeUi` + `Scheduler`. **◇** phụ thuộc phần cứng thật và môi trường nhà.
- **Công sức:** L
- **Rủi ro / ghi chú:** Robot di chuyển khi có người/trẻ em/thú nuôi; phải có nút dừng và giới hạn khu vực. Không bao giờ tự chạy nếu chưa bật rõ ràng.

**148. Chia sẻ trạng thái nhà cho người thân ở xa**
- **Mô tả:** Con cái ở xa xem được "bố/mẹ đã uống thuốc chưa, nhà có ai lạ không" qua một bản tóm tắt, không cần gọi điện.
- **Vì sao:** Chăm sóc bố mẹ ở xa là nhu cầu rất thật của người Việt đi làm xa.
- **Kỹ thuật:** `PeerTasks`/`PeerLink` (đã có kênh giữa hai Andee) + `NudgeUi`/thông báo + `People`. **◇** cần hai bên cùng cài và cùng đồng ý.
- **Công sức:** L
- **Rủi ro / ghi chú:** Đây là giám sát người khác; phải có sự đồng ý rõ ràng của người bị theo dõi, và tắt được một chiều.

**149. Gợi ý tiết kiệm điện từ thói quen sạc và dùng máy**
- **Mô tả:** Nó thấy máy sạc qua đêm 8 tiếng, app chạy nền nhiều, và gợi ý "tắt sạc nhanh khi đủ 80%", "đóng app X".
- **Vì sao:** Pin và tiền điện là mối quan tâm thật; trợ lý chạy nền cũng cần tự biết mình tốn điện.
- **Kỹ thuật:** `get_battery` + `AndroidGear` (`UsageStatsManager`) + `read_sensors`; gợi ý qua `NudgeUi`; báo cáo tuần trong `show_html`.
- **Công sức:** M
- **Rủi ro / ghi chú:** Đừng đưa lời khuyên điện năng chung chung; chỉ nói điều đo được từ máy này.

**150. Xem camera an ninh qua app rồi hỏi "có ai không"**
- **Mô tả:** Trợ lý mở app camera (nếu người dùng đã đăng nhập), chụp màn hình, và trả lời câu hỏi về khung hình.
- **Vì sao:** Nhiều nhà Việt Nam có camera IP xem qua app; hỏi bằng lời tiện hơn mở app.
- **Kỹ thuật:** `launch_app` + `take_screenshot`/`camera_turn` + `LlmClient` vision. **◇** phụ thuộc app camera và chất lượng ảnh.
- **Công sức:** M
- **Rủi ro / ghi chú:** Hình ảnh nhà gửi lên cloud là rủi ro riêng tư lớn; phải hỏi trước mỗi lần và ưu tiên mô hình chạy trên máy nếu có.

**151. Tablet cũ thành loa/bảng thông báo trong nhà**
- **Mô tả:** Một tablet treo tường đọc to lời nhắc, hiện lịch, và nhận lệnh giọng; nó là "thân" thứ hai của cùng một trợ lý.
- **Vì sao:** Đã có `BodyWsServer`/`BodyWsClient` và chế độ hub; chỉ thiếu UI treo tường đúng nghĩa.
- **Kỹ thuật:** `BodyWsServer` (LAN) + `TtsController` + `AsrController` + `LaunchPadActivity`/màn hình chờ (ý 11) + `MacNotifications`-tương đương cho thông báo.
- **Công sức:** M
- **Rủi ro / ghi chú:** Android cũ + màn hình sáng liên tục; phải xử lý cháy màn hình và nhiệt.

**152. Đánh giá Matter/Thread trước khi cam kết**
- **Mô tả:** Một tài liệu quyết định: có nên đi theo chuẩn Matter thay vì tích hợp từng hãng, chi phí và thời gian ra sao.
- **Vì sao:** Nhà thông minh đang chuyển sang Matter; chọn sai hướng nghĩa là làm lại. Đây là việc **nghiên cứu**, không phải code.
- **Kỹ thuật:** Không code. Cần trả lời: SDK nào chạy được trên Android + Kotlin hiện tại, cần quyền gì, có yêu cầu hub hay không, có phù hợp một app overlay không. **◇**
- **Công sức:** L
- **Rủi ro / ghi chú:** Matter cần thiết bị phần cứng thật để kiểm; đừng hứa tính năng trước khi có thiết bị thử.

---

## Lộ trình ưu tiên

Nguyên tắc xếp: **cái gì làm tăng niềm tin trước, cái gì làm tăng năng lực sau, cái gì cần đối tác/nghiên cứu để cuối.** Lý do là một trợ lý được cấp quyền chạm vào điện thoại mà người dùng không tin thì mọi tính năng sau đều vô nghĩa.

### Giai đoạn 1 — "bản Việt hoá đáng tin" (0–8 tuần)

Mục tiêu: người dùng Việt mở app thấy đúng tiếng Việt, hiểu nó đang làm gì, và không gặp lỗi "âm thầm" nữa. Toàn bộ nhóm này là S/M, không đụng vào lõi brain.

- **Sửa lỗi trước:** 86 (cuộc gọi Zalo/Messenger), 73 (điểm mù camera/màn hình khoá), 1 (chuỗi cứng).
- **Tin cậy & an toàn:** 82 (không tự bấm nút thanh toán), 70 (bảng quyền), 74 (cảnh báo ghi âm), 50 (OTP), 71 (danh sách app không tự bấm).
- **Hiểu người dùng Việt:** 17 (ngắn/dài), 18 (hỏi lại bằng nút), 23 (không dấu), 119 (từ địa phương).
- **Trải nghiệm hằng ngày:** 3 (bóng to/nhỏ), 5 (phụ đề to), 8 (tìm không dấu), 15 (im lặng hoàn toàn), 16 (nhãn trạng thái), 94 (earcon), 10 (bóng né chỗ).
- **Thấy được chi phí:** 61 (token/ngày), 56 (xem nó đang quan tâm gì).
- **Rẻ mà hữu ích:** 116 (đi chợ), 123 (gọi người thân), 127 (đọc chậm), 122 (chữ to), 104 (báo lỗi).

### Giai đoạn 2 — "trợ lý làm được việc thật" (2–4 tháng)

Mục tiêu: từ "trả lời" thành "làm xong việc", có kiểm soát chi phí và có đường lùi.

- **Hạ tầng phải có trước:** 58 + 59 (nhiều provider + key trong Vault), 60 (định tuyến theo việc), 62/67 (hạn mức, ước lượng), 38 (việc sống qua restart), 53 (export dữ liệu).
- **Tác vụ & tự động hoá:** 31 (mẫu tiếng Việt), 32 (sửa flow bằng lời), 34 (flow theo lịch), 41 (mẫu mỗi sáng), 39 (ảnh từng bước), 43 (hàng đợi).
- **Bộ nhớ:** 45 (mục lục + tìm), 46 (đọc lại context đã nén), 47 (hồ sơ người thân), 48 (wiki xuất được), 51 (xác nhận trước khi nhớ), 52 (xoá theo chủ đề).
- **Giọng nói:** 83 (wake word Việt), 84 (ASR có dấu), 85 (TTS chọn giọng), 87 (nói liên tục), 90 (biên bản họp), 93 (đọc màn hình).
- **Người dùng Việt:** 107 (chi tiêu từ SMS), 108 (hoá đơn), 111 (Zalo chỉ đọc), 112/113/114 (học tập), 115 (sức khoẻ), 117 (giá xăng/thời tiết), 120 (Tết).
- **Người lớn tuổi / khiếm thị (bước đầu):** 124 (nhắc thuốc), 125 (chống lừa đảo), 126 (điều khiển bằng giọng), 130 (overlay không phá TalkBack), 131 (khẩn cấp).
- **Công việc:** 132 (bản tin sáng), 133 (biên bản → việc), 135 (soạn + xuất), 136 (dự án bằng wiki), 137 (báo cáo ngày), 141 (chia sẻ WorkLog), 142 (dịch tài liệu).
- **Thiết bị:** 97 (hub gia đình), 99 (nhờ bạn), 100 (chuyển tiếp thông báo), 103 (QR có tên), 106 (intent thay vì chạm), 151 (tablet treo tường).

### Giai đoạn 3 — "nghiên cứu & đối tác" (4–12 tháng)

Mục tiêu: những việc cần dữ liệu thật, đối tác, phần cứng, hoặc thay đổi kiến trúc. Không hứa ngày.

- **Cần người dùng thật để thiết kế:** 121 (chế độ khiếm thị đầy đủ — phải làm với người khiếm thị), 75 (chế độ khách), 76 (PIN).
- **Cần thay đổi lõi:** 33 (flow rẽ nhánh), 63 (so hai model), 80 (mã hoá sổ tay), 101 (đồng bộ hai máy), 96 (chuyển phiên), 98 (nhờ máy khác làm hộ).
- **Cần phần cứng/đối tác:** 102 (robot dog), 143 + 145 (IoT nội bộ), 144 (Home Assistant), 146 (cảm biến), 147 (tuần tra), 148 (chia sẻ trạng thái nhà), 150 (camera), 152 (Matter).
- **Cần đối tác/API không tồn tại:** 109 (dịch vụ công), 110 (bệnh viện), 118 (vé Tết), 31 (một phần), 133 (email gửi thay), 134 (nội dung chat đầy đủ), 95 (đọc Zalo tự động).

**Thứ tự trong giai đoạn 3, nếu phải chọn một:** 143 + 145 (IoT nội bộ) vì nó dùng đúng hạ tầng đã có (`BodyWsServer` + tool HTTP), rẻ hơn Home Assistant và không phụ thuộc đối tác.

## Những ý chỉ ở mức mockup

Các ý dưới đây **không thể làm thật ngay** — hoặc vì app bên thứ ba, hoặc vì API/quyền không tồn tại, hoặc vì rủi ro pháp lý/phạm vi. Chúng vẫn đáng ghi lại vì chúng định hướng phần "làm được" ở trên:

| Ý | Vì sao chỉ là mockup |
|---|---|
| 31 (một phần) | Các app đặt cơm/đặt xe đổi giao diện liên tục; luồng ghi sẵn sẽ hỏng. Chỉ nên là "mẫu hướng dẫn", không phải flow tin cậy. |
| 63 | Cần hai provider thật, tốn gấp đôi; là quyết định sản phẩm, không phải kỹ thuật. |
| 95 | Đọc tin nhắn thoại trong Zalo = tự thao tác app chat, cùng loại rủi ro khiến repo cấm WeChat. |
| 109 | Không có API cổng dịch vụ công; trang đổi thường xuyên; phần "điền tự động" không bền. |
| 110 | Phụ thuộc app/website từng bệnh viện; không có API. |
| 118 | Phụ thuộc website bán vé; tỉ lệ thành công thấp là bản chất. |
| 133 (phần gửi) | Repo không có API email/chat để gửi; chỉ soạn nháp được. |
| 134 | Tóm tắt đầy đủ nội dung chat cần tự thao tác app chat — không làm. Chỉ tóm tắt từ thông báo. |
| 144 | Cần Home Assistant thật để kiểm; API đa phiên bản; chưa có tool. |
| 145 | Phụ thuộc 143/144 có trước. |
| 147 | Cần robot thật và môi trường nhà để kiểm an toàn. |
| 150 | Cần app camera và chất lượng ảnh; riêng tư là vấn đề chưa giải. |
| 152 | Là tài liệu quyết định, không phải tính năng. |

**Ghi chú cuối:** con số 152 ý tưởng ở trên không phải cam kết làm hết. Giá trị thật của tài liệu này là ba thứ: (1) mọi ý đều chỉ được tới một lớp/hàm có thật, để người làm không phải đoán; (2) những chỗ **phải sửa lỗi trước khi làm tính năng** (86, 73, 130) đã được nêu rõ; (3) các ý mockup đã được tách ra để không ai hứa với người dùng điều không làm được.
