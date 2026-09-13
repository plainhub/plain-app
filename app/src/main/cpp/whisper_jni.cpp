// JNI bridge for on-device Whisper lyrics transcription.
// Kotlin side: com.ismartcoding.plain.platform.WhisperJni
#include <jni.h>
#include <cstring>
#include <string>
#include <vector>
#include "whisper.h"

namespace {

struct ProgressCtx {
    JNIEnv* env = nullptr;
    jobject listener = nullptr;
    jmethodID method = nullptr;
    bool cancel = false;
};

// Progress notifications are advisory only (void return in whisper v1.9.x);
// user cancellation is picked up by abortCallback at the next graph compute.
void progressCallback(whisper_context*, whisper_state*, int progress, void* userData) {
    auto* ctx = static_cast<ProgressCtx*>(userData);
    if (ctx->env == nullptr || ctx->listener == nullptr || ctx->method == nullptr) return;
    jboolean stop = ctx->env->CallBooleanMethod(ctx->listener, ctx->method, static_cast<jint>(progress));
    if (ctx->env->ExceptionCheck()) {
        ctx->env->ExceptionDescribe();
        ctx->env->ExceptionClear();
        return;
    }
    ctx->cancel = (stop == JNI_TRUE);
}

// Returning true aborts the computation.
bool abortCallback(void* userData) {
    auto* ctx = static_cast<ProgressCtx*>(userData);
    return ctx != nullptr && ctx->cancel;
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_ismartcoding_plain_platform_WhisperJni_nativeLoadModel(JNIEnv* env, jobject, jstring path) {
    if (path == nullptr) return 0;
    const char* p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params params = whisper_context_default_params();
    whisper_context* ctx = whisper_init_from_file_with_params(p, params);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_ismartcoding_plain_platform_WhisperJni_nativeFreeModel(JNIEnv*, jobject, jlong handle) {
    if (handle == 0) return;
    whisper_free(reinterpret_cast<whisper_context*>(handle));
}

JNIEXPORT jobjectArray JNICALL
Java_com_ismartcoding_plain_platform_WhisperJni_nativeRun(
    JNIEnv* env, jobject, jlong handle, jfloatArray pcm, jint pcmLength,
    jstring language, jint threads, jobject progressListener) {
    if (handle == 0 || pcm == nullptr) return nullptr;
    auto* ctx = reinterpret_cast<whisper_context*>(handle);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.no_context = true;
    params.single_segment = false;
    params.language = "auto";
    if (language != nullptr) {
        const char* lang = env->GetStringUTFChars(language, nullptr);
        if (lang != nullptr && strlen(lang) > 0 && strcmp(lang, "auto") != 0) {
            params.language = lang;
        }
        if (lang != nullptr) env->ReleaseStringUTFChars(language, lang);
    }
    if (threads > 0) params.n_threads = static_cast<int>(threads);

    ProgressCtx progressCtx;
    if (progressListener != nullptr) {
        jclass cls = env->GetObjectClass(progressListener);
        jmethodID method = env->GetMethodID(cls, "onProgress", "(I)Z");
        env->DeleteLocalRef(cls);
        if (method != nullptr) {
            progressCtx.env = env;
            progressCtx.listener = progressListener;
            progressCtx.method = method;
            params.progress_callback = progressCallback;
            params.progress_callback_user_data = &progressCtx;
            params.abort_callback = abortCallback;
            params.abort_callback_user_data = &progressCtx;
        }
    }

    jfloat* samples = env->GetFloatArrayElements(pcm, nullptr);
    if (samples == nullptr) return nullptr;
    int rc = whisper_full(ctx, params, samples, static_cast<int>(pcmLength));
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);
    if (rc != 0 || progressCtx.cancel) return nullptr;

    int count = whisper_full_n_segments(ctx);
    if (count <= 0) return nullptr;

    jclass segmentClass = env->FindClass("com/ismartcoding/plain/platform/WhisperJni$Segment");
    if (segmentClass == nullptr) return nullptr;
    jmethodID ctor = env->GetMethodID(segmentClass, "<init>", "(JJ[B)V");
    if (ctor == nullptr) return nullptr;

    jobjectArray result = env->NewObjectArray(count, segmentClass, nullptr);
    for (int i = 0; i < count; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        // Whisper text is UTF-8; pass raw bytes so Kotlin decodes with
        // String(bytes, UTF_8) (NewStringUTF only accepts modified UTF-8).
        size_t len = text == nullptr ? 0 : strlen(text);
        jbyteArray bytes = env->NewByteArray(static_cast<jsize>(len));
        if (len > 0) {
            env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(len),
                                    reinterpret_cast<const jbyte*>(text));
        }
        jobject segment = env->NewObject(segmentClass, ctor,
                                         whisper_full_get_segment_t0(ctx, i),
                                         whisper_full_get_segment_t1(ctx, i),
                                         bytes);
        env->SetObjectArrayElement(result, i, segment);
        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(segment);
    }
    return result;
}

} // extern "C"
