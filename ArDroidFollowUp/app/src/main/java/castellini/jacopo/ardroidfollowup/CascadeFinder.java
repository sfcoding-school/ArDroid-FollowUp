package castellini.jacopo.ardroidfollowup;

import android.content.Context;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CascadeFinder {

    CascadeClassifier mCascadeClassifier;
    int mAbsoluteFaceSize = 0;

    public CascadeFinder(Context ctx) {
        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.unipg_cascade);
            File cascadeDir = ctx.getDir("cascade", Context.MODE_PRIVATE);
            File file_classifier = new File(cascadeDir, "unipg_cascade.xml");
            FileOutputStream os = new FileOutputStream(file_classifier);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            is.close();

            mCascadeClassifier = new CascadeClassifier(file_classifier.getAbsolutePath());
            if (mCascadeClassifier.empty())
                mCascadeClassifier = null;
            cascadeDir.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String doTheWork(Mat mRgba) {
        Mat mGray = new Mat();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0)
            mAbsoluteFaceSize = Math.round(mGray.rows() * 0.2f) > 0 ? Math.round(mGray.rows() * 0.2f) : 0;

        MatOfRect logos = new MatOfRect();
        if (mCascadeClassifier != null)
            mCascadeClassifier.detectMultiScale(mGray, logos, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        if (!(logos.empty())) {
            Rect[] facesArray = logos.toArray();

            Rect rect = new Rect(0, 0, 1, 1);
            for (Rect aFacesArray : facesArray) {
                if (aFacesArray.width * aFacesArray.height > rect.width * rect.height) {
                    rect = aFacesArray;
                }
            }

            double frameSize = mGray.size().width;

            Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 3);

            if (rect.tl().x + rect.width / 2 < frameSize / 3)
                return "left";
            else if (rect.tl().x + rect.width / 2 > 2 * frameSize / 3)
                return "right";
            else {
                if (rect.height * rect.width < (frameSize / 5) * (frameSize / 5))
                    return "forward";
                else if (rect.height * rect.width > (1.2 * frameSize / 5) * (1.2 * frameSize / 5))
                    return "backward";
                else
                    return "stop";
            }
        } else
            return "stop";
    }
}
