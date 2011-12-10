package mea.Chat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MeaConnections {

	private HashMap<Socket, PrintStream> writers = new HashMap<Socket, PrintStream>();
	private HashMap<Socket, BufferedReader> readers = new HashMap<Socket, BufferedReader>();
	private HashMap<Socket, MeaChatThread> chatThreads = new HashMap<Socket, MeaChatThread>();
	private Vector<Socket> clients = new Vector<Socket>();
	
	@SuppressWarnings("unused")
	private JavaPlugin plugin;
	private MeaChat chat;
	private MeaServerSocket server;
	private MeaIRC irc;
	
	public MeaConnections(JavaPlugin plugin, MeaChat chat, MeaServerSocket meaServerSocket, MeaIRC irc) {
		this.plugin = plugin;
		this.chat = chat;
		this.server = meaServerSocket;
		this.irc = irc;
		
		Runnable sanity = new Runnable(){
			public void run(){
				while(true){
					for(Socket client : clients){
						if(client.isClosed()){
							writers.remove(client);
							readers.remove(client);
							clients.remove(client);
						}
					}
					try{
						Thread.sleep(200);
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		};
		Thread sanityThread = new Thread(sanity);
		sanityThread.start();
	}
	
	public void setIRC(MeaIRC irc){
		this.irc = irc;
	}

	public void addConnection(Socket socket){
		try{
			readers.put(socket, new BufferedReader(new InputStreamReader(socket.getInputStream())));
			writers.put(socket, new PrintStream(socket.getOutputStream()));
			clients.add(socket);
			MeaChatThread chThread = new MeaChatThread(socket, writers.get(socket), readers.get(socket), chat, server, this, irc);
			Thread thread = new Thread(chThread);
			thread.start();
			chatThreads.put(socket, chThread);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void removeConnection(Socket socket, String username){
		readers.remove(socket);
		writers.remove(socket);
		clients.remove(socket);
		sendToIRC("[mea] Client Disconnected: "+username);
		sendToMinecraft("[mea] Client Disconnected: "+username);
		sendToMea("[mea] Client Disconnected: "+username);
	}
	
	public void removeConnectionNoClient(Socket socket, String username){
		if(!chatThreads.get(socket).getUsername().equalsIgnoreCase(username)){
			sendToIRC("[mea] Client Disconnected: "+username);
			sendToMinecraft("[mea] Client Disconnected: "+username);
			sendToMea("[mea] Client Disconnected: "+username);
		}
		readers.remove(socket);
		writers.remove(socket);
	}
	
	public void disconnectAll(){
		for(Socket client : clients){
			writers.get(client).println("Disconnected by server. (Crash? Shutdown? Don't ask me...)");
			removeConnectionNoClient(client, getUsername(client));
		}
		clients.clear();
	}
	
	public void killIRC(){
		if(irc!=null){
			irc.kill();
		}
	}
	
	public String getUsername(Socket socket){
		return chatThreads.get(socket).getUsername();
	}
	
	public void broadcast(String message){
		for(Socket client : clients){
			writers.get(client).println(message);
		}
	}
	
	public void broadcastToAll(String message){
		sendToIRC(message);
		sendToMinecraft(message);
		sendToMea(message);
	}
	
	public void sendToIRC(String message){
		if(irc!=null){
			irc.sendMessage(message);
		}
	}
	
	public void sendToMinecraft(String message){
		Bukkit.getServer().broadcastMessage(message);
	}
	
	public void sendToMea(String message){
		broadcast(message);
	}
	
	public void sendToMea(String message, Socket sock){
		writers.get(sock).println(message);
	}

	public boolean isLoggedIn(String username) {
		boolean loggedIn = false;
		for(Socket client : clients){
			if(getUsername(client).equalsIgnoreCase(username)){
				loggedIn = true;
				break;
			}
		}
		return loggedIn;
	}
}