package com.hackfest.greylock.musicasleep;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.view.View;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class OpenCVActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new myView(this));
    }

    private class myView extends View {

        private int imageWidth, imageHeight;
        private int numberOfFace = 1;
        private FaceDetector myFaceDetect;
        private FaceDetector.Face[] myFace;
        float myEyesDistance;
        int numberOfFaceDetected;

        Bitmap myBitmap;

        public myView(Context context) {
            super(context);

            BitmapFactory.Options BitmapFactoryOptionsbfo = new BitmapFactory.Options();
            BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565;
            myBitmap = BitmapFactory.decodeResource(getResources(),
                    R.raw.closed2, BitmapFactoryOptionsbfo);
            imageWidth = myBitmap.getWidth();
            imageHeight = myBitmap.getHeight();
            myFace = new FaceDetector.Face[numberOfFace];
            myFaceDetect = new FaceDetector(imageWidth, imageHeight,
                    numberOfFace);
            numberOfFaceDetected = myFaceDetect.findFaces(myBitmap, myFace);

        }

        @Override
        protected void onDraw(Canvas canvas) {

            canvas.drawBitmap(myBitmap, 0, 0, null);

            Paint myPaint = new Paint();
            myPaint.setColor(Color.GREEN);
            myPaint.setStyle(Paint.Style.STROKE);
            myPaint.setStrokeWidth(3);

            Face face = myFace[0];
            PointF myMidPoint = new PointF();
            face.getMidPoint(myMidPoint);
            myEyesDistance = face.eyesDistance();

            canvas.drawRect((int) (myMidPoint.x - myEyesDistance),
                    (int) (myMidPoint.y - (myEyesDistance/3.25)),
                    (int) (myMidPoint.x - myEyesDistance + (myEyesDistance/1.25)),
                    (int) (myMidPoint.y + (myEyesDistance/3.25)), myPaint);

            float whiteCount = 0;
            float totalCount = 0;
            for (int x = (int)(myMidPoint.x - myEyesDistance); x <= (myMidPoint.x - myEyesDistance + (myEyesDistance/1.25)); x++) {
                for (int y = (int)(myMidPoint.y - (myEyesDistance/3.25)); y <= (myMidPoint.y + (myEyesDistance/3.25)); y++) {
                    int full_color = myBitmap.getPixel(x, y);
                    if (Color.red(full_color) < 50 && Color.green(full_color) < 50 && Color.blue(full_color) < 50) {
                        whiteCount++;
                    }
                    totalCount++;
                }
            }
            float whitePercent = whiteCount / totalCount;
            System.out.println("z_whitePercent: " + whitePercent);
//            JL = .10327869
//            Face = .03178808
//            Closed = 0
//            Closed2 = .008506032

            canvas.drawRect((int) (myMidPoint.x + myEyesDistance - (myEyesDistance/1.25)),
                    (int) (myMidPoint.y - (myEyesDistance/3.25)),
                    (int) (myMidPoint.x + myEyesDistance),
                    (int) (myMidPoint.y + (myEyesDistance/3.25)), myPaint);
        }
    }

}
