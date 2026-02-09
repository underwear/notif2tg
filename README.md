# Notif2TG

Android app that captures **all** device notifications (push notifications) and forwards them to a Telegram chat/channel via Bot API.

## Features

- **Setup Wizard** — guided 4-step setup with bot token validation and chat auto-detection
- **Chat Auto-detect** — send a message to your bot, the app finds the chat automatically (private, group, supergroup, or channel)
- **Per-app Filtering** — toggle which apps' notifications get forwarded
- Deduplication — won't send the same notification twice in a row
- Auto-start on boot with foreground keep-alive service
- Retry with exponential backoff on network errors

## Requirements

- Android 8.0+ (API 26)
- Telegram bot token (the wizard will guide you through creating one)

## Setup

1. Download APK from [Releases](../../releases)
2. Install on your device
3. Open the app — the setup wizard will guide you:
   - **Step 1:** Create a bot via [@BotFather](https://t.me/BotFather) and paste the token
   - **Step 2:** Send any message to your bot — the app detects the chat automatically. Or enter Chat ID manually
   - **Step 3:** Grant Notification Access and disable Battery Optimization
4. Done — notifications will be forwarded to your chosen chat

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
