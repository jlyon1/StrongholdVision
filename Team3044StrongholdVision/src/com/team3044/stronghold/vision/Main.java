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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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

	public static void processRectangles(ArrayList<Rect> boundingRects, int biggestRectid, NetworkTable visionTable, Mat orig){
		if (boundingRects.size() > 0) {
			Rect r = boundingRects.get(biggestRectid);
			
			//System.out.println("Dist: " + r.y);
			//System.out.println("Center: " + (160 - (r.x + (r.width / 2.0)) + offset));
			//System.out.println(r.br());
			System.out.println(r.width);
			visionTable.putNumber("DIST", r.y);
			if(160 - (r.x + (r.width / 2.0)) + offset < 155 && r.width > 20){
				visionTable.putNumber("ANGLE", (160 - (r.x + (r.width / 2.0)) + offset));
				visionTable.putBoolean("TARGET", true);
			}else{
				visionTable.putBoolean("TARGET", false);
			}
			
			//Draw the target:
			//TODO: Update target Drawing, and onTargetCondidtions
			if (!(r.y > 130 && (160 - Math.abs((r.x + (r.width / 2.0)) + 7.5)) < 10)) {
				//Imgproc.rectangle(orig, new Point(r.tl().x,r.tl().y), new Point(r.br().x,r.br().y), new Scalar(255, 0, 255), 1);
			}else{
				//Imgproc.rectangle(orig, new Point(r.tl().x,r.tl().y), new Point(r.br().x,r.br().y), new Scalar(255, 0, 255), -1);
			}
		} else {
			visionTable.putBoolean("TARGET", false);
			//System.out.println(false);
		}
	}

	public static boolean openCamera() {
		System.out.println("Opening Camera: " + Address);
		v = new VideoCapture(Address);
		System.out.println("Opening Camera 2: " + Address);
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

	public static void main(String[] args) {
		System.out.println();
		int G_MIN = 50;
		int G_MAX = 255;
		int oldSerialValue = 0;
		double oldSerialReadTime = System.currentTimeMillis();
		String serialPort = "COM5";
		ConsoleWindow console = new ConsoleWindow("Console", 500, 300);
		console.setVisible(true);
		Loader.loadLibrary();
		//System.out.println(Core.NATIVE_LIBRARY_NAME);
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

				e.printStackTrace();
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

				e.printStackTrace();
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

		ImageWindow window = new ImageWindow("Main Image", 335, 279);
		window.setVisible(true);
		boolean serialFailed = false;
		SerialPort port = new SerialPort(serialPort);
		try {
			port.openPort();
			console.println("Serial Port openend");
		} catch (SerialPortException e1) {
			console.println("Serial Port Failed to open Sending auto 4 instead");
			serialFailed = true;
			//e1.printStackTrace();
		}
		

		for (int i = 0; i < 24; i++) {
			if (openCamera()) {
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
		console.println("Network Tables initialized: " + Robot.getHost());
		ArrayList<Rect> boundingRects;
		double start = 0;
		
		int[] s;
		ArrayList<Integer> finalInts = new ArrayList<Integer>();
		
		while (window.isVisible()) {
			
			System.out.println("-------- Start:" + (start = System.currentTimeMillis()) + "---------");
			if(start - oldSerialReadTime > 5000){
				
				oldSerialReadTime = System.currentTimeMillis();
				
				try {
					finalInts = new ArrayList<Integer>();
					if ((s = port.readIntArray()) != null && !serialFailed) {
						for(int i = 0; i < s.length; i ++){
							if(s[i] != 10 && s[i] != 13){
								finalInts.add(Integer.parseInt(String.valueOf((char)(s[i]))));
							}
						}
						int tmp = finalInts.get(finalInts.size() - 1);
						if(oldSerialValue != tmp){
							visionTable.putNumber("AUTO", tmp);
							oldSerialValue = tmp;
							console.println(String.valueOf(tmp));
						}
						
						
					}
					
					
				} catch (NumberFormatException | SerialPortException e) {
					visionTable.putNumber("AUTO", 4);
				}
			}
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
					Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(255, 0, 0), 1);
					boundingRects.add(i, r);
				}

				biggestRectid = 0;
				for (int i = 0; i < boundingRects.size(); i++) {
					Rect r = boundingRects.get(i);
					if (r.size().width * r.size().height > boundingRects.get(biggestRectid).size().width
							* r.size().height && r.x < 310) {
						Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(255, 0, 0), 1);
						biggestRectid = i;
					} else {
						Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(0, 0, 255), 1);

					}
				}

				processRectangles(boundingRects, biggestRectid, visionTable, orig);

				window.pushImage(orig);
				window.repaint();
				System.out.println("--------End: " + (System.currentTimeMillis() - start) + "---------");
				if (System.currentTimeMillis() - start < 30) {
					try {
						Thread.sleep((long) (30 - (System.currentTimeMillis() - start)));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
