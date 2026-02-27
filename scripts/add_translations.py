#!/usr/bin/env python3
import os

RES = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')

COMMON = [
    ('auto_check_update', {
        'bn': 'স্বয়ংক্রিয় আপডেট পরীক্ষা',
        'de': 'Automatisch nach Updates suchen',
        'es': 'Buscar actualizaciones automáticamente',
        'fr': 'Vérifier les mises à jour automatiquement',
        'hi': 'अपडेट स्वचालित रूप से जांचें',
        'it': 'Controlla aggiornamenti automaticamente',
        'ja': '自動でアップデートを確認',
        'ko': '자동 업데이트 확인',
        'nl': 'Automatisch controleren op updates',
        'pt': 'Verificar atualizações automaticamente',
        'ru': 'Автоматическая проверка обновлений',
        'ta': 'தானியங்கு புதுப்பிப்பு சரிபார்ப்பு',
        'tr': 'Güncellemeleri otomatik denetle',
        'vi': 'Tự động kiểm tra cập nhật',
        'zh-rCN': '自动检查更新',
        'zh-rTW': '自動檢查更新',
    }),
    ('auto_check_update_desc', {
        'bn': 'অ্যাপ চালু হলে স্বয়ংক্রিয়ভাবে নতুন সংস্করণ খোঁজে',
        'de': 'Beim App-Start automatisch nach neuen Versionen suchen',
        'es': 'Buscar nuevas versiones automáticamente al abrir la aplicación',
        'fr': "Rechercher automatiquement les nouvelles versions au démarrage de l\\'application",
        'hi': 'ऐप खुलने पर स्वचालित रूप से नए संस्करण की जांच करें',
        'it': "Controlla automaticamente le nuove versioni all\\'avvio dell\\'app",
        'ja': 'アプリ起動時に自動で新しいバージョンを確認する',
        'ko': '앱 실행 시 자동으로 새 버전 확인',
        'nl': 'Automatisch controleren op nieuwe versies bij het openen van de app',
        'pt': 'Verificar automaticamente novas versões ao iniciar o aplicativo',
        'ru': 'Автоматически проверять наличие новых версий при запуске приложения',
        'ta': 'பயன்பாடு திறக்கும்போது புதிய பதிப்புகளை தானியங்கியாக சரிபார்க்கவும்',
        'tr': 'Uygulama açıldığında otomatik olarak yeni sürümleri denetle',
        'vi': 'Tự động kiểm tra phiên bản mới khi khởi động ứng dụng',
        'zh-rCN': '应用启动时自动检查新版本',
        'zh-rTW': '應用程式啟動時自動檢查新版本',
    }),
    ('peer_chat_reply', {
        'bn': 'উত্তর দিন',
        'de': 'Antworten',
        'es': 'Responder',
        'fr': 'Répondre',
        'hi': 'उत्तर दें',
        'it': 'Rispondi',
        'ja': '返信',
        'ko': '답장',
        'nl': 'Beantwoorden',
        'pt': 'Responder',
        'ru': 'Ответить',
        'ta': 'பதிலளி',
        'tr': 'Yanıtla',
        'vi': 'Trả lời',
        'zh-rCN': '回复',
        'zh-rTW': '回覆',
    }),
    ('peer_chat_type_reply', {
        'bn': 'একটি উত্তর লিখুন\u2026',
        'de': 'Antwort eingeben\u2026',
        'es': 'Escribe una respuesta\u2026',
        'fr': 'Écrire une réponse\u2026',
        'hi': 'उत्तर टाइप करें\u2026',
        'it': 'Scrivi una risposta\u2026',
        'ja': '返信を入力\u2026',
        'ko': '답장 입력\u2026',
        'nl': 'Typ een antwoord\u2026',
        'pt': 'Digite uma resposta\u2026',
        'ru': 'Введите ответ\u2026',
        'ta': 'பதிலை தட்டச்சு செய்யவும்\u2026',
        'tr': 'Bir yanıt yazın\u2026',
        'vi': 'Nhập câu trả lời\u2026',
        'zh-rCN': '输入回复\u2026',
        'zh-rTW': '輸入回覆\u2026',
    }),
]

# Extra entries only for zh-rCN
EXTRA_ZH_CN = [
    ('chat_settings', '聊天设置'),
    ('chat_files_save_directory', '聊天文件保存目录'),
    ('default_app_directory', '默认（应用目录）'),
    ('computer', '电脑'),
    ('phone', '手机'),
    ('tablet', '平板'),
    ('tv', '电视和投影仪'),
]

LOCALE_LAST_LINE = {
    'bn': '    <string name="messages_cleared">বার্তাগুলি মুছে ফেলা হয়েছে!</string>',
    'de': '    <string name="messages_cleared">Nachrichten gelöscht!</string>',
    'es': '    <string name="messages_cleared">¡Mensajes borrados!</string>',
    'fr': '    <string name="messages_cleared">Messages effacés !</string>',
    'hi': '    <string name="messages_cleared">संदेश हटा दिए गए!</string>',
    'it': '    <string name="messages_cleared">Messaggi cancellati!</string>',
    'ja': '    <string name="messages_cleared">メッセージを削除しました！</string>',
    'ko': '    <string name="messages_cleared">메시지가 삭제되었습니다!</string>',
    'nl': '    <string name="messages_cleared">Berichten gewist!</string>',
    'pt': '    <string name="messages_cleared">Mensagens apagadas!</string>',
    'ru': '    <string name="messages_cleared">Сообщения удалены!</string>',
    'ta': '    <string name="messages_cleared">செய்திகள் அழிக்கப்பட்டன!</string>',
    'tr': '    <string name="messages_cleared">Mesajlar temizlendi!</string>',
    'vi': '    <string name="messages_cleared">Tin nhắn đã được xóa!</string>',
    'zh-rCN': '    <string name="messages_cleared">消息已清空！</string>',
    'zh-rTW': '    <string name="messages_cleared">訊息已清空！</string>',
}

LOCALE_DIR = {
    'zh-rCN': 'values-zh-rCN',
    'zh-rTW': 'values-zh-rTW',
}

def locale_dir(lang):
    return LOCALE_DIR.get(lang, f'values-{lang}')

for lang, anchor in LOCALE_LAST_LINE.items():
    filepath = os.path.join(RES, locale_dir(lang), 'strings.xml')
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    entries = [(k, d[lang]) for k, d in COMMON]
    if lang == 'zh-rCN':
        entries += EXTRA_ZH_CN

    new_lines = '\n'.join(f'    <string name="{k}">{v}</string>' for k, v in entries)
    old = anchor + '\n</resources>'
    new = anchor + '\n' + new_lines + '\n</resources>'

    if old in content:
        content = content.replace(old, new, 1)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'{lang}: added {len(entries)} entries')
    else:
        print(f'{lang}: WARNING - anchor not found, skipping')
