package com.team3044.stronghold.vision;

public class CameraIsNotOpenException extends Exception {
	public void printStackTrace(){
		System.out.println("Camera Is not Opened, Must have an opened VideoCapture");
	}

}
