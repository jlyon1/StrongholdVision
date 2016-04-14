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
import java.sql.Date;
import java.time.Instant;
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

import com.team3044.stronghold.vision.AxisGrabber;
import com.team3044.stronghold.vision.CameraIsNotOpenException;
import com.team3044.stronghold.vision.Main;

import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class VisionProcess implements KeyListener, MouseListener {

	private int state = 1;
	final int INIT = 6;
	final int CONNECT_CAMERA = 0;
	final int CONNECT_ROBOT = 1;
	final int CONNECT_SERIAL = 2;
	final int MAIN_LOOP = 3;
	final int CALIBRATE = 4;
	final int DEBUG = 5;

	VideoCapture camera = new VideoCapture();
	Mat cameraFrame = new Mat();
	int mainLoopCount = 0;

	int mouseX = 0;
	int mouseY = 0;
	int oldX = 0;
	int oldY = 0;
	
	int H_MIN = 0, S_MIN = 0, V_MIN = 0;
	int H_MAX = 180, S_MAX = 255, V_MAX = 255;
	double offset = 9;

	private Path configSaveLocation = Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt");

	ImageWindow thresholdWindow = new ImageWindow("DEBUG", 640, 480, this, false);
	ImageWindow mainImage = new ImageWindow("CameraImage", 640, 480, this, true);
	OptionSelector selector = new OptionSelector("Begin", 300, 200, this);
	ConsoleWindow output = new ConsoleWindow("Console", 500, 500, this, false);

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
	Mat noProcessing = new Mat();

	MatOfPoint contour;
	int biggestRectid = 0;

	ArrayList<Rect> boundingRects;
	double start = 0;
	Mat tmp2 = new Mat();
	int[] s;
	ArrayList<Integer> finalInts = new ArrayList<Integer>();

	boolean isAxis = true;
	private int clickCount = 0;
	private int oldClickCount = 0;

	NetworkTable visionTable;

	AxisGrabber grabber;

	int countDebug = 0;
	public ConsoleWindow console;
	public VisionProcess() {
		thresholdWindow.setVisible(false);
		selector.setVisible(true);
		output.setVisible(true);
		mainImage.setVisible(true);

	}

	public boolean openCamera(ConsoleWindow console) {
		console.println("Initializing VideoCapture Camera: " + cameraAddr);
		this.console = console;
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

	public void drawGuides(Mat image, Point p) {
		Imgproc.line(image, new Point(160 + offset + 5, 0), new Point(160 + offset + 5.0, 1000), new Scalar(0, 255, 0));
		Imgproc.line(image, new Point(160 + offset - 5, 0), new Point(160 + offset - 5.0, 1000), new Scalar(0, 255, 0));
		Imgproc.line(image, new Point(0, 140), new Point(1000, 140), new Scalar(0, 255, 0));
		Imgproc.line(image, new Point(0, 190), new Point(1000, 190), new Scalar(0, 255, 0));
		if (p != null) {
			if (p.inside(new Rect((int) (160 + offset - 5), 140, (int) (160 + offset - 5), 190))) {
				Imgproc.rectangle(image, new Point(0, 0), new Point(image.size().width, image.size().height),
						new Scalar(0, 255, 0), 8, 0, 0);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void process() {
		switch (state) {
		case INIT:
			onLoad();
			frame = new Mat();
			blank = new Mat();
			camera = new VideoCapture();
			threshold = new Mat();
			temp = new Mat();
			orig = new Mat();
			load = Mat.zeros(new Size(320, 240), CvType.CV_8UC3);
			hierarchy = new Mat();
			erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
			dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
			state = CONNECT_CAMERA;
			break;
		case CONNECT_CAMERA:

			if (isAxis) {
				if ((camera = Main.openCamera(output, camera)) != null) {
					output.print("Camera Opened");
					this.state = MAIN_LOOP;
					try {
						grabber = new AxisGrabber(camera, this);
						Thread t = new Thread(grabber);
						t.start();
					} catch (CameraIsNotOpenException e) {
						camera.release();
						this.state = INIT;
					}
				} else {
					output.println("Could Not Connect, Trying again");
				}
			} else {
				this.camera = new VideoCapture(0);
				
				if (camera.isOpened()) {
					try {
						grabber = new AxisGrabber(camera,this);
						Thread t = new Thread(grabber);
						t.start();
					} catch (CameraIsNotOpenException e) {
						camera.release();
						this.state = INIT;
					}
					state = MAIN_LOOP;
				}
			}
			break;

		case CONNECT_ROBOT:
			this.connectToRobot("roboRIO-3044-frc.local");
			if (visionTable.isConnected()) {
				this.state = INIT;
			} else {
				output.println("Could not connect to robot, Trying again");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			break;
		case CONNECT_SERIAL:
			break;
		case DEBUG:
			countDebug += 1;
			if (!thresholdWindow.isVisible()) {
				state = MAIN_LOOP;
			}
			thresholdWindow.repaint();
			if (!Files.exists(Paths.get("C:\\Opencv3.0.0\\images\\" + Date.from(Instant.now()).getHours() + "\\"
					+ Date.from(Instant.now()).getMinutes()))) {
				try {
					Files.createDirectories(Paths.get("C:\\Opencv3.0.0\\images\\" + Date.from(Instant.now()).getHours()
							+ "\\" + Date.from(Instant.now()).getMinutes()));
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
			Imgcodecs.imwrite("C:\\Opencv3.0.0\\images\\" + Date.from(Instant.now()).getHours() + "\\"
					+ Date.from(Instant.now()).getMinutes() + "\\" + countDebug + ".jpg", noProcessing);
		case MAIN_LOOP: {

			
			mainLoopCount += 1;
			
			Mat temporary = new Mat();
			if ((temporary = grabber.getBuffer()[grabber.getJ()]) != null && 
					grabber.getTimeStamps()[grabber.getJ()] != 10) {
				temporary.copyTo(frame);
				if (state == DEBUG)
					frame.copyTo(noProcessing);

				if (frame.size().width > 0) {
					mainLoopCount += 1;

					ArrayList<Mat> channels = new ArrayList<Mat>();

					Core.split(frame, channels);

					Core.merge(channels, frame);
					frame.copyTo(orig);
					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
					Core.inRange(frame, new Scalar(H_MIN, S_MIN, V_MIN), new Scalar(H_MAX, S_MAX, V_MAX), threshold);
					thresholdWindow.pushImage(threshold);
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
						if (approxContour.size().height < 12 && approxContour.size().height > 5) {

							Rect r = Imgproc.boundingRect(contour);
							contours.set(i, approxContour);

							boundingRects.add(r);
							if (state == DEBUG)
								Imgproc.drawContours(orig, contours, i, new Scalar(0, 255, 0));
						} else {
							if (state == DEBUG)
								Imgproc.drawContours(orig, contours, i, new Scalar(0, 0, 255));
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
						if (r.area() > 500 || (r.width / r.height > 2 && r.height < r.width)) {
							new Mat();
							Rect midSeventyFive = new Rect((int) (r.tl().x) + (int) (r.width * .25), (int) (r
									.tl().y)/* + (int)(r.height * .25) */, (int) (r.width * .5),
									(int) (r.height * .75));
							tmp2.submat(midSeventyFive);
							double count = 0;
							for (int j = midSeventyFive.x; j < midSeventyFive.x + midSeventyFive.size().width; j++) {
								for (int k = midSeventyFive.y; k < midSeventyFive.y
										+ midSeventyFive.size().height; k++) {

									if (tmp2.get(k, j)[0] == 255) {
										tmp2.put(k, j, 0);
										count += 1;
									} else {
										tmp2.put(k, j, 255);

									}
								}
							}
							if (state == DEBUG) {
								Imgproc.putText(orig, String.valueOf(count), midSeventyFive.tl(), 1, 0.5,
										new Scalar(0, 0, 255));
								Imgproc.rectangle(orig, midSeventyFive.tl(), midSeventyFive.br(),
										new Scalar(0, 0, 255));
								Imgproc.putText(orig, String.valueOf(Core.sumElems(tmp2.submat(r)).val[0] / r.area()),
										midSeventyFive.br(), 1, 0.5, new Scalar(0, 0, 255));
							}

							if (Core.sumElems(tmp2.submat(r)).val[0] / r.area() > 75
									&& Core.sumElems(tmp2.submat(r)).val[0] / r.area() < 250 && count < 150) {
								//System.out.println("Area: " + Core.sumElems(tmp2.submat(r)).val[0] / r.area());

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
					Point poi = null;
					if (boundingRect2.size() > 0) {
						Main.sendRectangles(boundingRect2, maxId, visionTable, orig, (int) (offset + .5));

						Imgproc.rectangle(orig, boundingRect2.get(maxId).tl(), boundingRect2.get(maxId).br(),
								new Scalar(255, 0, 0), 1);
						Imgcodecs.imwrite("C:\\opencv3.0.0\\" + String.valueOf(mainLoopCount) + ".jpg", frame);
						Imgproc.line(orig,
								new Point(boundingRect2.get(maxId).x + boundingRect2.get(maxId).width / 2, 0),
								new Point(boundingRect2.get(maxId).x + boundingRect2.get(maxId).width / 2, 10000),
								new Scalar(255, 0, 0));
						Imgproc.line(orig,
								new Point(0, boundingRect2.get(maxId).y + boundingRect2.get(maxId).height / 2),
								new Point(10000, boundingRect2.get(maxId).y + boundingRect2.get(maxId).height / 2),
								new Scalar(255, 0, 0));
						poi = new Point(boundingRect2.get(maxId).x + boundingRect2.get(maxId).width / 2,
								boundingRect2.get(maxId).y + boundingRect2.get(maxId).height / 2);
					}

					Mat finalMat = new Mat();

					this.drawGuides(orig, poi);
					orig.copyTo(finalMat);
					Imgproc.resize(finalMat, finalMat, new Size(640, 480));
					mainImage.pushImage(finalMat);
					mainImage.repaint();

					//System.out.println("--------End: " + (System.currentTimeMillis() - start) + "---------");
					if (System.currentTimeMillis() - start < 30) {
						try {
							Thread.sleep((long) (30 - (System.currentTimeMillis() - start)));
						} catch (InterruptedException e) {
							output.println(e.getMessage());
						}
					}
				}
			}
		}
			break;

		case CALIBRATE:
			frame.copyTo(orig);
			// Imgproc.cvtColor(this.frame, this.orig, Imgproc.COLOR_BGR2HSV);
			this.mainImage.pushImage(orig);
			//System.out.println(orig.size());
			if (this.clickCount != oldClickCount) {

				this.H_MIN = (int) orig.get(mouseX, mouseY)[0];
				this.S_MIN = (int) orig.get(mouseX, mouseY)[1];
				this.V_MIN = (int) orig.get(mouseX, mouseY)[2];
				this.H_MAX = (int) orig.get(mouseX, mouseY)[0];
				this.S_MAX = (int) orig.get(mouseX, mouseY)[1];
				this.V_MAX = (int) orig.get(mouseX, mouseY)[2];
				//System.out.println(H_MIN);
				this.oldClickCount = 0;
				output.println(String.valueOf(new Scalar(H_MIN, S_MIN, V_MIN)));
				clickCount = 0;
			}

			Core.inRange(orig, new Scalar(H_MIN, S_MIN, V_MIN), new Scalar(H_MAX, S_MAX, V_MAX), threshold);
			this.thresholdWindow.pushImage(threshold);
			if (thresholdWindow.isVisible() == false) {
				BufferedWriter writer;
				try {
					writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"),
							StandardOpenOption.WRITE);
					writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
							+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset
							+ "\nAXIS");

					writer.flush();
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				this.state = MAIN_LOOP;
			}
			break;

		}
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
			//System.out.println(H_MIN + " " + H_MAX + " " + V_MIN + " " + V_MAX + " " + S_MIN + " " + S_MAX);
			cameraAddr = reader.readLine();
			serialPort = reader.readLine();
			offset = Double.parseDouble(reader.readLine());
			this.isAxis = reader.readLine().contains("AXIS");
			//System.out.println(isAxis);
			reader.close();
			output.println("Done");
		} else {
			output.println("No Config File found, Making one");
			Files.createDirectories(Paths.get(System.getenv("APPDATA"), "\\3044Vision\\"));
			Files.createFile(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"));
			BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"), StandardOpenOption.WRITE);
			writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
					+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset + "\nAXIS");

			writer.flush();
			writer.close();
		}

	}

	public void connectToRobot(String host) {
		Robot.setHost(host);
		Robot.setUseMDNS(true);
		Robot.setTeam(3044);
		visionTable = NetworkTable.getTable("SmartDashboard");
	}

	public void initializeSerialConnection(String port) {

	}
	
	public void updateSmartDashboardValues(){
	//	this.shooterPercent = visionTable.getNumber(")
	}

	public void onLoad() {
		try {
			loadConfigFile("");
		} catch (IOException e) {

			e.printStackTrace();
		}
		//System.out.println("load");
	}

	public void onAxis() {
		camera.release();
		isAxis = true;
		onSave(true);
		state = INIT;

		//System.out.println("axis");
	}

	public void onLaptop() {

		camera.release();
		this.onSave(false);
		this.state = INIT;
		//System.out.println("laptop");
	}

	public void onCalib() {
		this.oldX = mouseX;
		this.oldY = mouseY;
		thresholdWindow.setVisible(true);
		state = CALIBRATE;

		//System.out.println("calib");
	}

	public void onDebug() {
		if (state == DEBUG) {
			this.state = MAIN_LOOP;
		} else {
			thresholdWindow.setVisible(true);
			state = DEBUG;
		}
		//System.out.println("debug");
	}

	public void onReconnect() {
		this.state = CONNECT_ROBOT;

		//System.out.println("reconnect");
	}

	private void onSave(boolean axis) {
		BufferedWriter writer;
		try {
			if (axis) {
				writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"),
						StandardOpenOption.WRITE);
				writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
						+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset
						+ "\nAXIS");

				writer.flush();
				writer.close();
				this.isAxis = true;
			} else {
				writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\config.txt"),
						StandardOpenOption.WRITE);
				writer.write(H_MIN + "\n" + S_MIN + "\n" + V_MIN + "\n" + H_MAX + "\n" + S_MAX + "\n" + V_MAX + "\n"
						+ "http://10.30.44.20/axis-cgi/mjpg/video.cgi?test.mjpeg" + "\n" + "COM5\n" + offset
						+ "\nLaptop");

				writer.flush();
				writer.close();
				this.isAxis = false;
			}
		} catch (IOException e1) {

			e1.printStackTrace();
		}

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
					//System.out.println("NOTHING");
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
			this.onSave(true);
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
		//System.out.println(mouse.getX());
		//System.out.println(mouse.getY());
		this.mouseX = mouse.getX();
		this.mouseY = mouse.getY();
		this.clickCount = mouse.getClickCount();
		this.oldClickCount = -1;
		// System.out.println(clickCount);

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
