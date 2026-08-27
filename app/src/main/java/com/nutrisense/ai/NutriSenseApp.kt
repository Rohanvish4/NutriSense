package com.nutrisense.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for NutriSense AI, annotated with @HiltAndroidApp to trigger
 * Hilt code generation for dependency injection across the app hierarchy.
 */
@HiltAndroidApp
class NutriSenseApp : Application()
