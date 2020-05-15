package com.example.bluejoysticktruck

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.util.*

const val TAG = "BLE"

class Bluetooth(val bluetoothAdapter: BluetoothAdapter) {
    var isConnected = false
    val mUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    var mSocket: BluetoothSocket? = null
    var connectThread: ConnectThread? = null

    fun sendCommand(char: String) {
        if (mSocket != null && isConnected) {
            try {
                mSocket!!.outputStream.write(char.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        if (mSocket != null) {
            try {
                mSocket?.close()
                mSocket = null
                isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun connect(mDevice: BluetoothDevice) {
        connectThread = ConnectThread(mDevice)
        connectThread?.execute()
    }

    fun cancel() {
        connectThread?.cancel(true)
    }

    inner class ConnectThread(val device: BluetoothDevice) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String? {
            if (mSocket == null || isConnected == false) {
                 try {
                     mSocket = device.createInsecureRfcommSocketToServiceRecord(mUUID)
                     bluetoothAdapter.cancelDiscovery()
                     mSocket!!.connect()
                     isConnected = true
                } catch (e: IOException) {
                     e.printStackTrace()
                }
            }

            return null
        }

    }

}