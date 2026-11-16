# Báo cáo build APK — VUDEV ANDEE 2

Ngày: 2026-11-16. Nền: commit 4a66230 (upstream + tiếng Việt) cùng các commit VUDEV 69ed0a4, 1dd4b7a, b300d8d, d872a69.

## 1. Kết quả

| Mục | Giá trị |
|---|---|
| Kết quả | BUILD SUCCESSFUL |
| Task | :app:assembleDebug |
| Tệp APK | app/build/outputs/apk/debug/app-debug.apk |
| Kích thước | 29.228.359 byte (~27,9 MiB) |
| SHA-256 | 81b58a26e51ac794dbf3ed331fa669e7663f04ecc4102c66a54b3e5abefa5fe1 |
| Package | net.kuafuai.andee |
| versionName / versionCode | 0.15.0 / 30 (giữ số của upstream; nhận diện bản build bằng commit) |
| minSdk / targetSdk / compileSdk | 27 / 34 / 34 |
| Nhãn ứng dụng | Andee (giữ nguyên theo giấy phép, không white-label) |
| Chữ ký | debug, bằng debug.keystore ở gốc kho (alias androiddebugkey) |

Lưu ý: ABI split đang tắt trong app/build.gradle (từ commit 4a66230), nên đầu ra là một APK phổ thông app-debug.apk, không phải app-<abi>-debug.apk như tài liệu upstream cũ.

## 2. Toolchain

| Thành phần | Phiên bản / đường dẫn |
|---|---|
| OS | Linux Mint 22.3 (Ubuntu 24.04), kernel 7.0.0-43, 4 nhân, 3,7 GB RAM |
| JDK build | Eclipse Temurin 17.0.21 (trong ~/.gradle/jdks) |
| Gradle | wrapper 9.3.1 |
| Android Gradle Plugin | 8.3.1 |
| Kotlin | 2.1.21 |
| R8 | 8.7.18 (ghim trên buildscript classpath) |
| Android SDK | platforms android-34 (AGP tự tải), 36, 36.1; build-tools 35.0.0 và 36.0.0 |
| Thiết bị đích | Redmi 15 5G (25057RN09G), Android 16, arm64-v8a |

## 3. Lệnh build

    export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :app:assembleDebug --console=plain --no-daemon -PdefaultLang=vi
      -Dorg.gradle.jvmargs="-Xmx1600m -XX:MaxMetaspaceSize=640m"
      -Dorg.gradle.parallel=false --max-workers=2
      -Dkotlin.daemon.jvmargs="-Xmx1000m"

Tham số bộ nhớ là bắt buộc trên máy 4 GB: cấu hình mặc định (3 GB heap + parallel + nhiều worker) làm Gradle/Kotlin daemon tranh RAM, build chậm hoặc bị kill. Với cấu hình trên: build đầy đủ đầu tiên khoảng 9 phút, build tăng dần khoảng 1 phút.

Trước khi build đã tạo local.properties (sdk.dir) và debug.keystore; cả hai có hướng dẫn trong docs/BUILD.md.

## 4. Cảnh báo

- 38 cảnh báo biên dịch, toàn bộ là API Android deprecated có sẵn từ upstream (ACTION_CLOSE_SYSTEM_DIALOGS, SMS manager mặc định, resolveService của NsdManager, SOFT_INPUT_ADJUST_RESIZE…). Không có cảnh báo nào từ mã mới của VUDEV.
- Gradle cảnh báo không tương thích Gradle 10 — wrapper 9.3.1 đi cùng AGP 8.3.1; cấu hình chạy được nhưng nên nâng AGP/Kotlin cùng lúc trước khi lên Gradle 10.
- stripDebugDebugSymbols không strip được libbarhopper_v3.so và libimage_processing_util_jni.so (ML Kit); chúng được đóng gói nguyên trạng — hành vi có sẵn.

## 5. Build từ xa (GitHub Actions)

Workflow .github/workflows/build-apk.yml:
- push nhánh main, pull_request hoặc workflow_dispatch: build debug, upload artifact app-debug.
- tag v* hoặc workflow_dispatch: build thêm release unsigned, artifact app-release-unsigned (phải ký trước khi cài).
- Dùng JDK 17, cài platforms;android-34 và build-tools;34.0.0, chạy :core:test rồi :app:assembleDebug, cache ~/.gradle.
- Không cần secret; không truyền khoá qua -P (build.gradle từ chối).
- debug.keystore đã commit nên chữ ký APK giữa các lần chạy ổn định; workflow chỉ sinh keystore dự phòng khi tệp vắng mặt.

## 6. Kết quả GitHub Actions

- Workflow build-apk (nhánh main, run 43789075876): THÀNH CÔNG sau 4 phút 41 giây. Artifact app-debug (26.763.412 byte, nén zip) đã sẵn sàng để tải và cài từ xa.
- Workflow build-apk cho tag v0.15.0-vudev2.1 (run 43789159733): THÀNH CÔNG; job debug APK và job release APK (unsigned) đều xanh, tạo thêm artifact app-release-unsigned.
- Workflow build của upstream: sau commit cập nhật golden (dbddf13), run 43790692282 xanh cả hai job release-gate và debug-apk. Trước đó debug-apk đỏ vì 3 test golden chưa được cập nhật từ upstream 6255a4f (lỗi có sẵn).
- Bản phát hành có APK tải trực tiếp: https://github.com/vukiengiang2025-sys/vudev-andee2/releases/tag/v0.15.0-vudev2.1

## 7. Cài đặt

    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell am start -n net.kuafuai.andee/.ui.SelfCheckActivity

Trong phiên này thiết bị mất kết nối adb trước bước cài, nên bước cài/chạy trên máy thật chưa thực hiện. APK đã được kiểm tra bằng aapt/aapt2: đúng package, nhãn, SDK và có đủ chuỗi tiếng Việt mới (ví dụ provider_center_title = Trung tâm AI, settings_pause = Tạm dừng trợ lý).
