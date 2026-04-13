#!/usr/bin/env node
/**
 * Apply new/changed translations from the last commit to all locale files.
 * Translations are manually curated based on context (not machine-translated).
 *
 * New strings from commit: "DLNA cast & Remove Firebase"
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const RES_DIR = path.join(__dirname, '../app/src/main/res');

const LOCALES = ['bn','de','es','fr','hi','it','ja','ko','nl','pt','ru','ta','tr','vi','zh-rCN','zh-rTW'];

/**
 * Translations keyed by locale -> file -> key -> value.
 * null = no translation (string not available, skip).
 *
 * Changed strings have new English values:
 *   cast_dialog_hint: "Choose a device to enter Cast Mode."
 *   cast_dialog_wireless_cast_tip: "Tip: Connect to the same Wi-Fi as your target screen"
 */
const TRANSLATIONS = {
  'bn': {
    strings_common: {
      crash_report_title: 'অ্যাপ ক্র্যাশ হয়েছে',
      crash_report_message: 'অ্যাপটি অপ্রত্যাশিতভাবে ক্র্যাশ হয়েছে। আমরা এটি ঠিক করতে পারি সে জন্য কি আপনি আমাদের সাথে ক্র্যাশ রিপোর্ট শেয়ার করতে চান?',
      crash_include_app_logs: 'অ্যাপ লগ অন্তর্ভুক্ত করুন',
      crash_share: 'শেয়ার করুন',
    },
    strings_media: {
      cast_searching_for_screen: 'স্ক্রিন খোঁজা হচ্ছে',
      cast_select_screen: 'একটি স্ক্রিন বেছে নিন',
      cast_looking_for_devices: 'কাছাকাছি ডিভাইস খোঁজা হচ্ছে...',
      cast_dialog_hint: 'কাস্ট মোডে প্রবেশ করতে একটি ডিভাইস বেছে নিন।',
      cast_dialog_wireless_cast_tip: 'টিপ: আপনার টার্গেট স্ক্রিনের মতো একই Wi-Fi-এ সংযুক্ত করুন',
    },
    strings_network: {
      how_to_use: 'ব্যবহারের নির্দেশনা',
      developer_options: 'ডেভেলপার বিকল্পসমূহ',
    },
    strings_tools: {
      dlna_cast_request_title: 'কাস্ট অনুরোধ',
      dlna_cast_request_desc: 'ডিভাইস %1$s এই ডিভাইসে কাস্ট করতে চায়',
      dlna_cast_accept: 'গ্রহণ করুন',
      dlna_cast_reject: 'প্রত্যাখ্যান করুন',
      dlna_cast_remember_choice: 'পছন্দ মনে রাখুন, আর জিজ্ঞেস করবেন না',
      dlna_cast_history: 'কাস্ট নিয়মাবলী',
      dlna_cast_accepted: 'গৃহীত',
      dlna_cast_rejected: 'প্রত্যাখ্যাত',
      dlna_receiver_start_error: 'রিসিভার শুরু করতে ব্যর্থ হয়েছে।',
      retry: 'পুনরায় চেষ্টা করুন',
    },
  },
  'de': {
    strings_common: {
      crash_report_title: 'App abgestürzt',
      crash_report_message: 'Die App ist unerwartet abgestürzt. Möchten Sie uns den Absturzbericht mitteilen, damit wir es beheben können?',
      crash_include_app_logs: 'App-Protokolle einschließen',
      crash_share: 'Teilen',
    },
    strings_media: {
      cast_searching_for_screen: 'Suche nach Bildschirm',
      cast_select_screen: 'Bildschirm auswählen',
      cast_looking_for_devices: 'Suche nach Geräten in der Nähe...',
      cast_dialog_hint: 'Gerät auswählen, um den Cast-Modus zu starten.',
      cast_dialog_wireless_cast_tip: 'Tipp: Mit demselben WLAN wie Ihren Zielbildschirm verbinden',
    },
    strings_network: {
      how_to_use: 'Verwendung',
      developer_options: 'Entwickleroptionen',
    },
    strings_tools: {
      dlna_cast_request_title: 'Cast-Anfrage',
      dlna_cast_request_desc: 'Gerät %1$s möchte auf dieses Gerät übertragen',
      dlna_cast_accept: 'Akzeptieren',
      dlna_cast_reject: 'Ablehnen',
      dlna_cast_remember_choice: 'Auswahl merken, nicht mehr fragen',
      dlna_cast_history: 'Cast-Regeln',
      dlna_cast_accepted: 'Akzeptiert',
      dlna_cast_rejected: 'Abgelehnt',
      dlna_receiver_start_error: 'Empfänger konnte nicht gestartet werden.',
      retry: 'Erneut versuchen',
    },
  },
  'es': {
    strings_common: {
      crash_report_title: 'La app se cerró inesperadamente',
      crash_report_message: 'La app se cerró de forma inesperada. ¿Deseas compartir el informe de cierre para que podamos solucionarlo?',
      crash_include_app_logs: 'Incluir registros de la app',
      crash_share: 'Compartir',
    },
    strings_media: {
      cast_searching_for_screen: 'Buscando pantalla',
      cast_select_screen: 'Seleccionar pantalla',
      cast_looking_for_devices: 'Buscando dispositivos cercanos...',
      cast_dialog_hint: 'Elige un dispositivo para entrar en el modo Cast.',
      cast_dialog_wireless_cast_tip: 'Consejo: conéctate al mismo Wi-Fi que tu pantalla de destino',
    },
    strings_network: {
      how_to_use: 'Cómo usar',
      developer_options: 'Opciones de desarrollador',
    },
    strings_tools: {
      dlna_cast_request_title: 'Solicitud de cast',
      dlna_cast_request_desc: 'El dispositivo %1$s quiere hacer cast en este dispositivo',
      dlna_cast_accept: 'Aceptar',
      dlna_cast_reject: 'Rechazar',
      dlna_cast_remember_choice: 'Recordar elección, no volver a preguntar',
      dlna_cast_history: 'Reglas de cast',
      dlna_cast_accepted: 'Aceptado',
      dlna_cast_rejected: 'Rechazado',
      dlna_receiver_start_error: 'Error al iniciar el receptor.',
      retry: 'Reintentar',
    },
  },
  'fr': {
    strings_common: {
      crash_report_title: 'Application plantée',
      crash_report_message: "L'application a planté de façon inattendue. Souhaitez-vous partager le rapport de plantage pour que nous puissions le corriger ?",
      crash_include_app_logs: "Inclure les journaux de l'application",
      crash_share: 'Partager',
    },
    strings_media: {
      cast_searching_for_screen: 'Recherche d\'écran',
      cast_select_screen: 'Sélectionner un écran',
      cast_looking_for_devices: 'Recherche d\'appareils à proximité...',
      cast_dialog_hint: 'Choisissez un appareil pour entrer en mode Cast.',
      cast_dialog_wireless_cast_tip: 'Astuce : connectez-vous au même Wi-Fi que votre écran cible',
    },
    strings_network: {
      how_to_use: 'Comment utiliser',
      developer_options: 'Options développeur',
    },
    strings_tools: {
      dlna_cast_request_title: 'Demande de diffusion',
      dlna_cast_request_desc: 'L\'appareil %1$s souhaite diffuser sur cet appareil',
      dlna_cast_accept: 'Accepter',
      dlna_cast_reject: 'Refuser',
      dlna_cast_remember_choice: 'Mémoriser le choix, ne plus demander',
      dlna_cast_history: 'Règles de diffusion',
      dlna_cast_accepted: 'Accepté',
      dlna_cast_rejected: 'Refusé',
      dlna_receiver_start_error: 'Impossible de démarrer le récepteur.',
      retry: 'Réessayer',
    },
  },
  'hi': {
    strings_common: {
      crash_report_title: 'ऐप क्रैश हो गया',
      crash_report_message: 'ऐप अप्रत्याशित रूप से क्रैश हो गया। क्या आप हमें क्रैश रिपोर्ट शेयर करना चाहेंगे ताकि हम इसे ठीक कर सकें?',
      crash_include_app_logs: 'ऐप लॉग शामिल करें',
      crash_share: 'शेयर करें',
    },
    strings_media: {
      cast_searching_for_screen: 'स्क्रीन खोज रहा है',
      cast_select_screen: 'स्क्रीन चुनें',
      cast_looking_for_devices: 'पास के डिवाइस खोज रहा है...',
      cast_dialog_hint: 'Cast मोड में प्रवेश करने के लिए एक डिवाइस चुनें।',
      cast_dialog_wireless_cast_tip: 'सुझाव: अपनी टार्गेट स्क्रीन के समान Wi-Fi से कनेक्ट करें',
    },
    strings_network: {
      how_to_use: 'उपयोग कैसे करें',
      developer_options: 'डेवलपर विकल्प',
    },
    strings_tools: {
      dlna_cast_request_title: 'Cast अनुरोध',
      dlna_cast_request_desc: 'डिवाइस %1$s इस डिवाइस पर Cast करना चाहता है',
      dlna_cast_accept: 'स्वीकार करें',
      dlna_cast_reject: 'अस्वीकार करें',
      dlna_cast_remember_choice: 'चुनाव याद रखें, दोबारा न पूछें',
      dlna_cast_history: 'Cast नियम',
      dlna_cast_accepted: 'स्वीकृत',
      dlna_cast_rejected: 'अस्वीकृत',
      dlna_receiver_start_error: 'रिसीवर शुरू नहीं हो सका।',
      retry: 'पुनः प्रयास करें',
    },
  },
  'it': {
    strings_common: {
      crash_report_title: 'App in crash',
      crash_report_message: "L'app si è arrestata in modo imprevisto. Vuoi condividere il report del crash per aiutarci a risolverlo?",
      crash_include_app_logs: "Includi log dell'app",
      crash_share: 'Condividi',
    },
    strings_media: {
      cast_searching_for_screen: 'Ricerca schermo',
      cast_select_screen: 'Seleziona uno schermo',
      cast_looking_for_devices: 'Ricerca dispositivi nelle vicinanze...',
      cast_dialog_hint: 'Scegli un dispositivo per entrare in modalità Cast.',
      cast_dialog_wireless_cast_tip: 'Suggerimento: connettiti allo stesso Wi-Fi dello schermo di destinazione',
    },
    strings_network: {
      how_to_use: 'Come si usa',
      developer_options: 'Opzioni sviluppatore',
    },
    strings_tools: {
      dlna_cast_request_title: 'Richiesta di cast',
      dlna_cast_request_desc: 'Il dispositivo %1$s vuole trasmettere su questo dispositivo',
      dlna_cast_accept: 'Accetta',
      dlna_cast_reject: 'Rifiuta',
      dlna_cast_remember_choice: 'Ricorda la scelta, non chiedere più',
      dlna_cast_history: 'Regole di cast',
      dlna_cast_accepted: 'Accettato',
      dlna_cast_rejected: 'Rifiutato',
      dlna_receiver_start_error: 'Impossibile avviare il ricevitore.',
      retry: 'Riprova',
    },
  },
  'ja': {
    strings_common: {
      crash_report_title: 'アプリがクラッシュしました',
      crash_report_message: 'アプリが予期せずクラッシュしました。修正のためにクラッシュレポートを共有していただけますか？',
      crash_include_app_logs: 'アプリログを含める',
      crash_share: '共有',
    },
    strings_media: {
      cast_searching_for_screen: '画面を検索中',
      cast_select_screen: '画面を選択',
      cast_looking_for_devices: '近くのデバイスを検索中...',
      cast_dialog_hint: 'キャストモードに入るデバイスを選択してください。',
      cast_dialog_wireless_cast_tip: 'ヒント: 対象画面と同じ Wi-Fi に接続してください',
    },
    strings_network: {
      how_to_use: '使い方',
      developer_options: '開発者オプション',
    },
    strings_tools: {
      dlna_cast_request_title: 'キャストリクエスト',
      dlna_cast_request_desc: 'デバイス %1$s がこのデバイスにキャストしようとしています',
      dlna_cast_accept: '許可',
      dlna_cast_reject: '拒否',
      dlna_cast_remember_choice: 'この選択を覚えて次回から確認しない',
      dlna_cast_history: 'キャストルール',
      dlna_cast_accepted: '許可済み',
      dlna_cast_rejected: '拒否済み',
      dlna_receiver_start_error: 'レシーバーの起動に失敗しました。',
      retry: '再試行',
    },
  },
  'ko': {
    strings_common: {
      crash_report_title: '앱이 충돌했습니다',
      crash_report_message: '앱이 예기치 않게 충돌했습니다. 문제를 해결할 수 있도록 충돌 보고서를 공유해 주시겠어요?',
      crash_include_app_logs: '앱 로그 포함',
      crash_share: '공유',
    },
    strings_media: {
      cast_searching_for_screen: '화면 검색 중',
      cast_select_screen: '화면 선택',
      cast_looking_for_devices: '주변 기기 검색 중...',
      cast_dialog_hint: '캐스트 모드로 전환할 기기를 선택하세요.',
      cast_dialog_wireless_cast_tip: '팁: 대상 화면과 동일한 Wi-Fi에 연결하세요',
    },
    strings_network: {
      how_to_use: '사용 방법',
      developer_options: '개발자 옵션',
    },
    strings_tools: {
      dlna_cast_request_title: '캐스트 요청',
      dlna_cast_request_desc: '기기 %1$s이(가) 이 기기로 캐스트하려고 합니다',
      dlna_cast_accept: '허용',
      dlna_cast_reject: '거부',
      dlna_cast_remember_choice: '선택 기억, 다시 묻지 않기',
      dlna_cast_history: '캐스트 규칙',
      dlna_cast_accepted: '허용됨',
      dlna_cast_rejected: '거부됨',
      dlna_receiver_start_error: '수신기를 시작하지 못했습니다.',
      retry: '다시 시도',
    },
  },
  'nl': {
    strings_common: {
      crash_report_title: 'App gecrasht',
      crash_report_message: 'De app is onverwacht gecrasht. Wil je het crashrapport met ons delen zodat we het kunnen oplossen?',
      crash_include_app_logs: 'App-logboeken opnemen',
      crash_share: 'Delen',
    },
    strings_media: {
      cast_searching_for_screen: 'Scherm zoeken',
      cast_select_screen: 'Selecteer een scherm',
      cast_looking_for_devices: 'Apparaten in de buurt zoeken...',
      cast_dialog_hint: 'Kies een apparaat om de Cast-modus in te gaan.',
      cast_dialog_wireless_cast_tip: 'Tip: Maak verbinding met hetzelfde Wi-Fi als je doelscherm',
    },
    strings_network: {
      how_to_use: 'Hoe te gebruiken',
      developer_options: 'Ontwikkelaarsopties',
    },
    strings_tools: {
      dlna_cast_request_title: 'Cast-verzoek',
      dlna_cast_request_desc: 'Apparaat %1$s wil naar dit apparaat casten',
      dlna_cast_accept: 'Accepteren',
      dlna_cast_reject: 'Weigeren',
      dlna_cast_remember_choice: 'Keuze onthouden, niet meer vragen',
      dlna_cast_history: 'Cast-regels',
      dlna_cast_accepted: 'Geaccepteerd',
      dlna_cast_rejected: 'Geweigerd',
      dlna_receiver_start_error: 'Kan de ontvanger niet starten.',
      retry: 'Opnieuw proberen',
    },
  },
  'pt': {
    strings_common: {
      crash_report_title: 'App encerrado inesperadamente',
      crash_report_message: 'O app encerrou de forma inesperada. Deseja compartilhar o relatório de falha para que possamos corrigi-la?',
      crash_include_app_logs: 'Incluir registros do app',
      crash_share: 'Compartilhar',
    },
    strings_media: {
      cast_searching_for_screen: 'Procurando tela',
      cast_select_screen: 'Selecionar uma tela',
      cast_looking_for_devices: 'Procurando dispositivos próximos...',
      cast_dialog_hint: 'Escolha um dispositivo para entrar no modo Cast.',
      cast_dialog_wireless_cast_tip: 'Dica: conecte-se ao mesmo Wi-Fi que sua tela de destino',
    },
    strings_network: {
      how_to_use: 'Como usar',
      developer_options: 'Opções do desenvolvedor',
    },
    strings_tools: {
      dlna_cast_request_title: 'Solicitação de cast',
      dlna_cast_request_desc: 'O dispositivo %1$s quer transmitir para este dispositivo',
      dlna_cast_accept: 'Aceitar',
      dlna_cast_reject: 'Rejeitar',
      dlna_cast_remember_choice: 'Lembrar escolha, não perguntar novamente',
      dlna_cast_history: 'Regras de cast',
      dlna_cast_accepted: 'Aceito',
      dlna_cast_rejected: 'Rejeitado',
      dlna_receiver_start_error: 'Falha ao iniciar o receptor.',
      retry: 'Tentar novamente',
    },
  },
  'ru': {
    strings_common: {
      crash_report_title: 'Приложение завершилось с ошибкой',
      crash_report_message: 'Приложение неожиданно завершилось. Хотите поделиться отчётом об ошибке, чтобы мы могли её исправить?',
      crash_include_app_logs: 'Включить журналы приложения',
      crash_share: 'Поделиться',
    },
    strings_media: {
      cast_searching_for_screen: 'Поиск экрана',
      cast_select_screen: 'Выбрать экран',
      cast_looking_for_devices: 'Поиск ближайших устройств...',
      cast_dialog_hint: 'Выберите устройство для входа в режим трансляции.',
      cast_dialog_wireless_cast_tip: 'Совет: подключитесь к тому же Wi-Fi, что и целевой экран',
    },
    strings_network: {
      how_to_use: 'Как пользоваться',
      developer_options: 'Параметры разработчика',
    },
    strings_tools: {
      dlna_cast_request_title: 'Запрос трансляции',
      dlna_cast_request_desc: 'Устройство %1$s хочет выполнить трансляцию на это устройство',
      dlna_cast_accept: 'Принять',
      dlna_cast_reject: 'Отклонить',
      dlna_cast_remember_choice: 'Запомнить выбор, больше не спрашивать',
      dlna_cast_history: 'Правила трансляции',
      dlna_cast_accepted: 'Принято',
      dlna_cast_rejected: 'Отклонено',
      dlna_receiver_start_error: 'Не удалось запустить приёмник.',
      retry: 'Повторить',
    },
  },
  'ta': {
    strings_common: {
      crash_report_title: 'ஆப் செயலிழந்தது',
      crash_report_message: 'ஆப் எதிர்பாராத விதமாக செயலிழந்தது. நாங்கள் சரிசெய்ய உதவ கிராஷ் அறிக்கையை பகிர விரும்புகிறீர்களா?',
      crash_include_app_logs: 'ஆப் பதிவுகளை சேர்க்கவும்',
      crash_share: 'பகிரவும்',
    },
    strings_media: {
      cast_searching_for_screen: 'திரையை தேடுகிறது',
      cast_select_screen: 'ஒரு திரையை தேர்ந்தெடுக்கவும்',
      cast_looking_for_devices: 'அருகில் உள்ள சாதனங்களை தேடுகிறது...',
      cast_dialog_hint: 'Cast பயன்முறையில் நுழைய ஒரு சாதனத்தை தேர்ந்தெடுக்கவும்.',
      cast_dialog_wireless_cast_tip: 'உதவிக்குறிப்பு: உங்கள் இலக்கு திரையின் அதே Wi-Fi உடன் இணைக்கவும்',
    },
    strings_network: {
      how_to_use: 'எப்படி பயன்படுத்துவது',
      developer_options: 'டெவலப்பர் விருப்பங்கள்',
    },
    strings_tools: {
      dlna_cast_request_title: 'Cast கோரிக்கை',
      dlna_cast_request_desc: 'சாதனம் %1$s இந்த சாதனத்திற்கு cast செய்ய விரும்புகிறது',
      dlna_cast_accept: 'ஏற்கவும்',
      dlna_cast_reject: 'நிராகரிக்கவும்',
      dlna_cast_remember_choice: 'தேர்வை நினைவில் வைத்திரு, மீண்டும் கேட்காதே',
      dlna_cast_history: 'Cast விதிகள்',
      dlna_cast_accepted: 'ஏற்கப்பட்டது',
      dlna_cast_rejected: 'நிராகரிக்கப்பட்டது',
      dlna_receiver_start_error: 'பெறுநரை தொடங்க முடியவில்லை.',
      retry: 'மீண்டும் முயற்சிக்கவும்',
    },
  },
  'tr': {
    strings_common: {
      crash_report_title: 'Uygulama çöktü',
      crash_report_message: 'Uygulama beklenmedik şekilde çöktü. Düzeltmemiz için kilitlenme raporunu paylaşmak ister misiniz?',
      crash_include_app_logs: 'Uygulama günlüklerini dahil et',
      crash_share: 'Paylaş',
    },
    strings_media: {
      cast_searching_for_screen: 'Ekran aranıyor',
      cast_select_screen: 'Bir ekran seçin',
      cast_looking_for_devices: 'Yakın cihazlar aranıyor...',
      cast_dialog_hint: 'Cast moduna girmek için bir cihaz seçin.',
      cast_dialog_wireless_cast_tip: 'İpucu: Hedef ekranınızla aynı Wi-Fi\'a bağlanın',
    },
    strings_network: {
      how_to_use: 'Nasıl kullanılır',
      developer_options: 'Geliştirici seçenekleri',
    },
    strings_tools: {
      dlna_cast_request_title: 'Cast isteği',
      dlna_cast_request_desc: '%1$s cihazı bu cihaza yayın yapmak istiyor',
      dlna_cast_accept: 'Kabul et',
      dlna_cast_reject: 'Reddet',
      dlna_cast_remember_choice: 'Seçimi hatırla, bir daha sorma',
      dlna_cast_history: 'Cast kuralları',
      dlna_cast_accepted: 'Kabul edildi',
      dlna_cast_rejected: 'Reddedildi',
      dlna_receiver_start_error: 'Alıcı başlatılamadı.',
      retry: 'Tekrar dene',
    },
  },
  'vi': {
    strings_common: {
      crash_report_title: 'Ứng dụng bị lỗi',
      crash_report_message: 'Ứng dụng bị lỗi bất ngờ. Bạn có muốn chia sẻ báo cáo lỗi để chúng tôi có thể khắc phục không?',
      crash_include_app_logs: 'Bao gồm nhật ký ứng dụng',
      crash_share: 'Chia sẻ',
    },
    strings_media: {
      cast_searching_for_screen: 'Đang tìm kiếm màn hình',
      cast_select_screen: 'Chọn màn hình',
      cast_looking_for_devices: 'Đang tìm kiếm thiết bị lân cận...',
      cast_dialog_hint: 'Chọn thiết bị để vào chế độ Cast.',
      cast_dialog_wireless_cast_tip: 'Mẹo: Kết nối vào cùng Wi-Fi với màn hình đích của bạn',
    },
    strings_network: {
      how_to_use: 'Cách sử dụng',
      developer_options: 'Tùy chọn dành cho nhà phát triển',
    },
    strings_tools: {
      dlna_cast_request_title: 'Yêu cầu Cast',
      dlna_cast_request_desc: 'Thiết bị %1$s muốn cast đến thiết bị này',
      dlna_cast_accept: 'Chấp nhận',
      dlna_cast_reject: 'Từ chối',
      dlna_cast_remember_choice: 'Ghi nhớ lựa chọn, không hỏi lại',
      dlna_cast_history: 'Quy tắc Cast',
      dlna_cast_accepted: 'Đã chấp nhận',
      dlna_cast_rejected: 'Đã từ chối',
      dlna_receiver_start_error: 'Không thể khởi động bộ thu.',
      retry: 'Thử lại',
    },
  },
  'zh-rCN': {
    strings_common: {
      crash_report_title: '应用崩溃',
      crash_report_message: '应用意外崩溃了。是否将崩溃报告分享给我们以便修复？',
      crash_include_app_logs: '包含应用日志',
      crash_share: '分享',
    },
    strings_media: {
      cast_searching_for_screen: '正在搜索屏幕',
      cast_select_screen: '选择屏幕',
      cast_looking_for_devices: '正在搜索附近设备...',
      cast_dialog_hint: '选择设备以进入投屏模式。',
      cast_dialog_wireless_cast_tip: '提示：连接与目标屏幕相同的 Wi-Fi',
    },
    strings_network: {
      how_to_use: '使用方法',
      developer_options: '开发者选项',
    },
    strings_tools: {
      dlna_cast_request_title: '投屏请求',
      dlna_cast_request_desc: '设备 %1$s 请求投屏到本设备',
      dlna_cast_accept: '接受',
      dlna_cast_reject: '拒绝',
      dlna_cast_remember_choice: '记住此选择，不再询问',
      dlna_cast_history: '投屏规则',
      dlna_cast_accepted: '已接受',
      dlna_cast_rejected: '已拒绝',
      dlna_receiver_start_error: '启动接收器失败。',
      retry: '重试',
    },
  },
  'zh-rTW': {
    strings_common: {
      crash_report_title: '應用程式崩潰',
      crash_report_message: '應用程式意外崩潰了。是否將崩潰報告分享給我們以便修復？',
      crash_include_app_logs: '包含應用程式日誌',
      crash_share: '分享',
    },
    strings_media: {
      cast_searching_for_screen: '正在搜尋螢幕',
      cast_select_screen: '選擇螢幕',
      cast_looking_for_devices: '正在搜尋附近裝置...',
      cast_dialog_hint: '選擇裝置以進入投影模式。',
      cast_dialog_wireless_cast_tip: '提示：連接與目標螢幕相同的 Wi-Fi',
    },
    strings_network: {
      how_to_use: '使用方式',
      developer_options: '開發者選項',
    },
    strings_tools: {
      dlna_cast_request_title: '投影請求',
      dlna_cast_request_desc: '裝置 %1$s 請求投影到本裝置',
      dlna_cast_accept: '接受',
      dlna_cast_reject: '拒絕',
      dlna_cast_remember_choice: '記住此選擇，不再詢問',
      dlna_cast_history: '投影規則',
      dlna_cast_accepted: '已接受',
      dlna_cast_rejected: '已拒絕',
      dlna_receiver_start_error: '啟動接收器失敗。',
      retry: '重試',
    },
  },
};

/** Escape XML special characters in string values */
function escapeXml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, "\\'")
    .replace(/"/g, '&quot;');
}

/**
 * Insert new string entries before </resources> if they don't already exist.
 * For changed strings, update the value in-place.
 */
function processFile(filePath, strings, changed = {}) {
  if (!fs.existsSync(filePath)) {
    console.warn(`  SKIP (not found): ${filePath}`);
    return;
  }

  let content = fs.readFileSync(filePath, 'utf8');
  let modified = false;

  // Handle changed (existing) strings — update value in-place
  for (const [key, value] of Object.entries(changed)) {
    const regex = new RegExp(`(<string name="${key}">)([^<]*)(</string>)`, 'g');
    if (regex.test(content)) {
      content = content.replace(regex, `$1${escapeXml(value)}$3`);
      modified = true;
    }
  }

  // Handle new strings — insert before </resources>
  const newEntries = [];
  for (const [key, value] of Object.entries(strings)) {
    // Skip if already present
    if (content.includes(`name="${key}"`)) continue;
    newEntries.push(`    <string name="${key}">${escapeXml(value)}</string>`);
  }

  if (newEntries.length > 0) {
    content = content.replace('</resources>', newEntries.join('\n') + '\n</resources>');
    modified = true;
  }

  if (modified) {
    fs.writeFileSync(filePath, content, 'utf8');
    console.log(`  Updated: ${path.relative(process.cwd(), filePath)}`);
  } else {
    console.log(`  No change: ${path.relative(process.cwd(), filePath)}`);
  }
}

// Changed strings: keys that already exist in locale files but have new English values
const CHANGED_MEDIA = {
  cast_dialog_hint: null,            // will be set per locale below
  cast_dialog_wireless_cast_tip: null,
};

for (const locale of LOCALES) {
  console.log(`\nProcessing locale: ${locale}`);
  const t = TRANSLATIONS[locale];
  if (!t) { console.warn('  No translations defined, skipping'); continue; }

  const dir = path.join(RES_DIR, `values-${locale}`);

  // strings_common.xml — new strings only
  if (t.strings_common) {
    processFile(path.join(dir, 'strings_common.xml'), t.strings_common, {});
  }

  // strings_media.xml — new strings + update changed ones
  if (t.strings_media) {
    const newMedia = {};
    const changedMedia = {};
    for (const [k, v] of Object.entries(t.strings_media)) {
      if (k === 'cast_dialog_hint' || k === 'cast_dialog_wireless_cast_tip') {
        changedMedia[k] = v;
      } else {
        newMedia[k] = v;
      }
    }
    processFile(path.join(dir, 'strings_media.xml'), newMedia, changedMedia);
  }

  // strings_network.xml — new strings only
  if (t.strings_network) {
    processFile(path.join(dir, 'strings_network.xml'), t.strings_network, {});
  }

  // strings_tools.xml — new strings only
  if (t.strings_tools) {
    processFile(path.join(dir, 'strings_tools.xml'), t.strings_tools, {});
  }
}

console.log('\nDone.');
