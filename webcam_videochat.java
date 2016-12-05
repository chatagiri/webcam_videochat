import java.io.*;
import java.awt.image.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.text.*;
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


	static JFrame mainFrame = null;
	static JLabel statusLabel = null;
	private JTextField sendPortField = null;
	private JTextField recvportField = null;
	private JTextField opponentIpField = null;
	static JFrame sessionFrame = null;
	static JPanel sessionPanel = null;
	static Webcam webcam = Webcam.getDefault();
	static DatagramSocket recvSocket = null;
	static DatagramSocket sendSocket = null;
	static byte[] buf = new byte[100000]; // ImagePacketBuffer
	static String opponentIp = null;
	static int opponentPort = 0;
	static InetSocketAddress sockaddr = null;

	public static void main (String args[]) throws IOException {

		String webcamName = webcam.getName();
		webcam.setViewSize(new Dimension (640,480));

		mainFrame = new JFrame("Main Menu");
		JPanel mainPanel = new JPanel();
		JPanel statusPanel = new JPanel();
		JPanel menuPanel = new JPanel();
		JLabel titleImage = new JLabel();
		JButton createbtn = new JButton("部屋を作る");
		JButton joinbtn = new JButton("部屋に入る");

		titleImage.setIcon(new ImageIcon("title.png"));

		sendPortField = new JTextField("12345",5);
		opponentIpField = new JTextField("172.16.127.160",10);
		recvPortField = new JTextField("12345",5);

		statusLabel = new JLabel("Waiting for commands...");
		
		createbtn.addActionListener(new createbtnListener());
		joinbtn.addActionListener(new joinbtnListener());

		mainPanel.setSize(150, 480);
		mainPanel.add(sendPortField);
		mainPanel.add(opponentIpField);
		mainPanel.add(recvPortField);
		mainPanel.add(createbtn);
		mainPanel.add(joinbtn);

		statusPanel.add(statusLabel);

		menuPanel.add(mainPanel,BorderLayout.NORTH);
		menuPanel.add(statusPanel,BorderLayout.SOUTH);

		mainFrame.add(menuPanel, BorderLayout.SOUTH);
		mainFrame.add(titleImage);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.pack() ;
		mainFrame.setVisible(true) ;
		

	}

	public static void makeSocket(String mode){
		try{
			if(mode.equals("join")){
				sendSocket = new DatagramSocket(Integer.parceInt()); //Port: using port in send the packet
				recvSocket = new DatagramSocket(12349); 

			}else if(mode.equals("create")){
				sendSocket = new DatagramSocket(12367);
				recvSocket = new DatagramSocket(Integer.parseInt(myportfield.getText()));
			}
			
			
		}catch(SocketException e){
	}

	}

	static void SendConnection(){
		
		sessionFrame = new JFrame("sessionFrame: Waiting for the host...");
		sessionFrame.setSize(640,960);

		try{
			String mkctn = "hello";

			sockaddr = new InetSocketAddress(ipfield.getText(), Integer.parseInt("12345")); //Port: sendtoOpponent port/address

			byte[] buf = mkctn.getBytes("UTF-8");

			DatagramPacket packet = new DatagramPacket(buf,buf.length,sockaddr);
			sendSocket.send(packet);

		}catch(Exception e){
			System.out.println("Socket Exception occurred in SendConnection() ");
			statusLabel.setText("Socket Exception");
		}
	}
	

	static void ReceiveConnection(){

		sessionFrame = new JFrame("sessionFrame: Waiting for the client...");
		sessionFrame.setSize(640,960);
		try{
	
			DatagramPacket packet = new DatagramPacket(buf,buf.length);

			recvSocket.receive(packet);
			opponentIp = packet.getAddress().toString();
			opponentPort = packet.getPort();

			sockaddr = new InetSocketAddress(opponentIp.substring(1),12349);
			sessionFrame.setTitle("sessionFrame: "+opponentIp.substring(1) +": "+ packet.getPort());

		}catch(Exception e){
			System.out.println("rcvSnc Socket Exception");
			statusLabel.setText("Socket Exception");
		}
	}

	

	public static class joinbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){

			makeSocket("join");
			SendConnection();
			SendThread st = new SendThread();
			st.start();		
			ReceiveThread rt = new ReceiveThread();
			System.out.println("receive st");
			rt.start();	
		}		
	}


	public static class createbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){

				makeSocket("create");
				ReceiveConnection();
				ReceiveThread rt = new ReceiveThread();
				System.out.println("receive st");
				rt.start();	
				SendThread st = new SendThread();
				st.start();
	
		}
	}
	
	public static class ReceiveThread extends Thread {
		

			public void run(){
				JLabel label = new JLabel();
				JPanel sessionPanel = new JPanel();

				sessionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				sessionPanel.add(new WebcamPanel(webcam) );
				try{
				Thread.sleep(5000);
			}catch(InterruptedException e){

			}
				sessionPanel.add(label);
				sessionFrame.add(sessionPanel);
				sessionFrame.setVisible(true);
				

				try{
					
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					System.out.println("waiting for input");

					for(;;){

						recvSocket.receive(packet);
						//System.out.println("aaaaaaaaaaaa  "+ packet.getSocketAddress());
						ByteArrayInputStream bais = new ByteArrayInputStream(buf);
						BufferedImage img = ImageIO.read(bais);		
						label.setIcon(new ImageIcon(img));

						}	

				}catch(Exception e){
					System.out.println("Socket error");
					statusLabel.setText("Socket error");
				}finally{
					recvSocket.close();
				}


			}

		

	}

	public static class SendThread extends Thread{

		public void run(){
				
				sessionFrame.setVisible(true);
				sessionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					try{

						while(true){
					/* Webカメラの画像をByte型に変換 */
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(webcam.getImage(), "jpg", baos);

						byte[] buf = baos.toByteArray();

						DatagramPacket packet = new DatagramPacket(buf, buf.length, sockaddr);
						sendSocket.send(packet);
						
						}
					}catch(IOException e){
						System.out.println("Input or Output error");
						statusLabel.setText("Input/Output error");
					}finally{
						sendSocket.close();
					}


		 }
		}



			
}

