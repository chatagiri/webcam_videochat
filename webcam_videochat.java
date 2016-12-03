import java.io.*;
import java.awt.image.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.text.*;
import java.applet.*;
import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.*;
import javax.imageio.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class webcam_videochat{


	static JFrame mainwindow = null;
	static JLabel statuslabel = null;
	static JTextField myportfield = null;
	static JTextField ipfield = null;
	static JTextField portfield = null;
	static JFrame sessionframe = null;
	static JPanel sessionpanel = null;
	static Webcam webcam = Webcam.getDefault();
	static int frame_count = 1;
	static DatagramSocket socket = null;
	static byte[] buf = new byte[100000];
	static String opponentIp = null;
	static InetSocketAddress sockaddr = null;

	public static void main (String args[]) throws IOException {

		String webcamname = webcam.getName();
		webcam.setViewSize(new Dimension (640,480));

		mainwindow = new JFrame("Main Menu");
		JPanel mainpanel = new JPanel();
		JPanel statuspanel = new JPanel();
		JPanel menupanel = new JPanel();
		JLabel titleImage = new JLabel();
		JButton createbtn = new JButton("部屋を作る");
		JButton joinbtn = new JButton("部屋に入る");

		titleImage.setIcon(new ImageIcon("title.png"));
		//Jpanel 

		myportfield = new JTextField("Port",5);
		ipfield = new JTextField("IP address",10);
		portfield = new JTextField("Port",5);

		statuslabel = new JLabel("Waiting for commands...");
		
		createbtn.addActionListener(new createbtnListener());
		joinbtn.addActionListener(new joinbtnListener());

		mainpanel.setSize(150, 480);
		mainpanel.add(myportfield);
		mainpanel.add(createbtn);
		mainpanel.add(ipfield);
		mainpanel.add(portfield);
		mainpanel.add(joinbtn);
		statuspanel.add(statuslabel);
		menupanel.add(mainpanel,BorderLayout.NORTH);
		menupanel.add(statuspanel,BorderLayout.SOUTH);
		mainwindow.add(menupanel, BorderLayout.SOUTH);
		mainwindow.add(titleImage);

		mainwindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainwindow.pack() ;
		mainwindow.setVisible(true) ;
		

	}

	static void SendConnection(){
		
		sessionframe = new JFrame("SessionFrame: Waiting for the host...");
		sessionframe.setSize(640,960);

		try{

			socket = new DatagramSocket(Integer.parseInt(portfield.getText()));
			sockaddr = new InetSocketAddress(ipfield.getText(), Integer.parseInt(portfield.getText()));

			DatagramPacket packet = new DatagramPacket(buf,buf.length,sockaddr);
			socket.send(packet);

		}catch(Exception e){
			System.out.println("Socket Exception");
			statuslabel.setText("Socket Exception");
		}
	}
	

	static void ReceiveConnection(){

		System.out.println("aaaa");

		sessionframe = new JFrame("SessionFrame: Waiting for the client...");
		
		sessionframe.setSize(640,960);
		try{

			socket = new DatagramSocket(Integer.parseInt(myportfield.getText()));
			DatagramPacket packet = new DatagramPacket(buf,buf.length);

			socket.receive(packet);
			opponentIp = packet.getAddress().toString();
			sessionframe.setTitle("SessionFrame: "+opponentIp.substring(1) + packet.getPort());

			ipfield.setText(opponentIp.substring(1));
			portfield.setText(myportfield.getText());


		}catch(Exception e){
			System.out.println("Socket Exception");
			statuslabel.setText("Socket Exception");
		}
	}

	

	public static class joinbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){
			SendConnection();
			
			SendThread st = new SendThread();
			st.start();		
			ReceiveThread rt = new ReceiveThread();
			rt.start();	
		}

		
	}

	public static class createbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){
				ReceiveConnection();
				ReceiveThread rt = new ReceiveThread();
				rt.start();	
				SendThread st = new SendThread();
				st.start();
	
		}
	}
	
	public static class ReceiveThread extends Thread {
		

			public void run(){
				JLabel label = new JLabel();
				JPanel sessionpanel = new JPanel();

				sessionframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				sessionpanel.add(new WebcamPanel(webcam) );
				sessionpanel.add(label);
				sessionframe.add(sessionpanel);
				sessionframe.setVisible(true);
				

				try{
					
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					System.out.println("waiting for input");

					for(;;){

						socket.receive(packet);
						//System.out.println("aaaaaaaaaaaa  "+ packet.getSocketAddress());
						ByteArrayInputStream bais = new ByteArrayInputStream(buf);
						BufferedImage img = ImageIO.read(bais);		
						label.setIcon(new ImageIcon(img));

						}	

				}catch(Exception e){
					System.out.println("Socket error");
					statuslabel.setText("Socket error");
				}finally{
					socket.close();
				}


			}

		

	}

	public static class SendThread extends Thread{

	//	static DatagramSocket socket = new DatagramSocket(Integer.parseInt(portfield.getText()));
		static InetSocketAddress sockaddr = new InetSocketAddress(ipfield.getText(), Integer.parseInt(portfield.getText()));

		public void run(){
				
			//	JFrame sessionframe = new JFrame("SessionFrame: " + frame_count);
				frame_count++;
			//	sessionframe.add(new WebcamPanel(webcam));
			//	sessionframe.add(label);
				sessionframe.setVisible(true);
				sessionframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					try{

						while(true){
					/* Webカメラの画像をByte型に変換 */
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(webcam.getImage(), "jpg", baos);

						byte[] buf = baos.toByteArray();
					//	System.out.println("length: == " + buf.length );

						DatagramPacket packet = new DatagramPacket(buf, buf.length, sockaddr);
						socket.send(packet);
						
						}
					}catch(IOException e){
						System.out.println("Input or Output error");
						statuslabel.setText("Input/Output error");
					}finally{
						socket.close();
					}


		 }
		}
}

