package com.example.phoneg

import DbHelper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.widget.TextView
import com.example.phoneg.R

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = DbHelper(this)
        /*
        // 刪除資料
        deleteData("9876543210")
        deleteData("9256543210")
        // 插入資料
        insertData("886906594199", "裕隆貸款")
        insertData("886982841539", "新光貸款")
        insertData("886266021919", "詐騙")
        insertData("886907912393", "一接就掛")
        insertData("886982437372", "貸款推銷")
        insertData("886989284240", "銀行推銷")
        insertData("886981700630", "汽車貸款")
        insertData("886982384038", "接了就掛")
        insertData("886982740238", "汽車借款")
        insertData("886965219483", "一接就掛")
        insertData("886972501665", "接起來就掛")
        insertData("886916350765", "詐騙")
        insertData("886982407733", "一接就掛")
        insertData("886974389446", "汽車貸款")
        insertData("886982438509", "貸款")
        insertData("886221715030", "中信 信貸")

        */
        //顯示資料
        insertData("+15555215554", "中信信貸")

        displayData()





        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
            }
        } else {
            // Permission is already granted
        }
    }

    fun insertData(phoneNumber: String, reason: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(Contract.COLUMN_PHONE_NUMBER, phoneNumber)
            put(Contract.COLUMN_REASON, reason)
        }

        db.insert(Contract.TABLE_NAME, null, values)
    }
    fun deleteData(phoneNumber: String) {
        val db = dbHelper.writableDatabase
        val selection = "${Contract.COLUMN_PHONE_NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        db.delete(Contract.TABLE_NAME, selection, selectionArgs)
    }

    private fun displayData() {
        val db = dbHelper.readableDatabase

        val projection = arrayOf(
            Contract.COLUMN_PHONE_NUMBER,
            Contract.COLUMN_REASON
        )

        val cursor: Cursor = db.query(
            Contract.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val dataTextView: TextView = findViewById(R.id.dataTextView)
        dataTextView.text = ""

        with(cursor) {
            while (moveToNext()) {
                val phoneNumber = getString(getColumnIndexOrThrow(Contract.COLUMN_PHONE_NUMBER))
                val reason = getString(getColumnIndexOrThrow(Contract.COLUMN_REASON))

                // 顯示資料
                dataTextView.append("電話號碼：$phoneNumber，原因：$reason\n")
            }
        }
        cursor.close()
    }


        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "NO", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
