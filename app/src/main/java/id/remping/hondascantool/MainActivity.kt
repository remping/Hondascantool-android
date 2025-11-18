package id.remping.hondascantool

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var connectBtn: Button
    private lateinit var rpmText: TextView
    private lateinit var adapter: DataAdapter
    private lateinit var btService: BluetoothService
    private val deviceName = "HondaScantool" // default ESP32 name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status)
        connectBtn = findViewById(R.id.btn_connect)
        rpmText = findViewById(R.id.rpm)
        val rv: RecyclerView = findViewById(R.id.recycler)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DataAdapter(emptyList())
        rv.adapter = adapter

        btService = BluetoothService(
            onLine = { line -> runOnUiThread { onNewLine(line) } },
            onError = { err -> runOnUiThread { statusText.text = err } }
        )

        checkPermissions()

        connectBtn.setOnClickListener {
            if (!btService.isAvailable()) {
                Toast.makeText(this, "Bluetooth tidak tersedia/ mati", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            statusText.text = "Mencoba connect..."
            btService.connectTo(deviceName)
        }
    }

    private fun onNewLine(line: String) {
        if (line == "CONNECTED") {
            statusText.text = "Terhubung ke $deviceName"
            return
        }
        statusText.text = "Receiving..."
        try {
            val map = mutableMapOf<String,String>()
            // If Format1 (KEY=VALUE;) present parse that, otherwise parse space separated
            if (line.contains('=')) {
                val parts = line.trim().split(';').map { it.trim() }.filter { it.isNotEmpty() }
                for (p in parts) {
                    val kv = p.split('=', limit=2)
                    if (kv.size == 2) map[kv[0].trim().uppercase()] = kv[1].trim()
                }
            } else {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 13) {
                    map["RPM"] = parts[0]
                    map["TPS1"] = parts[1]
                    map["TPS2"] = parts[2]
                    map["ECT"] = parts[3]
                    map["ECT2"] = parts[4]
                    map["IAT1"] = parts[5]
                    map["IAT2"] = parts[6]
                    map["MAP1"] = parts[7]
                    map["MAP2"] = parts[8]
                    map["BAT"] = parts[9]
                    map["SPD"] = parts[10]
                    map["INJ"] = parts[11]
                    map["IGT"] = parts[12]
                }
            }

            val d = EngineData(
                rpm = map["RPM"]?.toIntOrNull() ?: 0,
                tps1 = map["TPS1"]?.toFloatOrNull() ?: 0f,
                tps2 = map["TPS2"]?.toFloatOrNull() ?: 0f,
                ect1 = map["ECT"]?.toFloatOrNull() ?: map["ECT1"]?.toFloatOrNull() ?: 0f,
                ect2 = map["ECT2"]?.toIntOrNull() ?: 0,
                iat1 = map["IAT1"]?.toFloatOrNull() ?: 0f,
                iat2 = map["IAT2"]?.toIntOrNull() ?: 0,
                map1 = map["MAP1"]?.toFloatOrNull() ?: 0f,
                map2 = map["MAP2"]?.toIntOrNull() ?: 0,
                battery = map["BAT"]?.toFloatOrNull() ?: 0f,
                speed = map["SPD"]?.toIntOrNull() ?: 0,
                inj = map["INJ"]?.toFloatOrNull() ?: 0f,
                igt = map["IGT"]?.toFloatOrNull() ?: 0f
            )
            updateUI(d)
        } catch (e: Exception) {
            statusText.text = "Parse error: ${e.message}"
        }
    }

    private fun updateUI(d: EngineData) {
        rpmText.text = d.rpm.toString()
        val list = listOf(
            "RPM" to d.rpm.toString(),
            "TPS1 (mV)" to String.format("%.0f", d.tps1),
            "TPS2" to String.format("%.1f", d.tps2),
            "ECT (mV)" to String.format("%.0f", d.ect1),
            "ECT2 (°C)" to d.ect2.toString(),
            "IAT1 (mV)" to String.format("%.0f", d.iat1),
            "IAT2 (°C)" to d.iat2.toString(),
            "MAP1 (mV)" to String.format("%.0f", d.map1),
            "MAP2" to d.map2.toString(),
            "Battery (V)" to String.format("%.1f", d.battery),
            "Speed (km/h)" to d.speed.toString(),
            "INJ (µs)" to String.format("%.0f", d.inj),
            "IGT (°)" to String.format("%.1f", d.igt)
        )
        adapter.update(list)
    }

    private fun checkPermissions() {
        val perms = mutableListOf<String>()
        perms.add(Manifest.permission.BLUETOOTH)
        perms.add(Manifest.permission.BLUETOOTH_ADMIN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val toRequest = perms.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(toRequest.toTypedArray())
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // no action required
    }

    override fun onDestroy() {
        super.onDestroy()
        btService.close()
    }
}
