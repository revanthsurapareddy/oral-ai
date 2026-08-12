package com.oralai.scan

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    // Live counts from Supabase (accurate even when local list is a sample)
    private val _totalPatientsCount = androidx.compose.runtime.mutableStateOf(0)
    val totalPatientsCount: androidx.compose.runtime.State<Int> get() = _totalPatientsCount

    private val _totalScansCount = androidx.compose.runtime.mutableStateOf(0)
    val totalScansCount: androidx.compose.runtime.State<Int> get() = _totalScansCount

    private var prefs: android.content.SharedPreferences? = null
    private val deletedIds = mutableSetOf<String>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("oralai_reports", Context.MODE_PRIVATE)
            val savedDeleted = prefs?.getStringSet("deleted_ids", emptySet()) ?: emptySet()
            deletedIds.addAll(savedDeleted.map { it.lowercase() })
            loadReports()
            fetchReportsFromSupabase()
        }
    }

    private fun saveDeletedIds() {
        prefs?.edit()?.putStringSet("deleted_ids", deletedIds)?.apply()
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
        syncReportToCloud(report)
    }

    fun syncAllReportsToCloud() {
        _reports.forEach { report ->
            syncReportToCloud(report)
        }
    }

    fun fetchReportsFromSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co"
                val supabaseAnonKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"

                var totalScansFetched = 0

                // 0. Get accurate TOTAL counts from Supabase using Content-Range header & full MRN list
                try {
                    // Total scans count via Content-Range header
                    val scanReq = Request.Builder()
                        .url("$supabaseUrl/rest/v1/reports?select=id")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Prefer", "count=exact")
                        .addHeader("Range-Unit", "items")
                        .addHeader("Range", "0-0")
                        .get().build()
                    val scanRes = client.newCall(scanReq).execute()
                    val contentRange = scanRes.headers["Content-Range"] ?: ""
                    totalScansFetched = contentRange.substringAfter("/").trim().toIntOrNull() ?: 0
                    scanRes.close()

                    // Unique patient MRNs from reports
                    val mrnReq = Request.Builder()
                        .url("$supabaseUrl/rest/v1/reports?select=mrn,patient_id,patient_name&limit=5000")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .get().build()
                    val mrnRes = client.newCall(mrnReq).execute()
                    val bodyStr = mrnRes.body?.string() ?: "[]"
                    mrnRes.close()

                    val allMrns = JSONArray(bodyStr)
                    val uniqueMrns = mutableSetOf<String>()
                    for (i in 0 until allMrns.length()) {
                        val obj = allMrns.getJSONObject(i)
                        val m = obj.optString("mrn", obj.optString("patient_id", obj.optString("patient_name", ""))).trim().lowercase()
                        if (m.isNotEmpty()) uniqueMrns.add(m)
                    }

                    // Query patients table as well
                    try {
                        val patReq = Request.Builder()
                            .url("$supabaseUrl/rest/v1/patients?select=id,mrn,full_name&limit=5000")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .get().build()
                        val patRes = client.newCall(patReq).execute()
                        val patBodyStr = patRes.body?.string() ?: "[]"
                        patRes.close()
                        val allPats = JSONArray(patBodyStr)
                        for (i in 0 until allPats.length()) {
                            val obj = allPats.getJSONObject(i)
                            val m = obj.optString("mrn", obj.optString("id", obj.optString("full_name", ""))).trim().lowercase()
                            if (m.isNotEmpty()) uniqueMrns.add(m)
                        }
                    } catch (pe: Exception) { pe.printStackTrace() }

                    withContext(Dispatchers.Main) {
                        if (totalScansFetched > 0) _totalScansCount.value = totalScansFetched
                        if (uniqueMrns.isNotEmpty()) _totalPatientsCount.value = uniqueMrns.size
                    }
                } catch (ce: Exception) { ce.printStackTrace() }

                // 1. Fetch Patients from Supabase (limit 5000)
                val patReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/patients?select=id,mrn,full_name,age,gender,created_at&order=created_at.desc&limit=5000")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()

                val patRes = client.newCall(patReq).execute()
                val remotePatients = mutableListOf<JSONObject>()
                if (patRes.isSuccessful) {
                    val bodyStr = patRes.body?.string() ?: "[]"
                    val arr = JSONArray(bodyStr)
                    for (i in 0 until arr.length()) {
                        remotePatients.add(arr.getJSONObject(i))
                    }
                }
                patRes.close()

                // 2. Fetch Reports from Supabase (Light query - limit 5000)
                val repReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/reports?select=id,patient_id,mrn,patient_name,message,risk_level,risk_percentage,has_cancer,analysis_date&order=analysis_date.desc&limit=5000")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()

                val repRes = client.newCall(repReq).execute()
                val remoteReports = mutableListOf<SavedReport>()
                if (repRes.isSuccessful) {
                    val bodyStr = repRes.body?.string() ?: "[]"
                    val arr = JSONArray(bodyStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rId = obj.optString("id", java.util.UUID.randomUUID().toString())
                        val pMrn = obj.optString("mrn", obj.optString("patient_id", "PAT-101"))
                        val pName = obj.optString("patient_name", "Patient")
                        val msg = obj.optString("message", "Analysis Completed")
                        val dateStr = obj.optString("analysis_date", "")

                        remoteReports.add(
                            SavedReport(
                                id = rId,
                                patientId = pMrn,
                                patientName = pName,
                                patientAge = "30",
                                patientGender = "Unspecified",
                                imageUri = null,
                                analysisResult = msg,
                                timestamp = parseIsoEpoch(dateStr),
                                analyzedImageBase64 = null
                            )
                        )
                    }
                }
                repRes.close()

                // 3. Ensure ALL patients from Supabase patients table are present in reports list
                remotePatients.forEach { patObj ->
                    val pMrn = (patObj.optString("mrn").takeIf { it.isNotBlank() } ?: patObj.optString("id")).trim()
                    val pName = (patObj.optString("full_name").takeIf { it.isNotBlank() } ?: patObj.optString("name")).trim()
                    val pAge = patObj.optInt("age", 30).toString()
                    val pGender = patObj.optString("gender", "Unspecified")

                    if (pMrn.isNotEmpty() && remoteReports.none { it.patientId.equals(pMrn, ignoreCase = true) }) {
                        remoteReports.add(
                            SavedReport(
                                id = "PAT-STUB-" + pMrn,
                                patientId = pMrn,
                                patientName = if (pName.isNotEmpty()) pName else "Patient $pMrn",
                                patientAge = pAge,
                                patientGender = pGender,
                                imageUri = null,
                                analysisResult = "Patient Registered",
                                timestamp = System.currentTimeMillis(),
                                analyzedImageBase64 = null
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    val remoteReportIds = remoteReports.map { it.id }.toSet()
                    val remoteMrns = (remoteReports.map { it.patientId } + remotePatients.map { it.optString("mrn", it.optString("id", "")) }).filter { it.isNotEmpty() }.toSet()

                    // Purge any local reports/patients that are in deletedIds or deleted on cloud/web
                    _reports.removeAll { local ->
                        val isDeleted = local.id.lowercase() in deletedIds || local.patientId.lowercase() in deletedIds || local.patientName.lowercase() in deletedIds
                        val isRemoteReport = local.id in remoteReportIds
                        val isRemotePatient = local.patientId in remoteMrns
                        isDeleted || (!local.id.startsWith("PAT-LOCAL") && !isRemoteReport && !isRemotePatient)
                    }

                    // Update existing reports with remote data, ignoring deleted items
                    remoteReports.forEach { remote ->
                        val isDeleted = remote.id.lowercase() in deletedIds || remote.patientId.lowercase() in deletedIds || remote.patientName.lowercase() in deletedIds
                        if (!isDeleted) {
                            val index = _reports.indexOfFirst { it.id == remote.id || (it.id.startsWith("PAT-STUB") && it.patientId == remote.patientId) }
                            if (index != -1) {
                                val existing = _reports[index]
                                _reports[index] = remote.copy(
                                    imageUri = remote.imageUri ?: existing.imageUri,
                                    analyzedImageBase64 = remote.analyzedImageBase64 ?: existing.analyzedImageBase64
                                )
                            } else if (remote.id !in _reports.map { it.id }) {
                                _reports.add(remote)
                            }
                        }
                    }
                    
                    _reports.sortByDescending { it.timestamp }
                    _totalPatientsCount.value = _reports.distinctBy { (it.patientId.takeIf { id -> id.isNotBlank() } ?: it.patientName.takeIf { n -> n.isNotBlank() } ?: it.id).lowercase() }.size
                    _totalScansCount.value = totalScansFetched.coerceAtLeast(_reports.size)
                    saveReports()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncReportToCloud(report: SavedReport) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co"
                val supabaseAnonKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"

                val patientJson = JSONObject().apply {
                    put("mrn", report.patientId)
                    put("full_name", report.patientName)
                    put("age", report.patientAge.toIntOrNull() ?: 30)
                    put("gender", report.patientGender)
                }

                val postPatReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/patients")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(RequestBody.create("application/json; charset=utf-8".toMediaType(), patientJson.toString()))
                    .build()

                client.newCall(postPatReq).execute().close()

                val reportJson = JSONObject().apply {
                    put("id", report.id)
                    put("patient_id", report.patientId)
                    put("mrn", report.patientId)
                    put("patient_name", report.patientName)
                    put("age", report.patientAge.toIntOrNull() ?: 30)
                    put("gender", report.patientGender)
                    put("risk_level", if (report.analysisResult.contains("Cancer")) "High" else "Low")
                    put("risk_percentage", if (report.analysisResult.contains("Cancer")) 92 else 5)
                    put("has_cancer", report.analysisResult.contains("Cancer"))
                    put("message", report.analysisResult)
                    put("analysis_date", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(report.timestamp)))
                    if (report.analyzedImageBase64 != null) {
                        put("scan_image_url", report.analyzedImageBase64)
                    }
                }

                val postRepReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/reports")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(RequestBody.create("application/json; charset=utf-8".toMediaType(), reportJson.toString()))
                    .build()

                client.newCall(postRepReq).execute().close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeReport(reportId: String) {
        val target = _reports.find { it.id == reportId }
        val pid = target?.patientId ?: reportId
        deletedIds.add(reportId.lowercase())
        saveDeletedIds()

        _reports.removeAll { it.id == reportId }
        saveReports()

        val uniquePats = _reports.map { it.patientId.lowercase() }.filter { it.isNotEmpty() && it !in deletedIds }.toSet()
        _totalPatientsCount.value = uniquePats.size
        _totalScansCount.value = _reports.size

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co"
                val supabaseAnonKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"

                val encodedId = java.net.URLEncoder.encode(reportId, "UTF-8")
                val delRepReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/reports?or=(id.eq.$encodedId,mrn.eq.$encodedId)")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(delRepReq).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePatient(patientIdOrMrn: String) {
        val pid = patientIdOrMrn.trim()
        if (pid.isEmpty()) return

        val matchingReports = _reports.filter { it.patientId == pid || it.id == pid || it.patientName.equals(pid, ignoreCase = true) }
        deletedIds.add(pid.lowercase())
        matchingReports.forEach {
            deletedIds.add(it.id.lowercase())
            deletedIds.add(it.patientId.lowercase())
            deletedIds.add(it.patientName.lowercase())
        }
        saveDeletedIds()

        _reports.removeAll { it.patientId == pid || it.id == pid || it.patientName.equals(pid, ignoreCase = true) }
        saveReports()

        val uniquePats = _reports.map { it.patientId.lowercase() }.filter { it.isNotEmpty() && it !in deletedIds }.toSet()
        _totalPatientsCount.value = uniquePats.size
        _totalScansCount.value = _reports.size

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co"
                val supabaseAnonKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"

                val encodedPid = java.net.URLEncoder.encode(pid, "UTF-8")
                val delPatReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/patients?or=(id.eq.$encodedPid,mrn.eq.$encodedPid,full_name.eq.$encodedPid)")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(delPatReq).execute().close()

                val delRepReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/reports?or=(patient_id.eq.$encodedPid,mrn.eq.$encodedPid,id.eq.$encodedPid,patient_name.eq.$encodedPid)")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(delRepReq).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getReportById(reportId: String): SavedReport? {
        return _reports.find { it.id == reportId }
    }

    fun fetchReportImage(reportId: String, onLoaded: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co"
                val supabaseAnonKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"

                val req = Request.Builder()
                    .url("$supabaseUrl/rest/v1/reports?id=eq.$reportId&select=scan_image_url")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()

                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val imgStr = arr.getJSONObject(0).optString("scan_image_url", "")
                        if (imgStr.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val index = _reports.indexOfFirst { it.id == reportId }
                                if (index != -1) {
                                    _reports[index] = _reports[index].copy(analyzedImageBase64 = imgStr)
                                    saveReports()
                                }
                                onLoaded(imgStr)
                            }
                            res.close()
                            return@launch
                        }
                    }
                }
                res.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) { onLoaded(null) }
        }
    }

    private fun parseIsoEpoch(isoStr: String?): Long {
        if (isoStr.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoStr).toEpochMilli()
            } else {
                val cleanStr = isoStr.substring(0, 19).replace("T", " ")
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(cleanStr)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
