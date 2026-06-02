package com.lfc.consumer.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lfc.consumer.data.ApiClient
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

class AuthViewModel(
    private val tokenManager: TokenManager,
    private val context: Context,
) : ViewModel() {
    private val api = ApiClient.createApiService(tokenManager)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = api.login(LoginRequest(email, password))
                tokenManager.saveSession(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    userId = response.user.id,
                    email = response.user.email,
                    studentId = response.user.studentId,
                )
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = parseErrorMessage(e, "登录失败"))
            }
        }
    }

    fun register(
        email: String,
        password: String,
        studentId: String,
        studentCardUri: Uri?,
    ) {
        if (studentCardUri == null) {
            _uiState.value = AuthUiState(error = "请上传学生证照片")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val uploadFile = uriToUploadFile(studentCardUri)
                val requestFile = uploadFile.file.asRequestBody(uploadFile.mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "studentCard",
                    uploadFile.file.name,
                    requestFile,
                )

                val response = api.register(
                    email = email.toRequestBody("text/plain".toMediaTypeOrNull()),
                    password = password.toRequestBody("text/plain".toMediaTypeOrNull()),
                    studentId = studentId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    studentCard = part,
                )
                tokenManager.saveSession(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    userId = response.user.id,
                    email = response.user.email,
                    studentId = response.user.studentId,
                )
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = parseErrorMessage(e, "注册失败"))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private data class UploadFile(val file: File, val mimeType: String)

    private fun uriToUploadFile(uri: Uri): UploadFile {
        val rawMimeType = context.contentResolver.getType(uri)
        val extension = mimeTypeToExtension(rawMimeType, uri)
        val mimeType = normalizeMimeType(rawMimeType, extension)
        val file = File(context.cacheDir, "student_card_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("无法读取图片")
        return UploadFile(file, mimeType)
    }

    private fun mimeTypeToExtension(mimeType: String?, uri: Uri): String {
        return when {
            mimeType?.contains("png", ignoreCase = true) == true -> "png"
            mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
            uri.lastPathSegment?.endsWith(".png", ignoreCase = true) == true -> "png"
            uri.lastPathSegment?.endsWith(".webp", ignoreCase = true) == true -> "webp"
            else -> "jpg"
        }
    }

    private fun normalizeMimeType(mimeType: String?, extension: String): String {
        if (!mimeType.isNullOrBlank() && mimeType != "image/*" && mimeType.startsWith("image/")) {
            return mimeType
        }
        return when (extension) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun parseErrorMessage(e: Exception, fallback: String): String {
        if (e is retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                runCatching {
                    com.google.gson.JsonParser.parseString(body)
                        .asJsonObject["message"]?.asString
                }.getOrNull()?.let { return it }
            }
        }
        return e.message ?: fallback
    }
}

class AuthViewModelFactory(
    private val tokenManager: TokenManager,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(tokenManager, context) as T
    }
}
