package axxes.com.prototype.bluetoothtestble

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import axxes.com.prototype.bluetoothtestble.model.v2.Command
import axxes.com.prototype.bluetoothtestble.model.v2.Parameter
import axxes.com.prototype.bluetoothtestble.parameters.CommandDsrc2
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null

    private lateinit var set_pairedDevices: MutableList<BluetoothDevice>
    private lateinit var hashMap_pairedDevices: HashMap<String,BluetoothDevice>
    private lateinit var adapter_paired_devices: ListAdapter_BLE_Devices

    private lateinit var myCommand: Command

    var deviceDetected = false
    var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        if(!bluetoothAdapter?.isEnabled!!){
            val intentEnabledBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intentEnabledBluetooth, PERMISSION_BLUETOOTH_REQUEST_CODE)
        }

        if (!locationPermissionApproved()) {
            if(Build.VERSION.SDK_INT > 22){
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        }
        txt_log.movementMethod = ScrollingMovementMethod()
        button_start.setOnClickListener {
            txt_log.text = ""
            startScan()
        }
        initializeLayout()
    }

    private fun initializeLayout() {
        set_pairedDevices = mutableListOf()
        hashMap_pairedDevices = HashMap()

        adapter_paired_devices = ListAdapter_BLE_Devices(applicationContext,R.layout.ble_devices_listitems,set_pairedDevices)
        lv_paired_devices.adapter = adapter_paired_devices
        lv_paired_devices.onItemClickListener = this
    }

    override fun onStop() {
        super.onStop()
        bluetoothGatt?.close()
    }

    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun bluetoothPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        )
    }

    private fun bluetoothAdminPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST_CODE) return
        var idxPermFine = -1
        var i = 0
        while (i < permissions.size && idxPermFine == -1) {
            if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION)
                idxPermFine = i
            i++
        }
        if (grantResults[idxPermFine] == PackageManager.PERMISSION_GRANTED) initScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode){
            PERMISSION_BLUETOOTH_REQUEST_CODE -> {
                when(resultCode){
                    Activity.RESULT_OK -> {

                    }
                    Activity.RESULT_CANCELED -> {

                    }
                }
            }
        }
    }

    private fun initScan() {
        if (!bluetoothAdapter?.isEnabled!!) {
            val CODE_DEMANDE_ACTIVATION = 1234
            val demandeActivationBLE = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(demandeActivationBLE, CODE_DEMANDE_ACTIVATION)
        } else startScan()
    }

    private fun startScan() {
        showLog("Start scan ...")
        isScanning = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) scanAPI18() else scanAPI21()
    }

    @TargetApi(18)
    private fun scanAPI18() {
        //Création du callback
        val leScanCallback: BluetoothAdapter.LeScanCallback =
                BluetoothAdapter.LeScanCallback{ device: BluetoothDevice, rssi: Int, _: ByteArray ->
                    Log.d(TAG,"Device trouvé : ${device.uuids}, ${device.name} ${device.address} $rssi")
                    showLog(" Force du signal : $rssi")
                    set_pairedDevices.add(device)

                    //connectBLE()
        }
        showLog("Lancement du scan en API 18")
        bluetoothAdapter?.startLeScan(arrayOf(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")),leScanCallback)
        //Arrêt du scan 5s après
        Handler().postDelayed({
            bluetoothAdapter?.stopLeScan(leScanCallback)
            isScanning = false
            showLog("Arrêt du scan en API 18")
            if (!deviceDetected) relaunchScanDelayed()
        }, 5000)
    }

    @TargetApi(21)
    private fun scanAPI21() {
        //Création du callback
        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val rssi: Int = result.rssi
                showLog(" Force du signal : $rssi")
                bluetoothDevice = result.device
                addDevice(result.device)
            }
        }
        //Récupération du scanner
        val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
        //Début du scan
        val builderScanFilter = ScanFilter.Builder()
        builderScanFilter
            //.setServiceUuid(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")))
            .setDeviceAddress("1C:23:2C:46:43:52")
        val bScanSettings = ScanSettings.Builder()
        bScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //bluetoothLeScanner?.startScan(mutableListOf(builderScanFilter.build()),bScanSettings.build(),scanCallback)
        bluetoothLeScanner?.startScan(scanCallback)
        //showLog("Lancement du scan en API 21")
        //Arrêt du scan 5s plus tard
        Handler().postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            showLog("Arrêt du scan en API 21")
            Log.d(TAG,"${adapter_paired_devices.count}")
            //if (!deviceDetected) relaunchScanDelayed()
        }, 5000)
    }

    private fun relaunchScanDelayed() {
        showLog("Attente 5 sec.")
        Handler().postDelayed({
            startScan()
        }, 5000)
    }

    fun addDevice(device: BluetoothDevice) {
        val address = device.address
        if (!hashMap_pairedDevices.containsKey(address)) {
            hashMap_pairedDevices[address] = device
            set_pairedDevices.add(device)
        }
        adapter_paired_devices.notifyDataSetChanged()
    }

    private fun showLog(message: String) {
        txt_log.append("$message\n")
    }

    companion object {
        const val TAG = "BLE SCANNING"
        const val PERMISSION_REQUEST_CODE = 101
        const val PERMISSION_BLUETOOTH_REQUEST_CODE = 102
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val intent = Intent(this,DeviceActivity::class.java)
        intent.putExtra(DeviceActivity.EXTRA_DEVICE,set_pairedDevices[position])
        startActivity(intent)
        finish()
    }
}