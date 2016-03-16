package com.team3044.stronghold.vision;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.jfree.io.IOUtils;

public class Loader {
	public static void loadLibrary() {
		try {
			FileInputStream in = null;
			File fileOut = null;
			String osName = System.getProperty("os.name");
			System.out.println(osName);
			if (osName.startsWith("Windows")) {
				int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
				
				in = new FileInputStream("C:/Opencv3.0.0/opencv/build/java/x64/opencv_java300.dll");
				fileOut = File.createTempFile("lib", ".dll");

			}
			System.out.println(in == null);
			FileOutputStream out = new FileOutputStream(fileOut);
			IOUtils.getInstance().copyStreams(in, out);
			in.close();
			out.close();
			System.load(fileOut.toString());
		} catch (Exception e) {
			e.printStackTrace();
			//throw new RuntimeException("Failed to load opencv native library", e);
		}

	}
}
