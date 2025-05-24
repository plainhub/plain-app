# PlainApp

PlainApp is an open-source application that allows you to manage your phone through a web browser. Access files, videos, music, contacts, sms, calls, and more from your desktop using a secure, easy to use web interface!

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt='Get it on Google Play' height="80">](https://play.google.com/store/apps/details?id=com.ismartcoding.plain)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt='Get it on F-Droid' height="80">](https://f-droid.org/packages/com.ismartcoding.plain/)
[<img src="https://raw.githubusercontent.com/ismartcoding/plain-app/main/assets/get-it-on-github.png" alt='Get it on GitHub' height="80">](https://github.com/ismartcoding/plain-app/releases/latest)

## Features

- Privacy oriented: We take your privacy seriously and use TLS + AES-GCM-256 encryption to ensure your data is secure.
- Ad-free forever: You won't be bombarded with ads when using PlainApp.
- No cloud services or third-party data storage: All data is kept on your device, so you have complete control over your information.
- No Firebase Cloud Messaging/Analytics: We don't track your activity or use your data for advertising purposes. We only collect crash logs through Firebase Crashlytics.
- User-friendly: PlainApp has a modern, customizable interface with multi-language support, light/dark theme options, and e-ink screen compatibility.
- Desktop management: You can manage your phone from your desktop by visiting a self-hosted webpage wirelessly.
- Contacts management: You can manage these features on the self-hosted webpage as well.
- File management: Manage files(Internal storage, SD card, USB storage), images, videos, and audio on the self-hosted webpage.
- Note-taking: Use PlainApp to manage your notes with a built-in Markdown editor.
- RSS reader: Read articles in a clean UI.
- TV casting: Cast your videos, images, and audio to your TV.
- Video and audio playback: Play videos and audio in the app and on the webpage.
- Backup and export: Backup and export your app data for safekeeping.
- SMS, Calls: Read your SMS and calls on webpage.
- Apps: View your apps and download apk from phone.
- Screen mirror: Mirror your phone on webpage.
- Notifications: You can manage push notifications on the webpage.
- Device info: You can view your device info on the webpage.
- PWA support: You can add webpage to your home screen and experience the convenience of using the webpage like a desktop app.
- And more features are planned for the future.

PlainApp's minimalist design is intentional, so you can focus on what matters most: your valuable data.

Video: https://www.youtube.com/watch?v=TjRhC8pSQ6Q

Reddit: https://www.reddit.com/r/plainapp

Discord: https://discord.gg/RQWcS6DEEe

QQ Group: 812409393

## Disclaimer

- ⚠️ The project is under **very active** development.
- ⚠️ Expect bugs and breaking changes.
- ⚠️ It is not perfect, I am always looking for ways to improve. If you find that the app is missing a certain feature, please don't hesitate to submit a feature request.
- ⚠️ I kindly request everyone to ask questions and engage in discussions in a friendly manner.

## Donations :heart:

**This project needs you!** If you would like to support this project's further development, the creator of this project or the continuous maintenance of this project, **feel free to donate**.

Your donation is highly appreciated. Thank you!

- [Buy me a cup of Ko-Fi!](https://ko-fi.com/ismartcoding)

- Use WeChat to scan the code

<img src="assets/donate-wechat.jpeg" width="200"/>

## Screenshots

| ![files](screenshots/app-files.jpg)  | ![home](screenshots/app-home.jpg)        | ![music player](screenshots/app-music-player.jpg) |                                                  |
|--------------------------------------|------------------------------------------|---------------------------------------------------|--------------------------------------------------|
| ![audio](screenshots/app-audio.jpg)  | ![images](screenshots/app-images.jpg)    | ![videos](screenshots/app-videos.jpg)             | ![rss](screenshots/app-rss.jpg)                  |
| ![home](screenshots/web-home.png)    | ![images](screenshots/web-images.png)    | ![videos](screenshots/web-videos.png)             | ![notes](screenshots/web-notes.png)              |
| ![files](screenshots/web-files.png)  | ![encryption](screenshots/web-calls.png) | ![messages](screenshots/web-messages.png)         | ![contacts](screenshots/web-contacts.png)        |
| ![audio](screenshots/web-audios.png) | ![rss](screenshots/web-rss.png)          | ![encryption](screenshots/web-encryption.png)     | ![encryption](screenshots/web-screen-mirror.png) |
| ![audio](screenshots/web-image.png)  | ![rss](screenshots/web-video.png)        |                                                   |                                                  |

## Compatibility

PlainApp requires Android 9.0 or higher.

## FAQ

[http://docs.plain.icu/faq.html](http://docs.plain.icu/faq.html)

## Support

It's worth mentioning that this is an open-source application.
If you have doubts, you can either build your own version or choose not to use it.
If you encounter any issues, feel free to open an issue on GitHub. I am always happy to help. Please do not give a one-star rating arbitrarily.

## Build

1. Generate `release.jks` file under `$rootProject/app` folder.

```bash
keytool -genkey -v -keystore ./app/release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias plain
```

2. Create `keystore.properties` file with following content under `$rootProject` folder.

```
storePassword=
keyPassword=
keyAlias=plain
storeFile=release.jks
```

## Star history

[![Star History Chart](https://api.star-history.com/svg?repos=ismartcoding/plain-app&type=Date)](https://star-history.com/#ismartcoding/plain-app&Date)



