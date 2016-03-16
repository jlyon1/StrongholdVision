package com.team3044.stronghold.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Time;
import java.text.DateFormat;
import java.time.Instant;

import javax.swing.JTextArea;
import javax.swing.text.DateFormatter;

public class ConsoleWindow extends ImageWindow {
	JTextArea text = new JTextArea();
	File logFile;
	BufferedWriter writer;
	

	public ConsoleWindow(String name, int width, int height) {
		super(name, width, height);
		p.setVisible(false);
		this.setBounds(400, 400, width, height);
		text.setVisible(true);
		text.setLineWrap(true);
		this.getContentPane().add(text);
		try {
			writer = Files.newBufferedWriter(Paths
					.get(System.getenv("APPDATA") + "\\3044Vision\\" + String.valueOf(Date.from(Instant.now()).getDay())
							+ "-" + String.valueOf(Date.from(Instant.now()).getHours())
							+ String.valueOf(Date.from(Instant.now()).getMinutes()) + "log.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	public void println(String line) {
		text.setText(text.getText() + "\n" + line);
		try {
			writer.write("[" + String.valueOf(Time.from(Instant.now())) + "]" + line + "\n");
			writer.flush();
		} catch (IOException e) {
			
		}

	}

	public void print(String line) {
		text.setText(text.getText() + line);
		try {
			writer.write("[" + String.valueOf(Time.from(Instant.now())) + "]" + line + "\n");
			writer.flush();
		} catch (IOException e) {
			
		}
	}

	public void overWrite(String line) {
		text.setText(line);
		try {
		
			writer.write("[" + String.valueOf(Time.from(Instant.now())) + "]" + line + "\n");
			writer.flush();
		} catch (IOException e) {
			
		}
	}

}
