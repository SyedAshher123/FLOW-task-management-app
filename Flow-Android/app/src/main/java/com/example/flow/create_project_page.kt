package com.ahmedprojects.flow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import org.json.JSONObject
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Base64
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class create_project_page : AppCompatActivity() {

    private lateinit var etProjectName: EditText
    private lateinit var etProjectDescription: EditText
    private lateinit var btnCreateProject: RelativeLayout
    private lateinit var btnPickImage: RelativeLayout
    private lateinit var ivProjectImage: ImageView

    private var ip: IP_String = IP_String()
    private var selectedImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_project_page)

        etProjectName = findViewById(R.id.etProjectName)
        etProjectDescription = findViewById(R.id.etProjectDescription)
        btnCreateProject = findViewById(R.id.btnCreateProject)
        btnPickImage = findViewById(R.id.btnPickImage)
        ivProjectImage = findViewById(R.id.ivProjectImage)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Image picker launcher ---
        val pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                imageUri?.let { uri ->
                    ivProjectImage.setImageURI(uri)  // preview
                    selectedImagePath = saveUriImageToInternal(uri)
                }

            }
        }

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        btnCreateProject.setOnClickListener {
            val name = etProjectName.text.toString().trim()
            val description = etProjectDescription.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter project name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createProject(userId, name, description, selectedImagePath)
        }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createProject(userId: Int, name: String, description: String, base64Image: String?) {
        val db = AppDatabase.getInstance(this)
        val pendingDao = db.pendingProjectDao()
        val joinCode = UUID.randomUUID().toString().substring(0, 8)

        if (!isOnline()) {
            lifecycleScope.launch {
                pendingDao.insertPending(
                    PendingProjectEntity(
                        ownerId = userId,
                        name = name,
                        description = description,
                        joinCode = joinCode,
                        picturePath = selectedImagePath ?: ""
                    )
                )
                Toast.makeText(this@create_project_page, "Saved offline. Will sync later.", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        createProjectOnline(userId, name, description, joinCode, base64Image)
    }

    private fun createProjectOnline(userId: Int, name: String, description: String, joinCode: String, base64Image: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "create_project.php"
        val db = AppDatabase.getInstance(this)
        val pendingDao = db.pendingProjectDao()

        val jsonBody = JSONObject().apply {
            put("ownerId", userId)
            put("name", name)
            put("description", description)
            put("joinCode", joinCode)
            if (!selectedImagePath.isNullOrEmpty()) {
                val b64 = fileToBase64(selectedImagePath!!)
                put("picture_base64", b64 ?: "")
            } else {
                put("picture_base64", "")
            }
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Project created!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Toast.makeText(this, "Online failed. Saved to offline queue.", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    pendingDao.insertPending(
                        PendingProjectEntity(
                            ownerId = userId,
                            name = name,
                            description = description,
                            joinCode = joinCode,
                            picturePath = selectedImagePath ?: ""
                        )
                    )

                    finish()
                }
            })

        queue.add(request)
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }

    private fun saveUriImageToInternal(uri: Uri): String? {
        return try {
            // read bitmap with sampling/resizing if necessary
            val input = contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(input)
            input.close()

            // Resize to max width/height (preserve aspect) to keep size small
            val maxDim = 1024
            val (w, h) = bitmap.width to bitmap.height
            if (w > maxDim || h > maxDim) {
                val ratio = w.toFloat() / h.toFloat()
                val newW: Int
                val newH: Int
                if (ratio > 1) { // width > height
                    newW = maxDim
                    newH = (maxDim / ratio).toInt()
                } else {
                    newH = maxDim
                    newW = (maxDim * ratio).toInt()
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            }

            // compress to JPEG 80% (smaller than PNG)
            val fileName = "proj_img_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            fos.flush()
            fos.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fileToBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }




}
