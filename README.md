# PlainApp

## Introduction

PlainApp is an all-in-one application that aims to offer various frequently-used features.

In a world where many apps are cluttered with ads and invasive tracking, PlainApp offers a refreshing alternative. PlainApp is designed to help you manage your private data with ease, without sacrificing your privacy or wasting your time.

My goal is ambitious. I want to integrate multiple features into the app that will prove beneficial for learning and work purposes. All the data saved on the app will be shared with each feature, enabling users to access their data easily.

With PlainApp, your phone becomes more than just a plaything. It becomes a powerful tool for managing your digital life.

## Features

- Privacy oriented: We take your privacy seriously and use TLS + AES256-GCM encryption to ensure your data is secure.
- Ad-free forever: You won't be bombarded with ads when using PlainApp.
- No cloud services or third-party data storage: All data is kept on your device, so you have complete control over your information.
- No Firebase Cloud Messaging/Analytics: We don't track your activity or use your data for advertising purposes. We only collect crash logs through Firebase Crashlytics.
- User-friendly: PlainApp has a modern, customizable interface with multi-language support, light/dark theme options, and e-ink screen compatibility.
- Desktop management: You can manage your phone from your desktop by visiting a self-hosted webpage wirelessly.
- Contacts management: You can manage these features on the self-hosted webpage as well.
- File management: Manage files, images, videos, and audios on the self-hosted webpage.
- Note-taking: Use PlainApp to manage your notes with a built-in Markdown editor.
- RSS reader: Read articles in a clean UI.
- TV casting: Cast your videos, images, and audios to your TV.
- Video and audio playback: Play videos and audios in the app and on the webpage.
- Backup and export: Backup and export your app data for safekeeping.
- ChatGPT conversation UI (web only)
- And more features are planned for the future.

PlainApp's minimalist design is intentional, so you can focus on what matters most: your valuable data.

Watch the video: https://www.youtube.com/watch?v=RvO18j4r95o

App link: https://play.google.com/store/apps/details?id=com.ismartcoding.plain

Reddit: https://www.reddit.com/r/plainapp

## Screenshots

| ![home](screenshots/1.jpeg)    | ![files](screenshots/2.jpeg)  | ![web](screenshots/3.jpeg)    | ![notes](screenshots/4.jpeg) |
|--------------------------------|-------------------------------|-------------------------------|------------------------------|
| ![audios](screenshots/5.jpeg) | ![images](screenshots/6.jpeg) | ![videos](screenshots/7.jpeg)  |                              |

## Compatibility

PlainApp requires Android 9.0 or higher.

## Support

If you encounter any issues, feel free to open an issue on GitHub. We're always happy to help.

## Download GraphQL schema from PlainBox

```bash
./gradlew downloadApolloSchema \
  --endpoint="http://<box-ip>:8080/graphql" \
  --schema="app/src/main/graphql/com/ismartcoding/plain/schema.graphqls" \
  --header="Authorization: Bearer <token>"
```

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
