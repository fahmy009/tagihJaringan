package com.fahms.tagihjaringan.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("browser_download_url") val browserDownloadUrl: String = ""
)

data class GitHubReleaseResponse(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("assets") val assets: List<GitHubReleaseAsset> = emptyList()
)

data class AppUpdateInfo(
    val version: String,
    val changelog: String,
    val downloadUrl: String
)

interface GitHubApiService {
    @GET
    suspend fun getLatestRelease(@Url fullUrl: String): retrofit2.Response<GitHubReleaseResponse>
}

class GitHubUpdateManager(
    private val unsafeOkHttpClient: okhttp3.OkHttpClient
) {

    private val api: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(unsafeOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }

    suspend fun checkForUpdates(currentVersionName: String): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/fahmy009/tagihJaringan/releases/latest"
                val response = api.getLatestRelease(url)
                if (response.isSuccessful && response.body() != null) {
                    val release = response.body()!!
                    val latestTag = release.tagName.replace("v", "", ignoreCase = true).trim()
                    val cleanCurrent = currentVersionName.replace("v", "", ignoreCase = true).trim()

                    if (isVersionNewer(latestTag, cleanCurrent)) {
                        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                            ?: release.assets.firstOrNull()
                        val downloadUrl = apkAsset?.browserDownloadUrl ?: ""
                        if (downloadUrl.isNotBlank()) {
                            return@withContext AppUpdateInfo(
                                version = release.tagName,
                                changelog = release.body.ifBlank { release.name.ifBlank { "Pembaruan versi terbaru tersedia." } },
                                downloadUrl = downloadUrl
                            )
                        }
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val lPart = latestParts.getOrElse(i) { 0 }
                val cPart = currentParts.getOrElse(i) { 0 }
                if (lPart > cPart) return true
                if (lPart < cPart) return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) { onError("HTTP Error: ${connection.responseCode}") }
                    return@withContext
                }

                val fileLength = connection.contentLength
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val apkFile = File(downloadsDir, "tagihJaringan_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val input = connection.inputStream
                val output = FileOutputStream(apkFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastProgress = 0

                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    onCompleted()
                    launchApkInstaller(context, apkFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal mendownload pembaruan.") }
            }
        }
    }

    private fun launchApkInstaller(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
