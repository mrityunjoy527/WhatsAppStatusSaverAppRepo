package com.example.whatsappstatussaver

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.icu.text.SymbolTable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.view.Display
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.w3c.dom.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IndexOutOfBoundsException

class MainActivity : AppCompatActivity() {

    lateinit var recycler_view: RecyclerView
    lateinit var statusList: ArrayList<ModelClass>
    lateinit var statusAadpter: StatusAadpter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "All Status"
        recycler_view = findViewById(R.id.recycler_view)
        statusList = ArrayList()
        val result = readDataFromPrefs()
        if(result) {
            val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
            val uriPath = sh.getString("PATH", "")
            if(uriPath != null) {
                contentResolver.takePersistableUriPermission(Uri.parse(uriPath), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, Uri.parse(uriPath))
                for(file: DocumentFile in fileDoc!!.listFiles()) {
                    if(!file.name!!.endsWith(".nomedia")) {
                        val model = ModelClass(file.name!!, file.uri.toString())
                        statusList.add(model)
                    }
                }
            }
            setUpWithRecyclerView()
        }else {
            getFolderPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFolderPermission() {
        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
        var scheme = uri.toString()
        scheme = scheme.replace("/root/", "/tree/")
        scheme += "%3A$targetDirectory"
        uri = Uri.parse(scheme)
        intent.putExtra("android.provider.extra.INITIAL_URI", uri)
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        startActivityForResult(intent, 1244)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK) {
            val treeUri = data?.data
            val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
            val myEdit = sh.edit()
            myEdit.putString("PATH", treeUri.toString())
            myEdit.apply()
            if(treeUri != null) {
                contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
                for(file: DocumentFile in fileDoc!!.listFiles()) {
                    if(!file.name!!.endsWith(".nomedia")) {
                        val model = ModelClass(file.name!!, file.uri.toString())
                        statusList.add(model)
                    }
                }
            }
            setUpWithRecyclerView()
        }
    }

    private fun setUpWithRecyclerView() {
        statusAadpter = StatusAadpter(applicationContext, statusList) {
                selectedItem: ModelClass -> listItemClicked(selectedItem)
        }
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)
            adapter = statusAadpter
        }
    }

    private fun listItemClicked(selectedItem: ModelClass) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.show()
        val btn = dialog.findViewById<Button>(R.id.bt_download)
        btn.setOnClickListener {
            dialog.dismiss()
            saveFile(selectedItem)
        }
    }

    private fun saveFile(status: ModelClass) {
        if(status.fileName.endsWith(".mp4")) {
            val inputStream = contentResolver.openInputStream(Uri.parse(status.fileUri))
            val fileName = "${System.currentTimeMillis()}.mp4"
            try {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS+"/Videos/")
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                val outputStream = uri?.let { contentResolver.openOutputStream(it) }
                if(inputStream != null) {
                    outputStream?.write(inputStream.readBytes())
                }
                outputStream?.close()
                Toast.makeText(applicationContext, "Video Saved", Toast.LENGTH_SHORT).show()
            }catch (e: IOException) {
                Toast.makeText(applicationContext, "Failed", Toast.LENGTH_SHORT).show()
            }
        }else {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(status.fileUri))
            val fileName = "${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.also { resolver ->
                    val values = ContentValues()
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    fos = uri?.let { resolver.openOutputStream(it) }
                }
            }else {
                val imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imageDir, fileName)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                Toast.makeText(applicationContext, "Image Saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readDataFromPrefs(): Boolean {
        val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
        val uriPath = sh.getString("PATH", "")
        if(uriPath != null) {
            if(uriPath.isEmpty()) return false
        }
        return true
    }
}