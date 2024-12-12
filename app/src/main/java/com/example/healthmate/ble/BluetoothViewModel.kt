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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class BluetoothViewModel(
bluetoothHandler: BluetoothHandler,
private val repository: HealthMateRepository
) : ViewModel() {

    private val _connectState = MutableStateFlow<BluetoothHandler.ConnectionState>(BluetoothHandler.ConnectionState.DISCONNECTED)
    val connectState: StateFlow<BluetoothHandler.ConnectionState> = _connectState

//    fun onConnectionStateChanged(newState: BluetoothHandler.ConnectionState) {
//        // Zmieniamy stan połączenia w ViewModelu
//        _connectState.value = newState
//    }

    // Flow przechowujący stany połączenia BLE
    private val _connectionStateFlow = MutableSharedFlow<BluetoothHandler.ConnectionState>()
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()

    // Wywoływane przy każdej zmianie stanu połączenia
    fun onConnectionStateChanged(newState: BluetoothHandler.ConnectionState) {
        Log.e("BluetoothViewModel", "Received new state: $newState")
        viewModelScope.launch {
            _connectionStateFlow.emit(newState)
        }
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
            val nazwa = devName ?: unknown
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
        val timestamp = values[timeOfMeas].toString()

            viewModelScope.launch {
                // Zapisz pomiar
                val pomiar = Pomiar(urzadzenieId = deviceId, data = timestamp)
                val measExists = repository.checkIfMeasurementExists(pomiar.urzadzenieId, pomiar.data)
                if(pomiar.data != "null" && !measExists) {
                    Log.e(
                        TAG,
                        "Saving a measurement to device: ${pomiar.urzadzenieId} with values: ${pomiar.data}"
                    )
                    val pomiarId = repository.addMeasurement(pomiar)

                    if(deviceType == thermometerName){
                        val param = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = temperatureName,
                            wartosc = (values["TempResult"] as? Float) ?: 0.0f,
                            jednostka = values["Unit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${param.pomiarId} with values: ${param.nazwa}, ${param.wartosc}, ${param.jednostka}")

                        if(param.wartosc != 0.0f){
                            repository.addMeasurementParameter(param)
                        }else{
                            /*TODO usunąć pomiar?*/
                        }

                    }else if(deviceType == weightScaleName){
                        val param = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = weightScaleName,
                            wartosc = (values["WeightResult"] as? Float) ?: 0.0f,
                            jednostka = values["Unit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${param.pomiarId} with values: ${param.nazwa}, ${param.wartosc}, ${param.jednostka}")

                        if(param.wartosc != 0.0f){
                            repository.addMeasurementParameter(param)
                        }else{
                            /*TODO usunąć pomiar?*/
                        }

                    }else{
                        val paramSYS = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = "SYS",
                            wartosc = (values["SYSResult"] as? Float) ?: 0.0f,
                            jednostka = values["SYSUnit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${paramSYS.pomiarId} with values: ${paramSYS.nazwa}, ${paramSYS.wartosc}, ${paramSYS.jednostka}")

                        if(paramSYS.wartosc != 0.0f){
                            repository.addMeasurementParameter(paramSYS)
                        }else{
                            /*TODO usunąć pomiar?*/
                        }

                        val paramDIA = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = "DIA",
                            wartosc = (values["DIAResult"] as? Float) ?: 0.0f,
                            jednostka = values["DIAUnit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${paramDIA.pomiarId} with values: ${paramDIA.nazwa}, ${paramDIA.wartosc}, ${paramDIA.jednostka}")

                        if(paramDIA.wartosc != 0.0f){
                            repository.addMeasurementParameter(paramDIA)
                        }else{
                            /*TODO usunąć pomiar?*/
                        }

                        val paramMAP = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = "MAP",
                            wartosc = (values["MAPResult"] as? Float) ?: 0.0f,
                            jednostka = values["MAPUnit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${paramMAP.pomiarId} with values: ${paramMAP.nazwa}, ${paramMAP.wartosc}, ${paramMAP.jednostka}")

                        if(paramMAP.wartosc != 0.0f){
                            repository.addMeasurementParameter(paramMAP)
                        }else{
                            /*TODO usunąć POMIAR?*/
                        }

                        val paramPulse = ParametrPomiaru(
                            pomiarId = pomiarId,
                            nazwa = pulseName,
                            wartosc = (values["PulseResult"] as? Float) ?: 0.0f,
                            jednostka = values["PulseUnit"].toString()
                        )
                        Log.e(TAG, "Saving a parameter to measurement: ${paramPulse.pomiarId} with values: ${paramPulse.nazwa}, ${paramPulse.wartosc}, ${paramPulse.jednostka}")

                        if(paramPulse.wartosc != 0.0f){
                            repository.addMeasurementParameter(paramPulse)
                        }else{
                            /*TODO usunąć POMIAR?*/
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
}