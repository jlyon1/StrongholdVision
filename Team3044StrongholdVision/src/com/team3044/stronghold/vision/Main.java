package com.team3044.stronghold.vision;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Scanner;

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
import com.team3044.stronghold.gui.VisionProcess;

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

	public static VideoCapture openCamera(ConsoleWindow console, VideoCapture v) {
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
			return v;
		} else {
			System.out.println("not open");
			return null;
		}
	}

	static VideoCapture v;
	static int count = 0;

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VisionProcess vision = new VisionProcess();
		for(int i = 0; i < 10000000; i ++){
			vision.process();
		}
		
		(new Scanner(System.in)).nextLine();
		
	}

}
