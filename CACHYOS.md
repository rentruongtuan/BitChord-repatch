# BitChord trên CachyOS

Bản CachyOS dùng ứng dụng desktop Linux x86_64 của BitChord, đóng gói dạng AppImage và kèm Java runtime.

## Cài đặt / chạy

1. Tải `BitChord-1.8.1-linux-x86_64.AppImage` từ thư mục `dist/` hoặc dùng bản phát hành Linux tương ứng.
2. Mở terminal tại thư mục chứa file và chạy:

```bash
chmod +x BitChord-1.8.1-linux-x86_64.AppImage
./BitChord-1.8.1-linux-x86_64.AppImage
```

Nếu hệ thống không có FUSE 2, chạy AppImage ở chế độ tự giải nén:

```bash
./BitChord-1.8.1-linux-x86_64.AppImage --appimage-extract-and-run
```

Cần phiên làm việc desktop có GTK 3, X11 hoặc XWayland, OpenGL và ALSA. AppImage đóng gói Java và codec; nó không chứa driver đồ họa hoặc thư viện desktop hệ thống.

## Ghi chú bản build này

AppImage được build trên Linux x86_64 từ target desktop hiện có của repo. Môi trường build thiếu CMake nên thư viện native cho phân tích Automix không được biên dịch; những phần desktop khác đã được đóng gói.
