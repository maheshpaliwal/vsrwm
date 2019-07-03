
package com.maheshpaliwal.vrsrwm
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.maheshpaliwal.vrsrwm.MediaProjectionCallback


import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle

import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import br.com.duanniston.watermarklib.framework.WaterMark
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Recorder: AppCompatActivity(){
    companion object {
        val tag:String="watermarkscreenrecorder"
        var encodingBitRate =1299*1000
        val frameRate:Int=30
        val mediaTypeImage=1
        val mediaTypeVideo=2
        val metrics = DisplayMetrics() // initialize display metrics variable

        val requestPermissionKey = 1
        val requestCode = 1000
        var mScreenDensity:Int = 0 // Screen Density variable
        var mProjectionManager: MediaProjectionManager? = null // Projection manager variable
        var btn_action: Button?=null // Button to capture video
        var mMediaProjection: MediaProjection? = null // Media Projection
        var mVirtualDisplay: VirtualDisplay? = null // Creating a virtual display to project
        var mMediaProjectionCallback: MediaProjectionCallback? = null // callback

        val recorder=Recorder()
        var mMediaRecorder: MediaRecorder? = null // mediarecorder to set up camera parameters video/audio source, encoding , frame rate etc
        var isRecording = false // keeping track of recording
    }
    fun setCameraParameters(camera:Camera?,context: Context){
       var sizeOfPicture:Camera.Size?=null
        val mCamera:Camera=camera!!
        val autoFocusExecutor= ScheduledThreadPoolExecutor(1)
        try{
        mCamera?.apply {
            try{
                val params:Camera.Parameters=parameters
                val sizes: MutableList<Camera.Size>? =params.supportedPreviewSizes
                var size:Camera.Size?=null
                val aspectRatio:Double=1.77
                try{
                    for(element in sizes!!){
                        val aspectRatio:Double=(element.width).toDouble()/element.height.toDouble()
                        if(aspectRatio-2<=0.1&&aspectRatio-2>=0){
                            if(element.width>=1600){
                                size=element}
                        }
                        else if(aspectRatio-1.77<=0.1&&aspectRatio-1.77>=0){
                            if(element.width>=1600){
                                size=element}
                        }
                        else {
                        }
                    }
                } catch (e: Exception){
                    Log.d(tag,"$e")
                    size= sizes!!.get(1)
                }
                sizeOfPicture=size
                if(context.resources.configuration.orientation!= Configuration.ORIENTATION_LANDSCAPE){
                    params.set("orientation","portrait")
                    mCamera.setDisplayOrientation(90)
                    params.setRotation(90)
                }
                else{
                    params.set("orientation","landscape")
                    mCamera.setDisplayOrientation(0)
                    params.setRotation(0)
                }
                params.setPreviewSize(1280,720)
                params.setPictureSize(1280,720)
                params.setPreviewFpsRange(8000,10000)
                if(params.isAutoExposureLockSupported){
                    params.autoExposureLock=true
                }
                if(params.isAutoExposureLockSupported){
                    params.autoExposureLock=true
                }
                try {
                    params.focusMode=Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                }catch (e: Exception){
                    params.focusMode=Camera.Parameters.FOCUS_MODE_AUTO
                }
                try {
                    params.flashMode=Camera.Parameters.FLASH_MODE_TORCH
                }
                catch (e: Exception){
                    try {
                        params.flashMode=Camera.Parameters.FLASH_MODE_ON
                    }
                    catch (e: Exception){
                        Log.d(tag,"$e")
                    }
                }
                autoFocusExecutor.schedule({
                    if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    }
                },1000, TimeUnit.MILLISECONDS)
                parameters=params
            }catch (e: Exception){
                Log.d(tag,"$e")
            }
        }}
        catch (e:Exception){
            Log.d(tag,"$e")
        }
    }
    fun takePicture(it: View,mCamera:Camera,customFolderName: String?,waterMark: Boolean,context: Context?,path: File?,view:Int?) {
        it.visibility= View.GONE
        var pathToLocation:File?=null
        val mPicture:Camera.PictureCallback=object:Camera.PictureCallback{
            override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
                // Method to  click picture
                // starting preview of camera
                camera!!.stopPreview()
                  pathToLocation=getOutputMediaFile(1,customFolderName)
                //  val mSupportedPreviewSizes: MutableList<Camera.Size>? =camera!!.parameters.supportedPreviewSizes// getting supported preview size for a device
                val params:Camera.Parameters=camera!!.parameters // camera parameters variable
                params.setPictureSize(1280,720) // setting appropriate size of picture
                camera.parameters=params // setting params as parameters of camera
                camera.startPreview()
                val outStream: FileOutputStream = FileOutputStream(pathToLocation)// getting fileoutputstream to save image
                outStream.write(data) // writing data
                outStream.close() // closing stream
                outStream.flush()
                it.visibility=View.VISIBLE
                // setting view visibility visible
                if(waterMark==true){
                    ApplyWatermark(pathToLocation.toString(),context!!,view!!,customFolderName,path)
                }
            }
        }
        mCamera?.let {
            try {
                it.startPreview() // starting camera preview
                //// val params:Camera.Parameters=it.parameters // camera parameters variable
                // params.setPictureSize(1280,720) // setting appropriate size of picture
                //it.parameters=params // setting params as parameters of camera
                it.takePicture(null, null, null, mPicture)
                // calling takePicture Method
            }catch (e:Exception){
                Log.d(tag,"$e") // sending exception to logcat
            }

        }
      //  return pathToLocation!!
    }
     fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type,""))
    }
    /** Create a File for saving an image or video */
     fun getOutputMediaFile(type: Int,customFolderName: String?): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        var defaultFolder:String?=null
        if(customFolderName=="") {
            defaultFolder="MYAPPLICATION"
        }
        var mediaStorageDir:File
        if(customFolderName==""){
            mediaStorageDir= File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                defaultFolder
            )
        }
        else{
       mediaStorageDir= File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            customFolderName
        )}
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(tag, "failed to create directory")
                    return null
                }
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            mediaTypeImage -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.png")
            }
            mediaTypeVideo -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
    }
    fun setUpMediaRecorder(mMediaRecorder: MediaRecorder,pathToLocation:File){
        try {

             // media recorder

            mMediaRecorder?.run {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setVideoSize(1280, 720)
                setVideoEncodingBitRate(encodingBitRate)
                setVideoFrameRate(30)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOutputFile(pathToLocation.toString())
                try{
                    prepare()
                }
                catch (e:java.lang.Exception){
                    Log.d("Media recorder","$e")
                }
            }

        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }
  fun createVirtualDisplay(mMediaProjection:MediaProjection,tag:String,width:Int,height:Int,mScreenDensity:Int,mMediaRecorder: MediaRecorder):VirtualDisplay{
      return mMediaProjection.createVirtualDisplay(tag,width,height,mScreenDensity,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mMediaRecorder.surface,null,null)
  }

    fun releaseMediaRecorder(mMediaRecorder: MediaRecorder?){
        mMediaRecorder?.reset()
        mMediaRecorder?.release()

    }
    fun releaseCamera(mCamera: Camera){
        mCamera.release()
    }
    fun checkPermissions(context: Context,permissionArray: Array<String>):Boolean{
        if(context!=null&& permissionArray!=null){
        for(permission in permissionArray){
          if(ActivityCompat.checkSelfPermission(context,permission)!=PackageManager.PERMISSION_GRANTED){
            return false
        }}
    }
        return true
}
    fun genWaterMark(src: Bitmap, mContext:Context, view: Int): Bitmap {
        val waterMark: WaterMark = WaterMark(mContext)
        return waterMark.getImageWaterMarkFromView(src,view)!!
    }
    fun getBitmap(path:String):Bitmap{
        var bitmap:Bitmap?=null
        val file:File= File(path)
        val options: BitmapFactory.Options= BitmapFactory.Options()
        options.inPreferredConfig=Bitmap.Config.ARGB_8888
        bitmap= BitmapFactory.decodeStream(FileInputStream(file),null,options)
        return bitmap!!
    }
    fun ApplyWatermark(pathToLocation:String,context: Context,view: Int,customFolderName: String?,path: File?){

        val bitmap:Bitmap=getBitmap(pathToLocation)
        val result:Bitmap=genWaterMark(bitmap,context,view)
        storeImage(result,customFolderName,path)
    }
    fun storeImage(image:Bitmap,customFolderName:String?,path: File?){
        var fileOutputStream:FileOutputStream= FileOutputStream(path)
        image.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream)
        fileOutputStream.close()
    }
    fun actionBtnReload(btn_action:Button,isRecording:Boolean) {
        if (isRecording)
        { btn_action!!.text = "Stop Recording"
        }
        else
        { btn_action!!.text = "Start Recording"
        }
    }
    fun actionBtnImageReload(btn_action:ImageButton,isRecording: Boolean,context: Context,stopDrawable:Int,startDrawable:Int) {
        if (isRecording) { btn_action!!.setImageDrawable(context.resources.getDrawable(stopDrawable))
        }
        else { btn_action!!.setImageDrawable(context.resources.getDrawable(startDrawable))
        }
    }
     fun destroyMediaProjection(mMediaProjection: MediaProjection,mMediaProjectionCallback: MediaProjectionCallback) {
        if (mMediaProjection != null)
        {   mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
        }
        Log.i(tag, "MediaProjection Stopped")
    }
     fun stopScreenSharing(mVirtualDisplay:VirtualDisplay,mMediaProjection: MediaProjection,mMediaProjectionCallback: MediaProjectionCallback) {
        if (mVirtualDisplay == null)
        { return
        }
        mVirtualDisplay!!.release()
        destroyMediaProjection(mMediaProjection!!,mMediaProjectionCallback!!)

    }
     fun shareScreen(mMediaProjection: MediaProjection,mProjectionManager: MediaProjectionManager?,mVirtualDisplay: VirtualDisplay?,mScreenDensity: Int?,mMediaRecorder: MediaRecorder?) {
        if (mMediaProjection == null)
        { startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), requestCode) // creating screen capture intent
            return
        }
        var virtualDisplay:VirtualDisplay?=mVirtualDisplay
        virtualDisplay = createVirtualDisplay(mMediaProjection!!,"MAIN",1280,720,mScreenDensity!!,mMediaRecorder!!) // creating virtual display
        mMediaRecorder!!.start() // start media recorder
    }
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        if (requestCode != requestCode)
        {
            return
        }
        if (resultCode != Activity.RESULT_OK)
        {   Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            isRecording = false
            recorder.actionBtnReload(btn_action!!,isRecording)
            return
        }
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = recorder.createVirtualDisplay(mMediaProjection!!,"MAIN",1280,720,mScreenDensity,mMediaRecorder!!)
        mMediaRecorder!!.start()
        isRecording = true
        recorder.actionBtnReload(btn_action!!,isRecording)
    }
    fun stopRecording(mMediaRecorder: MediaRecorder,mVirtualDisplay: VirtualDisplay,mMediaProjection: MediaProjection,mMediaProjectionCallback: MediaProjectionCallback){
        recorder.releaseMediaRecorder(mMediaRecorder)
        recorder.stopScreenSharing(mVirtualDisplay!!,mMediaProjection!!,mMediaProjectionCallback!!) // stop screen sharing
        isRecording = false
        recorder.actionBtnReload(btn_action!!,isRecording)
    }
    fun activityResult(resultCode: Int,data: Intent?){
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = recorder.createVirtualDisplay(mMediaProjection!!,"MAIN",1280,720,mScreenDensity,mMediaRecorder!!)
        mMediaRecorder!!.start()
        isRecording = true
        recorder.actionBtnReload(btn_action!!,isRecording)
    }
}