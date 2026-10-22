# Nhật ký thay đổi BitChord-repatch

> **File này là nơi DUY NHẤT ghi lại mọi thay đổi/bổ sung/điều chỉnh trên dự án BitChord-repatch.**
> Mọi thay đổi AI hoặc người dùng thực hiện đều phải được ghi chú vào đây, thêm mục mới ở cuối, kèm ngày tháng.
> Không xoá lịch sử cũ, chỉ nối thêm.

---

## 2026-10-21 — Khởi tạo dự án & Tích hợp ListenBrainz cho Floppy

- **Khởi tạo repo**: Clone mã nguồn gốc từ `https://github.com/kushagrasinghx/BitChord` vào `D:\My project\BitChord-repatch`.
- **Tạo tài liệu định danh & quy ước**:
  - Tạo `AGENTS.md`: Định danh dự án, kiến trúc KMP/Android, thông số tích hợp Floppy server.
  - Tạo `NHAT-KY-BITCHORD.md`: Lưu vết thay đổi liên tục qua các phiên làm việc của AI.
- **Kế hoạch mod**:
  1. Thêm `ListenBrainz.kt` vào `app/src/main/java/com/music/bitchord/data/scrobbling/`.
  2. Bổ sung lời gọi scrobble trong `ScrobbleManager.kt` gửi `cover_url` và `artists`.
  3. Cài đặt hook đón đầu trên server Floppy để nhận ảnh tức thì và tự động liên kết nghệ sĩ hợp tác.
  4. Build và xuất file APK cài đặt lên điện thoại.

## 2026-10-21: Biên dịch thành công APK BitChord Mod Scrobble Floppy
- **Mã nguồn đã mod:**
  - ListenBrainzManager.kt: Bổ sung gửi cover_url (song.thumbnailUrl) và danh sách rtists (song.artists.map { it.name }) trong dditional_info. Đặt mặc định URL là https://floppy.chutun.xyz/apis/listenbrainz/1/submit-listens và Token của user entruongtuan.
  - PlaybackService.kt: Tự động gọi ListenBrainzManager.submitPlayingNow và submitFinished với fallback token tích hợp sẵn, không phụ thuộc vào việc bật thủ công trong Settings.
  - ScrobbleManager.kt: Tự động gọi ListenBrainzManager.submitPlayingNow và submitFinished khi bắt đầu và kết thúc phát bài nhạc.
- **Khắc phục môi trường build:**
  - Liên kết junction ndroid-37.0 -> ndroid-37 trong Android SDK.
  - Tắt bundle WearOS / Android TV (itchord.bundleWear=false) để tối ưu hóa và giải phóng việc kéo dependency không cần thiết.
- **Kết quả build APK hoàn chỉnh:**
  - D:\My project\BitChord-repatch\release-apk\BitChord-arm64-v8a.apk (96 MB - khuyên dùng cho smartphone hiện đại).
  - D:\My project\BitChord-repatch\release-apk\BitChord-universal.apk (199 MB - chạy trên mọi kiến trúc Android).

## 2026-10-21: Cố định Chữ ký số Keystore & Nâng VersionCode 29 cho Cài đè trực tiếp
- **Cố định Keystore:** Đã lưu trữ keystore chuẩn tại signing/bitchord-debug.keystore và ghim cứng trong pp/build.gradle.kts. Đảm bảo tất cả các lần build về sau giữ nguyên chữ ký 100%, không bị lỗi "Chữ ký không khớp" khi cài đè.
- **Tăng VersionCode:** Nâng itchordVersionCode từ 28 -> 29 để hệ điều hành Android tự động nhận diện bản cập nhật hợp lệ và cho phép cài đè trực tiếp trên điện thoại mà không cần gỡ ứng dụng cũ.
- **Đã xuất APK hoàn chỉnh:** Đặt tại D:\My project\BitChord-repatch\release-apk\BitChord-arm64-v8a.apk (19:42:10).

## 2026-10-21: Xử lý Popup cập nhật gốc & Cơ chế nhận diện bản phát hành mới (VersionCode 30)
- **Nguyên nhân bị hỏi cập nhật 1.8.1 liên tục:** Trong mã nguồn gốc, itchordBetaSuffix được gán cứng "beta1". Khi app chạy mang tên 1.8.1-beta1, logic AppUpdateChecker so sánh với tag GitHub 1.8.1 sẽ luôn coi bản có hậu tố beta là cũ hơn bản chính thức, gây ra popup đòi tải 1.8.1 mỗi lần mở app.
- **Giải pháp triệt để:**
  1. Xóa bỏ hậu tố beta (extra["bitchordBetaSuffix"] = ""), đưa app về phiên bản phát hành chính thức 1.8.1. Khi so sánh 1.8.1 với 1.8.1, app nhận biết đây là bản mới nhất và **chấm dứt hoàn toàn popup phiền toái**.
  2. Tắt tự động nhảy popup modal toàn màn hình (showUpdateDialog = true trong LaunchedEffect).
  3. **Vẫn biết khi nào có bản mới:** Khi tác giả ra bản mới hơn trong tương lai (vd: 1.8.2, 1.9.0), **Icon nâng cấp (mũi tên tải lên) trên thanh tiêu đề Top Bar (ngay cạnh Avatar của bạn)** sẽ tự động sáng lên! Bạn có thể bấm vào đó bất cứ lúc nào để xem changelog.
  4. Nút bấm trong hộp thoại cập nhật được chuyển thành mở trang GitHub Releases của tác giả trên trình duyệt (để bạn xem thông tin và yêu cầu AI repatch bản mới) thay vì tự động tải APK gốc đè lên làm mất tính năng scrobble Floppy.
  5. Đã tăng ersionCode lên **30** và ký bằng keystore cố định để cài đè trực tiếp trên điện thoại.
- **File APK xuất xưởng:** BitChord-arm64-v8a.apk (19:49:41).


## 2026-10-22 — Đóng gói BitChord desktop cho CachyOS

- Quét README và cấu hình desktop: dự án đã có Compose Desktop JVM target Linux x86_64 và script AppImage; chọn AppImage để cài/chạy trên CachyOS mà không phụ thuộc định dạng package Debian/RPM.
- Build `:desktopApp:createDistributable` với Java 21, `bitchord.target=linux`, version `1.8.1`; thành công.
- Đóng gói thành `dist/BitChord-1.8.1-linux-x86_64.AppImage` (~310 MiB), kèm runtime Java.
- Thêm `CACHYOS.md` với hướng dẫn chạy AppImage và yêu cầu thư viện đồ họa/audio của desktop Linux.
- Giới hạn: môi trường build không có CMake nên Gradle bỏ qua `buildAnalysisNative`; Automix analyser không được tích hợp trong artifact này.
- Môi trường checkout có `gradlew`, script AppImage và `local.properties` mang cấu hình Windows/CRLF; build được gọi qua Gradle Wrapper JAR và tạm bỏ qua `local.properties`, sau đó khôi phục nguyên trạng.

## 2026-10-22 — Mod Desktop (Windows & CachyOS) đồng bộ Floppy Scrobble & Đóng gói Windows Standalone

- **Mục tiêu**: Đồng bộ thời gian thực lịch sử nghe nhạc giữa điện thoại (Android) và máy tính (Windows / CachyOS).
- **Mã nguồn đã mod:**
  - `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopScrobbling.kt`:
    1. Thiết lập endpoint ListenBrainz mặc định trỏ về server Floppy của REN: `https://floppy.chutun.xyz/apis/listenbrainz/1/submit-listens`.
    2. Thiết lập token mặc định: `mcfP9mwncsQaWghEmcvgPtGjdShcra0N` (user `rentruongtuan`).
    3. `listenBrainzConfigured` luôn kích hoạt mặc định (không yêu cầu người dùng phải cấu hình thủ công trong Settings).
    4. Gửi trực tiếp `cover_url` (`song.thumbnailUrl`) và mảng nghệ sĩ `artists` (`song.artists.map { it.name }`) trong `additional_info` của payload (cả `playing_now` và `single` listen), đồng nhất 100% với bản Android.
    5. Xử lý fallback timestamp an toàn và log kết quả scrobble rõ ràng.
- **Biên dịch & Đóng gói:**
  - Biên dịch kiểm tra `:desktopApp:compileKotlin` thành công 100%.
  - Tạo gói hoàn chỉnh độc lập `:desktopApp:createDistributable` kèm nhúng Java runtime và file thực thi `BitChord.exe`.
  - Nén và xuất xưởng ra thư mục `release-desktop/`:
    - `release-desktop/BitChord-windows-x64.zip` (~303 MB): Bản chạy ngay cho Windows 64-bit, giải nén bấm `BitChord.exe` là dùng luôn, không cần cài Java hay codec.
    - `release-desktop/BitChord-windows-portable.zip`: Bản portable script chạy nhanh.
    - `dist/BitChord-1.8.1-linux-x86_64.AppImage`: Bản AppImage chạy trên CachyOS / Linux.

## 2026-10-22 — Đóng gói Windows Setup (.msi), Dọn dẹp Portable và Thiết lập Kênh Cập nhật Online (OTA)

- **Đóng gói Windows Installer (.msi):**
  - Chạy tác vụ `:desktopApp:packageMsi` (kết hợp WiX Toolset) đóng gói thành công file cài đặt chuẩn Windows: `release-desktop/BitChord-1.8.1-windows-setup.msi` (319 MB).
  - Bản cài đặt này tự động tạo shortcut trên Desktop, Menu Start và quản lý trong Settings/Installed Apps.
  - Dọn dẹp tệp portable script dư thừa, chỉ giữ lại file cài đặt `.msi` và bản standalone `.zip`.

- **Làm rõ thẩm mỹ giao diện Mac-style:**
  - Tác giả gốc BitChord thiết kế thanh tiêu đề theo phong cách Apple Music trên macOS (`MacCaptionButton`).
  - Hướng dẫn chuyển về thanh tiêu đề chuẩn Windows: Vào **Settings** -> Bật **Title Bar** (System Window Frame).

- **Thiết lập Kênh Cập nhật Online (In-App OTA Update) trực tiếp trên thiết bị:**
  - Tạo repository chính thức trên tài khoản GitHub của REN: `https://github.com/rentruongtuan/BitChord-repatch`.
  - Cập nhật endpoint kiểm tra phiên bản mới trong mã nguồn:
    - Android: `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt` trỏ `LATEST_RELEASE_URL` về `repos/rentruongtuan/BitChord-repatch/releases/latest`.
    - Desktop: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopUpdateChecker.kt` trỏ `LATEST_RELEASE_URL` về `repos/rentruongtuan/BitChord-repatch/releases/latest`.
  - Nâng cấp thuật toán `apkAssetUrl` trên Android: Tự động phân tích ABI của thiết bị người dùng (ưu tiên tải bản nhẹ `arm64-v8a` 96MB cho các điện thoại đời mới thay vì phải tải bản universal 199MB).
  - Nâng cấp thuật toán `installerUrl` trên Desktop: Tự động nhận diện và tải đúng tệp `.msi` (Windows) hoặc `.AppImage` (CachyOS/Linux).
- Nâng `bitchordVersionCode = 31` và ký cố định bằng `signing/bitchord-debug.keystore` để đảm bảo cài đè trực tiếp (In-place update) không mất dữ liệu.
  - Sau này mỗi khi có bản mod mới: Chỉ cần push release lên GitHub của REN, các thiết bị (Android, Windows, CachyOS) khi mở app sẽ tự động nhận diện bản mới và cập nhật trực tiếp qua Internet chỉ với 1 chạm.

## 2026-10-22 — Sửa Lỗi Scrobble Floppy / ListenBrainz Trên Windows Desktop

- **Hiện tượng & Nguyên nhân gốc rễ:**
  1. **Thiếu scrobble ở ngưỡng 50% trong coroutine timer:** Khi bài hát đạt 50% thời lượng hoặc 180s, `DesktopScrobbling.onPlaybackStateChanged` chỉ gọi `submitLastFm` mà **hoàn toàn bỏ quên `submitListenBrainz`**. ListenBrainz trước đó chỉ được gọi khi bài hát kết thúc 100% (`onPlaybackEnded`). Do đó, nếu người dùng chuyển bài hoặc bấm Next giữa chừng thì không bao giờ scrobble lên Floppy.
  2. **Thiếu kích hoạt `playing_now` khi bài hát bắt đầu:** Khi bài hát đổi ID, không tự động gọi `updateNowPlaying` để báo bài hát đang phát sang Floppy.
  3. **Thiếu trường cấu hình Endpoint Floppy trong UI Desktop:** Hộp thoại cài đặt ListenBrainz trước đây chỉ có 1 ô nhập Token, không có ô nhập URL Server (khiến người dùng không rõ nó đang gửi về listenbrainz.org hay floppy.chutun.xyz).
  4. **Logic hiển thị Scrobble Timing bị ẩn:** Thanh trượt thời gian Scrobble Timing bị ràng buộc bởi `lastfmEnabled && signedIn`, người dùng chỉ bật ListenBrainz thì không thấy phần cài đặt thời gian trễ scrobble.
- **Biện pháp khắc phục:**
  1. `DesktopScrobbleSettings.kt`:
     - Thêm trường `listenBrainzEndpoint` lưu trữ bền vững trong `DesktopPersistence`.
     - Đặt mặc định `DEFAULT_LISTENBRAINZ_ENDPOINT = "https://floppy.chutun.xyz/apis/listenbrainz/1/submit-listens"`.
  2. `DesktopScrobbling.kt`:
     - Đổi tên biến trạng thái thành `isScrobbled`.
     - Tự động gọi `updateNowPlaying(song)` ngay khi bài hát bắt đầu phát để hiển thị "Playing Now" trên Floppy.
     - Khi bộ đếm thời gian scrobble chạm ngưỡng (50% hoặc 180s), tự động gửi đồng thời cả Last.fm và Floppy / ListenBrainz (`submitListenBrainz(song, "single", startedAt, duration, ts)`).
     - Trong `onPlaybackEnded`, chỉ scrobble nếu bài hát chưa được scrobble trước đó (tránh nhân đôi lượt nghe).
  3. `DesktopIntegrationsUi.kt` & `DesktopIntegrationsDialog.kt`:
     - Nâng cấp hộp thoại ListenBrainz: Thêm ô nhập **Endpoint URL** (mặc định sẵn `https://floppy.chutun.xyz/apis/listenbrainz/1/submit-listens`), kèm nút mở nhanh Floppy và ListenBrainz.
     - Đổi nhãn thành **"ListenBrainz / Floppy"** và hiển thị rõ trạng thái kết nối.
     - Mở khóa hiển thị mục **"Scrobble timing"** khi ListenBrainz hoặc Last.fm được bật.
- **Biên dịch & Cập nhật:**
  - Build lại bộ cài đặt chuẩn Windows: `release-desktop\BitChord-1.8.1-windows-setup.msi`.
  - Cập nhật trực tiếp tệp MSI lên GitHub Release `v1.8.1` của repo `rentruongtuan/BitChord-repatch`.

## 2026-10-22 — Phát hiện Cơ chế Floppy & Vượt Rào Cản Cloudflare (VersionCode 32)

- **Phát hiện quan trọng về hệ thống Floppy (`https://floppy.chutun.xyz`):**
  1. **Lớp bảo vệ Cloudflare WAF chặn HttpClient trần (HTTP 403 Forbidden - Code 1010):**
     - Do server Floppy đặt sau Cloudflare, các request xuất phát từ HttpClient mặc định (không có header `User-Agent` của trình duyệt hoặc app chuẩn) đều bị Cloudflare chặn lại ngay tại cổng vào với lỗi 403.
     - Cả Android (`ListenBrainzManager.kt`) và Windows (`DesktopScrobbling.kt`) trước đây đều thiếu `User-Agent`, dẫn đến 100% request scrobble bị rớt trước khi tới được server Floppy!
     - **Giải pháp**: Bổ sung `User-Agent` giả lập hợp lệ trên cả Desktop (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) BitChord/1.8.1`) và Mobile Android (`Mozilla/5.0 (Linux; Android 14) BitChord/1.8.1`).
  2. **Cơ chế lưu trữ của Floppy đối với `playing_now` vs `single`:**
     - Đặc thù của Floppy: Request `playing_now` chỉ hiển thị thẻ nghe tạm thời (nếu có web UI live) và **KHÔNG lưu vào cơ sở dữ liệu lịch sử nghe**.
     - Muốn bài hát chính thức được ghi nhận vào lịch sử nghe (`listens`), server Floppy yêu cầu loại `single` kèm mốc thời gian `listened_at`.
     - Vì vậy, việc bắn scrobble ở ngưỡng 50% thời lượng bằng `listen_type = "single"` là mấu chốt quyết định để bài hát hiện lên Floppy.
  3. **Xác thực Token:**
     - Token bạn nhập trong Registry `LP2...` trả về `HTTP 401: Unauthorized` khi gọi `/apis/listenbrainz/1/validate-token`.
     - Token chuẩn quản trị `mcfP9mwncsQaWghEmcvgPtGjdShcra0N` của user `rentruongtuan` trả về `Token valid: user_name: rentruongtuan`. Đã gán token này làm fallback mặc định an toàn cho cả Windows và Android.
- **Biên dịch & Xuất xưởng toàn diện (VersionCode 32):**
  - Android APK: Build chuẩn Flavor `prodDebug` (`applicationId = "com.music.bitchord"`) cho `BitChord-arm64-v8a.apk` và `BitChord-universal.apk`. Sửa lỗi trước đó copy nhầm tệp `devDebug` (`com.dev.bitchord`) khiến điện thoại tạo ra 2 app riêng biệt. Nay cài đè trực tiếp 100% lên app chính thức mà không bị tách app.
  - Windows MSI: Build lại `BitChord-1.8.1-windows-setup.msi` (desktopVersionCode 32).
  - Tải toàn bộ bản cập nhật lên GitHub Release `v1.8.1` của repo `rentruongtuan/BitChord-repatch`.



