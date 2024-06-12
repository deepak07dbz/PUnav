#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#define LPR_FILT_LEN 9
#define DERIV_FILT_LEN 5
#define THRESHOLD_HEIGHT 1.0f
#define THRESHOLD_WIDTH 5

static int firKernel[LPR_FILT_LEN] = {-5, 6, 34, 68, 84, 68, 34, 6, -5};
//static int derivCoeff[DERIV_FILT_LEN] = {-6,31,0,-31,6};

JNIEXPORT jint;

jint Java_com_example_accelerometer_Pedometer_countSteps(JNIEnv *env, jobject instance, jdoubleArray xArray, jdoubleArray yArray, jdoubleArray zArray, jint size) {
    jfloat *x = (*env)->GetFloatArrayElements(env, xArray, 0);
    jfloat *y = (*env)->GetFloatArrayElements(env, yArray, 0);
    jfloat *z = (*env)->GetFloatArrayElements(env, zArray, 0);

    float* magnitude = (float*)malloc(size * sizeof(float));
    float* filteredMagnitude = (float*)malloc(size * sizeof(float));
    float* autocorrelation = (float*)malloc(size * sizeof(float));

    //calculate magnitude
    for (int i = 0; i < size; i++) {
        magnitude[i] = sqrt(x[i] * x[i] + y[i] * y[i] + z[i] * z[i]);
    }

    //apply low-pass filter
    for (int i = 0; i < size; i++) {
        filteredMagnitude[i] = 0.0f;
        for (int j = 0; j < LPR_FILT_LEN; j++) {
            if (i - j >= 0) {
                filteredMagnitude[i] += firKernel[j] * magnitude[i - j];
            }
        }
    }

    //remove mean
    float sum = 0.0f;
    for (int i = 0; i < size; i++) {
        sum += filteredMagnitude[i];
    }
    float mean = sum / size;
    for (int i = 0; i < size; i++) {
        filteredMagnitude[i] -= mean;
    }

    //autocorrelation
    for (int lag = 0; lag < size; lag++) {
        autocorrelation[lag] = 0.0f;
        for (int i = 0; i < size - lag; i++) {
            autocorrelation[lag] += filteredMagnitude[i] * filteredMagnitude[i + lag];
        }
    }

    int steps = 0;
    int peakStartIndex = 0;

    while (peakStartIndex < size) {

        int peakIndex = -1;
        for (int i = peakStartIndex; i < size - 1; i++) {
            if (autocorrelation[i] > THRESHOLD_HEIGHT && autocorrelation[i] > autocorrelation[i + 1]) {
                peakIndex = i;
                break;
            }
        }

        if (peakIndex == -1) {
            break;
        }

        int width = 0;
        for (int i = peakIndex; i < size; i++) {
            if (autocorrelation[i] < THRESHOLD_HEIGHT) {
                width = i - peakIndex;
                break;
            }
        }

        // Check if the peak is valid
        if (width > THRESHOLD_WIDTH) {
            steps++;
        }

        // Move to the next peak search starting after the current peak
        peakStartIndex = peakIndex + width;
    }

    // Free allocated memory
    free(magnitude);
    free(filteredMagnitude);
    free(autocorrelation);

    // Release array elements
    (*env)->ReleaseFloatArrayElements(env, xArray, x, 0);
    (*env)->ReleaseFloatArrayElements(env, yArray, y, 0);
    (*env)->ReleaseFloatArrayElements(env, zArray, z, 0);

    return steps;
}