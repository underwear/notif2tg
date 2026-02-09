# Notif2TG

Android app that captures **all** device notifications and forwards them to a Telegram chat/channel via Bot API.

## Features

- Forwards all notifications (except its own) to Telegram
- Deduplication — won't send the same notification twice in a row
- Auto-start on boot with foreground keep-alive service
- Retry with exponential backoff on network errors
- Minimal UI: just bot token + chat ID

## Requirements

- Android 8.0+ (API 26)
- Telegram bot token (create via [@BotFather](https://t.me/BotFather))
- Chat ID (use [@userinfobot](https://t.me/userinfobot) or channel ID)

## Setup

1. Download APK from [Releases](../../releases)
2. Install on your device
3. Open the app, enter bot token and chat ID
4. Tap **Save & Test** to verify
5. Tap **Open Notification Access Settings** and enable Notif2TG
6. Tap **Disable Battery Optimization**

### Xiaomi/MIUI extra steps

- Enable **AutoStart** in Security settings
- Disable battery optimization for the app
- Lock the app in Recent Apps (swipe down on the app card)

## Building from source

```bash
git clone https://github.com/underwear/notif2tg.git
cd notif2tg
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## License

MIT
