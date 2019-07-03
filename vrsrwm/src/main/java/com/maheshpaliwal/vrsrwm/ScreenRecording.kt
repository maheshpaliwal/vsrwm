package com.maheshpaliwal.vrsrwm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.maheshpaliwal.vrsrwm.Recorder.Companion.mediaTypeImage
import com.maheshpaliwal.vrsrwm.Recorder.Companion.mediaTypeVideo
import com.maheshpaliwal.vrsrwm.Recorder.Companion.requestCode
import com.maheshpaliwal.vrsrwm.Recorder.Companion.requestPermissionKey
import kotlinx.android.synthetic.main.activity_screen_recording.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
class ScreenRecording : AppCompatActivity() {
    val PERMISSION_CODE=102
    private var mCamera: Camera?=null //Camera Instance
    val recorder:Recorder=Recorder()
    // private var mShowCamera:showCamera?=null // variable to create instance of showCamera class( which implements surfaceView)
    private var cameraSurface:CameraSurface?=null
    var pathname:String?=null // Uri path to get snapshots
    var pathnameLogo:String?=null // logo path (uri)
    var chronometer: Chronometer?=null // count up timer
    private var snapshotsArray =ArrayList<String>()
    private val autoFocusExecutor= ScheduledThreadPoolExecutor(1)
    private var index:Int=0
    private var mScreenDensity:Int = 0 // Screen Density variable
    private var mProjectionManager: MediaProjectionManager? = null // Projection manager variable
    private var btn_action: ImageButton?=null // Button to capture video
    private var mMediaProjection: MediaProjection? = null // Media Projection
    private var mVirtualDisplay: VirtualDisplay? = null // Creating a virtual display to project
    private var mMediaProjectionCallback:MediaProjectionCallback?=null
    private var mMediaRecorder: MediaRecorder? = null // mediarecorder to set up camera parameters video/audio source, encoding , frame rate etc
    internal var isRecording = false // keeping track of recording
    var customFolderName:String?=null
    var mediaRecorder: MediaRecorder?=null
    var conClass:Class<*>?=null
    private fun releaseMediaRecorder() {
        mediaRecorder?.reset() // clear recorder configuration
        mediaRecorder?.release() // release the recorder object
        mediaRecorder = null
        mCamera?.lock() // lock camera for later use
    }
    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_recording)
        val permissions= arrayOf<String>(android.Manifest.permission.CAMERA,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.WAKE_LOCK,android.Manifest.permission.INTERNET,android.Manifest.permission.ACCESS_NETWORK_STATE)
        if(!Recorder.recorder.checkPermissions(this, permissions )){
            ActivityCompat.requestPermissions(this,permissions,PERMISSION_CODE)
        }
        var myclass:String=intent.getStringExtra("class")
        var drawableLogo:Int=intent.getIntExtra("logo",0)
        customFolderName=intent.getStringExtra("customFolderName")
        conClass =Class.forName(myclass)
        val customLogo:ImageView=this.logo3
        customLogo.setImageDrawable(applicationContext.resources.getDrawable(drawableLogo))
        chronometer=this.timer2 // initialize count up timer
        val metrics = DisplayMetrics() // initialize display metrics variable
        windowManager.defaultDisplay.getMetrics(metrics) // getting display metrics in metrics variable
        mScreenDensity = metrics.densityDpi  // density of screen
        DISPLAY_HEIGHT=metrics.heightPixels // height  of screen
        DISPLAY_WIDTH=metrics.widthPixels // width of screen

        // getting logo
        //val bitmap:Bitmap=(imageView.getDrawable() as BitmapDrawable).getBitmap()// converting imageView to drawable
        //storeImage(bitmap) // storing bitmap by converting it to jpeg/png/jpg format
        val snapshot: ImageButton =this.videoSnapshot2// button for snapshot
        mCamera = Camera.open() //opening camera
        recorder.setCameraParameters(mCamera,this)
        cameraSurface=CameraSurface(this,mCamera!!)

        // mShowCamera = mCamera?.let {
        //     showCamera(this, it) // calling surfaceview class
        // }
        cameraSurface?.also {
            val customLayout: FrameLayout = this.findViewById<FrameLayout>(R.id.customCamera) // adding view in framelayout
            customLayout.addView(it)
        }
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager// Media Preojection Manager
        btn_action=findViewById<ImageButton>(R.id.record2) // initialize record button
        mMediaProjection=null
        var displayMatrics: DisplayMetrics = DisplayMetrics()
        windowManager!!.defaultDisplay!!.getMetrics(displayMatrics)// screen metrics
        mScreenDensity=displayMatrics.densityDpi // screen density
        btn_action?.setOnClickListener{
            onToggleScreenShare() // decide what to do on the basis of recording status
        }

        snapshot.setOnClickListener {
            val pathToLocation: File?=recorder.getOutputMediaFile(1,customFolderName)
            recorder.takePicture(it,mCamera!!,customFolderName,false,null,pathToLocation,null)
            snapshotsArray.add(pathToLocation.toString())
        }
    }

    /** Create a file Uri for saving an image or video */
    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    /** Create a File for saving an image or video */
    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            customFolderName)
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(customFolderName, "failed to create directory")
                    return null
                }
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            mediaTypeImage -> {
                pathnameLogo="${mediaStorageDir.path}${File.separator}IMG_$timeStamp.png"

                snapshotsArray.add(pathnameLogo!!)
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.png")
            }
            mediaTypeVideo -> {
                pathname="${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4"
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
    }
    // a function which reacts to recording status
    fun onToggleScreenShare() {
        if (!isRecording)
        {  chronometer!!.base= SystemClock.elapsedRealtime()// setting up base
            chronometer!!.start() // starting count up timer
            val pathToLocation: File? =recorder.getOutputMediaFile(2,customFolderName)
            pathname=pathToLocation.toString()
            mMediaRecorder= MediaRecorder()
            recorder.setUpMediaRecorder(mMediaRecorder!!,pathToLocation!!) // setting up media recorder
            shareScreen() // share screen
            recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
            isRecording = true
        }
        else
        {   chronometer!!.stop() // stop count up timer
            chronometer!!.base= SystemClock.elapsedRealtime()// setting up count up timer base
            mMediaRecorder!!.stop() // stop media recorder
            mMediaRecorder!!.reset() // reset media recorder
            stopScreenSharing() // stop screen sharing
            isRecording = false
            recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
            //recorder.releaseCamera(mCamera!!) // releasing camera
            val intent:Intent= Intent(this@ScreenRecording,conClass)
            intent.putExtra("videopath",pathname)
            intent.putExtra("images",snapshotsArray)
            startActivity(intent)
        }
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay == null)
        { return
        }
        mVirtualDisplay!!.release()
        destroyMediaProjection()
        isRecording = false
        recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
    }
    private fun destroyMediaProjection() {
        if (mMediaProjection != null)
        {   mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
        Log.i(TAG, "MediaProjection Stopped")
    }
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        if (requestCode != requestCode)
        { Log.e(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != Activity.RESULT_OK)
        {   Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            isRecording = false
            recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
            return
        }
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
        isRecording = true
        recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
    }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults:IntArray) {
        when (requestCode) {
            requestPermissionKey -> {
                if ((grantResults.size > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED)
                { onToggleScreenShare()
                }
                else
                {   isRecording = false

                }
                return
            }
        }
    }
    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay("MainActivity", 1280, 720, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder!!.surface, null, null)
    }
    private inner class MediaProjectionCallback: MediaProjection.Callback() {
        override fun onStop() {
            if (isRecording)
            {   isRecording = false
                recorder.actionBtnImageReload(btn_action!!,isRecording,applicationContext,R.drawable.stop,R.drawable.start3)
                mMediaRecorder!!.stop()
                mMediaRecorder!!.reset()
            }
            mMediaProjection = null
            stopScreenSharing()
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
        destroyMediaProjection()
    }
    override fun onBackPressed() {
        if (isRecording)
        {
        }
        else
        { finish()
        }
    }
    companion object {
        private const val TAG = "MainActivity"
        private var DISPLAY_WIDTH = 1280
        private var DISPLAY_HEIGHT = 720
        private val ORIENTATIONS = SparseIntArray()
        init{
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
    private fun shareScreen() {
        if (mMediaProjection == null)
        { startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), requestCode)// creating screen capture intent
            return
        }
        mVirtualDisplay = createVirtualDisplay() // creating virtual display
        mMediaRecorder!!.start() // start media recorder
        isRecording = true // setting recording status true
        recorder.actionBtnImageReload(btn_action!!,isRecording,this,R.drawable.stop,R.drawable.start3)
    }
    fun storeImageClaim( image: Bitmap){
        val file: File? =getOutputMediaFile(mediaTypeImage)
        var fileOutputStream: FileOutputStream = FileOutputStream(file)
        image.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream)
        fileOutputStream.close()
    }

    fun autoFocus(mCamera:Camera){
        autoFocusExecutor.schedule({
            val params: Camera.Parameters = mCamera.parameters
            if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE


            }
            mCamera.parameters = params
        },100, TimeUnit.MILLISECONDS)




    }

}
