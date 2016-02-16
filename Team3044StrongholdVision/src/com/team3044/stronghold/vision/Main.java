package com.team3044.stronghold.vision;

import java.awt.image.BufferedImage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import com.team3044.stronghold.gui.ImageWindow;

public class Main {
	private BufferedImage myImage;

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture v = new VideoCapture(0);
		Mat m = new Mat();
		ImageWindow window = new ImageWindow("Hello", 640, 480);
		window.show();
		while (true) {
			v.read(m);

			window.pushImage(m);
			window.repaint();
		}
	}

}
