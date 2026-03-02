package com.mai.siarspmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private const val BASE_URL = "http://10.0.2.2:8080/"

data class DeliveryTaskDto(
    val id: Long,
    val status: String,
    val clientOrderNumber: String?,
    val plannedStartTime: String?,
    val currentLatitude: Double?,
    val currentLongitude: Double?
)

data class UpdateLocationRequest(val latitude: Double, val longitude: Double)

data class ApiResponse(val success: Boolean, val message: String)

interface CourierApi {
    @GET("employee/courier/deliveryTasks/mobile/myDeliveryTasks")
    suspend fun myDeliveryTasks(@Query("status") status: String = "active"): List<DeliveryTaskDto>

    @POST("employee/courier/deliveryTasks/mobile/updateLocation/{id}")
    suspend fun updateLocation(@Path("id") id: Long, @Body request: UpdateLocationRequest): ApiResponse
}

class InMemoryCookieJar : CookieJar {
    private val cookiesByHost = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val existing = cookiesByHost.getOrPut(url.host) { mutableListOf() }
        existing.removeAll { old -> cookies.any { it.name == old.name } }
        existing.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookiesByHost[url.host].orEmpty()
}

class SessionRepository {
    private val cookieJar = InMemoryCookieJar()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val req: Request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                return chain.proceed(req)
            }
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CourierApi::class.java)

    suspend fun login(username: String, password: String): Boolean {
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}employee/login")
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            val location = it.header("Location")
            return it.isRedirect && location != null && !location.contains("error")
        }
    }

    suspend fun loadTasks(): List<DeliveryTaskDto> = api.myDeliveryTasks("active")

    suspend fun sendLocation(taskId: Long, latitude: Double, longitude: Double): ApiResponse {
        return api.updateLocation(taskId, UpdateLocationRequest(latitude, longitude))
    }
}

class MainViewModel : ViewModel() {
    private val repo = SessionRepository()

    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var message by mutableStateOf("Введите логин сотрудника")
    var tasks by mutableStateOf<List<DeliveryTaskDto>>(emptyList())

    var latitudeInput by mutableStateOf("")
    var longitudeInput by mutableStateOf("")

    fun doLogin() {
        viewModelScope.launch {
            runCatching { repo.login(login, password) }
                .onSuccess {
                    isLoggedIn = it
                    message = if (it) "Вход выполнен" else "Ошибка логина"
                    if (it) refreshTasks()
                }
                .onFailure { message = "Ошибка сети: ${it.message}" }
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            runCatching { repo.loadTasks() }
                .onSuccess { tasks = it }
                .onFailure { message = "Не удалось загрузить задачи: ${it.message}" }
        }
    }

    fun sendLocation(taskId: Long) {
        val lat = latitudeInput.toDoubleOrNull()
        val lon = longitudeInput.toDoubleOrNull()
        if (lat == null || lon == null) {
            message = "Координаты должны быть числами"
            return
        }

        viewModelScope.launch {
            runCatching { repo.sendLocation(taskId, lat, lon) }
                .onSuccess { message = it.message }
                .onFailure { message = "Ошибка отправки: ${it.message}" }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = remember { MainViewModel() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!vm.isLoggedIn) {
                        LoginScreen(vm)
                    } else {
                        TaskScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(vm: MainViewModel) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SIARSP Courier", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = vm.login, onValueChange = { vm.login = it }, label = { Text("Логин") })
        OutlinedTextField(
            value = vm.password,
            onValueChange = { vm.password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = { vm.doLogin() }) { Text("Войти") }
        Text(vm.message)
    }
}

@Composable
private fun TaskScreen(vm: MainViewModel) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Мои активные доставки", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { vm.refreshTasks() }) { Text("Обновить") }

        OutlinedTextField(value = vm.latitudeInput, onValueChange = { vm.latitudeInput = it }, label = { Text("Latitude") })
        OutlinedTextField(value = vm.longitudeInput, onValueChange = { vm.longitudeInput = it }, label = { Text("Longitude") })

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.tasks) { task ->
                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Задача #${task.id}, заказ: ${task.clientOrderNumber ?: "-"}")
                        Text("Статус: ${task.status}")
                        Text("Текущие координаты: ${task.currentLatitude ?: "-"}, ${task.currentLongitude ?: "-"}")
                        if (task.status == "IN_TRANSIT") {
                            Button(onClick = { vm.sendLocation(task.id) }) {
                                Text("Отправить мои координаты")
                            }
                        }
                    }
                }
            }
        }
        Text(vm.message)
    }
}
