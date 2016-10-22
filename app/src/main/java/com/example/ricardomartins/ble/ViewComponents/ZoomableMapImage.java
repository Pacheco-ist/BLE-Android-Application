package com.example.ricardomartins.ble.ViewComponents;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import com.example.ricardomartins.ble.R;

public class ZoomableMapImage extends ImageView
{
    private static final String TAG = "MapView";


    Matrix matrix = new Matrix();

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int CLICK = 3;
    int mode = NONE;

    PointF last = new PointF();
    PointF start = new PointF();
    float minScale = 1f;
    float maxScale = 4f;
    float[] m;

    float redundantXSpace, redundantYSpace;
    float width, height;
    float saveScale = 1f;
    float right, bottom, origWidth, origHeight, bmWidth, bmHeight, SWidth, SHeight ;

    ScaleGestureDetector mScaleDetector;
    Context context;

    double xcoor, ycoor;

    public ZoomableMapImage(Context context, AttributeSet attr)
    {
        super(context, attr);
        super.setClickable(false);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener()
        {

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                mScaleDetector.onTouchEvent(event);

                matrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction())
                {
                    //when one finger is touching
                    //set the mode to DRAG
                    case MotionEvent.ACTION_DOWN:
                        last.set(event.getX(), event.getY());
                        start.set(last);
                        mode = DRAG;
                        break;
                    //when two fingers are touching
                    //set the mode to ZOOM
                    case MotionEvent.ACTION_POINTER_DOWN:
                        last.set(event.getX(), event.getY());
                        start.set(last);
                        mode = ZOOM;
                        break;
                    //when a finger moves
                    //If mode is applicable move image
                    case MotionEvent.ACTION_MOVE:
                        //if the mode is ZOOM or
                        //if the mode is DRAG and already zoomed
                        if (mode == ZOOM || (mode == DRAG && saveScale >= minScale))
                        {
                            float deltaX = curr.x - last.x;// x difference
                            float deltaY = curr.y - last.y;// y difference
                            float scaleWidth = Math.round(origWidth * saveScale);// width after applying current scale
                            float scaleHeight = Math.round(origHeight * saveScale);// height after applying current scale
                            //if scaleWidth is smaller than the views width
                            //in other words if the image width fits in the view
                            //limit left and right movement
                            if (scaleWidth < width)
                            {
                                deltaX = 0;
                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }
                            //if scaleHeight is smaller than the views height
                            //in other words if the image height fits in the view
                            //limit up and down movement
                            else if (scaleHeight < height)
                            {
                                deltaY = 0;
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);
                            }
                            //if the image doesnt fit in the width or height
                            //limit both up and down and left and right
                            else
                            {
                                if (x + deltaX > 0)
                                    deltaX = -x;
                                else if (x + deltaX < -right)
                                    deltaX = -(x + right);

                                if (y + deltaY > 0)
                                    deltaY = -y;
                                else if (y + deltaY < -bottom)
                                    deltaY = -(y + bottom);
                            }
                            //move the image with the matrix
                            matrix.postTranslate(deltaX, deltaY);
                            //set the last touch location to the current
                            last.set(curr.x, curr.y);
                        }
                        break;
                    //first finger is lifted
                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK)
                            performClick();
                        break;
                    // second finger is lifted
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }
                setImageMatrix(matrix);
                invalidate();
                return true;
            }

        });
    }

    @Override
    public void setImageBitmap(Bitmap bm)
    {
        super.setImageBitmap(bm);
        super.setClickable(true);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();
        SHeight = this.getHeight();
        SWidth = this.getWidth();
        Log.i(TAG,bmWidth + "-> " + bmHeight );
        Log.i(TAG,SWidth + "-> " + SHeight );


    }

    public void UpdateMap(Bitmap map, double xcoor, double ycoor){
        SHeight = this.getHeight();
        SWidth = this.getWidth();
        this.ycoor=ycoor;
        this.xcoor=xcoor;
        CreateBitmap(map);
    }

    private void CreateBitmap(Bitmap map){

        double ratio = ((map.getWidth()*1.0)/map.getHeight());

        Log.i(TAG, map.getWidth() + "/" +map.getHeight() + "-> ratio:" + ratio);
        Log.i(TAG,SWidth + "-> " + SHeight );
        Log.i(TAG,(SHeight*ratio) + "-> " + SHeight );


        int center_x = (int)(SHeight*ratio*xcoor);
        int center_y = (int)(SHeight*ycoor);


        Log.i(TAG,center_x + "-> " + center_y );




        map = Bitmap.createScaledBitmap(map, (int)(SHeight*ratio), (int) SHeight, false );
        Canvas canvas = new Canvas(map);
        Resources res = context.getResources();

        Bitmap indicator = BitmapFactory.decodeResource(res , R.drawable.place1);
        Drawable drawable = new BitmapDrawable(res,indicator);

        drawable.setBounds((center_x-25), center_y-62, center_x+25, center_y+63);
        drawable.draw(canvas);


        this.setImageBitmap(Bitmap.createScaledBitmap(map, (int)(SHeight*ratio), (int)SHeight, false ));
    }

    public void setMaxZoom(float x)
    {
        maxScale = x;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale)
            {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            }
            else if (saveScale < minScale)
            {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }
            right = width * saveScale - width - (2 * redundantXSpace * saveScale);
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
            if (origWidth * saveScale <= width || origHeight * saveScale <= height)
            {
                matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
                if (mScaleFactor < 1)
                {
                    matrix.getValues(m);
                    float x = m[Matrix.MTRANS_X];
                    float y = m[Matrix.MTRANS_Y];
                    if (mScaleFactor < 1)
                    {
                        if (Math.round(origWidth * saveScale) < width)
                        {
                            if (y < -bottom)
                                matrix.postTranslate(0, -(y + bottom));
                            else if (y > 0)
                                matrix.postTranslate(0, -y);
                        }
                        else
                        {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0);
                            else if (x > 0)
                                matrix.postTranslate(-x, 0);
                        }
                    }
                }
            }
            else
            {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
                matrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                if (mScaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0);
                    else if (x > 0)
                        matrix.postTranslate(-x, 0);
                    if (y < -bottom)
                        matrix.postTranslate(0, -(y + bottom));
                    else if (y > 0)
                        matrix.postTranslate(0, -y);
                }
            }
            return true;
        }
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        Log.i(TAG, "i-> " + width + "/ "+height);
        //Fit to screen.
        float scale;
        float scaleX =  width / bmWidth;
        float scaleY = height / bmHeight;
        scale = scaleY; //Math.min(scaleX, scaleY);
        matrix.setScale(scale, scale);
        setImageMatrix(matrix);
        saveScale = 1f;

        // Center the image
        redundantYSpace = height - (scale * bmHeight) ;
        redundantXSpace = width - (scale * bmWidth);
        redundantYSpace /= 2;
        redundantXSpace /= 2;

        matrix.postTranslate(redundantXSpace, redundantYSpace);

        origWidth = width - 2 * redundantXSpace;
        origHeight = height - 2 * redundantYSpace;
        right = width * saveScale - width - (2 * redundantXSpace * saveScale);
        bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
        setImageMatrix(matrix);
    }
}
