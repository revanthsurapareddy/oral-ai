package com.oralai.scan

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class SavedReport(
    val id: String, // E.g. UUID
    val patientId: String,
    val patientName: String,
    val patientAge: String,
    val patientGender: String,
    val imageUri: Uri?,
    val analysisResult: String,
    val timestamp: Long = System.currentTimeMillis(),
    val analyzedImageBase64: String? = null
)

object ReportRepository {
    private val _reports = mutableStateListOf<SavedReport>()
    val reports: List<SavedReport> get() = _reports

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("oralai_reports", Context.MODE_PRIVATE)
            loadReports()
        }
    }

    private fun loadReports() {
        val jsonString = prefs?.getString("saved_reports", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(jsonString)
            _reports.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val uriStr = obj.optString("imageUri", "")
                _reports.add(
                    SavedReport(
                        id = obj.getString("id"),
                        patientId = obj.getString("patientId"),
                        patientName = obj.getString("patientName"),
                        patientAge = obj.getString("patientAge"),
                        patientGender = obj.getString("patientGender"),
                        imageUri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null,
                        analysisResult = obj.getString("analysisResult"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        analyzedImageBase64 = obj.optString("analyzedImageBase64", null).takeIf { it?.isNotEmpty() == true }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveReports() {
        val jsonArray = JSONArray()
        _reports.forEach { report ->
            val obj = JSONObject().apply {
                put("id", report.id)
                put("patientId", report.patientId)
                put("patientName", report.patientName)
                put("patientAge", report.patientAge)
                put("patientGender", report.patientGender)
                put("imageUri", report.imageUri?.toString() ?: "")
                put("analysisResult", report.analysisResult)
                put("timestamp", report.timestamp)
                if (report.analyzedImageBase64 != null) {
                    put("analyzedImageBase64", report.analyzedImageBase64)
                }
            }
            jsonArray.put(obj)
        }
        prefs?.edit()?.putString("saved_reports", jsonArray.toString())?.apply()
    }

    fun addReport(report: SavedReport) {
        _reports.add(0, report) // Add to top
        saveReports()
    }

    fun removeReport(reportId: String) {
        _reports.removeAll { it.id == reportId }
        saveReports()
    }

    fun getReportById(reportId: String): SavedReport? {
        return _reports.find { it.id == reportId }
    }
}
