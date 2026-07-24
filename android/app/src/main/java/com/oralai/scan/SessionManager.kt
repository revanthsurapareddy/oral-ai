package com.oralai.scan

import android.net.Uri

object SessionManager {
    var currentImageUri: Uri? = null
    var currentPatientId: String = ""
    var currentPatientName: String = ""
    var currentPatientAge: String = ""
    var currentPatientGender: String = ""
    
    // Analysis results
    var analysisHasCancer: Boolean? = null
    var analysisImageBase64: String? = null
    var analysisRiskLevel: String? = null
    var analysisRiskPercentage: Int? = null

    fun clear() {
        currentImageUri = null
        currentPatientId = ""
        currentPatientName = ""
        currentPatientAge = ""
        currentPatientGender = ""
        analysisHasCancer = null
        analysisImageBase64 = null
        analysisRiskLevel = null
        analysisRiskPercentage = null
    }
}
