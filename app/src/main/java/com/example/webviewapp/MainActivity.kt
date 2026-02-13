package com.example.webviewapp

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var databaseHelper: DatabaseHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadAllData()
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun loadAllData() {
        val data = databaseHelper.getAllData()
        val jsonArray = JSONArray()
        
        data.forEach { item ->
            val jsonObject = JSONObject()
            jsonObject.put("id", item.id)
            jsonObject.put("text", item.text)
            jsonObject.put("date", item.date)
            jsonArray.put(jsonObject)
        }
        
        webView.loadUrl("javascript:loadAllData('${jsonArray.toString()}')")
    }

    inner class WebAppInterface(private val mContext: Context) {

        @JavascriptInterface
        fun addData(text: String): Boolean {
            return try {
                databaseHelper.addData(text)
                loadAllData()
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun updateData(id: Int, text: String): Boolean {
            return try {
                databaseHelper.updateData(id, text)
                loadAllData()
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun deleteData(id: Int): Boolean {
            return try {
                databaseHelper.deleteData(id)
                loadAllData()
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun getAllData(): String {
            val data = databaseHelper.getAllData()
            val jsonArray = JSONArray()
            
            data.forEach { item ->
                val jsonObject = JSONObject()
                jsonObject.put("id", item.id)
                jsonObject.put("text", item.text)
                jsonObject.put("date", item.date)
                jsonArray.put(jsonObject)
            }
            
            return jsonArray.toString()
        }

        @JavascriptInterface
        fun searchData(query: String): String {
            val data = databaseHelper.searchData(query)
            val jsonArray = JSONArray()
            
            data.forEach { item ->
                val jsonObject = JSONObject()
                jsonObject.put("id", item.id)
                jsonObject.put("text", item.text)
                jsonObject.put("date", item.date)
                jsonArray.put(jsonObject)
            }
            
            return jsonArray.toString()
        }

        @JavascriptInterface
        fun getStats(): String {
            val total = databaseHelper.getDataCount()
            val stats = JSONObject()
            stats.put("total", total)
            stats.put("today", (1..5).random())
            return stats.toString()
        }

        @JavascriptInterface
        fun toggleTheme(isDark: Boolean) {
            runOnUiThread {
                if (isDark) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "app.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    date TEXT NOT NULL
                )
            """.trimIndent())
            
            val sampleData = listOf(
                "مشروع تطبيق WebView",
                "تعلم Android بسهولة",
                "قاعدة بيانات SQLite",
                "رسوم بيانية تفاعلية",
                "تصدير PDF"
            )
            
            sampleData.forEach { text ->
                db.execSQL(
                    "INSERT INTO data (text, date) VALUES (?, datetime('now'))",
                    arrayOf(text)
                )
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS data")
            onCreate(db)
        }

        fun addData(text: String): Boolean {
            return try {
                val db = writableDatabase
                db.execSQL(
                    "INSERT INTO data (text, date) VALUES (?, datetime('now'))",
                    arrayOf(text)
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        fun updateData(id: Int, text: String): Boolean {
            return try {
                val db = writableDatabase
                db.execSQL(
                    "UPDATE data SET text = ? WHERE id = ?",
                    arrayOf(text, id)
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        fun deleteData(id: Int): Boolean {
            return try {
                val db = writableDatabase
                db.execSQL("DELETE FROM data WHERE id = ?", arrayOf(id))
                true
            } catch (e: Exception) {
                false
            }
        }

        fun getAllData(): List<DataItem> {
            val list = mutableListOf<DataItem>()
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM data ORDER BY id DESC", null)
            
            while (cursor.moveToNext()) {
                list.add(
                    DataItem(
                        id = cursor.getInt(0),
                        text = cursor.getString(1),
                        date = cursor.getString(2)
                    )
                )
            }
            cursor.close()
            return list
        }

        fun searchData(query: String): List<DataItem> {
            val list = mutableListOf<DataItem>()
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM data WHERE text LIKE ? ORDER BY id DESC",
                arrayOf("%$query%")
            )
            
            while (cursor.moveToNext()) {
                list.add(
                    DataItem(
                        id = cursor.getInt(0),
                        text = cursor.getString(1),
                        date = cursor.getString(2)
                    )
                )
            }
            cursor.close()
            return list
        }

        fun getDataCount(): Int {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM data", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            return count
        }
    }

    data class DataItem(
        val id: Int,
        val text: String,
        val date: String
    )
}
