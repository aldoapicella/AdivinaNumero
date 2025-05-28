package com.utp.adivinanumero

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------- ViewModel para aislar la lógica ----------
class GameViewModel : ViewModel() {

    private var _target: Int = Random.nextInt(0, 101)
    private var _attemptsLeft by mutableIntStateOf(3)
    private var _message by mutableStateOf("¡Adivina un número del 0 al 100!")
    private var _isGameOver by mutableStateOf(false)
    private val _timeLeft = mutableIntStateOf(60)          // segundos

    // Exposed states
    val message get() = _message
    val isGameOver get() = _isGameOver
    val timeLeft: State<Int> get() = _timeLeft

    private val timer = object : CountDownTimer(60_000, 1_000) {
        override fun onTick(ms: Long) { _timeLeft.intValue = (ms / 1_000).toInt() }
        override fun onFinish() { endGame(false, "⏰ Tiempo agotado. El número era $_target") }
    }

    init { timer.start() }

    fun makeGuess(guess: Int) {
        if (_isGameOver) return

        when {
            guess == _target -> {
                endGame(true, "🎉 ¡Correcto! Era $_target")
            }
            --_attemptsLeft == 0 -> {
                val hint = if (guess < _target) "mayor" else "menor" // última pista
                endGame(false, "Sin intentos. Era $_target (tu último tiro fue $hint)")
            }
            guess < _target -> _message = "🚀 Es **MAYOR**. Intentos restantes: $_attemptsLeft"
            else             -> _message = "⬇️ Es **MENOR**. Intentos restantes: $_attemptsLeft"
        }
    }

    fun reset() {
        _target = Random.nextInt(0, 101)
        _attemptsLeft = 3
        _isGameOver = false
        _message = "¡Adivina un número del 0 al 100!"
        _timeLeft.intValue = 60
        timer.cancel(); timer.start()
    }

    private fun endGame(win: Boolean, finalMsg: String) {
        _isGameOver = true
        _message = if (win) finalMsg else "🙁 $finalMsg"
        timer.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }
}

// ---------- Activity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdivinaNumeroApp() }
    }
}

// ---------- Composable raíz ----------
@Composable
fun AdivinaNumeroApp(vm: GameViewModel = viewModel()) {
    var input by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Título y time-left
            Text("Juego de Adivinar", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tiempo restante: ${vm.timeLeft.value}s",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(32.dp))

            // Mensaje / pista
            Text(vm.message)

            Spacer(Modifier.height(24.dp))

            // Campo numérico
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 3) input = it },
                enabled = !vm.isGameOver,
                label = { Text("Tu intento") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Botón intentar
            Button(
                onClick = {
                    val guess = input.toIntOrNull()
                    if (guess == null || guess !in 0..100) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Ingresa un número válido 0-100")
                        }
                    } else {
                        vm.makeGuess(guess)
                        input = ""
                    }
                },
                enabled = !vm.isGameOver
            ) { Text("¡PROBAR!") }

            Spacer(Modifier.height(24.dp))

            // Botón reiniciar
            if (vm.isGameOver) {
                OutlinedButton(onClick = { vm.reset(); input = "" }) {
                    Text("REINICIAR")
                }
            }

            // SnackbarHost para mostrar mensajes
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
