package com.team3044.stronghold.vision;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class AxisGrabber implements Runnable{

	private VideoCapture cap;
	private boolean isReady;
	private boolean running = true;
	
	private Mat[] buffer = new Mat[3];
	private double[] timeTags = new double[3];
	private int i = 0, j = 2;
	
	public AxisGrabber(VideoCapture capture) throws CameraIsNotOpenException{
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
	
	@Override
	public void run() {
		while(running){
			cap.read(buffer[i]);
			timeTags[i] = System.currentTimeMillis();
			
			j++;
			i++;
			
			if(j > 2){
				j = 0;
			}
			if(i > 2){
				i = 0;
			}
		}
		
	}
	

}
