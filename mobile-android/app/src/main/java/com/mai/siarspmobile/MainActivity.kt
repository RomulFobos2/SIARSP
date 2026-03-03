package com.mai.siarspmobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private const val BASE_URL = "http://10.0.2.2:8080/"

object AppConfig {
    const val AUTO_LOCATION_SEND_INTERVAL_MS = 60_000L
}

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

enum class AppPage(val title: String) {
    TASKS("Доставки"),
    PROFILE("Профиль")
}

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
        .followRedirects(false)
        .followSslRedirects(false)
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

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("username", username).add("password", password).build()

        val request = Request.Builder().url("${BASE_URL}employee/login").post(body).build()

        okHttpClient.newCall(request).execute().use { resp ->
            Log.e("SIARSP", "LOGIN resp.code=${resp.code}, isRedirect=${resp.isRedirect}, Location=${resp.header("Location")}")
            cookieJar.loadForRequest(BASE_URL.toHttpUrl()).any { it.name.equals("JSESSIONID", ignoreCase = true) }
        }
    }

    suspend fun loadTasks(status: String = "active"): List<DeliveryTaskDto> = api.myDeliveryTasks(status)

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
    var selectedPage by mutableStateOf(AppPage.TASKS)
    var selectedStatus by mutableStateOf("active")

    fun doLogin() {
        viewModelScope.launch {
            runCatching { repo.login(login, password) }
                .onSuccess {
                    isLoggedIn = it
                    message = if (it) "Вход выполнен" else "Ошибка логина"
                    if (it) refreshTasks()
                }
                .onFailure {
                    Log.e("SIARSP", "Login failed", it)
                    message = "Ошибка сети: ${it.message}"
                }
        }
    }

    fun refreshTasks(status: String = selectedStatus) {
        selectedStatus = status
        viewModelScope.launch {
            runCatching { repo.loadTasks(status) }
                .onSuccess { tasks = it }
                .onFailure { message = "Не удалось загрузить задачи: ${it.message}" }
        }
    }

    fun sendLocationForTask(taskId: Long, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            runCatching { repo.sendLocation(taskId, latitude, longitude) }
                .onSuccess { message = it.message }
                .onFailure { message = "Ошибка отправки: ${it.message}" }
        }
    }

    fun sendLocationForActiveTasks(latitude: Double, longitude: Double) {
        val inTransitTasks = tasks.filter { it.status == "IN_TRANSIT" }
        if (inTransitTasks.isEmpty()) {
            message = "Нет задач в пути для отправки координат"
            return
        }
        inTransitTasks.forEach { sendLocationForTask(it.id, latitude, longitude) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = remember { MainViewModel() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!vm.isLoggedIn) LoginScreen(vm) else MainAppScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(vm: MainViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SIARSP Courier", style = MaterialTheme.typography.headlineSmall)
                Text("Вход в мобильное приложение")
                OutlinedTextField(vm.login, { vm.login = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    vm.password,
                    { vm.password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { vm.doLogin() }, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
                Text(vm.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScreen(vm: MainViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(vm.selectedPage.title) }) },
        bottomBar = {
            NavigationBar {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = vm.selectedPage == page,
                        onClick = { vm.selectedPage = page },
                        icon = { Text(if (page == AppPage.TASKS) "🚚" else "👤") },
                        label = { Text(page.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (vm.selectedPage) {
            AppPage.TASKS -> TaskScreen(vm, paddingValues)
            AppPage.PROFILE -> ProfileScreen(vm, paddingValues)
        }
    }
}

@Composable
private fun TaskScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    val coroutineScope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) vm.message = "Нужно разрешение на геолокацию"
    }

    suspend fun sendCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        val location = runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
        if (location == null) {
            vm.message = "Не удалось получить текущее местоположение"
            return
        }

        vm.sendLocationForActiveTasks(location.latitude, location.longitude)
    }

    LaunchedEffect(Unit) {
        vm.refreshTasks(vm.selectedStatus)
        while (true) {
            sendCurrentLocation()
            delay(AppConfig.AUTO_LOCATION_SEND_INTERVAL_MS)
        }
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.selectedStatus == "active", onClick = { vm.refreshTasks("active") }, label = { Text("Активные") })
            FilterChip(selected = vm.selectedStatus == "completed", onClick = { vm.refreshTasks("completed") }, label = { Text("Завершённые") })
            FilterChip(selected = vm.selectedStatus == "all", onClick = { vm.refreshTasks("all") }, label = { Text("Все") })
        }
        Button(onClick = { vm.refreshTasks(vm.selectedStatus) }, modifier = Modifier.fillMaxWidth()) { Text("Обновить задачи") }
        Button(onClick = {
            vm.message = "Определяем координаты..."
            coroutineScope.launch { sendCurrentLocation() }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Отправить мои координаты")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.tasks) { task ->
                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Задача #${task.id}, заказ: ${task.clientOrderNumber ?: "-"}")
                        Text("Статус: ${task.status}")
                        Text("План: ${task.plannedStartTime ?: "-"}")
                        Text("Координаты: ${task.currentLatitude ?: "-"}, ${task.currentLongitude ?: "-"}")
                    }
                }
            }
        }
        Text(vm.message)
    }
}

@Composable
private fun ProfileScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Меню", style = MaterialTheme.typography.headlineSmall)
        Text("Используйте нижнее меню для перехода между страницами.")
        Text("Интервал автоотправки: ${AppConfig.AUTO_LOCATION_SEND_INTERVAL_MS / 1000} сек")
        Button(onClick = { vm.isLoggedIn = false }) { Text("Выйти") }
    }
}
