package com.example.encoder

import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.view.Surface

class WindowSurface(
    private val eglCore: EglCore,
    private val surface: Surface
) {
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    init {
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(
            eglCore.eglDisplay,
            eglCore.eglConfig,
            surface,
            surfaceAttribs,
            0
        )
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("failed to create EGL surface")
        }
    }

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglCore.eglDisplay, eglSurface, eglSurface, eglCore.eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun swapBuffers(): Boolean {
        return EGL14.eglSwapBuffers(eglCore.eglDisplay, eglSurface)
    }

    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglCore.eglDisplay, eglSurface, nsecs)
    }

    fun release() {
        if (eglSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglCore.eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        surface.release()
    }
}
