package com.team3044.stronghold.gui;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Mat;

public class ImageWindow extends JFrame {

	private BufferedImage myImage;
	ImagePanel p = new ImagePanel();

	public ImageWindow(String name, int width, int height) {
		setTitle(name);
		setSize(width, height); 
		setLocation(10, 200); 
		Container pane = this.getContentPane();
		p.setVisible(true);
		pane.add(p);
		pane.setVisible(true);
		this.setAlwaysOnTop(true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		

	}
	
	public void pushImage(Mat image){
		p.pushImage(image);
	
	}
	

	class ImagePanel extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(myImage, 0, 0, this);

		}

		protected void pushImage(Mat image) {
			myImage = toBufferedImage(image);
			this.repaint();

		}

		private BufferedImage toBufferedImage(Mat m) {
			int type = BufferedImage.TYPE_BYTE_GRAY;
			if (m.channels() > 1) {
				type = BufferedImage.TYPE_3BYTE_BGR;
			}
			int bufferSize = m.channels() * m.cols() * m.rows();
			byte[] b = new byte[bufferSize];
			m.get(0, 0, b); // get all the pixels
			BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
			final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
			System.arraycopy(b, 0, targetPixels, 0, b.length);
			return image;

		}

	}

}
