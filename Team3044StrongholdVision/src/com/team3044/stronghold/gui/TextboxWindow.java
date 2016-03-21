package com.team3044.stronghold.gui;

import java.awt.Container;
import java.awt.event.WindowAdapter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class TextboxWindow extends JFrame{
	public TextboxWindow(String name, int width, int height) {
		setTitle(name);
		setSize(width, height); 
		setLocation(10, 200); 
		Container pane = this.getContentPane();
		JPanel panel = new JPanel();
		
		JSlider h_min = new JSlider(0,180);
		h_min.setVisible(true);
		panel.add(h_min);
		
		JSlider s_min = new JSlider(0,180);
		s_min.setVisible(true);
		panel.add(s_min);
		
		JSlider v_min = new JSlider(0,180);
		v_min.setVisible(true);
		panel.add(v_min);
		v_min.setPaintTicks(true);
		v_min.setPaintLabels(true);

		pane.add(panel);
		pane.setVisible(true);
		this.setAlwaysOnTop(true);
		addWindowListener(new WindowAdapter() {

		});
		this.setVisible(true);
		
	}

}
