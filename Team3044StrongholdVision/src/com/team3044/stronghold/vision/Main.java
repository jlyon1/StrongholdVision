package com.team3044.stronghold.vision;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.team3044.stronghold.gui.ImageWindow;

import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Main {
	private BufferedImage myImage;

	final static String Address = "http://10.30.44.26/axis-cgi/mjpg/video.cgi?dummy=test.mjpeg";

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		VideoCapture v = new VideoCapture("http://10.30.44.26/axis-cgi/mjpg/video.cgi?test.mjpeg");
		v.open(Address);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (v.isOpened()) {
			System.out.println("opened");
		} else {
			System.out.println("not open");
		}
		Mat frame = new Mat();
		Mat blank = new Mat();
		Mat threshold = new Mat();
		Mat temp = new Mat();
		Mat orig = new Mat();
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

		ArrayList<Mat> channels = new ArrayList<Mat>();

		ImageWindow window = new ImageWindow("Main Image", 640, 480);
		window.show();

		final int G_MIN = 50;
		final int G_MAX = 255;

		Mat hierarchy = new Mat();
		Robot.setHost("roboRIO-3044-FRC.local");
		Robot.setUseMDNS(true);
		Robot.setTeam(3044);
		NetworkTable visionTable = NetworkTable.getTable("SmartDashboard");
		MatOfPoint contour;
		int biggestRectid = 0;
		ArrayList<Rect> boundingRects;
		// System.out.println(v.open("http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg"));
		while (window.isVisible()) {
			v.read(frame);
			if (frame.size().width > 0) {
				frame.copyTo(orig);
				Core.split(frame, channels);
				blank = Mat.zeros(channels.get(0).height(), channels.get(0).width(), channels.get(0).type());
				Core.subtract(channels.get(2), new Scalar(10, 10, 10), temp);
				Core.subtract(channels.get(1), temp, temp);

				channels.set(0, blank);
				channels.set(2, blank);
				channels.set(1, temp);

				Core.merge(channels, frame);
				Core.inRange(frame, new Scalar(0, G_MIN, 0), new Scalar(0, G_MAX, 0), threshold);

				Imgproc.erode(threshold, threshold, erodeElement);
				Imgproc.dilate(threshold, threshold, dilateElement);
				// window.pushImage(threshold);

				ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

				Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE,
						new Point(0, 0));

				//ArrayList<MatOfPoint2f> approxContours = new ArrayList<MatOfPoint2f>();
				boundingRects = new ArrayList<Rect>();

				for (int i = 0; i < contours.size(); i++) {
					//MatOfPoint2f approx = new MatOfPoint2f();
					contour = contours.get(i);
					Rect r = Imgproc.boundingRect(contour);
					// contour.convertTo(approx, CvType.CV_32FC2);
					// Imgproc.approxPolyDP(approx, approx, 5, true);
					// approxContours.add(i,approx);
					// approx.convertTo(contour, CvType.CV_32F);

					boundingRects.add(i, r);
				}
				biggestRectid = 0;
				for (int i = 0; i < boundingRects.size(); i++) {
					Rect r = boundingRects.get(i);
					if (r.size().area() > boundingRects.get(biggestRectid).size().area()
							&& !(boundingRects.get(i).x > 290 && boundingRects.get(i).y > 220)) {
						biggestRectid = i;
					} else {
						Imgproc.rectangle(frame, r.tl(), r.br(), new Scalar(0, 0, 255), 1);
					}

				}
				if (boundingRects.size() > 0) {
					Rect r = boundingRects.get(biggestRectid);
					System.out.println("Dist: " + r.y);
					System.out.println("Center: " + (160 - (r.x + (r.width / 2.0))));
					System.out.println(r.br());
					System.out.println(true);
					visionTable.putNumber("DIST", r.y);
					visionTable.putNumber("ANGLE", (160 - (r.x + (r.width / 2.0))));
					visionTable.putBoolean("TARGET", true);
					if (!visionTable.getBoolean("ALIGNED", false)) {
						Imgproc.rectangle(frame, r.tl(), r.br(), new Scalar(0, 255, 0), 1);
					}else{
						Imgproc.rectangle(frame, r.tl(), r.br(), new Scalar(0, 255, 0), -1);
					}
				} else {
					visionTable.putBoolean("TARGET", false);
					System.out.println(false);
				}
				window.pushImage(frame);
				window.repaint();
			}
		}
	}

}
