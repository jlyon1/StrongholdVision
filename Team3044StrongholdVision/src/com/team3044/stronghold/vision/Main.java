package com.team3044.stronghold.vision;

import java.awt.image.BufferedImage;
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
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.team3044.stronghold.gui.ImageWindow;

public class Main {
	private BufferedImage myImage;

	
	final String Address = "https://10.30.44.11/axis-cgi/mjpg/video.cgi";
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		VideoCapture v = new VideoCapture(0);
		
		Mat frame = new Mat();
		Mat blank = new Mat();
		Mat threshold = new Mat();
		Mat temp = new Mat();
		Mat orig = new Mat();
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(3,3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5));
		
		ArrayList<Mat> channels = new ArrayList<Mat>();
		
		ImageWindow window = new ImageWindow("Main Image", 640, 480);
		window.show();
		
		final int G_MIN = 100;
		final int G_MAX = 255;
		

	    Mat hierarchy = new Mat();

	    
		while (window.isVisible()) {
			v.read(frame);
			frame.copyTo(orig);
			Core.split(frame, channels);
			blank = Mat.zeros(channels.get(0).height(),channels.get(0).width(),channels.get(0).type());
			Core.subtract(channels.get(1),channels.get(2), temp);
			
			channels.set(0, blank);
			channels.set(2,blank);
			channels.set(1, temp);
			
			Core.merge(channels, frame);
			Core.inRange(frame, new Scalar(0,5,0), new Scalar(0,G_MAX,0), threshold);
			
			Imgproc.erode(threshold, threshold, erodeElement);
			Imgproc.dilate(threshold, threshold, dilateElement);
			//window.pushImage(threshold);
			
			ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		    Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
		    
		    ArrayList<MatOfPoint2f> approxContours = new ArrayList<MatOfPoint2f>();
			ArrayList<Rect> boundingRects = new ArrayList<Rect>();
			
			for(int i = 0; i < contours.size(); i ++){
				MatOfPoint2f approx = new MatOfPoint2f();
				MatOfPoint contour = contours.get(i);
				Rect r = Imgproc.boundingRect(contour);
				//contour.convertTo(approx, CvType.CV_32FC2);
				//Imgproc.approxPolyDP(approx, approx, 5, true);
				//approxContours.add(i,approx);
				//approx.convertTo(contour, CvType.CV_32F);
				
				boundingRects.add(i, r);
			}
			for(Rect r: boundingRects){
				Imgproc.rectangle(frame, r.tl(), r.br(), new Scalar(0,255,0),1);
				System.out.println(r.br());
			}
		    
			window.pushImage(frame);
			window.repaint();
		}
	}

}
