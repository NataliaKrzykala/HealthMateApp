package com.example.healthmate.ble

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothViewModel(
bluetoothHandler: BluetoothHandler,
private val repository: HealthMateRepository
) : ViewModel() {

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

    //region Database

    // Funkcja do zapisywania urządzenia i potem pomiaru
    fun saveDeviceAndMeasurement(values: Map<UUID, String>, deviceType: BluetoothDev, devName: String?, parsedData: Map<String, Any>,
                                 unknown: String, noInfo:String, thermometerName: String, weightScaleName: String, bpmName: String, temperatureName: String, pulseName: String, timeOfMeas: String) {

        viewModelScope.launch {
            // Przetwarzamy wartości charakterystyk na parametry obiektu Urzadzenie
            val nazwa = devName ?: unknown
            val producent = values[BluetoothUUIDs.UUID_MANUFACTURER] ?: noInfo
            val model = values[BluetoothUUIDs.UUID_MODEL_NUMBER] ?: noInfo
            val rodzaj = deviceType.name

            Log.e(TAG, "Saving a device: $nazwa, ${hexToString(producent)}, ${hexToString(model)}, $rodzaj")

            // Tworzymy obiekt Urzadzenie
            val urzadzenie = Urzadzenie(
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
                if(pomiar.data != "null") {
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


    private val _allSensors = MutableStateFlow<List<Urzadzenie>>(emptyList()) // Pusty stan początkowy
    val allSensors: StateFlow<List<Urzadzenie>> = _allSensors

    init {
        // Zbieranie danych z repozytorium i aktualizacja _allSensors
        viewModelScope.launch {
            repository.getAllSensors().collect { sensors ->
                _allSensors.value = sensors
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
    //endregion

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


    /////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
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

    suspend fun isAuthenticationWrong(): Boolean {
        val user = repository.getUserByLogin(username)
        val updatedUiState = if (user != null && password == user.haslo) {
            _uiState.value.copy(areCredentialsWrong = false)
        } else {
            _uiState.value.copy(areCredentialsWrong = true)
        }
        _uiState.value = updatedUiState
        Log.e(TAG, "ARE CREDENTIALS WRONG = ${uiState.value.areCredentialsWrong}")
        return updatedUiState.areCredentialsWrong
    }

//    fun isAuthenticationWrong() {
//        viewModelScope.launch {
//            val user0 = Uzytkownik(
//                imie = "admin",
//                login = "0",
//                haslo = "0"
//            )
//            //val user0Id = repository.addUser(user0)
//            //Log.e(TAG, "Added a default user")
//
//            val user = repository.getUserByLogin(username)
//
//            val updatedUiState = if (user != null && password.equals(user.haslo)) {
//                _uiState.value.copy(areCredentialsWrong = false)
//
//                /*
//                .also {
//                    loggedUser(user)
//                }
//                 */
//                //loggedUser(user)
//            } else {
//                _uiState.value.copy(areCredentialsWrong = true)
//            }
//            _uiState.value = updatedUiState
//            Log.e(TAG, "ARE CREDENTIALS WRONG = ${uiState.value.areCredentialsWrong}")
//        }
//    }

    fun isLoginWrong(){
        viewModelScope.launch {
            val user = repository.getUserByLogin(usernameRegister)

            val updatedUiState = if (user != null && usernameRegister.equals(user.login)) {
                /*_uiState.update { currentState ->
                    currentState.copy(loginAlreadyExists = true)
                }*/
                //HealthMateUiState(loginAlreadyExists = true)
                _uiState.value.copy(loginAlreadyExists = true)
            } else {
                /*_uiState.update { currentState ->
                    currentState.copy(loginAlreadyExists = false)
                }*/
                //HealthMateUiState(loginAlreadyExists = false)
                _uiState.value.copy(loginAlreadyExists = false)
            }
            _uiState.value = updatedUiState
        }
    }

//    fun attemptLogin(onSuccess: () -> Unit, onFailure: () -> Unit) {
//        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(areCredentialsWrong = false)
//            isAuthenticationWrong()
//            Log.e(TAG, "ARE CREDS WRONG IN attemptLogin: ${uiState.value.areCredentialsWrong}")
//
//            val isWrong = uiState.value.areCredentialsWrong
//            Log.e(TAG, "ARE CREDS WRONG IN attemptLogin2: ${uiState.value.areCredentialsWrong}")
//            if (!isWrong) {
//                authenticateAndSetUser(
//                    username = username,
//                    onSuccess = {
//                        // Po sukcesie resetuj pola logowania
//                        username = ""
//                        password = ""
//                        onSuccess()
//                    },
//                    onFailure = onFailure
//                )
//            } else {
//                // Resetujemy tylko dane hasła
//                password = ""
//                onFailure()
//            }
//        }
//    }

    fun attemptLogin(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(areCredentialsWrong = false)

            val areCredentialsWrong = isAuthenticationWrong() // Teraz jest suspend i zwraca wynik.
            Log.e(TAG, "ARE CREDS WRONG IN attemptLogin: $areCredentialsWrong")

            if (!areCredentialsWrong) {
                authenticateAndSetUser(
                    username = username,
                    onSuccess = {
                        // Po sukcesie resetuj pola logowania
                        username = ""
                        password = ""
                        onSuccess()
                    },
                    onFailure = onFailure
                )
            } else {
                // Resetujemy tylko dane hasła
                password = ""
                onFailure()
            }
        }
    }

    fun resetLoginState() {
        _uiState.value = _uiState.value.copy(areCredentialsWrong = false)
        username = ""
        password = ""
    }



    fun authenticateAndSetUser(username: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByLogin(username)
            if (user != null) {
                loggedUser(user) // Ustaw jako zalogowanego
                onSuccess()      // Wywołaj sukces
            } else {
                onFailure()      // Wywołaj błąd
            }
        }
    }


    fun addNewUser() {
        viewModelScope.launch {
            val user = Uzytkownik(
                imie = name,
                login = usernameRegister,
                haslo = passwordRegister
            )
            val userId = repository.addUser(user)
            //loggedUser(user)
        }
    }

    fun togglePasswordVisibility() {
        val updatedUiState = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
        _uiState.value = updatedUiState
    }

    fun selectDevice(selectedDevice: Urzadzenie) {
        _uiState.update { currentState ->
            currentState.copy(
                device = selectedDevice
            )
        }
    }

    fun loggedUser(loggedUser: Uzytkownik) {
        _uiState.update { currentState ->
            currentState.copy(
                user = loggedUser
            )
        }
        Log.e(TAG, "Updated a logged user: ${loggedUser.imie}")
    }
}