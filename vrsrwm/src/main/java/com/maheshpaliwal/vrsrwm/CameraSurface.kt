package com.maheshpaliwal.vrsrwm

import android.content.Context
import android.graphics.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import java.lang.Exception

class CameraSurface(context:Context,camera: android.hardware.Camera):SurfaceView(context),SurfaceHolder.Callback{
    private val mCamera=camera // initialize camera
    // surface holder
    val holderCamera:SurfaceHolder=holder.apply {
        addCallback(this@CameraSurface)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        val recorder:Recorder= Recorder()
        recorder.setCameraParameters(mCamera,context)// setting up camera parameters
        mCamera?.apply {
            setPreviewDisplay(holderCamera) // setting preview display
            startPreview() // start camera preview
        }
    }
    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }
    override fun surfaceCreated(holder: SurfaceHolder?) {
        val params: android.hardware.Camera.Parameters=mCamera.parameters
        mCamera.parameters=params
        if(holder!!.surface==null){
            return
        }
        try {
            mCamera.stopPreview()
        }
        catch (e: Exception){
            Toast.makeText(context,"$e", Toast.LENGTH_LONG).show()
        }
        mCamera.apply {
            setPreviewDisplay(holder)
            startPreview()
        }
    }
}