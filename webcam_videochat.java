import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.net.*;
import javax.swing.*;
import javax.imageio.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;

public class webcam_videochat{

	static JFrame mainFrame = null;
	static JLabel statusLabel = null;
	static JTextField opponentPortField = null;
	static JTextField recvPortField = null;
	static JTextField opponentIpField = null;
	static JFrame sessionFrame = null;
	static JPanel sessionPanel = null;
	static Webcam webcam = Webcam.getDefault();
	static DatagramSocket recvSocket = null;
	static DatagramSocket sendSocket = null;
	static DatagramSocket opponentSocket = null;
	static byte[] buf = new byte[100000]; // ImagePacketBuffer
	static String opponentIp = null;
	static int opponentPort = 0;
	static int sendSocketValue = 12776; //sending packet port
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

		recvPortField = new JTextField("Port",5);
		opponentPortField = new JTextField("Port",5);
		opponentIpField = new JTextField("IP address",10);
		statusLabel = new JLabel("Waiting for commands...");
		
		createbtn.addActionListener(new createbtnListener());
		joinbtn.addActionListener(new joinbtnListener());

		mainPanel.setSize(150, 480);
		mainPanel.add(recvPortField);
		mainPanel.add(opponentPortField);
		mainPanel.add(opponentIpField);
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
	

		public static class joinbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){

			makeSocket();
			SendConnection();
			SendThread st = new SendThread();
			ReceiveThread rt = new ReceiveThread();
			st.start();	 // Sending stream earlyer to send own Ip address	
			rt.start();	
		}		
	}


	public static class createbtnListener implements ActionListener{

		public void actionPerformed(ActionEvent ae){

			makeSocket();
			ReceiveConnection();
			SendThread st = new SendThread();
			ReceiveThread rt = new ReceiveThread();
			rt.start();	 // Receiving stream earlyer to get opponent IP address
			st.start();
		}
	}


	public static void makeSocket(){

		try{                                       
			sendSocket = new DatagramSocket(sendSocketValue);
			recvSocket = new DatagramSocket(Integer.parseInt(recvPortField.getText()));
		}catch(SocketException e){
			System.out.println("SocketException occurred in makeSocket()");
		}
	}


	static void SendConnection(){
		
		sessionFrame = new JFrame("sessionFrame: Waiting for the host...");
		sessionFrame.setSize(640,960);

		try{
			String msg = "hello";
			byte[] buf = msg.getBytes("UTF-8");
			sockaddr = new InetSocketAddress(opponentIpField.getText(),Integer.parseInt(opponentPortField.getText())); //Port: sendtoOpponent port/address
			DatagramPacket packet = new DatagramPacket(buf,buf.length,sockaddr);
			sendSocket.send(packet);

		}catch(Exception e){
			System.out.println("SocketException occurred in SendConnection()");
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
			sockaddr = new InetSocketAddress(opponentIp.substring(1),Integer.parseInt(opponentPortField.getText()));
			sessionFrame.setTitle("sessionFrame: "+opponentIp.substring(1) +":"+ packet.getPort());

		}catch(Exception e){
			System.out.println("SocketException occurred in ReceiveConnection()");
			statusLabel.setText("Socket Exception");
		}
	}

	
	public static class ReceiveThread extends Thread {

		public void run(){

			JLabel label = new JLabel();
			JPanel sessionPanel = new JPanel();
			sessionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			sessionPanel.add(new WebcamPanel(webcam));

			try{
				Thread.sleep(5000);
			}catch(InterruptedException e){
				System.out.println("InterruptedException occurred in ReceiveThread()");
			}

			sessionPanel.add(label);
			sessionFrame.add(sessionPanel);
			sessionFrame.setVisible(true);	

			try{	
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				System.out.println("waiting for input");

				for(;;){

					recvSocket.receive(packet);
					ByteArrayInputStream bais = new ByteArrayInputStream(buf);
					BufferedImage img = ImageIO.read(bais);		
					label.setIcon(new ImageIcon(img));
				}	
			}catch(Exception e){
				System.out.println("IOException accoured in ReceiveThread()");
				statusLabel.setText("Input/Output error");
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

				try{
					Thread.sleep(5000);
				}catch(InterruptedException e){
					System.out.println("InterruptedException occurred in ReceiveThread()");
				}

				while(true){
					/* Webカメラの画像をByte型に変換 */
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(webcam.getImage(), "jpg", baos);

					byte[] buf = baos.toByteArray();

					DatagramPacket packet = new DatagramPacket(buf, buf.length, sockaddr);
					sendSocket.send(packet);
						
				}
			}catch(IOException e){
				System.out.println("IOException accoured in SendThread()");
				statusLabel.setText("Input/Output error");
			}finally{
				sendSocket.close();
			}
		}
	}		
}

