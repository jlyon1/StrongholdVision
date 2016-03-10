package com.team3044.stronghold.gui;

import java.awt.TextField;

public class ConsoleWindow extends ImageWindow{
	TextField text = new TextField();
	public ConsoleWindow(String name, int width, int height) {
		super(name, width, height);
		p.setVisible(false);
		this.setBounds(400, 400, width, height);
		text.setVisible(true);
		this.getContentPane().add(text);
	}
	
	public void println(String line){
		text.setText(text.getText() + "\n" + line);
		
	}
	public void print(String line){
		text.setText(text.getText() + line);
	}

}
