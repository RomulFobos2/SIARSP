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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
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

// ========== КОНФИГУРАЦИЯ ==========

private const val BASE_URL = "http://10.0.2.2:8080/"

object AppConfig {
    const val AUTO_LOCATION_SEND_INTERVAL_MS = 60_000L
}

// ========== DTO ==========

data class DeliveryTaskDto(
    val id: Long,
    val status: String,
    val clientOrderNumber: String?,
    val plannedStartTime: String?,
    val currentLatitude: Double?,
    val currentLongitude: Double?,
    val driverFullName: String?,
    val vehicleRegistrationNumber: String?,
    val totalMileage: Int?,
    val routePoints: List<RoutePointDto> = emptyList(),
    val startMileage: Int?,
    val endMileage: Int?
)

data class RoutePointDto(
    val id: Long,
    val orderIndex: Int,
    val address: String?,
    val reached: Boolean,
    val actualArrivalTime: String?
)

data class UpdateLocationRequest(val latitude: Double, val longitude: Double)

data class StartDeliveryRequest(val startMileage: Int)
data class CompleteDeliveryRequest(
    val endMileage: Int,
    val clientRepresentative: String?,
    val actComment: String?
)

data class ApiResponse(val success: Boolean, val message: String)

data class EmployeeProfile(
    val id: Long,
    val lastName: String?,
    val firstName: String?,
    val patronymicName: String?,
    val fullName: String?,
    val username: String?,
    val roleName: String?,
    val roleDescription: String?,
    val active: Boolean
)

data class ClientDto(
    val id: Long,
    val organizationType: String?,
    val organizationName: String?,
    val inn: String?,
    val deliveryAddress: String?,
    val contactPerson: String?,
    val phoneNumber: String?,
    val email: String?
)

data class SupplierDto(
    val id: Long,
    val name: String?,
    val contactInfo: String?,
    val address: String?,
    val inn: String?,
    val fullName: String?
)

data class ProductDto(
    val id: Long,
    val name: String?,
    val article: String?,
    val stockQuantity: Int,
    val categoryName: String?,
    val availableQuantity: Int
)

data class WarehouseDto(
    val id: Long,
    val name: String?,
    val type: String?,
    val totalVolume: Double
)

data class VehicleDto(
    val id: Long,
    val registrationNumber: String?,
    val brand: String?,
    val model: String?,
    val type: String?,
    val status: String?,
    val currentMileage: Int?,
    val fullName: String?,
    val available: Boolean
)

data class ClientOrderDto(
    val id: Long,
    val orderNumber: String?,
    val orderDate: String?,
    val deliveryDate: String?,
    val status: String?,
    val statusDisplayName: String?,
    val totalAmount: Double?,
    val clientOrganizationName: String?,
    val responsibleEmployeeFullName: String?
)

data class TTNDto(
    val id: Long,
    val ttnNumber: String?,
    val issueDate: String?,
    val cargoDescription: String?,
    val totalWeight: Double?,
    val totalVolume: Double?,
    val vehicleRegistrationNumber: String?,
    val driverFullName: String?
)

data class AcceptanceActDto(
    val id: Long,
    val actNumber: String?,
    val actDate: String?,
    val clientRepresentative: String?,
    val signed: Boolean,
    val clientOrderNumber: String?,
    val clientOrganizationName: String?,
    val deliveredByFullName: String?
)

// ========== УТИЛИТЫ ФОРМАТИРОВАНИЯ ==========

/** Конвертирует ISO-дату (yyyy-MM-dd) в формат dd.MM.yyyy */
fun formatDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "—"
    return try {
        val parts = isoDate.split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } catch (e: Exception) { isoDate }
}

/** Конвертирует ISO-дату-время (yyyy-MM-ddTHH:mm:ss) в формат dd.MM.yyyy HH:mm */
fun formatDateTime(isoDateTime: String?): String {
    if (isoDateTime.isNullOrBlank()) return "—"
    return try {
        val dateTimeParts = isoDateTime.split("T")
        val datePart = dateTimeParts[0].split("-") // yyyy-MM-dd
        val timePart = dateTimeParts.getOrElse(1) { "00:00" }.substring(0, 5) // HH:mm
        "${datePart[2]}.${datePart[1]}.${datePart[0]} $timePart"
    } catch (e: Exception) { isoDateTime }
}

/** Возвращает человекочитаемое название по enum-имени */
fun displayName(value: String?, map: Map<String, String>): String =
    map[value] ?: value ?: "—"

val deliveryTaskStatusNames = mapOf(
    "PENDING" to "Ожидает выполнения",
    "LOADING" to "Идет погрузка",
    "LOADED" to "Ожидает отправки",
    "IN_TRANSIT" to "В пути",
    "DELIVERED" to "Доставлено",
    "CANCELLED" to "Отменено"
)

val vehicleStatusNames = mapOf(
    "AVAILABLE" to "Готов к работе",
    "IN_USE" to "В работе",
    "MAINTENANCE" to "На обслуживании",
    "BROKEN" to "Неисправен",
    "DECOMMISSIONED" to "Списан"
)

val vehicleTypeNames = mapOf(
    "STANDARD" to "Обычный",
    "REFRIGERATED" to "Рефрижератор"
)

val warehouseTypeNames = mapOf(
    "REGULAR" to "Обычный склад",
    "REFRIGERATOR" to "Холодильная камера"
)

// ========== НАВИГАЦИЯ ==========

enum class AppPage(val title: String) {
    HOME("Главная"),
    TASKS("Доставки"),
    CLIENTS("Клиенты"),
    SUPPLIERS("Поставщики"),
    PRODUCTS("Товары"),
    WAREHOUSES("Склады"),
    VEHICLES("Транспорт"),
    ORDERS("Заказы"),
    DELIVERY_TASKS("Задачи доставки"),
    DOCUMENTS("Документы"),
    EMPLOYEES("Сотрудники"),
    PROFILE("Профиль")
}

// ========== API ==========

interface CourierApi {
    @GET("employee/courier/deliveryTasks/mobile/myDeliveryTasks")
    suspend fun myDeliveryTasks(@Query("status") status: String = "active"): List<DeliveryTaskDto>

    @POST("employee/courier/deliveryTasks/mobile/updateLocation/{id}")
    suspend fun updateLocation(@Path("id") id: Long, @Body request: UpdateLocationRequest): ApiResponse

    @POST("employee/courier/deliveryTasks/mobile/startDelivery/{id}")
    suspend fun startDelivery(@Path("id") id: Long, @Body request: StartDeliveryRequest): ApiResponse

    @POST("employee/courier/deliveryTasks/mobile/markRoutePoint/{id}/{pointId}")
    suspend fun markRoutePoint(@Path("id") id: Long, @Path("pointId") pointId: Long): ApiResponse

    @POST("employee/courier/deliveryTasks/mobile/completeDelivery/{id}")
    suspend fun completeDelivery(@Path("id") id: Long, @Body request: CompleteDeliveryRequest): ApiResponse
}

interface MobileApi {
    @GET("api/mobile/profile")
    suspend fun getProfile(): EmployeeProfile

    @GET("api/mobile/clients")
    suspend fun getClients(): List<ClientDto>

    @GET("api/mobile/suppliers")
    suspend fun getSuppliers(): List<SupplierDto>

    @GET("api/mobile/products")
    suspend fun getProducts(): List<ProductDto>

    @GET("api/mobile/warehouses")
    suspend fun getWarehouses(): List<WarehouseDto>

    @GET("api/mobile/vehicles")
    suspend fun getVehicles(): List<VehicleDto>

    @GET("api/mobile/orders")
    suspend fun getOrders(): List<ClientOrderDto>

    @GET("api/mobile/deliveryTasks")
    suspend fun getDeliveryTasks(): List<DeliveryTaskDto>

    @GET("api/mobile/documents/ttn")
    suspend fun getTTNList(): List<TTNDto>

    @GET("api/mobile/documents/acts")
    suspend fun getAcceptanceActs(): List<AcceptanceActDto>

    @GET("api/mobile/employees")
    suspend fun getEmployees(): List<EmployeeProfile>
}

// ========== СЕТЬ ==========

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

                val response = chain.proceed(req)
                Log.d(
                    "SIARSP",
                    "HTTP ${req.method} ${req.url.encodedPath} -> ${response.code}, location=${response.header("Location") ?: "-"}"
                )
                return response
            }
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val courierApi = retrofit.create(CourierApi::class.java)
    val mobileApi: MobileApi = retrofit.create(MobileApi::class.java)

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim()
        val body = FormBody.Builder().add("username", normalizedUsername).add("password", password).build()

        val request = Request.Builder().url("${BASE_URL}employee/login").post(body).build()

        okHttpClient.newCall(request).execute().use { resp ->
            val redirectLocation = resp.header("Location").orEmpty()
            val hasSessionCookie = cookieJar
                .loadForRequest(BASE_URL.toHttpUrl())
                .any { it.name.equals("JSESSIONID", ignoreCase = true) }

            val isLoginRedirect = redirectLocation.contains("/employee/login", ignoreCase = true)
            val isLoginErrorRedirect = redirectLocation.contains("/employee/login?error", ignoreCase = true)

            Log.i(
                "SIARSP",
                "LOGIN request: user='$normalizedUsername', passwordLength=${password.length}, code=${resp.code}, isRedirect=${resp.isRedirect}, location=$redirectLocation, hasSessionCookie=$hasSessionCookie"
            )

            if (resp.isRedirect) {
                return@use hasSessionCookie && !isLoginRedirect && !isLoginErrorRedirect
            }

            resp.isSuccessful && hasSessionCookie
        }
    }

    suspend fun loadTasks(status: String = "active"): List<DeliveryTaskDto> = courierApi.myDeliveryTasks(status)

    suspend fun sendLocation(taskId: Long, latitude: Double, longitude: Double): ApiResponse {
        return courierApi.updateLocation(taskId, UpdateLocationRequest(latitude, longitude))
    }

    suspend fun startDelivery(taskId: Long, startMileage: Int): ApiResponse {
        return courierApi.startDelivery(taskId, StartDeliveryRequest(startMileage))
    }

    suspend fun markRoutePoint(taskId: Long, pointId: Long): ApiResponse {
        return courierApi.markRoutePoint(taskId, pointId)
    }

    suspend fun completeDelivery(taskId: Long, endMileage: Int, clientRepresentative: String?, actComment: String?): ApiResponse {
        return courierApi.completeDelivery(taskId, CompleteDeliveryRequest(endMileage, clientRepresentative, actComment))
    }
}

// ========== VIEWMODEL ==========

class MainViewModel : ViewModel() {
    private val repo = SessionRepository()

    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoggedIn by mutableStateOf(false)
    var message by mutableStateOf("Введите логин сотрудника")
    var tasks by mutableStateOf<List<DeliveryTaskDto>>(emptyList())
    var selectedPage by mutableStateOf(AppPage.HOME)
    var selectedStatus by mutableStateOf("active")

    // Профиль
    var profile by mutableStateOf<EmployeeProfile?>(null)
    var userRole by mutableStateOf("")

    // Данные справочников
    var clients by mutableStateOf<List<ClientDto>>(emptyList())
    var suppliers by mutableStateOf<List<SupplierDto>>(emptyList())
    var products by mutableStateOf<List<ProductDto>>(emptyList())
    var warehouses by mutableStateOf<List<WarehouseDto>>(emptyList())
    var vehicles by mutableStateOf<List<VehicleDto>>(emptyList())
    var orders by mutableStateOf<List<ClientOrderDto>>(emptyList())
    var allDeliveryTasks by mutableStateOf<List<DeliveryTaskDto>>(emptyList())
    var ttnList by mutableStateOf<List<TTNDto>>(emptyList())
    var acceptanceActs by mutableStateOf<List<AcceptanceActDto>>(emptyList())
    var employees by mutableStateOf<List<EmployeeProfile>>(emptyList())

    // Навигация
    var previousPage by mutableStateOf<AppPage?>(null)

    fun doLogin() {
        viewModelScope.launch {
            val usernameForLogin = login.trim()
            runCatching { repo.login(usernameForLogin, password) }
                .onSuccess {
                    isLoggedIn = it
                    message = if (it) "Вход выполнен" else "Ошибка логина"
                    Log.i("SIARSP", "Login result: success=$it, user='$usernameForLogin'")
                    if (it) {
                        loadProfile()
                    }
                }
                .onFailure {
                    Log.e("SIARSP", "Login failed", it)
                    message = "Ошибка сети: ${it.message}"
                }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getProfile() }
                .onSuccess {
                    profile = it
                    userRole = it.roleName ?: ""
                    Log.i("SIARSP", "Profile loaded: username=${it.username}, role=$userRole")
                    if (isCourier()) {
                        refreshTasks()
                    }
                }
                .onFailure {
                    Log.e("SIARSP", "Profile load failed", it)
                    message = "Не удалось загрузить профиль"
                }
        }
    }

    fun isCourier() = userRole == "ROLE_EMPLOYEE_COURIER"
    fun isAdmin() = userRole == "ROLE_EMPLOYEE_ADMIN"
    fun isManager() = userRole == "ROLE_EMPLOYEE_MANAGER"
    fun isWarehouseManager() = userRole == "ROLE_EMPLOYEE_WAREHOUSE_MANAGER"
    fun isWarehouseWorker() = userRole == "ROLE_EMPLOYEE_WAREHOUSE_WORKER"
    fun isAccounter() = userRole == "ROLE_EMPLOYEE_ACCOUNTER"

    fun navigateTo(page: AppPage) {
        previousPage = selectedPage
        selectedPage = page
    }

    fun goBack() {
        selectedPage = previousPage ?: AppPage.HOME
        previousPage = null
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

    fun startDelivery(taskId: Long, startMileage: Int) {
        viewModelScope.launch {
            runCatching { repo.startDelivery(taskId, startMileage) }
                .onSuccess {
                    message = it.message
                    refreshTasks(selectedStatus)
                }
                .onFailure { message = "Ошибка начала доставки: ${it.message}" }
        }
    }

    fun markRoutePoint(taskId: Long, pointId: Long) {
        viewModelScope.launch {
            runCatching { repo.markRoutePoint(taskId, pointId) }
                .onSuccess {
                    message = it.message
                    refreshTasks(selectedStatus)
                }
                .onFailure { message = "Ошибка отметки точки: ${it.message}" }
        }
    }

    fun completeDelivery(taskId: Long, endMileage: Int, clientRepresentative: String?, actComment: String?) {
        viewModelScope.launch {
            runCatching { repo.completeDelivery(taskId, endMileage, clientRepresentative, actComment) }
                .onSuccess {
                    message = it.message
                    refreshTasks(selectedStatus)
                }
                .onFailure { message = "Ошибка завершения доставки: ${it.message}" }
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

    fun loadClients() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getClients() }
                .onSuccess { clients = it }
                .onFailure { message = "Ошибка загрузки клиентов: ${it.message}" }
        }
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getSuppliers() }
                .onSuccess { suppliers = it }
                .onFailure { message = "Ошибка загрузки поставщиков: ${it.message}" }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getProducts() }
                .onSuccess { products = it }
                .onFailure { message = "Ошибка загрузки товаров: ${it.message}" }
        }
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getWarehouses() }
                .onSuccess { warehouses = it }
                .onFailure { message = "Ошибка загрузки складов: ${it.message}" }
        }
    }

    fun loadVehicles() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getVehicles() }
                .onSuccess { vehicles = it }
                .onFailure { message = "Ошибка загрузки транспорта: ${it.message}" }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getOrders() }
                .onSuccess { orders = it }
                .onFailure { message = "Ошибка загрузки заказов: ${it.message}" }
        }
    }

    fun loadAllDeliveryTasks() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getDeliveryTasks() }
                .onSuccess { allDeliveryTasks = it }
                .onFailure { message = "Ошибка загрузки задач: ${it.message}" }
        }
    }

    fun loadTTNList() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getTTNList() }
                .onSuccess { ttnList = it }
                .onFailure { message = "Ошибка загрузки ТТН: ${it.message}" }
        }
    }

    fun loadAcceptanceActs() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getAcceptanceActs() }
                .onSuccess { acceptanceActs = it }
                .onFailure { message = "Ошибка загрузки актов: ${it.message}" }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            runCatching { repo.mobileApi.getEmployees() }
                .onSuccess { employees = it }
                .onFailure { message = "Ошибка загрузки сотрудников: ${it.message}" }
        }
    }
}

// ========== ACTIVITY ==========

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

// ========== ЭКРАН ЛОГИНА ==========

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
                Text("SIARSP Mobile", style = MaterialTheme.typography.headlineSmall)
                Text("Вход в мобильное приложение")
                OutlinedTextField(
                    vm.login,
                    { vm.login = it },
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii
                    )
                )
                OutlinedTextField(
                    vm.password,
                    { vm.password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password
                    )
                )
                Button(onClick = { vm.doLogin() }, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
                Text(vm.message)
            }
        }
    }
}

// ========== ГЛАВНЫЙ ЭКРАН ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScreen(vm: MainViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.selectedPage.title) },
                navigationIcon = {
                    if (vm.selectedPage != AppPage.HOME && vm.selectedPage != AppPage.PROFILE) {
                        IconButton(onClick = { vm.goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = vm.selectedPage == AppPage.HOME,
                    onClick = { vm.selectedPage = AppPage.HOME },
                    icon = { Text("\uD83C\uDFE0") },
                    label = { Text("Главная") }
                )
                if (vm.isCourier()) {
                    NavigationBarItem(
                        selected = vm.selectedPage == AppPage.TASKS,
                        onClick = { vm.selectedPage = AppPage.TASKS },
                        icon = { Text("\uD83D\uDE9A") },
                        label = { Text("Доставки") }
                    )
                }
                NavigationBarItem(
                    selected = vm.selectedPage == AppPage.PROFILE,
                    onClick = { vm.selectedPage = AppPage.PROFILE },
                    icon = { Text("\uD83D\uDC64") },
                    label = { Text("Профиль") }
                )
            }
        }
    ) { paddingValues ->
        when (vm.selectedPage) {
            AppPage.HOME -> HomeScreen(vm, paddingValues)
            AppPage.TASKS -> TaskScreen(vm, paddingValues)
            AppPage.CLIENTS -> ClientsScreen(vm, paddingValues)
            AppPage.SUPPLIERS -> SuppliersScreen(vm, paddingValues)
            AppPage.PRODUCTS -> ProductsScreen(vm, paddingValues)
            AppPage.WAREHOUSES -> WarehousesScreen(vm, paddingValues)
            AppPage.VEHICLES -> VehiclesScreen(vm, paddingValues)
            AppPage.ORDERS -> OrdersScreen(vm, paddingValues)
            AppPage.DELIVERY_TASKS -> DeliveryTasksScreen(vm, paddingValues)
            AppPage.DOCUMENTS -> DocumentsScreen(vm, paddingValues)
            AppPage.EMPLOYEES -> EmployeesScreen(vm, paddingValues)
            AppPage.PROFILE -> ProfileScreen(vm, paddingValues)
        }
    }
}

// ========== ДОМАШНИЙ ЭКРАН ==========

data class MenuTile(val title: String, val emoji: String, val page: AppPage)

@Composable
private fun HomeScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    val tiles = buildList {
        // Курьер — свои задачи
        if (vm.isCourier()) {
            add(MenuTile("Мои доставки", "\uD83D\uDE9A", AppPage.TASKS))
        }
        // Клиенты
        if (vm.isAdmin() || vm.isManager() || vm.isWarehouseManager() || vm.isAccounter()) {
            add(MenuTile("Клиенты", "\uD83C\uDFE2", AppPage.CLIENTS))
        }
        // Поставщики
        if (vm.isAdmin() || vm.isManager() || vm.isWarehouseManager() || vm.isAccounter()) {
            add(MenuTile("Поставщики", "\uD83D\uDCE6", AppPage.SUPPLIERS))
        }
        // Товары
        if (!vm.isCourier()) {
            add(MenuTile("Товары", "\uD83D\uDED2", AppPage.PRODUCTS))
        }
        // Склады
        add(MenuTile("Склады", "\uD83C\uDFED", AppPage.WAREHOUSES))
        // Транспорт
        if (vm.isManager() || vm.isCourier()) {
            add(MenuTile("Транспорт", "\uD83D\uDE97", AppPage.VEHICLES))
        }
        // Заказы
        if (!vm.isCourier()) {
            add(MenuTile("Заказы", "\uD83D\uDCCB", AppPage.ORDERS))
        }
        // Задачи на доставку
        if (vm.isManager() || vm.isWarehouseManager() || vm.isCourier()) {
            add(MenuTile("Задачи доставки", "\uD83D\uDCCD", AppPage.DELIVERY_TASKS))
        }
        // Документы
        if (vm.isManager() || vm.isAccounter() || vm.isWarehouseManager() || vm.isCourier()) {
            add(MenuTile("Документы", "\uD83D\uDCC4", AppPage.DOCUMENTS))
        }
        // Сотрудники
        if (vm.isAdmin()) {
            add(MenuTile("Сотрудники", "\uD83D\uDC65", AppPage.EMPLOYEES))
        }
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        val roleName = vm.profile?.roleDescription ?: vm.userRole
        Text(
            "Добро пожаловать, ${vm.profile?.fullName ?: ""}",
            style = MaterialTheme.typography.titleMedium
        )
        Text("Роль: $roleName", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tiles) { tile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { vm.navigateTo(tile.page) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tile.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ========== ЭКРАН ЗАДАЧ КУРЬЕРА (существующий) ==========

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
                var startMileageText by remember(task.id) { mutableStateOf("") }
                var endMileageText by remember(task.id) { mutableStateOf("") }
                var representativeText by remember(task.id) { mutableStateOf("") }
                var commentText by remember(task.id) { mutableStateOf("") }

                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Задача #${task.id}, заказ: ${task.clientOrderNumber ?: "—"}")
                        Text("Статус: ${displayName(task.status, deliveryTaskStatusNames)}")
                        Text("План: ${formatDateTime(task.plannedStartTime)}")
                        Text("Координаты: ${task.currentLatitude ?: "—"}, ${task.currentLongitude ?: "—"}")

                        if (task.status == "LOADED") {
                            OutlinedTextField(
                                value = startMileageText,
                                onValueChange = { startMileageText = it.filter(Char::isDigit) },
                                label = { Text("Начальный пробег") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val mileage = startMileageText.toIntOrNull()
                                    if (mileage == null) {
                                        vm.message = "Введите корректный начальный пробег"
                                    } else {
                                        vm.startDelivery(task.id, mileage)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Начать доставку") }
                        }

                        if (task.status == "IN_TRANSIT" && task.routePoints.isNotEmpty()) {
                            Text("Маршрутные точки:", fontWeight = FontWeight.Medium)
                            task.routePoints.sortedBy { it.orderIndex }.forEach { point ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${point.orderIndex + 1}. ${point.address ?: "—"}")
                                    if (point.reached) {
                                        Text("Пройдена", color = Color(0xFF2E7D32))
                                    } else {
                                        Button(onClick = { vm.markRoutePoint(task.id, point.id) }) {
                                            Text("Отметить")
                                        }
                                    }
                                }
                            }
                        }

                        if (task.status == "IN_TRANSIT") {
                            OutlinedTextField(
                                value = endMileageText,
                                onValueChange = { endMileageText = it.filter(Char::isDigit) },
                                label = { Text("Конечный пробег") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = representativeText,
                                onValueChange = { representativeText = it },
                                label = { Text("Представитель клиента (необязательно)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                label = { Text("Комментарий к акту (необязательно)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val mileage = endMileageText.toIntOrNull()
                                    if (mileage == null) {
                                        vm.message = "Введите корректный конечный пробег"
                                    } else {
                                        vm.completeDelivery(
                                            task.id,
                                            mileage,
                                            representativeText.ifBlank { null },
                                            commentText.ifBlank { null }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Завершить доставку") }
                        }
                    }
                }
            }
        }
        Text(vm.message)
    }
}

// ========== ЭКРАН КЛИЕНТОВ ==========

@Composable
private fun ClientsScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadClients() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.clients) { client ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(client.organizationName ?: "—", fontWeight = FontWeight.Bold)
                    Text("Тип: ${client.organizationType ?: "—"}")
                    Text("ИНН: ${client.inn ?: "—"}")
                    Text("Адрес доставки: ${client.deliveryAddress ?: "—"}")
                    Text("Контакт: ${client.contactPerson ?: "—"}")
                    if (!client.phoneNumber.isNullOrBlank()) Text("Тел: ${client.phoneNumber}")
                    if (!client.email.isNullOrBlank()) Text("Email: ${client.email}")
                }
            }
        }
    }
}

// ========== ЭКРАН ПОСТАВЩИКОВ ==========

@Composable
private fun SuppliersScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadSuppliers() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.suppliers) { supplier ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(supplier.name ?: "—", fontWeight = FontWeight.Bold)
                    Text("ИНН: ${supplier.inn ?: "—"}")
                    Text("Адрес: ${supplier.address ?: "—"}")
                    Text("Контакт: ${supplier.contactInfo ?: "—"}")
                }
            }
        }
    }
}

// ========== ЭКРАН ТОВАРОВ ==========

@Composable
private fun ProductsScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadProducts() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.products) { product ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(product.name ?: "—", fontWeight = FontWeight.Bold)
                    Text("Артикул: ${product.article ?: "—"}")
                    Text("Категория: ${product.categoryName ?: "—"}")
                    Text("На складе: ${product.stockQuantity}, доступно: ${product.availableQuantity}")
                }
            }
        }
    }
}

// ========== ЭКРАН СКЛАДОВ ==========

@Composable
private fun WarehousesScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadWarehouses() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.warehouses) { warehouse ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(warehouse.name ?: "—", fontWeight = FontWeight.Bold)
                    Text("Тип: ${displayName(warehouse.type, warehouseTypeNames)}")
                    Text("Объём: ${warehouse.totalVolume} м³")
                }
            }
        }
    }
}

// ========== ЭКРАН ТРАНСПОРТА ==========

@Composable
private fun VehiclesScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadVehicles() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.vehicles) { vehicle ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(vehicle.fullName ?: "${vehicle.brand ?: ""} ${vehicle.model ?: ""}", fontWeight = FontWeight.Bold)
                    Text("Номер: ${vehicle.registrationNumber ?: "—"}")
                    Text("Тип: ${displayName(vehicle.type, vehicleTypeNames)}")
                    Text("Статус: ${displayName(vehicle.status, vehicleStatusNames)}")
                    if (vehicle.currentMileage != null) Text("Пробег: ${vehicle.currentMileage} км")
                }
            }
        }
    }
}

// ========== ЭКРАН ЗАКАЗОВ ==========

@Composable
private fun OrdersScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadOrders() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.orders) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${order.orderNumber ?: "—"}", fontWeight = FontWeight.Bold)
                    Text("Клиент: ${order.clientOrganizationName ?: "—"}")
                    Text("Статус: ${order.statusDisplayName ?: order.status ?: "—"}")
                    Text("Дата заказа: ${formatDate(order.orderDate)}")
                    Text("Дата доставки: ${formatDate(order.deliveryDate)}")
                    if (order.totalAmount != null) Text("Сумма: ${order.totalAmount} руб.")
                }
            }
        }
    }
}

// ========== ЭКРАН ЗАДАЧ НА ДОСТАВКУ ==========

@Composable
private fun DeliveryTasksScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadAllDeliveryTasks() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.allDeliveryTasks) { task ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Задача #${task.id}, заказ: ${task.clientOrderNumber ?: "—"}", fontWeight = FontWeight.Bold)
                    Text("Статус: ${displayName(task.status, deliveryTaskStatusNames)}")
                    Text("Водитель: ${task.driverFullName ?: "—"}")
                    Text("Автомобиль: ${task.vehicleRegistrationNumber ?: "—"}")
                    Text("Плановый старт: ${formatDateTime(task.plannedStartTime)}")
                    if (task.totalMileage != null) Text("Пробег: ${task.totalMileage} км")
                }
            }
        }
    }
}

// ========== ЭКРАН ДОКУМЕНТОВ ==========

@Composable
private fun DocumentsScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    var showTTN by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        vm.loadTTNList()
        vm.loadAcceptanceActs()
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = showTTN, onClick = { showTTN = true }, label = { Text("ТТН") })
            FilterChip(selected = !showTTN, onClick = { showTTN = false }, label = { Text("Акты приёмки") })
        }

        if (showTTN) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.ttnList) { ttn ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ttn.ttnNumber ?: "—", fontWeight = FontWeight.Bold)
                            Text("Дата: ${formatDate(ttn.issueDate)}")
                            Text("Груз: ${ttn.cargoDescription ?: "—"}")
                            Text("Водитель: ${ttn.driverFullName ?: "—"}")
                            Text("Автомобиль: ${ttn.vehicleRegistrationNumber ?: "—"}")
                            if (ttn.totalWeight != null) Text("Вес: ${ttn.totalWeight} кг")
                            if (ttn.totalVolume != null) Text("Объём: ${ttn.totalVolume} м³")
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.acceptanceActs) { act ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(act.actNumber ?: "—", fontWeight = FontWeight.Bold)
                            Text("Дата: ${formatDate(act.actDate)}")
                            Text("Заказ: ${act.clientOrderNumber ?: "—"}")
                            Text("Клиент: ${act.clientOrganizationName ?: "—"}")
                            Text("Подписан: ${if (act.signed) "Да" else "Нет"}")
                            Text("Передал: ${act.deliveredByFullName ?: "—"}")
                            if (!act.clientRepresentative.isNullOrBlank()) Text("Представитель: ${act.clientRepresentative}")
                        }
                    }
                }
            }
        }
    }
}

// ========== ЭКРАН СОТРУДНИКОВ ==========

@Composable
private fun EmployeesScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    LaunchedEffect(Unit) { vm.loadEmployees() }

    LazyColumn(
        modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vm.employees) { employee ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(employee.fullName ?: "—", fontWeight = FontWeight.Bold)
                    Text("Логин: ${employee.username ?: "—"}")
                    Text("Роль: ${employee.roleDescription ?: employee.roleName ?: "—"}")
                    Text("Активен: ${if (employee.active) "Да" else "Нет"}")
                }
            }
        }
    }
}

// ========== ЭКРАН ПРОФИЛЯ ==========

@Composable
private fun ProfileScreen(vm: MainViewModel, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        if (vm.profile != null) {
            Text("ФИО: ${vm.profile!!.fullName ?: "—"}")
            Text("Логин: ${vm.profile!!.username ?: "—"}")
            Text("Роль: ${vm.profile!!.roleDescription ?: vm.profile!!.roleName ?: "—"}")
        }
        if (vm.isCourier()) {
            Text("Интервал автоотправки: ${AppConfig.AUTO_LOCATION_SEND_INTERVAL_MS / 1000} сек")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            vm.isLoggedIn = false
            vm.profile = null
            vm.userRole = ""
            vm.selectedPage = AppPage.HOME
        }) { Text("Выйти") }
    }
}
