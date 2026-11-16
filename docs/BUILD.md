# Tự build VUDEV ANDEE 2

Hướng dẫn build APK Android trên **Linux Mint** (và Ubuntu/Debian tương tự). Dành cho người đã quen dùng terminal một chút; không cần biết Kotlin.

- Chỉ muốn cài bản có sẵn: xem [INSTALL.md](INSTALL.md).
- Lỗi thường gặp khi build: xem [mục 10](#10-lỗi-gradle-thường-gặp) và [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

---

## 1. Dự án gồm những gì

Ba module Gradle (tên dự án gốc: `body`):

| Module | Vai trò | Ghi chú build |
| --- | --- | --- |
| `core/` | Bộ não dùng chung: vòng lặp agent, prompt, bộ chuyển đổi nhà cung cấp AI, sổ tay, bạn bè | JVM thuần, test chạy trên máy tính, không cần Android SDK |
| `app/` | Phần Android: quả cầu nổi, dịch vụ trợ năng, micro, loa, cài đặt | Cần Android SDK 34 |
| `desktop/` | Bản macOS (Compose Desktop) | Chỉ build trên macOS; trên Mint bỏ qua |

APK Android nằm ở module `app`. Kết quả build là **APK debug** ký bằng khoá debug (cài được ngay) hoặc APK release **chưa ký** (phải tự ký mới cài được).

Thông số hiện tại (xem `app/build.gradle`, `gradle/libs.versions.toml`):

| Mục | Giá trị |
| --- | --- |
| `compileSdk` / `targetSdk` | 34 (Android 14) |
| `minSdk` | 27 (Android 8.1) |
| `versionCode` / `versionName` | 30 / 0.15.0 |
| AGP | 8.3.1 |
| Kotlin | 2.1.21 |
| Gradle wrapper | 9.3.1 |
| JDK khuyến nghị | **17** (dự án ghi 17–20; CI dùng Temurin 17) |
| Java/Kotlin JVM target | 1.8 |

> Bản fork đã bỏ `jvmToolchain(17)` và nâng wrapper từ 8.4 lên 9.3.1 trong commit `4a66230`. Vì vậy JDK 21 trên Mint có thể build được, nhưng **17 là mức được CI và tài liệu dự án kiểm chứng**; nếu gặp lỗi công cụ lạ, hạ về 17 trước khi tìm nguyên nhân khác.

---

## 2. Cài JDK 17 trên Linux Mint

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk unzip wget
java -version
```

Kết quả phải ghi `openjdk version "17…"`. Nếu máy có nhiều JDK, chọn đúng JDK cho phiên làm việc:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Để cố định cho các lần mở terminal sau, thêm hai dòng `export` vào `~/.bashrc`.

**Không sửa `gradle.properties` của kho để nhúng đường dẫn JDK.** Nếu cần cố định riêng cho máy, thêm vào `~/.gradle/gradle.properties`:

```properties
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
```

Nếu bạn dùng Android Studio: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → chọn JDK 17.

---

## 3. Cài Android SDK 34

### 3.1. Tải command-line tools

Tải bản "Command line tools only" cho Linux tại trang chính thức:
`https://developer.android.com/studio#command-line-tools-only`

Ví dụ (đổi số phiên bản nếu Google đã phát hành bản mới):

```bash
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d "$HOME/Android/Sdk/cmdline-tools"
mv "$HOME/Android/Sdk/cmdline-tools/cmdline-tools" "$HOME/Android/Sdk/cmdline-tools/latest"
```

### 3.2. Đặt biến môi trường và cài các gói

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --version
yes | sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Kiểm tra:

```bash
sdkmanager --list_installed | grep -E 'platforms;android-34|build-tools;34.0.0|platform-tools'
adb version
```

Nếu `sdkmanager` báo thiếu Java, quay lại mục 2 — nó dùng `JAVA_HOME`.

---

## 4. Trỏ Gradle vào SDK

Tạo `local.properties` ở thư mục gốc kho (tệp này **không được commit** — đã nằm trong `.gitignore`):

```bash
cd ~/vscode/vudev-andee2
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

Kiểm tra:

```bash
cat local.properties
# sdk.dir=/home/ten-nguoi-dung/Android/Sdk   (đường dẫn tuyệt đối, $HOME được mở rộng khi chạy lệnh trên)
```

Không đặt khoá API vào `local.properties`. Bản fork cố tình không nhúng khoá khi build (xem phần "Credentials are hard-coded empty" trong `app/build.gradle`).

---

## 5. debug.keystore

`app/build.gradle` cấu hình bản debug ký bằng tệp `debug.keystore` ở **thư mục gốc kho**:

```gradle
signingConfigs {
    debugConfig {
        storeFile file("${rootDir}/debug.keystore")
        storePassword "android"
        keyAlias "androiddebugkey"
        keyPassword "android"
    }
}
```

Tệp này **được commit trong kho** (khoá debug, không phải khoá release) để mọi lần build — trên máy và trên GitHub Actions — dùng **cùng một chữ ký**, nhờ đó adb install -r cập nhật được mà không mất dữ liệu. Nếu checkout của bạn thiếu tệp, tạo lại bằng lệnh:

```bash
cd ~/vscode/vudev-andee2
keytool -genkeypair -v \
  -keystore debug.keystore \
  -storepass android -keypass android \
  -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

Nếu không tạo, Gradle dừng với lỗi kiểu:

```text
Keystore file '/…/debug.keystore' not found for signing config 'debugConfig'.
```

**Giữ tệp này cố định.** Android chỉ cho cập nhật ứng dụng nếu bản mới cùng chữ ký. Keystore debug trong kho là khoá dùng chung cho bản debug; khoá ký **release** thật thì không bao giờ được commit.

> Nếu bạn không muốn dùng keystore cố định, có thể bỏ cấu hình `signingConfigs` và để Gradle dùng debug keystore mặc định của máy (`~/.android/debug.keystore`). Nhưng đừng sửa file gradle nếu bạn chỉ đang làm theo tài liệu này; hãy chọn một cách và giữ nguyên.

---

## 6. Build APK debug

```bash
cd ~/vscode/vudev-andee2
chmod +x gradlew
./gradlew :app:assembleDebug --max-workers=2 -Dorg.gradle.jvmargs=-Xmx2g
```

Lần đầu sẽ tải Gradle + toàn bộ thư viện (khoảng 1 GB), có thể mất 5–15 phút. Các lần sau nhanh hơn nhờ cache.

### Đường dẫn APK

Cấu hình **hiện tại** trong `app/build.gradle` đặt `splits.abi.enable = false` (thay đổi trong commit `4a66230`), nên Gradle tạo **một APK duy nhất**:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Nếu bạn (hoặc bản fork khác) bật lại ABI splits bằng `enable true`, Gradle tạo một APK cho mỗi kiến trúc:

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk      ← Redmi 15 5G, phần lớn điện thoại
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk    ← máy 32-bit cũ
app/build/outputs/apk/debug/app-x86_64-debug.apk         ← emulator x86_64
```

> **Lưu ý tài liệu cũ:** `README.md` và `CLAUDE.md` vẫn mô tả "không còn `app-debug.apk`, mỗi ABI một APK". Điều đó đúng với upstream trước `4a66230`, nhưng **không đúng với cấu hình hiện tại của bản fork**. Hãy kiểm tra `app/build.gradle` (dòng `splits → abi → enable`) và dùng đúng tệp có trên đĩa.
>
> Script `build-and-install.sh` vẫn tìm `app-<abi>-debug.apk` trước, rồi fallback sang `app-*-debug.apk`. Với cấu hình hiện tại, hãy cài thủ công `app-debug.apk` bằng `adb install -r` để tránh nhầm.

### Cài trực tiếp lên máy đang cắm USB

```bash
./gradlew :app:installDebug
# hoặc
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. Chạy unit test

```bash
./gradlew :core:test --max-workers=2
```

Báo cáo HTML:

```text
core/build/reports/tests/test/index.html
```

Kết quả từng test (XML, dễ đọc bằng máy):

```bash
ls core/build/test-results/test/
grep -R "failure" core/build/test-results/test/*.xml | head
```

App Android có test JVM riêng:

```bash
./gradlew :app:testDebugUnitTest
```

Test cần thiết bị thật/emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

> **Trạng thái hiện tại:** một số test của `core` có thể đỏ ở trạng thái đang phát triển (WIP). Đọc tên test trong báo cáo trước khi kết luận bản build hỏng. Chỉ cần `:app:assembleDebug` xanh là bạn có APK cài được.

---

## 8. Build bản release (chưa ký)

```bash
./gradlew :app:assembleRelease --max-workers=2
```

Kết quả:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
# hoặc nhiều tệp app-<abi>-release-unsigned.apk nếu bật ABI splits
```

APK này **không cài được** cho đến khi ký. Ký bằng khoá của bạn:

```bash
"$ANDROID_HOME/build-tools/34.0.0/apksigner" sign \
  --ks my-release.keystore \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --print-certs app-release-signed.apk
```

Release bật R8 (thu nhỏ + obfuscate), nên nếu ứng dụng chạy debug tốt mà release lỗi, đó thường là thiếu keep rule trong `app/proguard-rules.pro`.

---

## 9. Build trên GitHub Actions

Kho có ba workflow:

| Workflow | Khi nào chạy | Cho ra gì |
| --- | --- | --- |
| `build-apk.yml` | Push lên `main`, tag `v*`, pull request, hoặc chạy tay | Artifact **app-debug** (APK debug) và **app-release-unsigned** (khi tag/chạy tay) |
| `build.yml` | Push/PR | Cổng kiểm tra compile + `:app:testDebugUnitTest` |
| `dco.yml` | Pull request | Kiểm tra commit sign-off |

Cách tải APK:

1. Mở **Actions** → workflow **build-apk** → chọn lần chạy xanh.
2. Mục **Artifacts** → tải **app-debug** (zip) → giải nén → cài `app-debug.apk`.

Workflow dùng **JDK 17**, cài `platforms;android-34` và `build-tools;34.0.0`, chạy `:core:test` rồi `:app:assembleDebug`.

> ✅ **Chữ ký ổn định giữa các lần chạy.** debug.keystore được commit trong kho nên APK tải từ Actions và APK build trên máy có **cùng chữ ký**; cài đè (adb install -r) giữ được dữ liệu. Workflow chỉ tạo keystore dự phòng khi tệp vắng mặt. Bản **release** vẫn không có khoá ký trong kho nên được phát hành dưới dạng **unsigned** và phải ký thủ công trước khi cài.

Nếu không muốn dùng Actions, có thể build trên máy như mục 6.

---

## 10. Lỗi Gradle thường gặp

| Thông báo / triệu chứng | Nguyên nhân | Cách sửa |
| --- | --- | --- |
| `Unsupported class file major version 69` hoặc lỗi "Gradle cannot run on Java …" | JDK quá mới/không khớp wrapper | Dùng JDK 17: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` |
| `SDK location not found` | Thiếu `local.properties` hoặc sai `sdk.dir` | Làm lại mục 4; kiểm tra `echo $ANDROID_HOME` |
| `Failed to install the following Android SDK packages… licenses not accepted` | Chưa đồng ý license | `yes | sdkmanager --licenses` rồi cài lại gói |
| `platforms;android-34 not found` | Thiếu platform 34 | `sdkmanager --install "platforms;android-34"` |
| `Keystore file '…/debug.keystore' not found` | Thiếu keystore debug | Tạo theo mục 5 |
| `Permission denied: ./gradlew` | Mất quyền thực thi | `chmod +x gradlew` |
| `Java heap space` / `Daemon disappeared` / build bị kill | Máy thiếu RAM, Gradle chạy quá nhiều worker | `--max-workers=2 -Dorg.gradle.jvmargs=-Xmx2g`; đóng Android Studio/trình duyệt; không chạy hai build cùng lúc |
| `Could not resolve com.github.mik3y:usb-serial-for-android` | Không vào được JitPack | Kiểm tra mạng/proxy; chạy lại; xem `settings.gradle` có `jitpack.io` |
| `Could not download gradle-9.3.1-bin.zip` | Mạng chặn services.gradle.org | Tải thủ công rồi đặt vào `~/.gradle/wrapper/dists/`, hoặc dùng proxy |
| `AAPT2 aapt2-…-linux: error` | Thư viện build-tools hỏng hoặc thiếu gói phụ thuộc | Cài lại `build-tools;34.0.0`: `sdkmanager --install "build-tools;34.0.0"` rồi thử lại |
| R8 lỗi metadata Kotlin 2.1 khi `assembleRelease` | AGP 8.3 dùng R8 cũ | Root `build.gradle` đã pin `com.android.tools:r8:8.7.18`; nếu bạn nâng Kotlin, nâng cả R8 |
| Build xanh nhưng `adb install` báo `INSTALL_FAILED_NO_MATCHING_ABIS` | APK không có ABI của máy | Dùng `app-debug.apk` (đa ABI khi tắt splits) hoặc đúng APK arm64-v8a |
| Build xanh nhưng cài báo chữ ký | Khác keystore | Xem [INSTALL.md](INSTALL.md#8-cập-nhật-và-xung-đột-chữ-ký-apk) |

Xem thêm log đầy đủ:

```bash
./gradlew :app:assembleDebug --stacktrace --info 2>&1 | tee /tmp/build.log
```

---

## 11. Build trên macOS (module desktop) — chỉ khi cần

Module `desktop` là bản macOS (Compose Desktop + Swift bridge), không build được trên Linux Mint. Trên Mac có Xcode Command Line Tools:

```bash
./gradlew :desktop:createDistributable
# → desktop/build/compose/binaries/main/app/Andee.app
```

Không cần bước này để có APK Android.

---

## 12. Những điều không nên làm

- **Không chạy hai build Gradle cùng lúc** trên máy ít RAM; chúng tranh daemon và dễ bị OOM-kill.
- **Không commit local.properties, khoá ký release, hay khoá API** vào kho. debug.keystore là ngoại lệ có chủ đích (khoá debug dùng chung cho CI). Build đã có kiểm tra từ chối khoá truyền qua `-P`.
- **Không sửa `app/build.gradle` chỉ để "cho nhanh"** nếu bạn không định duy trì fork: bật/tắt splits, đổi `applicationId` hay bỏ signingConfig đều làm APK không cập nhật được hoặc không cài được.
- **Không chạy `./gradlew clean` mỗi lần** — cache là thứ giúp build nhanh; chỉ clean khi gặp lỗi tài nguyên kỳ lạ.

---

## 13. Sau khi build

- Cài lên máy: [INSTALL.md](INSTALL.md).
- Nhập khoá AI: [AI-PROVIDERS.md](AI-PROVIDERS.md).
- Nếu ứng dụng mở lên nhưng không thấy quả cầu: [TROUBLESHOOTING.md](TROUBLESHOOTING.md#1-không-thấy-quả-cầu).
