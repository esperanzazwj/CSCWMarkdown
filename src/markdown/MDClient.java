package markdown;

import java.io.IOException;

public class MDClient implements Runnable {

	private Markdown markdown;
	
	public MDClient(Markdown markdown){
		this.markdown=markdown;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true){
			try {
				//获得从服务器发来的修改信息，进行对文本的修改
				String Msg=markdown.in.readUTF();
				//System.out.println("Msg reci= " + Msg);
				if (Msg.length()>0){//如果有消息
					
					if (Msg.equals("Refresh")){ //若为刷新消息
						String pos=markdown.in.readUTF();
						String wholeText=markdown.in.readUTF();
						
						markdown.lock.lock();
						//刷新全部本文
						markdown.content=wholeText;
						markdown.text.setText(markdown.content);
						markdown.text.setCaretPosition(Integer.parseInt(pos));
						//刷新markdown文本，刷新左边的struct panel
						markdown.refresh();
						markdown.refreshPanel();
						markdown.lock.unlock();
					}
					else{
						int split=Msg.indexOf(' ');//从第一个空格开始分割，前面为光标位置，后面为输入字符
						//System.out.println("split pos= " + split);
						
						String pos=Msg.substring(0, split);
						String character=Msg.substring(split+1);
						
						//System.out.println("pos= " + pos);
						//System.out.println("char= " + character);
						
						int posInt=Integer.parseInt(pos);
						if (posInt<=markdown.content.length())//若光标位置合法
						{
							markdown.lock.lock();
							markdown.content=new StringBuilder(markdown.content).insert(posInt, character).toString();
							//刷新文本
							System.out.println("content = " + markdown.content);
							
							markdown.text.setText(markdown.content);
							markdown.text.setCaretPosition(Integer.parseInt(pos)+1);
							//若按下回车，则刷新markdown文本，刷新左边的struct panel
							if (character.charAt(0) == 10){
								System.out.println("check_point refresh");
								markdown.refresh();
								markdown.refreshPanel();
							}
							markdown.lock.unlock();
						}
					}
					//发回修改完毕信号
					markdown.out.writeUTF("completed");
					//可能要加锁对out
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}
