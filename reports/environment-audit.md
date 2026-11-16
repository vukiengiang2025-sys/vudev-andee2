# Báo cáo kiểm tra môi trường — VUDEV ANDEE 2

Ngày kiểm tra: 2026-11-16 (Asia/Ho_Chi_Minh)
Máy: vukgg04-Inspiron-14-3467

## 1. Hệ điều hành và phần cứng

| Mục | Giá trị | Ghi chú |
|---|---|---|
| OS | Linux Mint 22.3 (Zena), nền Ubuntu 24.04 | OK |
| Kernel | 7.0.0-43-generic x86_64 | OK |
| CPU | 4 lõi | Đủ cho Gradle 2 worker |
| RAM | 3,7 GB (khả dụng lúc đầu ~1,4 GB) | **Thấp** — đã cấu hình lại Gradle |
| Swap | 20 GB (gồm zram 1,5 GB) | Bù RAM, chậm hơn |
| Đĩa /home | 122 GB, còn 36 GB | Đủ (Gradle cache + SDK + build ~10 GB) |

## 2. Công cụ

| Công cụ | Trạng thái | Ghi chú |
|---|---|---|
| git | 2.43.0 | Có sẵn |
| gh (GitHub CLI) | **Đã cài** 2.104.0 → ~/.local/bin/gh | Tải bản release chính thức vì sudo cần mật khẩu |
| JDK hệ thống | OpenJDK 21.0.13 | Không dùng để build (wrapper 9.3.1 + AGP 8.3 cần 17–20) |
| JDK 17 (Temurin) | **Đã có** ở ~/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 | Gradle tự tải; dùng cho mọi lệnh build |
| Android SDK | ~/Android/Sdk | platforms android-34 (AGP tự tải thêm), 36, 36.1; build-tools 35.0.0, 36.0.0; platform-tools; cmdline-tools latest; licenses đã chấp nhận |
| Gradle | Wrapper 9.3.1 (đã có trong ~/.gradle) | Không cần cài Gradle hệ thống |
| Kotlin | 2.1.21 (do project khai báo) | Không cần kotlinc hệ thống |
| adb | Có sẵn (/usr/bin/adb) | Thiết bị đã kết nối |
| jq / ripgrep | Không có | Không cần: agent dùng tool grep/read nội bộ |
| Android Studio / Emulator | **Không cài** | Không cần thiết; build bằng CLI |

## 3. Thiết bị Android

- Model: **25057RN09G** (Redmi 15 5G), Android **16**, ABI arm64-v8a.
- adb: đã kết nối; lệnh adb devices trả về trạng thái device (đã cấp quyền USB debugging).
- Đích cài đặt: APK debug theo ABI app-arm64-v8a-debug.apk.

## 4. Mạng

| Đích | Kết quả |
|---|---|
| github.com | 200 |
| repo.maven.apache.org | 200 |
| dl.google.com (Maven) | truy cập được (AGP đã tải platform 34, thư viện) |
| services.gradle.org | truy cập được |
| maven.aliyun.com | truy cập được |
| Proxy | Không đặt biến proxy |

## 5. Hạn mức DeepSeek API

Không có biến môi trường hay tệp key DeepSeek trong shell của phiên làm việc, nên **không đọc được số dư/hạn mức** từ máy. Phiên agent chạy qua Harness của DeepSeek, việc đếm token do Harness quản lý. Để tránh hết hạn mức giữa việc:

- Ưu tiên đọc/ghi có mục tiêu, không đọc cả tệp lớn, cắt log;
- Build/test chạy nền, gộp một lần thay vì lặp;
- Không gọi API nhà cung cấp AI thật trong lúc phát triển (chỉ dùng mock server cục bộ trong unit test);
- Nếu hạn mức cạn: phần còn lại (build APK, test thiết bị, tài liệu) không phụ thuộc API sinh văn bản.

## 6. Quyền và giới hạn

- **sudo cần mật khẩu** → không cài gói apt. Mọi công cụ bổ sung được cài trong thư mục người dùng (~/.local/bin, ~/.gradle/jdks).
- Không đổi cấu hình bảo mật hệ thống, không cài trình giả lập, không cài Android Studio.
- Thiết bị Android đã bật USB debugging và đã cấp quyền cho máy tính này.

## 7. Cấu hình build cho máy RAM thấp

Đã dùng cho toàn bộ quá trình:

    export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :app:assembleDebug --console=plain --no-daemon \
      -Dorg.gradle.jvmargs="-Xmx1600m -XX:MaxMetaspaceSize=640m" \
      -Dorg.gradle.parallel=false --max-workers=2 \
      -Dkotlin.daemon.jvmargs="-Xmx1000m"

Ghi chú thêm:

- Wrapper của fork là **9.3.1** (upstream gốc 8.4); AGP 8.3.1 chấp nhận cấu hình này trong thực tế nhưng có cảnh báo deprecated của Gradle 10.
- gradlew và build-and-install.sh bị commit thiếu bit thực thi (kiểu Windows) — đã chmod +x và sẽ commit lại mode.
- app/build.gradle trỏ chữ ký debug tới debug.keystore ở gốc repo, tệp này không có trong git. Đã tạo keystore debug cục bộ và thêm vào .gitignore; workflow GitHub Actions cũng tự tạo khi thiếu.
- Tổng RAM thấp là nguyên nhân chính khiến lần build đầu chậm/thrash; đã dừng các Gradle daemon cũ trước mỗi lần build.

## 8. Kết luận

Môi trường **đủ điều kiện** để build APK Android và kiểm thử trên thiết bị thật. Các mục không thực hiện được trong phiên: cài gói hệ thống (thiếu mật khẩu sudo), đọc hạn mức DeepSeek (không có key trong môi trường), kiểm thử API thật của từng nhà cung cấp (không có key người dùng).
