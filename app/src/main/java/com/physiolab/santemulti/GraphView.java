package com.physiolab.santemulti;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphView  extends SurfaceView implements SurfaceHolder.Callback  {
    private static final String TAG = "SurfaceView-Graph";
    private static final boolean D = false;

    private GraphViewThread gvThread;
    private SurfaceHolder mHolder;
    private Bitmap bufferGraph=null;
    private Canvas bufferCanvas=null;
    private Bitmap gridGraph=null;
    private Canvas gridCanvas=null;

    private boolean isRedraw=false;

    private RectF area = new RectF();

    private Paint linePnt;
    private Paint gridPnt;

    private float[] data = new float[60*5];
    private int cnt=0;
    private boolean isReverse = false;

    public GraphView(Context context) {
        super(context);
        Init();
    }

    public GraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        Init();
    }

    public GraphView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        Init();
    }

    private void Init()
    {
        mHolder = getHolder();
        mHolder.addCallback(this);

        gridPnt = new Paint();
        gridPnt.setColor(Color.argb(255,200,200,200));
        gridPnt.setStyle(Paint.Style.STROKE);
        gridPnt.setStrokeWidth(2);

        linePnt = new Paint();
        linePnt.setColor(Color.argb(255,250,0,0));
        linePnt.setStyle(Paint.Style.STROKE);
        linePnt.setStrokeWidth(1);

        cnt=0;
        isReverse = false;
    }

    public void finalize()
    {
        if (bufferGraph!=null)
        {
            try {
                bufferGraph.recycle();
                gridGraph.recycle();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gvThread = new GraphViewThread(getHolder(), this);
        gvThread.setRunning(true);
        gvThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (bufferGraph==null)
        {
            bufferGraph = Bitmap.createBitmap(width ,height ,Bitmap.Config.RGB_565);
            bufferCanvas = new Canvas(bufferGraph);
            gridGraph = Bitmap.createBitmap(width ,height ,Bitmap.Config.RGB_565);
            gridCanvas = new Canvas(gridGraph);
            Refresh();
        }
        else if(bufferGraph.getWidth()!=width || bufferGraph.getHeight()!=height)
        {
            try {
                bufferGraph.recycle();
                gridGraph.recycle();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            bufferGraph = Bitmap.createBitmap(width ,height ,Bitmap.Config.RGB_565);
            bufferCanvas = new Canvas(bufferGraph);
            gridGraph = Bitmap.createBitmap(width ,height ,Bitmap.Config.RGB_565);
            gridCanvas = new Canvas(gridGraph);
            Refresh();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        gvThread.setRunning(false);
        while(retry)
        {
            try
            {
                gvThread.join();
                retry=false;
            }
            catch (InterruptedException e)
            {

            }
        }
    }

    public void Clear()
    {
        cnt=0;
        isReverse=false;

        Refresh();
    }

    public void Add(float rate)
    {
        data[cnt++] = rate;

        synchronized (bufferCanvas) {
            float xPos1 = 0.0f, yPos1 = 0.0f;
            float xPos2 = 0.0f, yPos2 = 0.0f;

            if (cnt>=2)
            {
                yPos1 = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[cnt-2])/100.0f);
                xPos1 = area.left +2.0f+ (area.width()-3.0f) * ((float)(cnt-2)/300.0f);

                yPos2 = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[cnt-1])/100.0f);
                xPos2 = area.left +2.0f+ (area.width()-3.0f) * ((float)(cnt-1)/300.0f);

                if (true)
                {
                    Rect rect = new Rect((int)xPos1+1,(int)area.top,(int)xPos2+20,(int)area.bottom);
                    bufferCanvas.drawBitmap(gridGraph,rect,rect,null);
                }

                bufferCanvas.drawLine(xPos1,yPos1,xPos2,yPos2,linePnt);
            }

        }

        if (cnt==300)
        {
            cnt=0;
            isReverse=true;
        }
    }

    public void Refresh()
    {
        CalcArea();
        isRedraw = true;
    }

    private void CalcArea()
    {
        if (bufferCanvas!=null)
        {
            int H = bufferCanvas.getHeight();
            int W = bufferCanvas.getWidth();

            int margin=20;

            area = new RectF(margin,margin,W-margin,H-margin);
        }
    }

    private void Redraw() {
        if (D) Log.d(TAG, "Redraw Start");

        synchronized (bufferCanvas) {
            gridCanvas.drawColor(Color.rgb(255,255,255));
            gridCanvas.drawRect(area,gridPnt);
            for(int i=1;i<5;i++) gridCanvas.drawLine((area.width()*i)/5+area.left,area.top,(area.width()*i)/5+area.left,area.bottom,gridPnt);
            for(int i=1;i<4;i++) gridCanvas.drawLine(area.left,(area.height()*i)/4+area.top,area.right,(area.height()*i)/4+area.top,gridPnt);

            bufferCanvas.drawBitmap(gridGraph,0,0,null);

            float xPos = 0.0f, yPos = 0.0f;
            Path path = new Path();
            path.reset();

            yPos = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[0])/100.0f);
            xPos = area.left +2.0f+ (area.width()-3.0f) * (0.0f/300.0f);
            path.moveTo(xPos,yPos);
            for(int i=1;i<cnt;i++)
            {
                yPos = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[i])/100.0f);
                xPos = area.left +2.0f+ (area.width()-3.0f) * ((float)i/300.0f);
                path.lineTo(xPos,yPos);
            }
            bufferCanvas.drawPath(path,linePnt);

            if (isReverse && cnt+3<300)
            {
                path.reset();

                yPos = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[cnt+2])/100.0f);
                xPos = area.left +2.0f+ (area.width()-3.0f) * ((float)(cnt+2)/300.0f);
                path.moveTo(xPos,yPos);
                for(int i=cnt+3;i<5*60;i++)
                {
                    yPos = area.top +2.0f+ (area.height()-3.0f) * ((100.0f-data[i])/100.0f);
                    xPos = area.left +2.0f+ (area.width()-3.0f) * ((float)i/300.0f);
                    path.lineTo(xPos,yPos);
                }
            }
            bufferCanvas.drawPath(path,linePnt);

        }

        if (D) Log.d(TAG, "Redraw Stop");
    }

    class GraphViewThread extends Thread
    {
        private SurfaceHolder sHolder;
        private boolean running = false;

        public GraphViewThread(SurfaceHolder h, GraphView v)
        {
            sHolder = h;
            if (D) Log.d(TAG, "thread constructor");
        }

        public void setRunning(boolean run)
        {
            running=run;
        }

        @Override
        public void run()
        {
            Canvas c;

            while(running)
            {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                c=null;

                if (sHolder.getSurface().isValid()) {
                    c = sHolder.lockCanvas();
                }

                if(c==null) continue;

                try
                {
                    synchronized (sHolder)
                    {
                        if (c != null && c.getWidth() > 0 && c.getHeight() > 0) {
                            if (isRedraw)
                            {
                                Redraw();
                                isRedraw=false;
                            }

                            if (bufferGraph!=null)
                            {
                                synchronized (bufferCanvas) {
                                    c.drawBitmap(bufferGraph,0,0,null);
                                }
                            }
                        }
                    }

                }
                finally
                {
                    if (sHolder.getSurface().isValid())
                    {
                        sHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }
}
