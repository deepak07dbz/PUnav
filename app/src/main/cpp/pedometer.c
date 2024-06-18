#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <android/log.h>

#define LOG_TAG "NativeLib"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define NUM_AUTOCORR_LAGS 50
#define DERIV_FILT_LEN  5
#define LPF_FILT_LEN 9
#define AUTOCORR_DELTA_AMPLITUDE_THRESH 10e6
#define AUTOCORR_MIN_HALF_LEN 3
#define SAMPLING_RATE 20
#define WINDOW_LENGTH 4


static int lpfCoeff[LPF_FILT_LEN] = {-5, 6, 34, 68, 84, 68, 34, 6, -5};
static int derivCoeff[DERIV_FILT_LEN] = {-6, 31, 0, -31, 6};

void lowPassFilter(float* in, float* out, int size) {
    for (int i = 0; i < size; i++) {
        float temp = 0;
        for (int j = 0; j < LPF_FILT_LEN; j++) {
            if (i - j >= 0) {
                temp += lpfCoeff[j] * in[i - j];
            }
        }
        out[i] = temp;
    }
}

void removeMean(float* lpf, int size) {
    float sum = 0;
    for (int i = 0; i < size; i++) {
        sum += lpf[i];
    }
    sum = sum / size;
    for (int i = 0; i < size; i++) {
        lpf[i] -= sum;
    }
}

void autoCorr(float* in, long* out, int size) {
    for (int lag = 0; lag < NUM_AUTOCORR_LAGS; lag++) {
        long temp = 0;
        for (int i = 0; i < size - lag; i++) {
            temp += (long)in[i] * (long)in[i + lag];
        }
        out[lag] = temp;
    }
}

void derivative(long* in, long* out) {
    for (int i = 0; i < NUM_AUTOCORR_LAGS; i++) {
        long temp = 0;
        for (int j = 0; j < DERIV_FILT_LEN; j++) {
            if (i - j >= 0) {
                temp += derivCoeff[j] * in[i - j];
            }
        }
        out[i] = temp;
    }
}

int firstZero(long* deriv) {
    for (int i = 0; i < NUM_AUTOCORR_LAGS; i++) {
        if (deriv[i] > 0 && deriv[i - 1] > 0 && deriv[i - 2] < 0 && deriv[i - 3] < 0) {
            return i - 1;
        }
    }
    return 0;
}

int getPrecisePeak(long* in, int peak) {
    if (in[peak] > in[peak - 1] && in[peak] > in[peak + 1]) {

    } else if (in[peak] > in[peak + 1] && in[peak] < in[peak - 1]) {
        while (in[peak] > in[peak + 1] && in[peak] < in[peak - 1]) {
            peak--;
        }
    } else {
        while (in[peak] > in[peak - 1] && in[peak] < in[peak + 1]) {
            peak++;
        }
    }
    return peak;
}

void getAutoPeakStats(long* buff, int* negSlopeCount, long* deltaAmpRight, int* posSlopeCount, long* deltaAmpLeft, int peakIndex) {
    int negSlopeInd = peakIndex;
    int loopLimit = NUM_AUTOCORR_LAGS - 1;
    while ((buff[negSlopeInd + 1] - buff[negSlopeInd] < 0) && (negSlopeInd < loopLimit)) {
        *negSlopeCount = *negSlopeCount + 1;
        negSlopeInd++;
    }

    *deltaAmpRight = buff[peakIndex] - buff[negSlopeInd];

    int posSlopeInd = peakIndex;
    loopLimit = 0;
    while ((buff[posSlopeInd] - buff[posSlopeInd - 1] > 0) && (posSlopeInd > loopLimit)) {
        *posSlopeCount = *posSlopeCount + 1;
        posSlopeInd--;
    }

    *deltaAmpLeft = buff[peakIndex] - buff[posSlopeInd];
}


JNIEXPORT jint JNICALL Java_com_example_accelerometer_Pedometer_countSteps(JNIEnv *env, jobject instance, jfloatArray xArray, jfloatArray yArray, jfloatArray zArray, jint size) {
    jfloat *x = (*env)->GetFloatArrayElements(env, xArray, 0);
    jfloat *y = (*env)->GetFloatArrayElements(env, yArray, 0);
    jfloat *z = (*env)->GetFloatArrayElements(env, zArray, 0);


    float* magnitude = (float*)malloc(size * sizeof(float));
    float* lpf = (float*)malloc(size * sizeof(float));
    long* autoCorrBuff = (long*)malloc(NUM_AUTOCORR_LAGS * sizeof(long));
    long* deriv = (long*)malloc(NUM_AUTOCORR_LAGS * sizeof(long));


    //calculate magnitude
    for (int i = 0; i < size; i++) {
        magnitude[i] = sqrt(x[i] * x[i] + y[i] * y[i] + z[i] * z[i]);
    }

    //apply low-pass filter
    lowPassFilter(magnitude, lpf, size);

    //remove mean
    removeMean(lpf, size);

    //autocorrelation
    autoCorr(lpf, autoCorrBuff, size);

    //derivative
    derivative(autoCorrBuff, deriv);


    //first zero crossing
    int peakIndex = firstZero(deriv);
    LOGD("PEAK: %d\n", peakIndex);

    //get precise peak index
    peakIndex = getPrecisePeak(autoCorrBuff, peakIndex);
    LOGD("PRECISED PEAK: %d\n", peakIndex);

    //get autocorrelation peak stats
    int negSlopeCount = 0;
    long deltaAmpRight = 0;
    int posSlopeCount = 0;
    long deltaAmpLeft = 0;
    getAutoPeakStats(autoCorrBuff, &negSlopeCount, &deltaAmpRight, &posSlopeCount, &deltaAmpLeft, peakIndex);
    LOGD("PEAK STATS: %d\t%ld\t%d\t%ld\t%d\n",negSlopeCount, deltaAmpRight, posSlopeCount, deltaAmpLeft, peakIndex);

    int steps;
    if ((posSlopeCount > AUTOCORR_MIN_HALF_LEN) && (negSlopeCount > AUTOCORR_MIN_HALF_LEN) && (deltaAmpRight > AUTOCORR_DELTA_AMPLITUDE_THRESH) && (deltaAmpLeft > AUTOCORR_DELTA_AMPLITUDE_THRESH)) {
        LOGD("VALID PEAKS");
        steps = (SAMPLING_RATE*WINDOW_LENGTH)/ peakIndex;
    } else {
        LOGD("INVALID PEAKS");
        steps = 0;
    }

    LOGD("steps: %d", steps);

    // Free allocated memory
    free(magnitude);
    free(lpf);
    free(autoCorrBuff);
    free(deriv);

    // Release array elements
    (*env)->ReleaseFloatArrayElements(env, xArray, x, 0);
    (*env)->ReleaseFloatArrayElements(env, yArray, y, 0);
    (*env)->ReleaseFloatArrayElements(env, zArray, z, 0);

    return steps;
}