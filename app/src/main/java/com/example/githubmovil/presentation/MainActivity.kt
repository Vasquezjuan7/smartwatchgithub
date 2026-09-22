package com.example.githubmovil.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.githubmovil.presentation.theme.GithubMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GithubMovilTheme {
                DeploymentManagerApp()
            }
        }
    }
}

// Estados del flujo. Agregamos CHECKING y IDLE para buscar en GitHub
enum class PrState { CHECKING_GITHUB, IDLE, AI_SUMMARY, PENDING, APPROVING, REJECTING, SUCCESS, ERROR }

@Composable
fun DeploymentManagerApp() {
    // Iniciamos buscando en GitHub
    var currentState by remember { mutableStateOf(PrState.CHECKING_GITHUB) }
    // Guardaremos el título y rama del PR si encontramos uno
    var prTitle by remember { mutableStateOf("") }
    var prNumber by remember { mutableIntStateOf(0) }
    var prBranchInfo by remember { mutableStateOf("") }
    
    DeploymentManagerScreen(
        currentState = currentState,
        prTitle = prTitle,
        prNumber = prNumber,
        prBranchInfo = prBranchInfo,
        onStateChange = { newState -> currentState = newState },
        onPrFound = { title, number, branchInfo -> 
            prTitle = title
            prNumber = number
            prBranchInfo = branchInfo
        }
    )
}

@Composable
fun DeploymentManagerScreen(
    currentState: PrState,
    prTitle: String,
    prNumber: Int,
    prBranchInfo: String,
    onStateChange: (PrState) -> Unit,
    onPrFound: (String, Int, String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // 🔥 CAMBIA ESTO: Pon tu usuario y repositorio de GitHub real
    val githubOwner = "Vasquezjuan7" 
    // Asegúrate de que este sea el nombre exacto de tu repositorio en GitHub
    val githubRepo = "smartwatchgithub" 
    
    val backendUrl = "http://10.0.2.2:3000/api/decision" 

    // 1. Función para conectarse a GitHub y buscar PRs abiertos
    fun checkPendingPullRequests() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Añadimos sort=updated&direction=desc para que SIEMPRE traiga el PR más reciente, de cualquier rama
                val url = URL("https://api.github.com/repos/$githubOwner/$githubRepo/pulls?state=open&sort=updated&direction=desc")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    
                    if (jsonArray.length() > 0) {
                        // Tomamos el PR más reciente
                        val firstPr = jsonArray.getJSONObject(0)
                        val title = firstPr.getString("title")
                        val number = firstPr.getInt("number")
                        
                        // Extraemos los nombres de las ramas
                        val headBranch = firstPr.getJSONObject("head").getString("ref")
                        val baseBranch = firstPr.getJSONObject("base").getString("ref")
                        val branchInfo = "$headBranch ➔ $baseBranch"
                        
                        onPrFound(title, number, branchInfo)
                        onStateChange(PrState.AI_SUMMARY) // Avanzamos a la pantalla del resumen
                    } else {
                        // No hay PRs, todo está en orden
                        onStateChange(PrState.IDLE)
                    }
                } else {
                    println("Error en GitHub API: ${connection.responseCode} - ${connection.responseMessage}")
                    onStateChange(PrState.ERROR)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateChange(PrState.ERROR)
            }
        }
    }

    // Al iniciar la pantalla en estado CHECKING, llama a la función
    LaunchedEffect(currentState) {
        if (currentState == PrState.CHECKING_GITHUB) {
            checkPendingPullRequests()
        }
    }

    fun sendDecisionToServer(isApproved: Boolean, prNumber: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Pasamos los datos directamente en la URL (Query String) como rescate absoluto para el emulador
                val action = if (isApproved) "merge" else "close"
                val url = URL("$backendUrl?pr_number=$prNumber&action=$action")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // También lo mandamos por JSON para intentar que pase
                val jsonObject = JSONObject()
                jsonObject.put("pr_number", prNumber)
                jsonObject.put("action", action)

                connection.outputStream.use { os ->
                    val input = jsonObject.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                    os.flush() 
                }

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                println("Respuesta del servidor: $responseCode - $responseMessage")

                if (responseCode in 200..299) {
                    onStateChange(PrState.SUCCESS)
                } else {
                    onStateChange(PrState.ERROR)
                }
            } catch (e: Exception) {
                println("Error de conexión HTTP: ${e.message}")
                e.printStackTrace()
                onStateChange(PrState.ERROR)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Fondo negro puro para pantallas OLED
        contentAlignment = Alignment.Center
    ) {
        when (currentState) {
            PrState.CHECKING_GITHUB -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = Color.White,
                            trackColor = Color.DarkGray
                        ),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Buscando Pull\nRequests...",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
            
            PrState.IDLE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Todo al día",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Todo al día.\nNo hay PRs pendientes.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onStateChange(PrState.CHECKING_GITHUB) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Actualizar", fontSize = 12.sp)
                    }
                }
            }

            PrState.AI_SUMMARY -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Icon(
                        imageVector = Icons.Default.Build, 
                        contentDescription = "IA",
                        tint = Color(0xFF00E5FF), // Cyan/Neón para la IA
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Análisis IA",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"Se ajustó el padding del botón de pago, se optimizó la carga de imágenes del catálogo y se resolvió el desbordamiento en la vista móvil. Riesgo de conflicto: Bajo.\"",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStateChange(PrState.PENDING)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continuar a decisión"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            PrState.PENDING -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¿Aprobar Despliegue?",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = prTitle.ifEmpty { "Ajuste UI Carrito VisioStock" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Mostramos la rama de origen y destino
                    Text(
                        text = prBranchInfo.ifEmpty { "feature ➔ main" },
                        color = Color(0xFF00E5FF), // Cyan para resaltar la rama
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Rechazar
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStateChange(PrState.REJECTING)
                                sendDecisionToServer(isApproved = false, prNumber = prNumber)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF073A), // Rojo Neón
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rechazar"
                            )
                        }

                        // Botón Aprobar
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStateChange(PrState.APPROVING)
                                sendDecisionToServer(isApproved = true, prNumber = prNumber)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF39FF14), // Verde Neón
                                contentColor = Color.Black // Contraste oscuro
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Aprobar"
                            )
                        }
                    }
                }
            }

            PrState.APPROVING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = Color(0xFF39FF14), // Verde Neón
                            trackColor = Color.DarkGray
                        ),
                        strokeWidth = 4.dp // Un poco más grueso para que resalte
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Desplegando en\nproducción...",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            PrState.REJECTING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = Color(0xFFFF073A), // Rojo Neón
                            trackColor = Color.DarkGray
                        ),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cancelando\ndespliegue...",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            PrState.SUCCESS -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Éxito",
                        tint = Color(0xFF39FF14),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¡Operación\nExitosa!",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    
                    // Volver al estado IDLE buscando de nuevo
                    LaunchedEffect(Unit) {
                        delay(3000)
                        onStateChange(PrState.CHECKING_GITHUB)
                    }
                }
            }

            PrState.ERROR -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color(0xFFFF073A),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Error de red.\nRevisa el servidor",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                    
                    // Volver atrás después de 3 segundos
                    LaunchedEffect(Unit) {
                        delay(3000)
                        onStateChange(PrState.CHECKING_GITHUB)
                    }
                }
            }
        }
    }
}

// Previsualizaciones
@WearPreviewDevices
@Composable
fun Preview_Idle() {
    GithubMovilTheme { DeploymentManagerScreen(PrState.IDLE, "", 0, "", {}, {_,_,_->}) }
}

@WearPreviewDevices
@Composable
fun Preview_AiSummary() {
    GithubMovilTheme { DeploymentManagerScreen(PrState.AI_SUMMARY, "Ajuste UI Carrito", 1, "feature/carrito ➔ main", {}, {_,_,_->}) }
}