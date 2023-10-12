package com.example.phoneg

import DbHelper

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat

class Call : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val extras = intent.extras
                if (extras != null) {
                    val state = extras.getString(TelephonyManager.EXTRA_STATE)
                    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                        // 檢查權限
                        if (context != null && ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                        ){
                            // 使用 TelephonyManager 獲取來電號碼
                            val telephonyManager =
                                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                            val incomingNumber = telephonyManager.line1Number
                            if (incomingNumber != null && incomingNumber.isNotEmpty()) {
                                // 在這裡可以執行你想要的處理程序
                                if (isPhoneNumberInDatabase(context, incomingNumber)) {
                                    // 來電號碼匹配資料庫中的資料，顯示相關資訊
                                    val reason = getReasonForPhoneNumber(context, incomingNumber)
                                    showToast(context, "來電號碼: $incomingNumber，原因: $reason")
                                } else {
                                    // 來電號碼不在資料庫中
                                    showToast(context, "來電號碼: $incomingNumber，未知原因")
                                }
                            }
                        } else {
                            // 處理權限被拒絕的情況
                            context?.let { showToast(it, "需要 READ_PHONE_STATE 權限以獲取來電號碼") }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isPhoneNumberInDatabase(context: Context, phoneNumber: String): Boolean {
        val dbHelper = DbHelper(context)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(Contract.COLUMN_PHONE_NUMBER)
        val selection = "${Contract.COLUMN_PHONE_NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        val cursor: Cursor = db.query(
            Contract.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    private fun getReasonForPhoneNumber(context: Context, phoneNumber: String): String {
        val dbHelper = DbHelper(context)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(Contract.COLUMN_REASON)
        val selection = "${Contract.COLUMN_PHONE_NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        val cursor: Cursor = db.query(
            Contract.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        var reason = ""
        if (cursor.moveToFirst()) {
            reason = cursor.getString(cursor.getColumnIndexOrThrow(Contract.COLUMN_REASON))
        }
        cursor.close()
        return reason
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
