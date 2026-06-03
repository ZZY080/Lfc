package com.lfc.consumer.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lfc.consumer.data.ApiClient
import com.lfc.consumer.data.MediaUploadHelper
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
                val uploadFile = MediaUploadHelper.uriToUploadFile(
                    context = context,
                    uri = studentCardUri,
                    prefix = "student_card",
                )
                val requestFile = uploadFile.file.asRequestBody(uploadFile.mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "studentCard",
                    uploadFile.fileName,
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
