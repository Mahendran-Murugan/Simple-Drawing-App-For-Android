package com.example.drawingpadformobile

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var smallButton : ImageButton? = null
    private var mediumButton : ImageButton? = null
    private var largeButton : ImageButton? = null
    private var imgView : ImageView? = null
    private var imgUndo: ImageButton? = null
    private var imgRedo: ImageButton? = null
    private var imgErase: ImageButton? = null
    private var imgSave: ImageButton? = null
    private var customProgressDialog: Dialog? = null
    private val openGallery: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                imgView = findViewById(R.id.imv_bg)
                imgView?.setImageURI(result.data?.data)
            }
        }
    private var drawingView : DrawingView? = null
    private var drawBtn : ImageButton? = null
    private var mImgButtonPressedPaint: ImageButton? = null
    private var gallImageButton: ImageButton? = null
    private var k: Boolean = true
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permission ->
            permission.entries.forEach{
                val permissionName = it.key
                val permissionValue = it.value
                val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGallery.launch(intent)

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.draw_view)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        drawingView?.setSizeOfBrush(10.toFloat())
        val linearLayoutForImageButtons = findViewById<LinearLayout>(R.id.ll_layout_for_color)
        mImgButtonPressedPaint = linearLayoutForImageButtons[1] as ImageButton
        drawBtn = findViewById(R.id.draw_btn)
        drawBtn?.setOnClickListener(){
            showTheBrushSizeDialogue()
        }
        imgUndo = findViewById(R.id.undo_btn)
        imgUndo?.setOnClickListener(){
            drawingView?.OnclickUndo()
        }
        imgRedo = findViewById(R.id.redo_btn)
        imgRedo?.setOnClickListener(){
            drawingView?.OnclickRedo()
        }
        imgErase = findViewById(R.id.erase_btn)
        imgErase?.setOnClickListener(){
            drawingView?.OnClickErase()
        }
        imgSave = findViewById(R.id.save_btn)
        imgSave?.setOnClickListener(){
            if(isReadStorageIsAllowed()){
                displayProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView : FrameLayout = findViewById(R.id.f_layout)
                    saveBitmap(getBitmapView(flDrawingView))
                }
            }
        }
        gallImageButton = findViewById(R.id.gallery_btn)
        gallImageButton?.setOnClickListener(){
            requestForStorage()
        }
    }

    private fun isReadStorageIsAllowed(): Boolean{
        return true
    }

    private fun requestForStorage() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialogue("Drawing Pad For Mobile","Drawing Pad For Mobile "+"Need Access to Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE ,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showTheBrushSizeDialogue(){
        val brushDialogue = Dialog(this)
        brushDialogue.setContentView(R.layout.dialogue_for_brush_size)
        brushDialogue.setTitle("Brush Size: ")
        smallButton = brushDialogue.findViewById(R.id.small_btn)
        mediumButton = brushDialogue.findViewById(R.id.medium_btn)
        largeButton = brushDialogue.findViewById(R.id.large_btn)
        smallButton?.setOnClickListener(){
            drawingView?.setSizeOfBrush(10.toFloat())
            brushDialogue.dismiss()
        }
        mediumButton?.setOnClickListener(){
            drawingView?.setSizeOfBrush(20.toFloat())
            brushDialogue.dismiss()
        }
        largeButton?.setOnClickListener(){
            drawingView?.setSizeOfBrush(30.toFloat())
            brushDialogue.dismiss()
        }
        brushDialogue.show()
    }

    private fun getBitmapView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    fun paintClicked(view: View){
        if(view != mImgButtonPressedPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_pressed
                )
            )
            mImgButtonPressedPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            mImgButtonPressedPaint = view
        }
    }
    private fun showRationaleDialogue(title: String,message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){
            dialog , _ -> dialog.dismiss()
        }
        builder.create().show()
    }

    private suspend fun saveBitmap(mBitmap: Bitmap): String{
         var result = ""
        withContext(Dispatchers.IO){
            try{
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                val f = File(externalCacheDir?.absoluteFile.toString()+
                        File.separator +" Drawing Pad"+ System.currentTimeMillis()/1000 + ".png")
                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()

                result =f.absolutePath

                runOnUiThread{
                    cancelCustomProgressDialog()
                    if(result.isNotEmpty()){
                        Toast.makeText(this@MainActivity,"File save Successfully at: $result",Toast.LENGTH_SHORT).show()
                    }
                    else{
                    Toast.makeText(this@MainActivity,"OOPS,File can't be saved",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            catch (e: Exception){
                result = ""
                e.printStackTrace()
            }
        }
        return result
    }
    private fun displayProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.progress_dialog_activity)
        customProgressDialog?.show()
    }
    private fun cancelCustomProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}