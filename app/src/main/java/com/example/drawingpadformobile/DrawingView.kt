package com.example.drawingpadformobile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var mDrawingPath:  CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mCanvasPaint: Paint? = null
    private var color = Color.BLACK
    private var mDrawPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private var mUndoPaths = ArrayList<CustomPath>()
    init{
      setupDrawing()
    }
    fun OnclickUndo(){
        if(mPaths.size > 0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }
    fun OnclickRedo(){
        if(mUndoPaths.size > 0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }
    fun OnClickErase(){
        if(mPaths.size > 0){
            mUndoPaths = mPaths.clone() as ArrayList<CustomPath>
            mPaths.clear()
            invalidate()
        }
    }
    private fun setupDrawing() {
        mDrawPaint = Paint()
        mDrawingPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mCanvasBitmap!!, 0f,0f, mCanvasPaint)
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas?.drawPath(path, mDrawPaint!!)
        }
        if(!mDrawingPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawingPath!!.brushThickness
            mDrawPaint!!.color = mDrawPaint!!.color
            canvas?.drawPath(mDrawingPath!!, mDrawPaint!!)
        }
    }
    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val TouchX = event?.x
        val TouchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawingPath!!.color = color
                mDrawingPath!!.brushThickness = mBrushSize
                mDrawingPath!!.reset()
                if(TouchX != null) {
                    if(TouchY != null) {
                        mDrawingPath!!.moveTo(TouchX, TouchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if(TouchX != null) {
                    if (TouchY != null) {
                        mDrawingPath!!.lineTo(TouchX, TouchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawingPath!!)
                mDrawingPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
    }
    fun setSizeOfBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }
    internal inner class CustomPath(var color: Int, var brushThickness: Float): Path(){
    }
}