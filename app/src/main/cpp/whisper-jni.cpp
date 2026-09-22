#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_ogdenkids_speech_WhisperEngine_nativeInit(JNIEnv* env, jobject /*thiz*/, jstring modelPath, jint nThreads) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("nativeInit: path=%s nThreads=%d", path, nThreads);
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    struct whisper_context* ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params failed for %s", path);
        return 0;
    }
    LOGI("whisper model loaded (%d threads): %s", nThreads, whisper_print_system_info());
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_example_ogdenkids_speech_WhisperEngine_nativeFree(JNIEnv* /*env*/, jobject /*thiz*/, jlong ctxPtr) {
    if (ctxPtr != 0) {
        whisper_free(reinterpret_cast<struct whisper_context*>(ctxPtr));
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_ogdenkids_speech_WhisperEngine_nativeTranscribe(
    JNIEnv* env, jobject /*thiz*/, jlong ctxPtr, jfloatArray samples, jint nSamples, jint nThreads,
    jstring prompt, jfloatArray confidenceOut) {
    auto* ctx = reinterpret_cast<struct whisper_context*>(ctxPtr);
    if (ctx == nullptr || nSamples <= 0) {
        LOGE("nativeTranscribe: bad ctx=%lld nSamples=%d", (long long) ctxPtr, nSamples);
        return env->NewStringUTF("");
    }
    const char* promptStr = nullptr;
    if (prompt != nullptr) {
        promptStr = env->GetStringUTFChars(prompt, nullptr);
    }
    LOGI("nativeTranscribe: nSamples=%d nThreads=%d prompt=%s", nSamples, nThreads,
         promptStr ? promptStr : "(null)");

    std::vector<float> audio(static_cast<size_t>(nSamples));
    env->GetFloatArrayRegion(samples, 0, nSamples, audio.data());

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "en";
    params.n_threads = nThreads;
    params.no_context = true;
    params.single_segment = true;
    params.no_timestamps = true;
    params.print_progress = false;
    params.print_realtime = false;
    params.suppress_blank = true;
    params.greedy.best_of = 1;
    // 用目标词做 decoder 初始 prompt，让识别朝目标词靠，减少初学口音的近音误判
    params.initial_prompt = promptStr;

    int ret = whisper_full(ctx, params, audio.data(), nSamples);
    LOGI("whisper_full returned %d", ret);
    if (promptStr != nullptr) {
        env->ReleaseStringUTFChars(prompt, promptStr);
    }
    if (ret != 0) {
        LOGE("whisper_full failed");
        return env->NewStringUTF("");
    }

    std::string result;
    const int n = whisper_full_n_segments(ctx);
    LOGI("whisper_full segments=%d", n);
    for (int i = 0; i < n; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            result += text;
        }
    }

    // 置信度 = 文本 token 的平均概率（跳过时间戳等特殊 token）
    float confidence = 0.0f;
    int tokenCount = 0;
    const whisper_token eot = whisper_token_eot(ctx);
    for (int s = 0; s < n; ++s) {
        const int nTok = whisper_full_n_tokens(ctx, s);
        for (int t = 0; t < nTok; ++t) {
            const whisper_token_data td = whisper_full_get_token_data(ctx, s, t);
            if (td.id < eot && td.p > 0.0f) {
                confidence += td.p;
                tokenCount++;
            }
        }
    }
    if (tokenCount > 0) {
        confidence /= tokenCount;
    }
    LOGI("nativeTranscribe result: \"%s\" confidence=%.3f", result.c_str(), confidence);
    if (confidenceOut != nullptr) {
        jfloat* out = env->GetFloatArrayElements(confidenceOut, nullptr);
        if (out != nullptr) {
            out[0] = confidence;
            env->ReleaseFloatArrayElements(confidenceOut, out, 0);
        }
    }
    return env->NewStringUTF(result.c_str());
}

} // extern "C"
