package com.mobileapp.inspestor_vr5

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mobileapp.inspestor_vr5.databinding.ActivityMainBinding
import com.mobileapp.inspestor_vr5.ml.TestTrainMetadata3
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var captured_Image: ImageView
    private lateinit var result_insect: TextView
    private lateinit var prob_score: TextView
    private lateinit var rec_act_ing_list: TextView
    private val GALLERY_REQUEST_CODE= 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
//Main app functions and buttons activation
        captured_Image = binding.capturedImage
        result_insect=binding.resultInsect
        rec_act_ing_list=binding.recActIngList

        binding.cameraBtn.setOnClickListener{
            takePicturePreview.launch(null)

        }

        binding.galleryBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type= "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            intent.flags= Intent.FLAG_GRANT_READ_URI_PERMISSION
            onResult.launch(intent)

        }

        binding.libNav.setOnClickListener {
            startActivity(Intent(this, Library::class.java))

        }
        binding.manNav.setOnClickListener {
            startActivity(Intent(this, Instruction::class.java))

        }
    }

    private val takePicturePreview = registerForActivityResult (ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if(bitmap != null){
            captured_Image.setImageBitmap(bitmap)
            outputGenerator(bitmap)
        }
    }

    private val onResult= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        Log.i("TAG", "this is the result: ${result.data} ${result.resultCode}")
        onResultReceived(GALLERY_REQUEST_CODE, result)
    }

    private fun onResultReceived(requestCode:Int, result: ActivityResult?){
        when(requestCode){
            GALLERY_REQUEST_CODE->{
                if(result?.resultCode== Activity.RESULT_OK){
                    result.data?.data?.let{uri->
                        Log.i("Tag", "onResultReceived: $uri")
                        val bitmap= BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        captured_Image.setImageBitmap(bitmap)
                    }
                }
                else{
                    Log.e("Tag", "onActivityResult:error in selecting image")
                }
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun outputGenerator(bitmap: Bitmap){
        val TestTrainModel = TestTrainMetadata3.newInstance(this)

        //Creates inputs for reference.
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val tfImage= TensorImage.fromBitmap(newBitmap)

        //Runs model inference and gets result
        val outputs=TestTrainModel.process(tfImage).detectionResultList.apply {
            sortByDescending {
                it.scoreAsFloat
            }
        }
        val detectionResult=outputs[0]
        if(detectionResult.scoreAsFloat <= .5){
            result_insect.text="No pest detected"
            rec_act_ing_list.text=" "
            //prob_score.text=" "
        }else{
            result_insect.text = detectionResult.categoryAsString + " " + detectionResult.scoreAsFloat
            //prob_score.text="$detectionResult"
            Log.i("Tag", "outputGenerator: $detectionResult")
            if (detectionResult.categoryAsString == "Rice Grain Bug"){
                rec_act_ing_list.text = "LAMBDA-CYHALOTHRIN 25 g/L" + "\n" + "N CYPERMETHRIN 50g/L" + "\n" + "DIAZINON 600 g/L"
            }
            else if (detectionResult.categoryAsString == "Rice Bug"){
                rec_act_ing_list.text = "LAMBDA-CYHALOTHRIN 25 g/L" + "\n" + "DIAZINON 600 g/L" + "\n" + "N CYPERMETHRIN 50g/L"
            }
            else if (detectionResult.categoryAsString == "Brown Planthopper"){
                rec_act_ing_list.text = "PHENTHOATE 500 g/L" + "\n" + "PHENTHOATE+BPMC 250 g/L" + "\n" + "CYPERMETHRIN 50g/L"
            }
            else if (detectionResult.categoryAsString == "Leaf Folder"){
                rec_act_ing_list.text = "ETOFENPROX 25 g/L" + "\n" + "CYPERMETHRIN 55 g/L" + "\n" + "DIAZINON 600g/L"
            }
            else if (detectionResult.categoryAsString == "Green Planthopper"){
                rec_act_ing_list.text = "LAMBDA-CYHALOTHRIN 25 g/L" + "\n" + "CYPERMETHRIN 50 g/L" + "\n" + "PHENTOATE 500g/L"
            }
            else if (detectionResult.categoryAsString == "Rice Black Bug"){
                rec_act_ing_list.text = "DELTAMETHRIN 25 g/L" + "\n" + "BETA-CYPHERMETRIN 25 g/L" + "\n" + "AZADIRACHTIN 3g/L"
            }
            else {
                rec_act_ing_list.text = " "
            }
        }

        TestTrainModel.close()
    }

}