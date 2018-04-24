package markdown;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MDServer implements Runnable{
	private ServerSocket s;
	private int port;
	public MDServer(int port){
		this.port=port;
	}
	
	@Override
	protected void finalize(){	
		try {
			if (s!=null)
				s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static class MDServerListener implements Runnable{

		private DataInputStream in;
		private DataOutputStream out;
		
		private static Lock lock= new ReentrantLock();
		private static boolean locking = false;
		private static int ClientCompleteModify=0;
		private static int ClientCounter=0;
		private static ArrayList<MDServerListener> ClientList=new ArrayList<>();
		
		public MDServerListener(Socket socket){
			
			lock.lock();
			ClientCounter++;
			ClientList.add(this);
			lock.unlock();
			
		    try {
		    	in = new DataInputStream(socket.getInputStream());
		    	out =  new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			while (true){
				try {
					String Msg = in.readUTF();
					if (Msg.length()>0){ //若有消息
						if (Msg.equals("completed")){ //若为同步完成消息
							lock.lock();
							ClientCompleteModify++;
							if (ClientCompleteModify==ClientCounter){
								locking=false; //如果所有客户机都同步完成，则解锁
							}
							lock.unlock();
						}
						else if (Msg.equals("Refresh")){ //若为刷新消息
							String pos=in.readUTF();
							String wholeText=in.readUTF();
							//System.out.println("wholeText in Server= " + wholeText);
							
							lock.lock();
							if (locking==false){
								//可做修改，发送消息至每个client
								for (int i=0;i<ClientList.size();i++){
									ClientList.get(i).getOut().writeUTF(Msg);
									ClientList.get(i).getOut().writeUTF(pos);
									ClientList.get(i).getOut().writeUTF(wholeText);
								}
								locking=true;
								ClientCompleteModify=0;
							}
							lock.unlock();
						}
						else{ //普通消息
							lock.lock();
							if (locking==false){
								//可做修改，发送消息至每个client
								for (int i=0;i<ClientList.size();i++){
									ClientList.get(i).getOut().writeUTF(Msg);
								}
								locking=true;
								ClientCompleteModify=0;
							}
							lock.unlock();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public DataOutputStream getOut(){
			return out;
		}
		
	}

	@Override
	public void run() {
		try {
			s = new ServerSocket(port);
			System.out.println("Server Started");
			while (true)
			{
				Socket socket = null;
				socket = s.accept();
				new Thread(new MDServerListener(socket)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
