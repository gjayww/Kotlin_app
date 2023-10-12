package com.example.pikbon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import android.util.Log
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import android.view.inputmethod.EditorInfo
import org.json.JSONException
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    lateinit var outputTV: TextView
    lateinit var micIV: ImageView
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val PERMISSION_CODE = 2
    lateinit var txtResponse: TextView
    lateinit var idTVQuestion: TextView
    lateinit var etQuestion: TextInputEditText
    lateinit var sendButton: Button


    // 記錄對話
    private val conversation = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendButton = findViewById<Button>(R.id.sendButton)
        etQuestion = findViewById<TextInputEditText>(R.id.etQuestion)
        idTVQuestion = findViewById<TextView>(R.id.idTVQuestion)
        txtResponse = findViewById<TextView>(R.id.txtResponse)
        outputTV = findViewById(R.id.idTVOutput)
        micIV = findViewById(R.id.idIVMic)

        val editText = findViewById<EditText>(R.id.edit)
        val textToSpeechBtn = findViewById<Button>(R.id.texttoSpeechBtn)

        textToSpeech = TextToSpeech(this) { status ->
            textToSpeech.setPitch(2.0f)
            textToSpeech.setSpeechRate(1.0f)
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "不支援語言", Toast.LENGTH_LONG).show()
                }
            }
        }


        etQuestion.addTextChangedListener {
            // 當用戶輸入問題時，更新最後一條對話記錄
            conversation.takeIf { it.isNotEmpty() }?.let { conversation[it.size - 1] = Pair("user", it.toString()) }
        }

        etQuestion.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val question = etQuestion.text.toString().trim()
                if (question.isNotEmpty()) {
                    addMessage("user", question)
                    getResponse(question)
                }
                return@setOnEditorActionListener true
            }
            false
        }
        sendButton.setOnClickListener {
            val question = etQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                addMessage("user", question)
                getResponse(question)
            }
        }
        micIV.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE)
            } else {
                startSpeechRecognition()
            }
        }
    }

    private fun generateUniqueFileName(): String {
        val timestamp = System.currentTimeMillis()
        val cacheDir = cacheDir // 获取应用程序的缓存目录
        val fileName = "audio_$timestamp.mp3"
        return File(cacheDir, fileName).absolutePath
    }



    private fun addMessage(role: String, content: String) {
        conversation.add(Pair(role, content))
    }

    fun getResponse(question: String) {
        val apiKey = "your_API key"
        val url = "https://api.openai.com/v1/chat/completions"
        val jsonObject = JSONObject()
        jsonObject.put("model", "gpt-3.5-turbo")

        val messagesArray = JSONArray()
        for (message in conversation) {
            messagesArray.put(JSONObject().put("role", message.first).put("content", message.second))
        }
        jsonObject.put("messages", messagesArray)

        jsonObject.put("max_tokens", 50)
        jsonObject.put("temperature", 0.1)

        val requestBody = jsonObject.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        // 檢查是否已包含相同的問題
        if (!conversation.any { it.second == question }) {
            addMessage("user", question)
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API failed", e)
                runOnUiThread {
                    txtResponse.text = "API请求失败：$e"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    Log.v("API Response", body)

                    try {
                        val jsonObject = JSONObject(body)
                        val jsonArray = jsonObject.optJSONArray("choices")

                        if (jsonArray != null && jsonArray.length() > 0) {
                            val messageObj = jsonArray.getJSONObject(0).optJSONObject("message")

                            if (messageObj != null) {
                                val textResult = messageObj.optString("content", "No text found in response")

                                runOnUiThread {
                                    txtResponse.text = textResult
                                    etQuestion.text?.clear()// 清空etQuestion
                                }

                                if (textResult.isNotEmpty()) {
                                    textToSpeech.speak(textResult, TextToSpeech.QUEUE_ADD, null, null)
                                    addMessage("assistant", textResult)
                                }
                            } else {
                                runOnUiThread {
                                    txtResponse.text = "No 'message' object found in response"
                                }
                            }
                        } else {
                            runOnUiThread {
                                txtResponse.text = "No 'choices' array found in response"
                            }
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            txtResponse.text = "JSON解析错误：$e"
                        }
                    }
                } else {
                    Log.v("data", "empty")
                    runOnUiThread {
                        txtResponse.text = "API响应为空"
                    }
                }
            }
        })
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, " " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val res: ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                val recognizedText = res[0]
                etQuestion.setText(recognizedText)
                sendButton.performClick()
            }
        }
    }

    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
    }
}


