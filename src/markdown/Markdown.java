package markdown;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.markdown4j.Markdown4jProcessor;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Markdown {
	
	private JFrame frame;
	private JPanel panel;
	private MenuBar bar;
	private Menu fileMenu,toolMenu;
	private MenuItem openItem,saveItem,saveAsHTMLItem,saveAsWordItem;
	private MenuItem addCSSItem;
	
	private JScrollPane jsp;
	public JTextArea text;
	public String content=new String();
	private TextArea text_converted; 
	private int height;
	
	private JList<String> structList;
	private Vector<String> listData=new Vector<>();
	private Vector<Integer> lineNumber=new Vector<>();
	
	private boolean loaded=false;
	private String dirPath=null;
	private String fileName=null;
	private String cssInfo=new String();
	
	private MDClient mdClient;
	private int port;
	
	Socket socket;
	public DataInputStream in;
	public DataOutputStream out;
	
	public Lock lock= new ReentrantLock();
	
	private int insertFlag=0;
	private int removeFlag=0;
	private boolean ctrlDown=false;
	
	public Markdown(int port)
	{
		this.port=port;
		
		frame=new JFrame("markdown");
		frame.setSize(1280, 720);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		LayoutManager layout=new BorderLayout();
		frame.setLayout(layout);
		initMenu();
		initText();
		initPanel();
		initEvent();
		initConnect();
		initThread();
		//frame.pack();
		frame.setVisible(true);
	}
	
	public void initMenu(){
		bar = new MenuBar();
		
		fileMenu = new Menu("Menu");
		toolMenu = new Menu("Tool");
			
		openItem = new MenuItem("Open");
		saveItem = new MenuItem("Save");
		saveAsHTMLItem = new MenuItem("SaveAs HTML");
		saveAsWordItem = new MenuItem("SaveAs Word");
		
		addCSSItem=new MenuItem("Add CSS");
		
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsHTMLItem);
		fileMenu.add(saveAsWordItem);
		
		toolMenu.add(addCSSItem);
		
		bar.add(fileMenu);
		bar.add(toolMenu);
		
		frame.setMenuBar(bar);
		
	}

	public void initText(){
		text=new JTextArea();
		Font f=new Font("serif", Font.BOLD, 18);
		text.setFont(new Font("serif", Font.BOLD, 18));
		
		FontMetrics fm = text.getFontMetrics(f);
		height=fm.getHeight();	
		
		jsp=new JScrollPane(text);
		text_converted=new TextArea();
		frame.add(jsp,BorderLayout.CENTER);
		frame.add(text_converted,BorderLayout.EAST);
		cssInfo="";
	}

	private void initPanel(){
		panel=new JPanel();
		structList=new JList<String>();
		structList.setBorder(BorderFactory.createTitledBorder("structure"));
		structList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		refreshPanel();
		panel.add(structList);
		frame.add(panel,BorderLayout.WEST);
	}
	private void initEvent()
	{
		openItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog openDia = new FileDialog(frame,"open",FileDialog.LOAD);	
				openDia.setAutoRequestFocus(true);
				openDia.setVisible(true);
				dirPath=openDia.getDirectory();
				fileName=openDia.getFile();
				
				if (dirPath==null || fileName==null) return;
				try{
					BufferedReader bufr = new BufferedReader(new FileReader(new File(dirPath,fileName)));
					lock.lock();
					text.setText("");
					String line = null;
					while( (line = bufr.readLine())!= null)
					{
						text.append(line +"\r\n");
					}
					bufr.close();
					loaded=true;
					cssInfo="";
					refresh();
					
					out.writeUTF("Refresh");
					out.writeUTF("0");
					out.writeUTF(text.getText());
					
					lock.unlock();
				}catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
			
		});
		
		saveItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!loaded){
					FileDialog saveDia=new FileDialog(frame,"save",FileDialog.SAVE);
					saveDia.setAutoRequestFocus(true);
					saveDia.setVisible(true);
					dirPath = saveDia.getDirectory();
					fileName = saveDia.getFile();
				}
				if (dirPath==null || fileName==null) return;
				try
				{
					BufferedWriter bufw = new BufferedWriter(new FileWriter(new File(dirPath,fileName)));
					String content = text.getText();
					bufw.write(content);
					bufw.close();
					loaded=true;
				}catch (IOException ex){
					ex.printStackTrace();
				}	
			}
			
		});
		
		saveAsHTMLItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				FileDialog saveAsDia=new FileDialog(frame,"save as",FileDialog.SAVE);
				saveAsDia.setAutoRequestFocus(true);
				saveAsDia.setVisible(true);
				String SaveAsdir=saveAsDia.getDirectory();
				String SaveAsname=saveAsDia.getFile();
				if (!SaveAsname.endsWith(".html")){
					 JOptionPane.showMessageDialog(null, "not html", "Error", JOptionPane.ERROR_MESSAGE);
					 return;
				}
				try{
					BufferedWriter bufw = new BufferedWriter(new FileWriter(new File(SaveAsdir,SaveAsname)));
					
					lock.lock();
					bufw.write(cssInfo + new Markdown4jProcessor().process(text.getText()));
					lock.unlock();
					
					bufw.close();
				}catch (IOException ex){
					ex.printStackTrace();
				}
			}
		});
		
		saveAsWordItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog saveAsDia=new FileDialog(frame,"save as",FileDialog.SAVE);
				saveAsDia.setAutoRequestFocus(true);
				saveAsDia.setVisible(true);
				String SaveAsdir=saveAsDia.getDirectory();
				String SaveAsname=saveAsDia.getFile();
				if (!SaveAsname.endsWith(".docx")){
					 JOptionPane.showMessageDialog(null, "not docx", "Error", JOptionPane.ERROR_MESSAGE);
					 return;
				}
				
				String content;
				try {
					
					lock.lock();
					content = cssInfo + new Markdown4jProcessor().process(text.getText());		
					lock.unlock();
					
					byte b[] = content.getBytes();
					ByteArrayInputStream bais = new ByteArrayInputStream(b);
					POIFSFileSystem poifs = new POIFSFileSystem();
					DirectoryEntry directory = poifs.getRoot();
					DocumentEntry documentEntry = directory.createDocument("WordDocument", bais);
					FileOutputStream ostream = new FileOutputStream(SaveAsdir + SaveAsname);
					poifs.writeFilesystem(ostream);
					bais.close();
					ostream.close();
					poifs.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
		});
		
		//add css item
		addCSSItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog cssDia=new FileDialog(frame,"open css",FileDialog.LOAD);
				cssDia.setVisible(true);
				String fdir=cssDia.getDirectory();
				String fname=cssDia.getFile();
				if (fdir==null || fname==null) return;
				if (!fname.endsWith(".css")){
					 JOptionPane.showMessageDialog(null, "not css", "Error", JOptionPane.ERROR_MESSAGE);
					 return;
				}
				cssInfo+="<link href=\""+fdir+fname+"\" rel=\"stylesheet\" type=\"text/css\" />\n";
				
				lock.lock();
				refresh();
				lock.unlock();
			}
			
		});
		
		text.addKeyListener(new KeyListener(){
			//when typed send message to server
			@Override
			public void keyTyped(KeyEvent e) {
				char ch=e.getKeyChar();
				//判断是否为有效字符
				if (Character.isLetter(ch) || ch == '\n' || ch == '\t' || (ch>=32 && ch<=126)){
					lock.lock();
					
					String Msg=null;
					if (ch =='\n')
						Msg = Integer.toString(text.getCaretPosition()-1)+" "+"\n";
					else if (ch =='\t')
						Msg = Integer.toString(text.getCaretPosition()-1)+" "+"\t";
					else 
						Msg = Integer.toString(text.getCaretPosition())+" "+ch;
					lock.unlock();
					try {
						out.writeUTF(Msg);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					//System.out.println("Msg out= " + Msg);
					//System.out.println("Msg out keycode= " + e.getKeyCode());
					
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				//判断ctrl,backspace,delete
				switch (e.getKeyCode()){
					case 8:removeFlag=1;break;
					case 127:removeFlag=2;break;
					case 17:ctrlDown=true;break;//ctrl是否按下
					case 88:if (ctrlDown) removeFlag=2;break;//ctrl+x
					case 86:if (ctrlDown) insertFlag=1;break;//ctrl+v
				}
				//System.out.println("Msg out keycode= " + e.getKeyCode());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==17)
					ctrlDown=false;
			}
			
		});
		
		text.getDocument().addDocumentListener(new DocumentListener(){

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				
				//Ctrl+V
				if (insertFlag>0){
					lock.lock();
					try {
						out.writeUTF("Refresh");
						out.writeUTF(Integer.toString(text.getCaretPosition() + arg0.getLength()));
						out.writeUTF(text.getText());
					} catch (IOException e) {
						e.printStackTrace();
					}
					lock.unlock();
					insertFlag=0;
				}
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				
				//backspace和delete则全部刷新,Ctrl+x
				if (removeFlag>0){
					lock.lock();
					try {
						out.writeUTF("Refresh");
						out.writeUTF(Integer.toString(arg0.getOffset()));	
						out.writeUTF(text.getText());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					lock.unlock();
					removeFlag=0;
				}
			}
			
		});
		structList.addListSelectionListener(new ListSelectionListener(){

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int[] index = structList.getSelectedIndices();
		        int idx=-1;
		        if (index.length>0)
		        	idx=index[0];
		        
		        if (idx!=-1){
		        	Point p = new Point();
		        	p.setLocation(0, lineNumber.get(idx) * height); 
		        	jsp.getViewport().setViewPosition(p);	     
		        }
		        
		        text.requestFocus();
			}
			
		});
	}
	
	private void initThread(){
		mdClient = new MDClient(this);
		Thread thread=new Thread(mdClient);
		thread.start();
	}
	
	private void initConnect(){
		try {
			socket = new Socket("localhost",port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    try {
	    	in = new DataInputStream(socket.getInputStream());
	    	out =  new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public void refresh(){
		try {
			text_converted.setText(cssInfo + new Markdown4jProcessor().process(text.getText()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void refreshPanel(){
		try
		{
			listData.clear();lineNumber.clear();
			listData.add("no structure selected");
			lineNumber.add(0);
			BufferedReader br = new BufferedReader(new StringReader(text.getText()));
			String line = null;
			int count=0;
			while( (line = br.readLine())!= null)
			{
				int h=0;
				while (h<line.length() && line.charAt(h)=='#') h++;
				if (h!=0){
					listData.add(line.substring(h));
					lineNumber.add(count);
				}
				count++;
			}
			structList.setListData(listData);
			br.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}	
	}
	
}