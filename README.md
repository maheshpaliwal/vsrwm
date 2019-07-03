# WaterMark-ScreenRecorder
[![](https://jitpack.io/v/maheshpaliwal/vsrwm.svg)](https://jitpack.io/#maheshpaliwal/vsrwm)
### Android library for screen recording, video recording, capturing snapshots while recording, watermark and video compression while recording.
##To get a Git project into your build:

### Step 1. Add the JitPack repository to your build file
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
### Step 2. Add the dependency
```
dependencies {
	        implementation 'com.github.maheshpaliwal:vsrwm:1.0'
	}
```
## Why this library ?
#### You can use default camera which records video with lower file size
#### You can capture images in just a few lines of code
#### You can record screen in just few lines of code
#### You can apply Waterark on Images 
#### If you want to record video in lower size while maintaining quality of video then this is the best library for you
#### Check permissions
#### Save image from bitmap
#### Get Bitmp from image

# Usage
## By using default camera of library
### Pass your logo, folder name and activity(where this library will post data after recording)
```
// call default activity from library 
 val intent:Intent=Intent(this@MainActivity,ScreenRecording::class.java)
                intent.putExtra("class","com.example.test.WaterMarkActivity")// class where library will redirectafter screen recording or capturing images
                intent.putExtra("logo",R.drawable.pb) // add your custom logo
                intent.putExtra("customFolderName","MYAPP") // YOUR FOLDERNAME where file will be saved
                startActivity(intent)
// do not forget to recieve data in another activity

```
### Recieve data

```
  val pathToVideo = intent.getStringExtra("videopath") // path to saved video
   val images = intent.getStringArrayListExtra("images") // array containing paths of images
```
## If you want to customize everything 
### Check permissions
```
//checkPermissions(context: Context,permissionArray: Array<String>)
var recorder:Recorder=Recorder()
val permissions= arrayOf<String>(android.Manifest.permission.CAMERA,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,android.Manifest.permission.WAKE_LOCK,android.Manifest.permission.INTERNET,android.Manifest.permission.ACCESS_NETWORK_STATE)
recorder.checkPermissions(this,permissions)
```
### An example to use library for capturing images
``` mCamera = Camera.open() //opening camera
    val recorder:Recorder= Recorder() //creating object of recorder class
    val cameraSurface:CameraSurface= CameraSurface(this,mCamera!!) // object of camera surface class
    val customLayout: FrameLayout = this.findViewById<FrameLayout>(R.id.myCameraLayout) // Importing camera layout
    customLayout.addView(cameraSurface) // putting camera surface view into customLayout
    recorder.setCameraParameters(mCamera,this) // setting up camera parameters
    snapshot.setOnClickListener {
        // give your custom folder or leave it blank to use default settings
            recorder.takePicture(it,mCamera!!,"MYAPP",false,null,null,null) // call take picture function of recorder class
        }
```
### Apply Watermark on Images from layout
```
val recorder:Recorder= Recorder()
snapshot.setOnClickListener {
        // give your custom folder or leave it blank to use default settings 
        // takePicture(it: View,mCamera:Camera,customFolderName: String?,waterMark: Boolean,context: Context?,path: File?,view:Int?)
            recorder.takePicture(it,mCamera!!,"MYAPP",true,this,pathToLocation,R.layout.watermark) // call take picture function of recorder class
        }
```
### Apply watermark on image from layout (using path of image)
```
///ApplyWatermark(pathToLocation:String,context: Context,view: Int,customFolderName: String?,path: File?)
val recorder:Recorder= Recorder()
val pathToLocation:File?=recorder.getOutputMediaFile(1,"MYAPP") // path to save image after applying watermark
recorder.ApplyWatermark(pathToFile,context,R.layout.watermark,myCustomFolder,pathToLocation) //  call apply watermark function
```
### Save image from Bitmap
```
//storeImage(image:Bitmap,customFolderName:String?,path: File?)
val recorder:Recorder= Recorder()
val pathToLocation:File?=recorder.getOutputMediaFile(1,"MYAPP")
recorder.storeImage(bitmap,"MYAPP",pathToLocation)
```
### Get Bitmap from path of an image
```
// getBitmap(path:String):Bitmap
val recorder:Recorder= Recorder()
val bitmap:Bitmap=recorder.getBitmap(pathToFile)
```
### An example to record screen
```windowManager.defaultDisplay.getMetrics(Recorder.metrics) // getting display metrics in metrics variable
        mScreenDensity = Recorder.metrics.densityDpi  // density of screen
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        btn_action=findViewById<Button>(R.id.captureScreen)
        btn_action?.setOnClickListener{
            onToggleScreenShare() }
  fun onToggleScreenShare() {
        if (!isRecording) { val pathToLocation: File? =recorder.getOutputMediaFile(2,"MYAPP")
            mMediaRecorder=MediaRecorder()
            recorder.setUpMediaRecorder(mMediaRecorder!!,pathToLocation!!) // setting up media recorder
            if (mMediaProjection == null) { startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), requestCode)// creating screen capture intent
                return }
            mVirtualDisplay = recorder.createVirtualDisplay(mMediaProjection!!,"MAIN",1280,720,mScreenDensity,mMediaRecorder!!) // creating virtual display
            mMediaRecorder!!.start() // start media recorder
            recorder.actionBtnReload(btn_action!!,isRecording)
            isRecording = true }
        else { recorder.stopRecording(mMediaRecorder!!, mVirtualDisplay!!, mMediaProjection!!, mMediaProjectionCallback!!) } }
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        if (requestCode != constants.requestCode)
        { return
        }
        if (resultCode != Activity.RESULT_OK)
        {   Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            isRecording = false
            recorder.actionBtnReload(btn_action!!,isRecording)
            return
        }
        recorder.activityResult(resultCode,data)
    }
    public override fun onDestroy() { super.onDestroy()
        recorder.destroyMediaProjection(mMediaProjection!!,mMediaProjectionCallback!!) }
````
