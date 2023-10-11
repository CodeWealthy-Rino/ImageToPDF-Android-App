#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <setjmp.h>
#include "hpdf.h"
#include <android/log.h>

static HPDF_Doc  pdf;

void error_handler  (HPDF_STATUS error_no,
                     HPDF_STATUS detail_no,
                     void *user_data)
{
    __android_log_print(ANDROID_LOG_DEBUG,"Tag","error %x detail %x", (unsigned int)error_no, (unsigned  int)detail_no);
}

void  draw_image (HPDF_Doc     pdf,
                  const char  *filename,
                  float        x,
                  float        y)
{
    HPDF_Page page = HPDF_GetCurrentPage (pdf);
    HPDF_Image image;

    image = HPDF_LoadJpegImageFromFile (pdf, filename);

    /* Draw image to the canvas. */
    HPDF_Page_DrawImage (page, image, x, y, HPDF_Image_GetWidth (image),
                         HPDF_Image_GetHeight (image));
}

JNIEXPORT jint JNICALL
Java_com_kumazaku_pdfcreator_PDFCreateActivity_PDFInit(JNIEnv *env, jobject instance) {
    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFInit Enter");

    pdf = HPDF_New (error_handler, NULL);
    if (!pdf) {
        __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFInit fails");
        return -1;
    }
    HPDF_SetCompressionMode (pdf, HPDF_COMP_ALL);
    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFInit Leave with success");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_kumazaku_pdfcreator_PDFCreateActivity_PDFAddJpeg(JNIEnv *env, jobject instance, jint width, jint height, jstring path_) {
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);

    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFAddJpeg Enter");

    HPDF_Page page;
    HPDF_Destination dst;

    page = HPDF_AddPage (pdf);

    HPDF_Page_SetWidth (page, width);
    HPDF_Page_SetHeight (page, height);

    dst = HPDF_Page_CreateDestination (page);
    HPDF_Destination_SetXYZ (dst, 0, HPDF_Page_GetHeight (page), 1);
    HPDF_SetOpenAction(pdf, dst);

    draw_image(pdf, path, 0, 0);

    (*env)->ReleaseStringUTFChars(env, path_, path);

    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFAddJpeg Leave");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_kumazaku_pdfcreator_PDFCreateActivity_PDFSave(JNIEnv *env, jobject instance, jstring path_) {
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);

    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFSave Enter");

    /* save the document to a file */
    HPDF_SaveToFile (pdf, path);

    /* clean up */
    HPDF_Free (pdf);


    (*env)->ReleaseStringUTFChars(env, path_, path);

    __android_log_print(ANDROID_LOG_DEBUG,"Tag", "PDFSave Leave");
    return 0;
}