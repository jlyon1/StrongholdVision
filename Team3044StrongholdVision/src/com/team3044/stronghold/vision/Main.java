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

	final static String Address = "http://10.30.44.26/axis-cgi/mjpg/video.cgi?test.mjpeg";
	
	public static void processRectangles(ArrayList<Rect> boundingRects, int biggestRectid, NetworkTable visionTable, Mat orig){
		if (boundingRects.size() > 0) {
			Rect r = boundingRects.get(biggestRectid);
			System.out.println("Dist: " + r.y);
			System.out.println("Center: " + (160 - (r.x + (r.width / 2.0)) + 4));
			System.out.println(r.br());
			System.out.println(true);
			visionTable.putNumber("DIST", r.y);
			visionTable.putNumber("ANGLE", (160 - (r.x + (r.width / 2.0)) + 4));
			visionTable.putBoolean("TARGET", true);
			if (!(r.y > 130 && (160 - Math.abs((r.x + (r.width / 2.0)) + 7.5)) < 10)) {
				Imgproc.rectangle(orig, new Point(r.tl().x + r.width/3,r.tl().y), new Point(r.br().x - r.width/3,r.br().y - r.height /4), new Scalar(255, 0, 255), 1);
			}else{
				Imgproc.rectangle(orig, new Point(r.tl().x + r.width/3,r.tl().y), new Point(r.br().x - r.width/3,r.br().y - r.height /4), new Scalar(255, 0, 255), -1);
			}
		} else {
			visionTable.putBoolean("TARGET", false);
			System.out.println(false);
		}
	}
	
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		VideoCapture v = new VideoCapture(Address);
		v.open(Address);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
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

		ImageWindow window = new ImageWindow("Main Image", 335, 279);
		window.setVisible(true);

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
		
		while (window.isVisible()) {
			v.read(frame);
			if (frame.size().width > 0) {
				frame.copyTo(orig);
				Core.split(frame, channels);
				blank = Mat.zeros(channels.get(0).height(), channels.get(0).width(), channels.get(0).type());
				Core.subtract(channels.get(2), new Scalar(0, 0, 0), temp);
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

				boundingRects = new ArrayList<Rect>();

				for (int i = 0; i < contours.size(); i++) {

					contour = contours.get(i);
					Rect r = Imgproc.boundingRect(contour);

					boundingRects.add(i, r);
				}
				
				biggestRectid = 0;
				for (int i = 0; i < boundingRects.size(); i++) {
					Rect r = boundingRects.get(i);
					if (r.size().area() > boundingRects.get(biggestRectid).size().area()
							&& boundingRects.get(i).br().y < 220) {
						
						biggestRectid = i;
					} else {
						Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(0, 0, 255), 1);
						boundingRects.remove(i);
						i -= 1;
					}
				}
				
				processRectangles(boundingRects, biggestRectid, visionTable, orig);

				window.pushImage(orig);
				window.repaint();
			}
		}
	}

}
