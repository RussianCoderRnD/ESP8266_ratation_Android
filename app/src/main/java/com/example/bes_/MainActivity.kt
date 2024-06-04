package com.example.bes_

// Импортируем необходимые классы
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Константа для задержки в миллисекундах
const val delays = 100

// Данные сенсора (примеры полей, замените на реальные)
data class SensorData(
    val temp: String,
    val angleZ: String,
    val ind_ves: String
)

// Основной класс активности
class MainActivity : AppCompatActivity() {
    // Объявляем необходимые переменные
    private lateinit var mitTempSensor1: TextView
    private lateinit var mitIndDatchik: TextView
    private lateinit var mitIndVes: TextView
    private lateinit var imageButtonReturn: ImageButton
    private lateinit var statusBatt: ImageView
    private lateinit var imageView: ImageView

    private val gson = Gson() // Создаем экземпляр Gson для парсинга JSON

    // URL для обращения к ESP8266
    private val url = "http://192.168.14.51/out"

    // Переменная для угла поворота
    private var corner: Float = 0f

    // Handler для выполнения задач с задержкой
    private val handler = Handler(Looper.getMainLooper())

    // Runnable для периодического выполнения fetchDataFromESP8266
    private val fetchDataRunnable = object : Runnable {
        override fun run() {
            fetchDataFromESP8266()
            // Перезапускаем runnable через 1 секунду
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Устанавливаем макет активности
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // Устанавливаем ориентацию на ландшафтную

        // Проверяем, нужно ли закрыть приложение
        if (intent.getBooleanExtra("EXIT", false)) {
            finish()
        }

        enableEdgeToEdge() // Включаем режим полноэкранного отображения

        // Инициализируем переменные
        imageView = findViewById(R.id.imageView)
        mitTempSensor1 = findViewById(R.id.mit_tempSensor1)
        mitIndDatchik = findViewById(R.id.mit_ind_datchik)
        mitIndVes = findViewById(R.id.mit_ind_ves)

        fetchDataFromESP8266() // Получаем данные с ESP8266

        // Запускаем периодическое выполнение fetchDataFromESP8266
        handler.postDelayed(fetchDataRunnable, 1000)
    }

    // Функция для получения данных с ESP8266
    private fun fetchDataFromESP8266() {
        Thread {
            try {
                val client = OkHttpClient.Builder() // Настройка клиента OkHttp
                    .connectTimeout(30, TimeUnit.SECONDS) // Установка таймаута соединения
                    .readTimeout(30, TimeUnit.SECONDS) // Установка таймаута чтения
                    .writeTimeout(30, TimeUnit.SECONDS) // Установка таймаута записи
                    .build()

                // Создаем запрос к серверу
                val request = Request.Builder().url(url).build()

                // Выполняем запрос
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let {
                            // Парсим JSON данные
                            val sensorData = gson.fromJson(it, SensorData::class.java)
                            // Обновляем UI на главном потоке
                            runOnUiThread {
                                updateUI(sensorData)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Если произошла ошибка, показываем сообщение об ошибке
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка связи с устройством: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                Log.e("MainActivity", "Ошибка при отправке или получении данных: ${e.message}", e)
            }
        }.start()
    }

    // Функция для отображения данных с датчиков на экране
    private fun updateUI(sensorData: SensorData) {
        mitTempSensor1.text = sensorData.temp
        mitIndDatchik.text = sensorData.angleZ
        mitIndVes.text = sensorData.ind_ves
        corner = sensorData.angleZ.toFloat()
        imageView.rotation = corner // Применяем угол поворота к изображению
    }
}
