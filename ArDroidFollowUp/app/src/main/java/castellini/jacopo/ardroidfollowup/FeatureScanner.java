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
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeatureScanner {

    private Mat mImage;
    private Mat cornersImage;

    private Mat descriptorsImage;
    private FeatureDetector featureDetector;
    private MatOfKeyPoint keypointsImage;
    private DescriptorExtractor descriptorExtractor;
    private DescriptorMatcher descriptorMatcher;

    public FeatureScanner(Context ctx) {
        try {
            mImage = Utils.loadResource(ctx, R.drawable.unipg);
            Imgproc.cvtColor(mImage, mImage, Imgproc.COLOR_RGBA2GRAY);
        } catch (IOException e) {
            e.printStackTrace();
        }

        keypointsImage = new MatOfKeyPoint();

        featureDetector = FeatureDetector.create(FeatureDetector.ORB);
        featureDetector.detect(mImage, keypointsImage);

        descriptorsImage = new Mat();

        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        descriptorExtractor.compute(mImage, keypointsImage, descriptorsImage);

        descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        cornersImage = new Mat(4, 1, CvType.CV_32FC2);
        cornersImage.put(0, 0, new double[]{0, 0});
        cornersImage.put(1, 0, new double[]{mImage.cols(), 0});
        cornersImage.put(2, 0, new double[]{mImage.cols(), mImage.rows()});
        cornersImage.put(3, 0, new double[]{0, mImage.rows()});
    }

    public String doTheWork(Mat mRgba, Mat mGray) {
        MatOfKeyPoint frameKeypoints = new MatOfKeyPoint();
        Mat frameDescriptors = new Mat();

        featureDetector.detect(mGray, frameKeypoints);
        descriptorExtractor.compute(mGray, frameKeypoints, frameDescriptors);

        if (keypointsImage.size().height >= 2 && frameKeypoints.size().height >= 2) {
            ArrayList<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
            descriptorMatcher.knnMatch(descriptorsImage, frameDescriptors, matches, 2);

            ArrayList<List<DMatch>> matchesList = new ArrayList<List<DMatch>>();
            for (MatOfDMatch dm : matches)
                matchesList.add(dm.toList());

            ArrayList<DMatch> goodMatches = new ArrayList<DMatch>();

            for (int i = 0; i < Math.min(frameDescriptors.rows() - 1, matches.size()); i++) {
                if (matchesList.get(i).get(0).distance < 0.7 * matchesList.get(i).get(1).distance)
                    goodMatches.add(matchesList.get(i).get(0));
            }

            if (goodMatches.size() >= 10) {
                ArrayList<Point> objAux = new ArrayList<Point>();
                ArrayList<Point> frameAux = new ArrayList<Point>();
                List<KeyPoint> objList = keypointsImage.toList();
                List<KeyPoint> frameList = frameKeypoints.toList();

                for (int i = 0; i < goodMatches.size(); i++) {
                    objAux.add(objList.get(goodMatches.get(i).queryIdx).pt);
                    frameAux.add(frameList.get(goodMatches.get(i).trainIdx).pt);
                }

                MatOfPoint2f objMat = new MatOfPoint2f();
                objMat.fromList(objAux);
                MatOfPoint2f frameMat = new MatOfPoint2f();
                frameMat.fromList(frameAux);
                Mat homography = Calib3d.findHomography(objMat, frameMat, Calib3d.RANSAC, 6);

                Mat frameCorners = new Mat(4, 1, CvType.CV_32FC2);
                Core.perspectiveTransform(cornersImage, frameCorners, homography);

                Core.line(mRgba, new Point(frameCorners.get(0, 0)), new Point(frameCorners.get(1, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(1, 0)), new Point(frameCorners.get(2, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(2, 0)), new Point(frameCorners.get(3, 0)), new Scalar(0, 255, 0), 3);
                Core.line(mRgba, new Point(frameCorners.get(3, 0)), new Point(frameCorners.get(0, 0)), new Scalar(0, 255, 0), 3);

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

                double frameSize = mGray.size().width;
                Rect rect = new Rect(new Point(minX, minY), new Point(maxX, maxY));

                if (rect.tl().x + rect.width / 2 < frameSize / 3)
                    return "left";
                else if (rect.tl().x + rect.width / 2 > 2 * frameSize / 3)
                    return "right";
                else {
                    if (rect.height * rect.width < 155 * 155)
                        return "forward";
                    else if (rect.height * rect.width > 170 * 170)
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