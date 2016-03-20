package com.team3044.stronghold.gui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

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

import jssc.SerialPortException;

public class VisionProcess implements KeyListener,MouseListener {

	private int state = 6;
	final int INIT = 6;
	final int CONNECT_CAMERA = 0;
	final int CONNECT_ROBOT = 1;
	final int CONNECT_SERIAL = 2;
	final int MAIN_LOOP = 3;
	final int CALIBRATE = 4;
	final int DEBUG = 5;

	VideoCapture camera = new VideoCapture();
	Mat cameraFrame = new Mat();
	int count = 0;
	
	int mouseX = 0;
	int mouseY = 0;
	int oldX = 0;
	int oldY = 0;
	
	public void process() {
		switch (state) {
		case INIT:
			if(camera.isOpened())
				camera.release();
			if (isAxis) {
				if (openCamera(output)) {
					output.print("Camera Opened");
					this.state = MAIN_LOOP;
				} else {

				}
			} else {
				this.camera = new VideoCapture(0);
				System.out.println("Using Laptop Camera");
				if (camera.isOpened())
					this.state = MAIN_LOOP;
			}
			break;
		case CONNECT_CAMERA:
			break;
		case CONNECT_ROBOT:
			break;
		case CONNECT_SERIAL:
			break;
		case MAIN_LOOP:{

		System.out.println("-------- Start:" + (start = System.currentTimeMillis()) + "---------");
		
		camera.read(frame);
		if (frame.size().width > 0) {
			count += 1;
			frame.copyTo(orig);

			Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
			Core.inRange(frame, new Scalar(H_MIN, S_MIN, V_MIN), new Scalar(H_MAX, S_MAX, V_MAX), threshold);

			threshold.copyTo(tmp2);

			Imgproc.erode(threshold, threshold, erodeElement);
			
			Imgproc.dilate(threshold, threshold, dilateElement);
			
			

			ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
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
				
					for (int j = midSeventyFive.x; j < midSeventyFive.x + midSeventyFive.size().width; j++) {
						for (int k = midSeventyFive.y; k < midSeventyFive.y + midSeventyFive.size().height; k++) {
							
							if (tmp2.get(k, j)[0] == 255) {
								tmp2.put(k, j, 0);
								
							} else {
								tmp2.put(k, j, 255);
							
							}
						}
					}

					if (Core.sumElems(tmp2.submat(midSeventyFive)).val[0] / r.area() > 100
							&& Core.sumElems(tmp2.submat(midSeventyFive)).val[0] / r.area() < 200) {

						if (r.area() > 7000) {

						} else {
							if (!largeBadRectangle.contains(r.tl())) {
								boundingRect2.add(r);
								Imgproc.rectangle(orig, r.tl(), r.br(), new Scalar(0, 255, 0));
							}
						}

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
				//sendRectangles(boundingRect2, maxId, visionTable, orig);
			}

			// Imgproc.cvtColor(threshold, threshold,
			// Imgproc.COLOR_BGR2HSV);
			Mat finalMat = new Mat();
			orig.copyTo(finalMat);
			Imgproc.resize(finalMat, finalMat, new Size(640, 480));
			mainImage.pushImage(finalMat);
			mainImage.repaint();

			System.out.println("--------End: " + (System.currentTimeMillis() - start) + "---------");
			if (System.currentTimeMillis() - start < 30) {
				try {
					Thread.sleep((long) (30 - (System.currentTimeMillis() - start)));
				} catch (InterruptedException e) {
					output.println(e.getMessage());
				}
			}
		}
		}
			break;
		
		case CALIBRATE:
			Imgproc.cvtColor(this.frame, this.orig, Imgproc.COLOR_BGR2HSV);
			this.mainImage.pushImage(orig);
			
			break;
		case DEBUG:
			break;
		}
	}

	int H_MIN = 0, S_MIN = 0, V_MIN = 0;
	int H_MAX = 180, S_MAX = 255, V_MAX = 255;
	double offset = 9;

	private Path configSaveLocation = Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt");

	ImageWindow thresholdWindow = new ImageWindow("DEBUG", 640, 480, this);
	ImageWindow mainImage = new ImageWindow("CameraImage", 640, 480, this);
	OptionSelector selector = new OptionSelector("Begin", 300, 200, this);
	ConsoleWindow output = new ConsoleWindow("Console", 500, 500, this);

	private String cameraAddr = "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg";
	private String serialPort = "COM5";
	
	Mat frame = new Mat();
	Mat blank = new Mat();
	Mat threshold = new Mat();
	Mat temp = new Mat();
	Mat orig = new Mat();
	Mat load = Mat.zeros(new Size(320, 240), CvType.CV_8UC3);
	Mat hierarchy = new Mat();
	Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
	Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

	
	MatOfPoint contour;
	int biggestRectid = 0;

	ArrayList<Rect> boundingRects;
	double start = 0;
	Mat tmp2 = new Mat();
	int[] s;
	ArrayList<Integer> finalInts = new ArrayList<Integer>();

	boolean isAxis = true;

	public VisionProcess() {
		thresholdWindow.setVisible(false);
		selector.setVisible(true);
		output.setVisible(true);
		mainImage.setVisible(true);

	}

	public boolean openCamera(ConsoleWindow console) {
		console.println("Initializing VideoCapture Camera: " + cameraAddr);

		camera.open(cameraAddr);

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

		if (camera.isOpened()) {
			return true;
		} else {
			return false;
		}

	}

	public void restart() {

	}

	public void loadConfigFile(String file) throws IOException {
		if (Files.isReadable(configSaveLocation)) {
			BufferedReader reader = Files
					.newBufferedReader(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"));
			output.print("Loading Config");
			H_MIN = Integer.parseInt(reader.readLine());
			S_MIN = Integer.parseInt(reader.readLine());
			V_MIN = Integer.parseInt(reader.readLine());
			H_MAX = Integer.parseInt(reader.readLine());
			S_MAX = Integer.parseInt(reader.readLine());
			V_MAX = Integer.parseInt(reader.readLine());
			System.out.println(H_MIN + " " + H_MAX + " " + V_MIN + " " + V_MAX + " " + S_MIN + " " + S_MAX);
			cameraAddr = reader.readLine();
			serialPort = reader.readLine();
			offset = Double.parseDouble(reader.readLine());
			this.isAxis = reader.readLine().contains("AXIS");
			System.out.println(isAxis);
			reader.close();
			output.println("Done");
		} else {
			output.println("No Config File found, Making one");
			Files.createDirectories(Paths.get(System.getenv("APPDATA"), "\\3044Vision\\"));
			Files.createFile(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"));
			BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"), StandardOpenOption.WRITE);
			writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
					+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset + "/nAXIS");

			writer.flush();
			writer.close();
		}

	}

	public void connectToCamera(String address) {

	}

	public void connectToRobot(String host) {

	}

	public void initializeSerialConnection(String port) {

	}

	public void onLoad() {
		try {
			loadConfigFile("");
		} catch (IOException e) {

			e.printStackTrace();
		}
		System.out.println("load");
	}

	public void onAxis() {
		this.state = INIT;
		
		BufferedWriter writer;
		try {
			writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"),
					StandardOpenOption.WRITE);
			writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
					+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset + "/nAXIS");

			writer.flush();
			writer.close();
			this.isAxis = true;
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		System.out.println("axis");
	}

	public void onLaptop() {
		this.state = INIT;
		camera.release();
		BufferedWriter writer;
		try {
			writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"),
					StandardOpenOption.WRITE);
			writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
					+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset + "/nLAPTOP");

			writer.flush();
			writer.close();
			this.isAxis = false;
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		System.out.println("laptop");
	}

	public void onCalib() {
		state = CALIBRATE;
		System.out.println("calib");
	}

	public void onDebug() {
		System.out.println("debug");
	}

	public void onReconnect() {
		this.state = INIT;

		
		System.out.println("reconnect");
	}

	private void onSave() {
		// TODO Auto-generated method stub

	}

	private class OptionSelector extends JFrame implements ActionListener {
		private VisionProcess process;

		public OptionSelector(String name, int width, int height, VisionProcess process) {
			this.process = process;

			setTitle(name);
			setSize(width, height);
			setLocation(800, 200);
			Container pane = this.getContentPane();
			pane.setVisible(true);
			this.setAlwaysOnTop(true);

			JPanel panel = new JPanel();

			JButton axisSource = new JButton("AXIS");
			axisSource.setVisible(true);
			axisSource.addActionListener(this);

			JButton reconnect = new JButton("Reconnect");
			reconnect.setVisible(true);
			reconnect.addActionListener(this);
			reconnect.setMnemonic(KeyEvent.VK_R);

			JButton debug = new JButton("Debug");
			debug.setVisible(true);
			debug.addActionListener(this);
			debug.setMnemonic(KeyEvent.VK_D);

			JButton calib = new JButton("Calibrate");
			calib.setVisible(true);
			calib.addActionListener(this);
			calib.setMnemonic(KeyEvent.VK_C);

			JButton laptopSource = new JButton("Laptop");
			laptopSource.setVisible(true);
			laptopSource.addActionListener(this);

			JButton load = new JButton("Load");
			load.setVisible(true);
			load.addActionListener(this);

			panel.add(laptopSource);
			panel.setVisible(true);
			panel.add(axisSource);
			panel.add(load);
			panel.add(reconnect);
			panel.add(calib);
			panel.add(debug);

			pane.add(panel);

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.out.println("NOTHING");
				}
			});
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == "Calibrate") {
				process.onCalib();
			} else if (e.getActionCommand() == "Laptop") {
				process.onLaptop();
			} else if (e.getActionCommand() == "Reconnect") {
				process.onReconnect();
			} else if (e.getActionCommand() == "Load") {
				process.onLoad();
			} else if (e.getActionCommand() == "AXIS") {
				process.onAxis();
			} else if (e.getActionCommand() == "Debug") {
				process.onDebug();
			}

		}
	}

	@Override
	public void keyPressed(KeyEvent arg0) {

	}

	@Override
	public void keyReleased(KeyEvent arg0) {

	}

	@Override
	public void keyTyped(KeyEvent key) {
		if (key.getKeyChar() == 's') {
			this.onSave();
		} else if (key.getKeyChar() == 'c') {
			this.onCalib();
		} else if (key.getKeyChar() == 'd') {
			this.onDebug();
		} else if (key.getKeyChar() == 'r') {
			this.onAxis();
		}

	}

	@Override
	public void mouseClicked(MouseEvent mouse) {
		System.out.println(mouse.getX());
		System.out.println(mouse.getY());
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		
		
	}

}
