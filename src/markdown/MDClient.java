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
				//��ôӷ������������޸���Ϣ�����ж��ı����޸�
				String Msg=markdown.in.readUTF();
				//System.out.println("Msg reci= " + Msg);
				if (Msg.length()>0){//�������Ϣ
					
					if (Msg.equals("Refresh")){ //��Ϊˢ����Ϣ
						String pos=markdown.in.readUTF();
						String wholeText=markdown.in.readUTF();
						
						markdown.lock.lock();
						//ˢ��ȫ������
						markdown.content=wholeText;
						markdown.text.setText(markdown.content);
						markdown.text.setCaretPosition(Integer.parseInt(pos));
						//ˢ��markdown�ı���ˢ����ߵ�struct panel
						markdown.refresh();
						markdown.refreshPanel();
						markdown.lock.unlock();
					}
					else{
						int split=Msg.indexOf(' ');//�ӵ�һ���ո�ʼ�ָǰ��Ϊ���λ�ã�����Ϊ�����ַ�
						//System.out.println("split pos= " + split);
						
						String pos=Msg.substring(0, split);
						String character=Msg.substring(split+1);
						
						//System.out.println("pos= " + pos);
						//System.out.println("char= " + character);
						
						int posInt=Integer.parseInt(pos);
						if (posInt<=markdown.content.length())//�����λ�úϷ�
						{
							markdown.lock.lock();
							markdown.content=new StringBuilder(markdown.content).insert(posInt, character).toString();
							//ˢ���ı�
							System.out.println("content = " + markdown.content);
							
							markdown.text.setText(markdown.content);
							markdown.text.setCaretPosition(Integer.parseInt(pos)+1);
							//�����»س�����ˢ��markdown�ı���ˢ����ߵ�struct panel
							if (character.charAt(0) == 10){
								System.out.println("check_point refresh");
								markdown.refresh();
								markdown.refreshPanel();
							}
							markdown.lock.unlock();
						}
					}
					//�����޸�����ź�
					markdown.out.writeUTF("completed");
					//����Ҫ������out
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}
