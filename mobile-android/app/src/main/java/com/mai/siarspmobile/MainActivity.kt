package com.mai.siarspmobile

import android.Manifest
import android.location.Location
import android.os.Bundle
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume

data class DeliveryTaskDto(
    val id: Long,
    val status: String,
    val clientOrderNumber: String?,
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
                val req: Request = chain.request().newBuilder().header("Accept", "application/json").build()
                return chain.proceed(req)
            }
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CourierApi::class.java)

    suspend fun login(username: String, password: String): Boolean {
        val body = FormBody.Builder().add("username", username).add("password", password).build()
        val request = Request.Builder().url("${BuildConfig.BASE_URL}employee/login").post(body).build()

        return okHttpClient.newCall(request).execute().use {
            val location = it.header("Location")
            it.isRedirect && location != null && !location.contains("error")
        }
    }

    suspend fun loadTasks(): List<DeliveryTaskDto> = api.myDeliveryTasks("active")

    suspend fun sendLocation(taskId: Long, latitude: Double, longitude: Double): ApiResponse {
        return api.updateLocation(taskId, UpdateLocationRequest(latitude, longitude))
    }
}

enum class AppScreen { TASKS, PROFILE, ABOUT }

class MainViewModel : ViewModel() {
    private val repo = SessionRepository()

    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var message by mutableStateOf("Введите логин сотрудника")
    var tasks by mutableStateOf<List<DeliveryTaskDto>>(emptyList())
    var screen by mutableStateOf(AppScreen.TASKS)

    fun doLogin() {
        viewModelScope.launch {
            isLoading = true
            runCatching { repo.login(login, password) }
                .onSuccess {
                    isLoggedIn = it
                    message = if (it) "Вход выполнен" else "Ошибка логина"
                    if (it) refreshTasks()
                }
                .onFailure { message = "Ошибка сети: ${it.message}" }
            isLoading = false
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            runCatching { repo.loadTasks() }
                .onSuccess { tasks = it }
                .onFailure { message = "Не удалось загрузить задачи: ${it.message}" }
        }
    }

    suspend fun sendLocation(taskId: Long, latitude: Double, longitude: Double) {
        runCatching { repo.sendLocation(taskId, latitude, longitude) }
            .onSuccess { message = it.message }
            .onFailure { message = "Ошибка отправки: ${it.message}" }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val vm = remember { MainViewModel() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!vm.isLoggedIn) BeautifulLoginScreen(vm)
                    else CourierApp(vm, fusedClient)
                }
            }
        }
    }
}

@Composable
private fun BeautifulLoginScreen(vm: MainViewModel) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF102A43), Color(0xFF243B53), Color(0xFF486581)))

    Box(
        modifier = Modifier.fillMaxSize().background(gradient).padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SIARSP Courier",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Вход для ROLE_EMPLOYEE_COURIER",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF334E68)
                )

                OutlinedTextField(vm.login, { vm.login = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    vm.password,
                    { vm.password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { vm.doLogin() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !vm.isLoading
                ) {
                    if (vm.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Войти")
                }

                Text(vm.message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334E68))
            }
        }
    }
}

@Composable
private fun CourierApp(vm: MainViewModel, fusedClient: FusedLocationProviderClient) {
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        vm.refreshTasks()
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = hasLocationPermission()
    }

    LaunchedEffect(vm.isLoggedIn, hasLocationPermission) {
        if (!vm.isLoggedIn || !hasLocationPermission) return@LaunchedEffect
        while (true) {
            vm.refreshTasks()
            val currentLocation = fusedClient.awaitLastLocation()
            if (currentLocation != null) {
                vm.tasks.filter { it.status == "IN_TRANSIT" }.forEach { task ->
                    vm.sendLocation(task.id, currentLocation.latitude, currentLocation.longitude)
                }
            }
            delay(BuildConfig.LOCATION_UPDATE_INTERVAL_MS)
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                MenuButton("Доставки", Icons.Default.List) { vm.screen = AppScreen.TASKS }
                MenuButton("Профиль", Icons.Default.Person) { vm.screen = AppScreen.PROFILE }
                MenuButton("О приложении", Icons.Default.Info) { vm.screen = AppScreen.ABOUT }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!hasLocationPermission) {
                PermissionCard {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            }

            when (vm.screen) {
                AppScreen.TASKS -> TaskScreen(vm, hasLocationPermission) { taskId ->
                    scope.launch {
                        if (!hasLocationPermission) {
                            vm.message = "Нужно разрешение на геолокацию"
                            return@launch
                        }
                        val currentLocation = fusedClient.awaitLastLocation()
                        if (currentLocation == null) {
                            vm.message = "Не удалось определить местоположение"
                            return@launch
                        }
                        vm.sendLocation(taskId, currentLocation.latitude, currentLocation.longitude)
                    }
                }

                AppScreen.PROFILE -> ProfileScreen(vm)
                AppScreen.ABOUT -> AboutScreen()
            }
        }
    }
}

@Composable
private fun MenuButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title)
            Spacer(modifier = Modifier.size(4.dp))
            Text(title)
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(modifier = Modifier.padding(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Нужно разрешение на геолокацию")
            Button(onClick = onGrant) { Text("Разрешить") }
        }
    }
}

@Composable
private fun TaskScreen(vm: MainViewModel, hasLocationPermission: Boolean, onSendCurrentLocation: (Long) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Мои активные доставки", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { vm.refreshTasks() }) { Text("Обновить") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(vm.tasks) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Задача #${task.id}, заказ: ${task.clientOrderNumber ?: "-"}")
                        Text("Статус: ${task.status}")
                        Text("Текущие координаты: ${task.currentLatitude ?: "-"}, ${task.currentLongitude ?: "-"}")
                        if (task.status == "IN_TRANSIT") {
                            Button(onClick = { onSendCurrentLocation(task.id) }, enabled = hasLocationPermission) {
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

@Composable
private fun ProfileScreen(vm: MainViewModel) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        Text("Пользователь: ${vm.login}")
        Text("Роль: ROLE_EMPLOYEE_COURIER")
    }
}

@Composable
private fun AboutScreen() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("О приложении", style = MaterialTheme.typography.headlineSmall)
        Text("Учебный мобильный клиент SIARSP для курьера.")
        Text("Координаты отправляются вручную кнопкой и автоматически по таймеру.")
    }
}

@Composable
private fun hasLocationPermission(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}

private suspend fun FusedLocationProviderClient.awaitLastLocation(): Location? {
    return suspendCancellableCoroutine { cont ->
        lastLocation.addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
    }
}
