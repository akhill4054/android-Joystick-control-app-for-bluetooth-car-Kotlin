package com.example.bluejoysticktruck

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.paired_device_list.view.*
import java.io.IOException
import java.util.*

fun toast_short(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun timedCheck(time: Long, runnable: () -> Unit) {
    runnable()
    Handler().postDelayed({
        timedCheck(time, runnable)
    }, time)
}

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity() {

    lateinit var joyStick: JoyStick
    var strength = 0;

    // Bluetooth stuff
    var bluetoothAdapter: BluetoothAdapter? = null
    var mDevice: BluetoothDevice? = null
    lateinit var bluetooth: Bluetooth

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joyStick = findViewById(R.id.joyStick)

        if (bluetoothAdapter != null) {
            bluetooth = Bluetooth(bluetoothAdapter!!)
            // recursively checking in each 50 ms for joystick's status
            timedCheck(50) {
                if (bluetooth.mSocket != null && bluetooth.isConnected) {
                    strength = (joyStick.strength * 9.5 / joyStick.r).toInt()
                    strength_value.text = strength.toString()

                    if (strength >= 2) {
                        angle_value.text = joyStick.getJoystickAngle().toString()
                        bluetooth.sendCommand(strength.toString())

                        when (joyStick.getJoystickAngle()) {
                            // up
                            in 78..102 -> bluetooth.sendCommand("U")
                            // down
                            in 258..282 -> bluetooth.sendCommand("D")
                            // left
                            in 168..192 -> bluetooth.sendCommand("L")
                            // right
                            in 0..12 -> bluetooth.sendCommand("R")
                            in 348..360 -> bluetooth.sendCommand("R")
                            // forward left
                            in 103..167 -> bluetooth.sendCommand("G")
                            // forward right
                            in 13..77 -> bluetooth.sendCommand("I")
                            // backward left
                            in 193..257 -> bluetooth.sendCommand("H")
                            // backward right
                            in 283..347 -> bluetooth.sendCommand("J")
                        }

                    } else bluetooth.sendCommand("S") // if strength >= 2 else .. stop
                } // if connected
            }
        }

        var frontLights = false; var tailLights = false

        // extra buttons
        btn_headlights.setOnClickListener {
            if (frontLights) {
                btn_headlights.setImageDrawable(getDrawable(R.drawable.icon_head_lights_off))
                bluetooth.sendCommand("w")
            } else {
                btn_headlights.setImageDrawable(getDrawable(R.drawable.icon_head_lights_on))
                bluetooth.sendCommand("W")
            }
            frontLights = !frontLights
        }

        btn_taillights.setOnClickListener {
            if (frontLights) {
                btn_taillights.setImageDrawable(getDrawable(R.drawable.icon_tail_lights_off))
                bluetooth.sendCommand("u")
            } else {
                btn_taillights.setImageDrawable(getDrawable(R.drawable.icon_tail_lights_on))
                bluetooth.sendCommand("U")
            }
            frontLights = !frontLights
        }

        btn_connect.setOnClickListener {
            if (bluetoothAdapter == null) {
                toast_short(this, "this device doesn't support Bluetooth : (")
            } else if (bluetoothAdapter?.isEnabled == false) {
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                    startActivityForResult(this, REQUEST_ENABLE_BT)
                }
            } else if (mDevice == null) {
                showPairedDevices()
            } else if (btn_connect.text != "Connecting") {
                // disconnecting
                btn_connect.text = "Connect"
                btn_connect.background = getDrawable(R.drawable.btn_bg)
                bluetooth.disconnect()
                mDevice = null
            }
        }

    }

    // Bluetooth stuff

    private fun showPairedDevices() {
        val device_names = mutableListOf<String>()
        val devices = mutableListOf<BluetoothDevice>()

        bluetoothAdapter!!.bondedDevices.forEach {
            device_names.add(it.name)
            devices.add(it)
        }

        if (device_names.size > 0) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.paired_device_list, null)

            val adapter =
                ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, device_names)

            val builder = AlertDialog.Builder(this)
                .setTitle("Select a Device to Connect")
                .setIcon(R.drawable.icon_bluetooth)
                .setCancelable(false)
                .setView(dialogView)
                .setAdapter(adapter) { dialog, which ->
                    mDevice = devices[which]

                    toast_short(this, "${mDevice!!.name} selected.")

                    // Connect to device here
                    bluetooth.connect(mDevice!!)
                    btn_connect.text = "Connecting"

                    Handler().postDelayed({
                        if (!bluetooth.isConnected) {
                            // connecting
                            toast_short(this, "couldn't connect to selected device. : (")
                            btn_connect.text = "Connect"
                            mDevice = null
                            bluetooth.cancel()
                        } else {
                            btn_connect.text = "Disconnect"
                            btn_connect.background = getDrawable(R.drawable.btn_bg_green)
                        }
                    }, 4000)
                }
                .create()

            builder.show()

            dialogView.pair_btn.setOnClickListener {
                val intent = Intent()
                    .setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
                builder.dismiss()
            }
        } else {
            toast_short(this, "please pair a new device before continuing")
            val intent = Intent()
                .setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                toast_short(this, "Bluetooth's been enabled")
                showPairedDevices()
            } else toast_short(this, "Bluetooth enabling cancelled : (")
        }
    }

    override fun onStop() {
        super.onStop()
        if (bluetoothAdapter != null) {
            if (bluetooth.isConnected) {
                // disconnecting
                bluetooth.disconnect()
                mDevice = null
                btn_connect.text = "Connect"
                btn_connect.background = getDrawable(R.drawable.btn_bg)
            }
            bluetooth.cancel()
        }
    }

}
