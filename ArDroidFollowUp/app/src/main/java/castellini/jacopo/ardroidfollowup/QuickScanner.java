package castellini.jacopo.ardroidfollowup;

import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuickScanner {

    private Mat mImage;
    private Mat cornersImage;

    private Mat descriptorsImage;
    private FeatureDetector featureDetector;
    private MatOfKeyPoint keypointsImage;
    private DescriptorExtractor descriptorExtractor;
    private DescriptorMatcher descriptorMatcher;

    public QuickScanner(Context ctx) {
        try {
            mImage = Utils.loadResource(ctx, R.drawable.unipg);
            Imgproc.cvtColor(mImage, mImage, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(mImage, mImage, new Size(3, 3), 2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        keypointsImage = new MatOfKeyPoint();

        featureDetector = FeatureDetector.create(FeatureDetector.BRISK);
        featureDetector.detect(mImage, keypointsImage);

        descriptorsImage = new Mat();

        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.FREAK);
        descriptorExtractor.compute(mImage, keypointsImage, descriptorsImage);

        descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        cornersImage = new Mat(4, 1, CvType.CV_32FC2);
        cornersImage.put(0, 0, new double[]{0, 0});
        cornersImage.put(1, 0, new double[]{mImage.cols(), 0});
        cornersImage.put(2, 0, new double[]{mImage.cols(), mImage.rows()});
        cornersImage.put(3, 0, new double[]{0, mImage.rows()});
    }

    public String doTheWork(Mat mRgba) {
        Mat mGray = new Mat();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        MatOfKeyPoint frameKeypoints = new MatOfKeyPoint();
        Mat frameDescriptors = new Mat();

        featureDetector.detect(mGray, frameKeypoints);
        descriptorExtractor.compute(mGray, frameKeypoints, frameDescriptors);

        if (keypointsImage.size().height >= 2 && frameKeypoints.size().height >= 2) {
            MatOfDMatch matches = new MatOfDMatch();
            descriptorMatcher.match(frameDescriptors, descriptorsImage, matches);

            List<DMatch> matchesList = matches.toList();

            if (matchesList.size() >= 4) {

                List<KeyPoint> objList = keypointsImage.toList();
                List<KeyPoint> frameList = frameKeypoints.toList();

                double maxDist = Double.MIN_VALUE;
                double minDist = Double.MAX_VALUE;
                for (DMatch match : matchesList) {
                    double dist = match.distance;
                    if (dist < minDist) {
                        minDist = dist;
                    }
                    if (dist > maxDist) {
                        maxDist = dist;
                    }
                }
                if (minDist > 40.0) {
                    return "stop";
                }

                ArrayList<Point> objAux = new ArrayList<Point>();
                ArrayList<Point> frameAux = new ArrayList<Point>();

                double maxGoodMatchDist = 1.75 * minDist;
                for (DMatch match : matchesList) {
                    if (match.distance < maxGoodMatchDist) {
                        objAux.add(objList.get(match.trainIdx).pt);
                        frameAux.add(frameList.get(match.queryIdx).pt);
                    }
                }

                if (objAux.size() < 4 || frameAux.size() < 4) {
                    return "stop";
                }

                MatOfPoint2f objMat = new MatOfPoint2f();
                objMat.fromList(objAux);
                MatOfPoint2f frameMat = new MatOfPoint2f();
                frameMat.fromList(frameAux);
                Mat homography = Calib3d.findHomography(objMat, frameMat, Calib3d.RANSAC, 5);

                Mat frameCorners = new Mat(4, 1, CvType.CV_32FC2);
                Core.perspectiveTransform(cornersImage, frameCorners, homography);

                /*Core.line(mRgba, new Point(frameCorners.get(0, 0)), new Point(frameCorners.get(1, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(1, 0)), new Point(frameCorners.get(2, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(2, 0)), new Point(frameCorners.get(3, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(3, 0)), new Point(frameCorners.get(0, 0)), new Scalar(0, 255, 0), 3);*/

                Point temp = new Point(frameCorners.get(0, 0));
                int maxX = (int) temp.x;
                int maxY = (int) temp.y;
                int minX = (int) temp.x;
                int minY = (int) temp.y;

                for (int i = 1; i < 4; i++) {
                    temp = new Point(frameCorners.get(i, 0));
                    if (temp.x > maxX)
                        maxX = (int) temp.x;
                    else if (temp.x < minX)
                        minX = (int) temp.x;
                    if (temp.y > maxY)
                        maxY = (int) temp.y;
                    else if (temp.y < minY)
                        minY = (int) temp.y;
                }

                Size frameSize = mGray.size();
                Rect rect = new Rect(new Point(minX, minY), new Point(maxX, maxY));

                Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0), 3);

                if (rect.tl().x + rect.width / 2 < frameSize.width / 3)
                    return "left";
                else if (rect.tl().x + rect.width / 2 > 2 * frameSize.width / 3)
                    return "right";
                else {
                    if (rect.height * rect.width < 210 * 210)
                        return "forward";
                    else if (rect.height * rect.width > 240 * 240)
                        return "backward";
                    else
                        return "stop";
                }
            } else
                return "stop";
        } else
            return "stop";
    }
}