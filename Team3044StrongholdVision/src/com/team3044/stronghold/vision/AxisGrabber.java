package com.team3044.stronghold.vision;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import com.team3044.stronghold.gui.VisionProcess;

public class AxisGrabber implements Runnable{

	private VideoCapture cap;
	private boolean isReady;
	private boolean running = true;
	
	private Mat[] buffer = new Mat[3];
	private double[] timeTags = new double[3];
	private int i = 0, j = 2;
	
	VisionProcess p;
	public AxisGrabber(VideoCapture capture, VisionProcess p) throws CameraIsNotOpenException{
		this.p = p;
		this.cap = capture;
		if(!cap.isOpened()){
			this.isReady = false;
			throw new CameraIsNotOpenException();
		}else{
			this.isReady = true;
		}
		timeTags[2] = 10;
		timeTags[1] = 10;
		
	}
	
	public boolean isReady(){
		return isReady;
	}
		
	public void kill(){
		this.running = false;
	}
	
	public double[] getTimeStamps(){
		return timeTags;
	}
	
	public int getI(){
		return i;
	}
	
	public int getJ(){
		return j;
	}
	
	public Mat[] getBuffer(){
		return buffer;
	}
	long averageTime = 0;
	long minTime = 0;
	long maxTime = 0;
	int count = 0;
	
	@Override
	public void run() {
		long timeToRun = System.currentTimeMillis();
		while(running){

			timeToRun = System.currentTimeMillis();
			Mat temp = new Mat();
			cap.read(temp);
			buffer[i] = temp;
			System.out.println("RUN");
			timeTags[i] = System.currentTimeMillis();
			
			j++;
			i++;
			
			if(j > 2){
				j = 0;
			}
			if(i > 2){
				i = 0;
			}
			
			long tmp = (System.currentTimeMillis() - timeToRun);
			if (tmp >= 10){
			averageTime += tmp;
			if(tmp > maxTime){
				maxTime = tmp;
			}
			if(tmp < minTime){
				minTime = tmp;
			}
			count += 1;
			}
			
			if(count > 200){
				break;
			}

		}
		BufferedWriter writer;
		System.out.println("Writing");
		try {
			writer = Files.newBufferedWriter(Paths.get(System.getenv("APPDATA") + "\\3044Vision\\out.txt"),
					StandardOpenOption.WRITE);
			writer.write("avg: " + String.valueOf(averageTime/count) + "\n" + "min: "
					+ String.valueOf(minTime) + "\n Max: " + String.valueOf(maxTime));

			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	

}
