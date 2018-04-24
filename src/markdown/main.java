package markdown;

import markdown.Markdown;

public class main{
	
	public static void main(String[] args) {
		
		int port=8000;
		//start sever
		new Thread(new MDServer(port)).start();
		//open 2 markdown
		Markdown markdown0 = new Markdown(port);
		Markdown markdown1 = new Markdown(port);
		while (true){}
	}

}