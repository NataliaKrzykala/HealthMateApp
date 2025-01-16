package com.example.healthmate.ble

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
//import com.patrykandpatrick.vico.core.entry.FloatEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthmate.data.HealthMateRepository
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.ParametrPomiaru
import com.example.healthmate.data.Pomiar
import com.example.healthmate.data.PomiarZParametrami
import com.example.healthmate.data.Urzadzenie
import com.example.healthmate.data.Uzytkownik
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class BluetoothViewModel(
        bluetoothHandler: BluetoothHandler,
        private val repository: HealthMateRepository
    ) : ViewModel() {

        // Flow przechowujący stany połączenia BLE
        private val _connectionStateFlow = MutableStateFlow<BluetoothHandler.ConnectionState>(
            BluetoothHandler.ConnectionState.DISCONNECTED // Wartość początkowa
        )
        val connectionStateFlow = _connectionStateFlow.asStateFlow()

        // Wywoływane przy każdej zmianie stanu połączenia
        fun onConnectionStateChanged(newState: BluetoothHandler.ConnectionState) {
            Log.e("BluetoothViewModel", "Received new state: $newState")
            _connectionStateFlow.value = newState
        }

        companion object{
            private const val TAG = "BluetoothViewModel"
        }
        private val _currentDevice = MutableStateFlow<BluetoothDev?>(null)
        val currentDevice: StateFlow<BluetoothDev?> = _currentDevice.asStateFlow()

        private val _characteristicValue = MutableStateFlow<ByteArray?>(null)
        val characteristicValue: StateFlow<ByteArray?> = _characteristicValue.asStateFlow()

        // Mapa z wartościami charakterystyk
        private val _characteristicValues = MutableStateFlow<Map<UUID, String>>(emptyMap())
        val characteristicValues: StateFlow<Map<UUID, String>> = _characteristicValues

        // Funkcja do aktualizacji wartości charakterystyk
        fun updateCharacteristicValues(values: Map<UUID, String>) {
            _characteristicValues.value = values
        }

        fun setCurrentDevice(device: BluetoothDev) {
            Log.e(
                TAG,
                "Changing current device type: $device"
            )
            _currentDevice.value = device
        }
        fun updateCharacteristicValue(value: ByteArray) {
            _characteristicValue.value = value
        }

        //region Database: device, meas and meas parameters save + devices & last meas display

        // Funkcja do zapisywania urządzenia i potem pomiaru
        fun saveDeviceAndMeasurement(userId: Long, values: Map<UUID, String>, deviceType: BluetoothDev, devName: String?, parsedData: Map<String, Any>,
                                     unknown: String, noInfo:String, thermometerName: String, weightScaleName: String, bpmName: String, temperatureName: String, pulseName: String, timeOfMeas: String) {

            viewModelScope.launch {
                // Przetwarzamy wartości charakterystyk na parametry obiektu Urzadzenie
                val nazwa = devName?: "unknown"
                val producent = values[BluetoothUUIDs.UUID_MANUFACTURER] ?: noInfo
                val model = values[BluetoothUUIDs.UUID_MODEL_NUMBER] ?: noInfo
                val rodzaj = deviceType.name

                //Log.e(TAG, "Saving a device: $nazwa, ${hexToString(producent)}, ${hexToString(model)}, $rodzaj")

                // Tworzymy obiekt Urzadzenie
                val urzadzenie = Urzadzenie(
                    uzytkownikId = userId,
                    nazwa = nazwa,
                    producent = hexToString(producent),
                    rodzaj = rodzaj,
                    model = hexToString(model)
                )

                // Zapisz urządzenie do bazy danych
                val deviceId = repository.addSensor(urzadzenie)

                // Zapisz dane, wykorzystując deviceId
                saveParsedData(deviceId, parsedData, deviceType.name, thermometerName, weightScaleName, bpmName, temperatureName, pulseName, timeOfMeas)
            }
        }

        // Funkcja do zapisania sparsowanych danych pomiaru
        @SuppressLint("SuspiciousIndentation")
        private fun saveParsedData(deviceId: Long, values: Map<String, Any>, deviceType: String, thermometerName: String, weightScaleName: String, bpmName: String, temperatureName: String, pulseName: String, timeOfMeas: String) {
            val timestamp = values[timeOfMeas] as? String
            if (timestamp != null) {
                val formatterInput = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")
                val timestampParsed = LocalDateTime.parse(timestamp, formatterInput)

                val isoDate = timestampParsed.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                viewModelScope.launch {
                    // Zapisz pomiar
                    val pomiar = Pomiar(urzadzenieId = deviceId, data = isoDate)
                    val measExists =
                        repository.checkIfMeasurementExists(pomiar.urzadzenieId, pomiar.data)
                    if (pomiar.data != "null" && !measExists) {
                        Log.e(
                            TAG,
                            "Saving a measurement to device: ${pomiar.urzadzenieId} with values: ${pomiar.data}"
                        )
                        val pomiarId = repository.addMeasurement(pomiar)

                        if (deviceType == thermometerName) {
                            val param = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = temperatureName,
                                wartosc = (values["TempResult"] as? Float) ?: 0.0f,
                                jednostka = values["Unit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${param.pomiarId} with values: ${param.nazwa}, ${param.wartosc}, ${param.jednostka}"
                            )

                            if (param.wartosc != 0.0f && param.wartosc < 45 && param.wartosc > 30) {
                                repository.addMeasurementParameter(param)
                            } else {
                                /*TODO usunąć pomiar?*/
                            }

                        }
                        if (deviceType == weightScaleName) {
                            val param = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = weightScaleName,
                                wartosc = (values["WeightResult"] as? Float) ?: 0.0f,
                                jednostka = values["Unit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${param.pomiarId} with values: ${param.nazwa}, ${param.wartosc}, ${param.jednostka}"
                            )

                            if (param.wartosc != 0.0f) {
                                repository.addMeasurementParameter(param)
                            } else {
                                /*TODO usunąć pomiar?*/
                            }

                        }
                        if (deviceType == bpmName) {
                            val paramSYS = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = "SYS",
                                wartosc = (values["SYSResult"] as? Float) ?: 0.0f,
                                jednostka = values["SYSUnit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${paramSYS.pomiarId} with values: ${paramSYS.nazwa}, ${paramSYS.wartosc}, ${paramSYS.jednostka}"
                            )

                            if (paramSYS.wartosc != 0.0f) {
                                repository.addMeasurementParameter(paramSYS)
                            } else {
                                /*TODO usunąć pomiar?*/
                            }

                            val paramDIA = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = "DIA",
                                wartosc = (values["DIAResult"] as? Float) ?: 0.0f,
                                jednostka = values["DIAUnit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${paramDIA.pomiarId} with values: ${paramDIA.nazwa}, ${paramDIA.wartosc}, ${paramDIA.jednostka}"
                            )

                            if (paramDIA.wartosc != 0.0f) {
                                repository.addMeasurementParameter(paramDIA)
                            } else {
                                /*TODO usunąć pomiar?*/
                            }

                            val paramMAP = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = "MAP",
                                wartosc = (values["MAPResult"] as? Float) ?: 0.0f,
                                jednostka = values["MAPUnit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${paramMAP.pomiarId} with values: ${paramMAP.nazwa}, ${paramMAP.wartosc}, ${paramMAP.jednostka}"
                            )

                            if (paramMAP.wartosc != 0.0f) {
                                repository.addMeasurementParameter(paramMAP)
                            } else {
                                /*TODO usunąć POMIAR?*/
                            }

                            val paramPulse = ParametrPomiaru(
                                pomiarId = pomiarId,
                                nazwa = pulseName,
                                wartosc = (values["PulseResult"] as? Float) ?: 0.0f,
                                jednostka = values["PulseUnit"].toString()
                            )
                            Log.e(
                                TAG,
                                "Saving a parameter to measurement: ${paramPulse.pomiarId} with values: ${paramPulse.nazwa}, ${paramPulse.wartosc}, ${paramPulse.jednostka}"
                            )

                            if (paramPulse.wartosc != 0.0f) {
                                repository.addMeasurementParameter(paramPulse)
                            } else {
                                /*TODO usunąć POMIAR?*/
                            }
                        }
                    }
                }
            }
        }

        fun clearDatabase() {
            viewModelScope.launch {
                repository.clearDatabase()
            }
        }

        private val _sensorsForUser = MutableStateFlow<List<Urzadzenie>>(emptyList())
        val sensorsForUser: StateFlow<List<Urzadzenie>> = _sensorsForUser

        fun loadSensorsForUser(userId: Long) {
            viewModelScope.launch {
                repository.getAllSensorsForUser(userId).collect { sensors ->
                    _sensorsForUser.value = sensors
                }
            }
        }

        private val _lastPomiarWithParameters = MutableStateFlow<PomiarZParametrami?>(null)
        val lastPomiarWithParameters: StateFlow<PomiarZParametrami?> = _lastPomiarWithParameters

        // Funkcja inicjująca zapytanie o ostatni pomiar
        fun loadLastPomiarWithParameters(urzadzenieId: Long) {
            viewModelScope.launch {
                val result = repository.getLastPomiarWithParametersByUrzadzenieId(urzadzenieId)
                _lastPomiarWithParameters.value = result
            }
        }

        private val _allPomiaryWithParameters = MutableStateFlow<List<PomiarZParametrami>>(emptyList())
        val allPomiaryWithParameters: StateFlow<List<PomiarZParametrami>> = _allPomiaryWithParameters

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        fun loadAllPomiaryWithParameters(urzadzenieId: Long) {
            viewModelScope.launch {
                _isLoading.value = true
                repository.getAllMeasWithParametersByDevId(urzadzenieId)
                    .collect { result ->
                        _allPomiaryWithParameters.value = result
                        _isLoading.value = false
                    }
            }
        }

        fun hexToString(hex: String): String {
            val output = StringBuilder("")

            // Usuń wszystkie niepoprawne znaki (nie należące do zakresu 0-9, a-f, A-F)
            val sanitizedHex = hex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

            // Przechodzimy przez hex parami (każde dwie cyfry reprezentują jeden znak)
            for (i in sanitizedHex.indices step 2) {
                // Upewniamy się, że mamy parzystą liczbę znaków
                if (i + 2 <= sanitizedHex.length) {
                    val str = sanitizedHex.substring(i, i + 2)
                    // Konwersja z hex do wartości liczbowej, a następnie na znak
                    val char = str.toInt(16).toChar()
                    output.append(char)
                }
            }

            return output.toString()
        }
    //endregion

    //region Login&Register + add new user
    private val _uiState = MutableStateFlow(HealthMateUiState())
    val uiState: StateFlow<HealthMateUiState> = _uiState.asStateFlow()

    var name by mutableStateOf("")
        private set

    var usernameRegister by mutableStateOf("")
        private set

    var passwordRegister by mutableStateOf("")
        private set

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun updateUserName(nameInput: String){
        name = nameInput
    }

    fun updateUserLogin(usernameInput: String){
        username = usernameInput
    }

    fun updateUserPassword(passwordInput: String){
        password = passwordInput
    }
    fun updateUserLoginRegister(usernameInput: String){
        usernameRegister = usernameInput
    }

    fun updateUserPasswordRegister(passwordInput: String){
        passwordRegister = passwordInput
    }

    fun togglePasswordVisibility() {
        val updatedUiState = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
        _uiState.value = updatedUiState
    }

    suspend fun isAuthenticationWrong(): Boolean {
        val user = repository.getUserByLogin(username)

        val updatedUiState = if (user != null && password == user.haslo) {
            _uiState.value.copy(areCredentialsWrong = false)
        } else {
            _uiState.value.copy(areCredentialsWrong = true)
        }
        _uiState.value = updatedUiState

        return updatedUiState.areCredentialsWrong
    }

    fun attemptLogin(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val areCredentialsWrong = isAuthenticationWrong()

            if (!areCredentialsWrong) {
                val user = repository.getUserByLogin(username)
                if (user != null) {
                    loggedUser(user) // Ustaw jako zalogowanego
                    onSuccess()
                }
            } else {
                username = ""
                password = ""
                onFailure()
            }
        }
    }

    suspend fun isLoginWrong(): Boolean{
        val user = repository.getUserByLogin(usernameRegister)

        val updatedUiState = if (user != null && usernameRegister.equals(user.login)) {
            _uiState.value.copy(loginAlreadyExists = true)
        } else {
            _uiState.value.copy(loginAlreadyExists = false)
        }
        _uiState.value = updatedUiState

        return updatedUiState.loginAlreadyExists
    }

    fun attemptRegistration(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val loginAlreadyExists = isLoginWrong()

            if (!loginAlreadyExists) {
                addNewUser()
                onSuccess()
            } else {
                usernameRegister = ""
                passwordRegister = ""
                onFailure()
            }
        }
    }

    suspend fun addNewUser() {
        //viewModelScope.launch {
            val user = Uzytkownik(
                imie = name,
                login = usernameRegister,
                haslo = passwordRegister
            )
            val userId = repository.addUser(user)
            if (userId != null) {
                val newUser = user.copy(uzytkownikId = userId)
                loggedUser(newUser)
            } else {
            }
       // }
    }

    fun loggedUser(loggedUser: Uzytkownik) {
        _uiState.update { currentState ->
            currentState.copy(
                user = loggedUser
            )
        }
        Log.e(TAG, "Updated a logged user: ${loggedUser.imie}")
    }

    fun selectDevice(selectedDevice: Urzadzenie) {
        _uiState.update { currentState ->
            currentState.copy(
                device = selectedDevice
            )
        }
    }

    fun resetLoginState() {
        _uiState.value = _uiState.value.copy(areCredentialsWrong = false)
        username = ""
        password = ""
    }

    fun resetRegisterState() {
        _uiState.value = _uiState.value.copy(loginAlreadyExists = false)
        usernameRegister = ""
        passwordRegister = ""
        name = ""
    }

    //endregion

    private val firestore = FirebaseFirestore.getInstance()

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    suspend fun backupData(loggedUser: Uzytkownik) {
        _backupState.value = BackupState.InProgress

        try {
            val totalSteps = 3 // Dostosuj do liczby etapów
            var completedSteps = 0

            // Krok 1: Sprawdź użytkownika
            _backupState.value = BackupState.Progress(++completedSteps, totalSteps)
            val userDocRef = firestore.collection("users").document(loggedUser.uzytkownikId.toString())
            val userSnapshot = userDocRef.get().await()

            if (!userSnapshot.exists()) {
                val newUser = mapOf(
                    "imie" to loggedUser.imie,
                    "login" to loggedUser.login,
                    "haslo" to loggedUser.haslo
                )
                userDocRef.set(newUser).await()
            }

            // Krok 2: Synchronizacja urządzeń
            _backupState.value = BackupState.Progress(++completedSteps, totalSteps)
            syncDevices(loggedUser.uzytkownikId)

            // Krok 3: Synchronizacja pomiarów
            _backupState.value = BackupState.Progress(++completedSteps, totalSteps)
            syncMeasurementsAndParameters(loggedUser.uzytkownikId)

            _backupState.value = BackupState.Success("Backup completed successfully!")
        } catch (e: Exception) {
            _backupState.value = BackupState.Error("Error during backup: ${e.message}")
        } finally {
            delay(2000)
            _backupState.value = BackupState.Idle
        }
    }

    private suspend fun syncDevices(userId: Long) = withContext(Dispatchers.IO)  {
        val devicesInFirestore = getDevicesFromFirestore(userId)
        val devicesInLocal = repository.getAllSensors().filter { it.uzytkownikId == userId }

        devicesInLocal.forEach { device ->
            if (devicesInFirestore.none { it["urzadzenieId"] == device.urzadzenieId.toString() }) {
                val deviceData = hashMapOf(
                    "urzadzenieId" to device.urzadzenieId,
                    "nazwa" to device.nazwa,
                    "rodzaj" to device.rodzaj,
                    "model" to device.model,
                    "producent" to device.producent,
                    "uzytkownikId" to userId
                )
                firestore.collection("devices").document(device.urzadzenieId.toString()).set(deviceData).await()
            }
        }

        Log.d("DatabaseSyncWorker", "Device synchronization completed")

        //syncMeasurementsAndParameters(userId)
    }

    private suspend fun getDevicesFromFirestore(userId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("devices")
                .whereEqualTo("uzytkownikId", userId)
                .get()
                .await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            Log.e("DatabaseSyncWorker", "Error fetching devices from Firestore: ${e.message}")
            emptyList()
        }
    }

    private suspend fun syncMeasurementsAndParameters(userId: Long) = withContext(Dispatchers.IO) {
        val devicesInLocal = repository.getAllSensors().filter { it.uzytkownikId == userId }

        devicesInLocal.forEach { device ->
            val lastFirestoreMeasurementTimestamp = getLastMeasurementTimestampFromFirestore(device.urzadzenieId)
            val newMeasurements = repository.getMeasurementsBySensor(device.urzadzenieId.toInt())
                .filter { it.data > lastFirestoreMeasurementTimestamp }

            newMeasurements.forEach { measurement ->
                val measurementData = hashMapOf(
                    "pomiarId" to measurement.pomiarId,
                    "urzadzenieId" to measurement.urzadzenieId,
                    "data" to measurement.data
                )
                firestore.collection("measurements").document(measurement.pomiarId.toString()).set(measurementData).await()

                val parameters = repository.getAllMeasurementParameters().filter { it.pomiarId == measurement.pomiarId }
                parameters.forEach { parameter ->
                    val parameterData = hashMapOf(
                        "parametrId" to parameter.parametrId,
                        "pomiarId" to parameter.pomiarId,
                        "nazwa" to parameter.nazwa,
                        "wartosc" to parameter.wartosc,
                        "jednostka" to parameter.jednostka
                    )
                    firestore.collection("parameters").document(parameter.parametrId.toString()).set(parameterData).await()
                }
            }
        }

        Log.d("DatabaseSyncWorker", "Measurements and parameters synchronization completed")
    }

    private suspend fun getLastMeasurementTimestampFromFirestore(deviceId: Long) : String {
        return try {
            val snapshot = firestore.collection("measurements")
                .whereEqualTo("urzadzenieId", deviceId)
                .orderBy("data", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().getString("data") ?: "1970-01-01T00:00:00"
            } else {
                "1970-01-01T00:00:00"
            }
        } catch (e: Exception) {
            Log.e("DatabaseSyncWorker", "Error fetching last measurement timestamp: ${e.message}")
            "1970-01-01T00:00:00"
        }
    }

    private val _restoreState = MutableStateFlow<RevertBackupState>(RevertBackupState.Idle)
    val restoreState: StateFlow<RevertBackupState> = _restoreState

    //region Restoring data from Firestore
    suspend fun restoreData(loggedUser: Uzytkownik) {
        _restoreState.value = RevertBackupState.InProgress
        Log.d("Backup restore", "Restoring data started")
        //_backupState.value = BackupState.Loading

        try {
            val totalSteps = 3 // Liczba etapów do śledzenia postępu
            var completedSteps = 0

            // Pobierz urządzenia użytkownika z Firestore
            _restoreState.value = RevertBackupState.Progress(completedSteps, totalSteps)
            val devicesInFirestore = getDevicesFromFirestore(loggedUser.uzytkownikId)

            devicesInFirestore.forEach { device ->
                val deviceIdLong: Long = (device["urzadzenieId"] as? Long) ?: -1L

                val deviceExistsLocally = withContext(Dispatchers.IO) {
                    repository.getAllSensors().any { it.urzadzenieId == deviceIdLong }
                }

                if (!deviceExistsLocally) {
                    val newDevice = Urzadzenie(
                        urzadzenieId = deviceIdLong,
                        nazwa = (device["nazwa"] as? String) ?: "",
                        rodzaj = (device["rodzaj"] as? String) ?: "",
                        model = (device["model"] as? String) ?: "",
                        producent = (device["producent"] as? String) ?: "",
                        uzytkownikId = loggedUser.uzytkownikId // Przypisujemy użytkownika
                    )

                    withContext(Dispatchers.IO) {
                        repository.addSensor(newDevice)
                    }

                    Log.d("Backup restore", "Device $deviceIdLong added to local database")

                    // Pobierz pomiary dla urządzenia
                    _restoreState.value = RevertBackupState.Progress(++completedSteps, totalSteps)
                    val measurementsFromFirestore = getMeasurementsFromFirestore(deviceIdLong)

                    // Zapisz pomiary lokalnie
                    measurementsFromFirestore.forEach { measurement ->
                        val measIdLong: Long = (measurement["pomiarId"] as? Long) ?: 0L
                        val pomiar = Pomiar(
                            pomiarId = measIdLong,
                            urzadzenieId = deviceIdLong,
                            data = (measurement["data"] as? String) ?: ""
                        )

                        withContext(Dispatchers.IO) {
                            repository.addMeasurement(pomiar) // Zapisz pomiar lokalnie
                        }

                        // Pobierz parametry pomiaru
                        _restoreState.value = RevertBackupState.Progress(++completedSteps, totalSteps)
                        val parametersFromFirestore = getParametersFromFirestore(measIdLong)

                        // Zapisz parametry pomiaru lokalnie
                        parametersFromFirestore.forEach { parameter ->
                            val paramIdLong: Long = (parameter["parameterId"] as? Long) ?: 0L
                            val measIdLongFromParam: Long = (parameter["pomiarId"] as? Long) ?: 0L
                            val wartoscDouble: Double = (parameter["wartosc"] as? Double) ?: 0.0
                            val wartoscFloat: Float = wartoscDouble.toFloat()
                            val parametrPomiaru = ParametrPomiaru(
                                parametrId = paramIdLong,
                                pomiarId = measIdLongFromParam,
                                nazwa = (parameter["nazwa"] as? String) ?: "",
                                wartosc = wartoscFloat,
                                jednostka = (parameter["jednostka"] as? String) ?: ""
                            )

                            withContext(Dispatchers.IO) {
                                repository.addMeasurementParameter(parametrPomiaru) // Zapisz parametr pomiaru lokalnie
                            }
                        }
                        Log.d("Backup restore", "All parameters for device $deviceIdLong added to local database")

                    }
                    Log.d("Backup restore", "All measurements for device $deviceIdLong added to local database")

                } else {

                // Dla każdego urządzenia sprawdź, jakie pomiary są nowsze w Firestore
                val lastLocalMeasurementTimestamp = withContext(Dispatchers.IO) {
                    getLastMeasurementTimestampFromLocal(deviceIdLong) // Tutaj wykonujemy operację w tle
                }

                // Pobierz nowe pomiary
                    _restoreState.value = RevertBackupState.Progress(++completedSteps, totalSteps)
                val newMeasurements = withContext(Dispatchers.IO) {
                    getNewMeasurementsFromFirestoreLast(
                        deviceIdLong,
                        lastLocalMeasurementTimestamp
                    )
                }

                // Jeśli są nowsze pomiary, zaktualizuj je w lokalnej bazie danych
                if (newMeasurements.isNotEmpty()) {
                    // Zapisz pomiary do lokalnej bazy danych
                    newMeasurements.forEach { measurement ->
                        val measIdLong: Long = (measurement["pomiarId"] as? Long) ?: 0L
                        val pomiar = Pomiar(
                            pomiarId = measIdLong,
                            urzadzenieId = deviceIdLong,
                            data = (measurement["data"] as? String) ?: ""
                        )

                        // Zapisz pojedynczy pomiar
                        withContext(Dispatchers.IO) {
                            repository.addMeasurement(pomiar) // Operacja na bazie danych w tle
                        }

                        // Pobierz parametry dla danego pomiaru z Firestore
                        _restoreState.value = RevertBackupState.Progress(++completedSteps, totalSteps)
                        val parameters = withContext(Dispatchers.IO) {
                            getMeasurementParametersFromFirestoreLast(measIdLong) // Operacja w tle
                        }

                        // Zapisz parametry pomiarów do lokalnej bazy danych
                        parameters.forEach { parameter ->
                            val paramIdLong: Long = (parameter["parameterId"] as? Long) ?: 0L
                            val measIdLongFromParam: Long = (parameter["pomiarId"] as? Long) ?: 0L
                            val wartoscDouble: Double = (parameter["wartosc"] as? Double) ?: 0.0
                            val wartoscFloat: Float = wartoscDouble.toFloat()
                            val parametrPomiaru = ParametrPomiaru(
                                parametrId = paramIdLong,
                                pomiarId = measIdLongFromParam,
                                nazwa = (parameter["nazwa"] as? String) ?: "",
                                wartosc = wartoscFloat,
                                jednostka = (parameter["jednostka"] as? String) ?: ""
                            )
                            withContext(Dispatchers.IO) {
                                repository.addMeasurementParameter(parametrPomiaru) // Operacja w tle
                            }
                        }
                        Log.d(
                            "Backup restore",
                            "Restoring parameters for measurement ${measurement["pomiarId"].toString()} completed"
                        )
                    }
                    Log.d(
                        "Backup restore",
                        "Restoring measurements for device $deviceIdLong completed"
                    )
                }

                Log.d("Backup restore", "Restoring devices completed")
            }
            }

            //_backupState.value = BackupState.Completed
            _restoreState.value = RevertBackupState.Success("Restore completed successfully")
        } catch (e: Exception) {
            _restoreState.value = RevertBackupState.Error("Error during restore: ${e.message}")
            Log.e("Backup restore", "Error occured: ${e.message}")
        } finally {
            delay(2000)
            _restoreState.value = RevertBackupState.Idle
        }
        Log.d("Backup restore", "Restoring data completed")
    }

    // Pobierz ostatnią datę pomiaru z lokalnej bazy danych
    private fun getLastMeasurementTimestampFromLocal(deviceId: Long): String {
        // Zwróć datę ostatniego pomiaru dla urządzenia z lokalnej bazy danych
        return repository.getLastMeasurementTimestamp(deviceId)
    }

    // Pobierz nowe pomiary z Firestore, które są nowsze niż ostatnia data w lokalnej bazie
    private suspend fun getNewMeasurementsFromFirestoreLast(deviceId: Long, lastTimestamp: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("measurements")
                .whereEqualTo("urzadzenieId", deviceId)
                .whereGreaterThan("data", lastTimestamp)
                .get()
                .await()

            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    //Pobierz parametry pomiarów dla danego pomiaru z Firestore
    private suspend fun getMeasurementParametersFromFirestoreLast(measurementId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("parameters")
                .whereEqualTo("pomiarId", measurementId)
                .get()
                .await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getMeasurementsFromFirestore(deviceId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("measurements")
                .whereEqualTo("urzadzenieId", deviceId)
                .get()
                .await()

            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getParametersFromFirestore(measurementId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("parameters")
                .whereEqualTo("pomiarId", measurementId)
                .get()
                .await()

            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    //endregion


    sealed class BackupState {
        object Idle : BackupState()
        object InProgress : BackupState()
        data class Success(val message: String) : BackupState()
        data class Error(val error: String) : BackupState()
        data class Progress(val completedSteps: Int, val totalSteps: Int) : BackupState()
    }

    sealed class RevertBackupState {
        object Idle : RevertBackupState()
        object InProgress : RevertBackupState()
        data class Success(val message: String) : RevertBackupState()
        data class Error(val error: String) : RevertBackupState()
        data class Progress(val completedSteps: Int, val totalSteps: Int) : RevertBackupState()
    }

}