package com.team3044.stronghold.vision;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.team3044.stronghold.gui.ConsoleWindow;
import com.team3044.stronghold.gui.ImageWindow;

import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import jssc.SerialPort;
import jssc.SerialPortException;

public class Main {

	static String Address = "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg";

	public static double offset = 9;

	static Point lastGood = new Point(0, 0);
	int count2 = 0;
	

	
	public static void sendRectangles(ArrayList<Rect> boundingRects, int biggestRectid, NetworkTable visionTable,
			Mat orig) {
		if (boundingRects.size() > 0) {
			Rect r = boundingRects.get(biggestRectid);
			if (lastGood.x == 0) {
				lastGood = r.tl();
			}
			if (count > 10) {
				count = 0;
				lastGood = r.tl();
			}
			if (Math.abs(lastGood.x - r.x) < 30) {
				lastGood = r.tl();
				System.out.println(r.width);
				visionTable.putNumber("DIST", r.y);
				if (160 - (r.x + (r.width / 2.0)) + offset < 155 && r.width > 20) {
					visionTable.putNumber("ANGLE", (160 - (r.x + (r.width / 2.0)) + offset));
					visionTable.putBoolean("TARGET", true);
				} else {
					visionTable.putBoolean("TARGET", false);
				}
			} else {
				count += 1;
			}
			// Draw the target:
			// TODO: Update target Drawing, and onTargetCondidtions
			if (!(r.y > 130 && (160 - Math.abs((r.x + (r.width / 2.0)) + 7.5)) < 10)) {
				// Imgproc.rectangle(orig, new Point(r.tl().x,r.tl().y), new
				// Point(r.br().x,r.br().y), new Scalar(255, 0, 255), 1);
			} else {
				// Imgproc.rectangle(orig, new Point(r.tl().x,r.tl().y), new
				// Point(r.br().x,r.br().y), new Scalar(255, 0, 255), -1);
			}
		} else {
			visionTable.putBoolean("TARGET", false);
			// System.out.println(false);
		}
	}

	public static boolean openCamera(ConsoleWindow console) {
		console.println("Initializing VideoCapture Camera: " + Address);
		v = new VideoCapture(Address);
		console.println("Opening Camera: " + Address);
		v.open(Address);

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (v.isOpened()) {
			System.out.println("opened");
			return true;
		} else {
			System.out.println("not open");
			return false;
		}
	}

	static VideoCapture v;
	static int count = 0;

	public static void main(String[] args) {
		int state = 0;
		final int CONNECTING = 0;
		final int MAIN = 1;
		final int DEBUG = 2;
		final int CALIBRATE = 3;

		boolean ignoreRobot = false;
	
		int G_MIN = 50;
		int G_MAX = 255;
		int oldSerialValue = 0;
		double oldSerialReadTime = System.currentTimeMillis();
		String serialPort = "COM5";
		ConsoleWindow console = new ConsoleWindow("Console", 500, 300);
		console.setVisible(true);
		// Loader.loadLibrary();
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// System.out.println(Core.NATIVE_LIBRARY_NAME);
		if (Files.isReadable(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"))) {
			try {
				BufferedReader reader = Files
						.newBufferedReader(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"));
				console.print("------Reading from config-----");
				String G_MIN_READ = reader.readLine();
				String G_MAX_READ = reader.readLine();
				Address = reader.readLine();
				serialPort = reader.readLine();
				offset = Double.parseDouble(reader.readLine());
				console.println("GMIN: " + G_MIN_READ);
				console.println(" GMAX: " + G_MAX_READ);
				console.println("Adress: " + Address);
				console.println("Serial Port: " + serialPort);
				console.println("Offset:" + offset);
				console.println("----------Done----------");
				reader.close();

			} catch (IOException e) {

				console.println(e.getMessage());
			}
		} else {
			try {
				Files.createDirectories(Paths.get(System.getenv("APPDATA"), "\\3044Vision\\"));
				Files.createFile(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"));
				BufferedWriter writer = Files.newBufferedWriter(
						Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"), StandardOpenOption.WRITE);
				writer.write(G_MIN + "\n" + G_MAX + "\n" + "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg"
						+ "\n" + "COM5\n" + offset);

				writer.flush();
				writer.close();
			} catch (IOException e) {

				console.println(e.getMessage());
			}
		}
		console.println("Opencv Loaded");
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		Mat frame = new Mat();
		Mat blank = new Mat();
		Mat threshold = new Mat();
		Mat temp = new Mat();
		Mat orig = new Mat();
		Mat load = Mat.zeros(new Size(320, 240), CvType.CV_8UC3);

		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

		ArrayList<Mat> channels = new ArrayList<Mat>();

		ImageWindow window = new ImageWindow("Main Image", 640, 480);
		window.setVisible(true);
		boolean serialFailed = false;
		SerialPort port = new SerialPort(serialPort);
		try {
			port.openPort();
			console.println("Serial Port openend");
		} catch (SerialPortException e1) {
			console.println("Serial Port Failed to open Sending auto 4 instead");
			serialFailed = true;
			// e1.printStackTrace();
		}

		for (int i = 0; i < 10; i++) {
			if (openCamera(console)) {
				console.print("Camera Opened");
				i = 25;
				break;
			} else {
				System.out.println("Could Not open Camera (Attempts): " + i);
				Imgproc.rectangle(load, new Point(10 * i, 0), new Point((10 * i) + 10, 255), new Scalar(0, 0, 255), -1);
				Imgproc.putText(load, "Attempting to connect", new Point(60, 120), 0, .5, new Scalar(255, 0, 0));
				window.pushImage(load);
				window.repaint();
			}
		}
		// if(!openCamera()){
		// System.exit(0);
		// }

		Mat hierarchy = new Mat();
		Robot.setHost("roboRIO-3044-FRC.local");
		Robot.setUseMDNS(true);
		Robot.setTeam(3044);
		NetworkTable visionTable = NetworkTable.getTable("SmartDashboard");
		MatOfPoint contour;
		int biggestRectid = 0;
		/* console.println("Network Tables initialized: " + Robot.getHost()); */
		ArrayList<Rect> boundingRects;
		double start = 0;
		Mat tmp2 = new Mat();
		int[] s;
		ArrayList<Integer> finalInts = new ArrayList<Integer>();

		while (window.isVisible()) {
			count += 1;
			if (count % 10 == 0) {
				// Imgcodecs.imencode("C:\\opencv3.0.0\\" +
				// String.valueOf(count) + ".jpg",
				// MatOfByte.fromNativeAddr(orig.getNativeObjAddr()));

			}
			System.out.println("-------- Start:" + (start = System.currentTimeMillis()) + "---------");
			if (start - oldSerialReadTime > 5000) {

				oldSerialReadTime = System.currentTimeMillis();

				try {
					finalInts = new ArrayList<Integer>();
					if ((s = port.readIntArray()) != null && !serialFailed) {
						for (int i = 0; i < s.length; i++) {
							if (s[i] != 10 && s[i] != 13) {
								finalInts.add(Integer.parseInt(String.valueOf((char) (s[i]))));
							}
						}
						int tmp = finalInts.get(finalInts.size() - 1);
						if (oldSerialValue != tmp) {
							visionTable.putNumber("AUTO", tmp);
							oldSerialValue = tmp;
							console.println(String.valueOf(tmp));
						}

					}

				} catch (NumberFormatException | SerialPortException e) {
					visionTable.putNumber("AUTO", 4);
				}
			}
			//frame = Imgcodecs.imread("D:\\img\\7.jpg");
			v.read(frame);
			if (frame.size().width > 0) {

				frame.copyTo(orig);

				Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
				Core.inRange(frame, new Scalar(20, 00, 50), new Scalar(170, 150, 256), threshold);
				Imgcodecs.imwrite("C:\\Users\\Joey\\Desktop\\test.jpg",
				 orig);
				threshold.copyTo(tmp2);
				//threshold.copyTo(tmp2);
				//Imgproc.erode(threshold, threshold, erodeElement);
				Imgproc.erode(threshold, threshold, erodeElement);
				//Imgproc.dilate(threshold, threshold, dilateElement);
				Imgproc.dilate(threshold, threshold, dilateElement);
				
				// window.pushImage(threshold);

				ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
				// window.pushImage(threshold);
				Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE,
						new Point(0, 0));

				boundingRects = new ArrayList<Rect>();

				for (int i = 0; i < contours.size(); i++) {
					
					contour = contours.get(i);
					MatOfPoint2f thisContour2f = new MatOfPoint2f();
					MatOfPoint approxContour = new MatOfPoint();
					MatOfPoint2f approxContour2f = new MatOfPoint2f();
					contour.convertTo(thisContour2f, CvType.CV_32FC2);
					Imgproc.approxPolyDP(thisContour2f, approxContour2f, 2, true);
					approxContour2f.convertTo(approxContour, CvType.CV_32S);
					if (approxContour.size().height < 100 && approxContour.size().height > 0) {
						
						System.out.println(approxContour.get(0, 0)[0]);
						Rect r = Imgproc.boundingRect(contour);
						contours.set(i, approxContour);
						// System.out.println(r.size());
						boundingRects.add(r);
						Imgproc.drawContours(orig, contours, i, new Scalar(0, 0, 255));
					}else{
						contours.set(i, new MatOfPoint());
					}
					
				}

				biggestRectid = 0;
				ArrayList<Rect> boundingRect2 = new ArrayList<Rect>();
				Rect largeBadRectangle = new Rect();
				for (int i = 0; i < boundingRects.size(); i++) {

					Rect r = boundingRects.get(i);
					if (r.area() > 7000) {
						largeBadRectangle = r;
					}
					if (r.area() > 500 || r.width / r.height > 2) {
						// System.out.println(r.area());
						Scalar mean = Core.mean(orig.submat(r));
						Mat centerSeventyFive = new Mat();
						Rect midSeventyFive = new Rect((int) (r.tl().x) + (int) (r.width * .25),
								(int) (r.tl().y)/* + (int)(r.height * .25) */, (int) (r.width * .75),
								(int) (r.height * .75));
						centerSeventyFive = tmp2.submat(midSeventyFive);
						// System.out.println(midSeventyFive.size());
						for (int j = midSeventyFive.x; j < midSeventyFive.x + midSeventyFive.size().width; j++) {
							for (int k = midSeventyFive.y; k < midSeventyFive.y + midSeventyFive.size().height; k++) {
								// System.out.println(j + " " + k);
								if (tmp2.get(k, j)[0] == 255) {
									tmp2.put(k, j, 0);
									// System.out.println("0");
								} else {
									tmp2.put(k, j, 255);
									// System.out.println("1");
								}
							}
						}

						// System.out.println(Core.sumElems(tmp2.submat(midSeventyFive)).val[0]
						// / r.area());
						if (Core.sumElems(tmp2.submat(midSeventyFive)).val[0] / r.area() > 100
								&& Core.sumElems(tmp2.submat(midSeventyFive)).val[0] / r.area() < 200) {

							if (r.area() > 7000) {

							} else {
								if (!largeBadRectangle.contains(r.tl())) {
									boundingRect2.add(r);
									Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(0, 255, 0));
								}
							}
							// new Scanner((System.in)).nextLine();
							// window.pushImage(tmp2.submat(r));

							// new Scanner((System.in)).nextLine();
						}

					}

				}
				int maxId = 0;
				for (int i = 0; i < boundingRect2.size(); i++) {
					Rect r = boundingRect2.get(i);
					if (r.size().area() > boundingRect2.get(maxId).size().area()
							&& (r.size().height / r.size().width < 1)) {
						maxId = i;
					} else if ((r.size().height / r.size().width < 1) && r.size().area() > 1000) {
						maxId = i;
					}
				}
				if (boundingRect2.size() > 0) {
					Imgproc.rectangle(orig, boundingRect2.get(maxId).tl(), boundingRect2.get(maxId).br(),
							new Scalar(255, 0, 0), 1);
					Imgcodecs.imwrite("C:\\opencv3.0.0\\" + String.valueOf(count) + ".jpg", orig);
					sendRectangles(boundingRect2, maxId, visionTable, orig);
				}

				// Imgproc.cvtColor(threshold, threshold,
				// Imgproc.COLOR_BGR2HSV);
				Mat finalMat = new Mat();
				orig.copyTo(finalMat);
				Imgproc.resize(finalMat, finalMat, new Size(640, 480));
				window.pushImage(finalMat);
				window.repaint();

				System.out.println("--------End: " + (System.currentTimeMillis() - start) + "---------");
				if (System.currentTimeMillis() - start < 30) {
					try {
						Thread.sleep((long) (30 - (System.currentTimeMillis() - start)));
					} catch (InterruptedException e) {
						console.println(e.getMessage());
					}
				}
			}
		}
	}

}
