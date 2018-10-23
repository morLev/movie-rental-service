package bgu.spl181.net.impl.bidi;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.srv.bidi.ClientStatus;
import bgu.spl181.net.srv.bidi.LockType;
import bgu.spl181.net.srv.bidi.MovieForUser;
import bgu.spl181.net.srv.bidi.SharedData;
import bgu.spl181.net.srv.bidi.User;

public class UserServiceTextbasedprotocol implements BidiMessagingProtocol<String> {

	protected int connectionId;
	protected Connections<String> connections;
	protected boolean shouldTerminate;
	protected SharedData sharedData;
	protected ClientStatus clientStatus;
	protected String username;
	
	public UserServiceTextbasedprotocol(SharedData sharedData) {
		this.sharedData = sharedData;
		shouldTerminate = false;
		clientStatus = ClientStatus.UNLOGGED;
		username = null;
	}
	
	@Override
	public void start(int connectionId, Connections<String> connections) {
		this.connectionId = connectionId;
		this.connections = connections;
	}

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}

	@Override
	public void process(String message) {	
		
		if(message.equals("SIGNOUT"))
			signOut();
		else{
			String messagePrefix = message.substring(0, message.indexOf(" "));
			String messageSuffix = message.substring(message.indexOf(" ")+1);
		
			if(messagePrefix.equals("REGISTER"))
				register(messageSuffix);
			else if(messagePrefix.equals("LOGIN"))
				login(messageSuffix);
			else if(messagePrefix.equals("REQUEST"))
				request(messageSuffix);
		}
	}

	private boolean isLoggedIn() {
		return clientStatus != ClientStatus.UNLOGGED;
	}
	
	
	//****************************** Client commands ******************************

	public void register(String message){
			
		LinkedList<User> users = sharedData.getUsers(LockType.WRITE);
		String username;
		String password;
		String dataBlock;
		String restOfMassage;

		// check if some info is missing (username/password)
		// or if the client performing the register call is already logged in
		if(!message.contains(" ") || isLoggedIn()){
			sharedData.unlockUsers(LockType.WRITE);
			sendERROR("registration failed");
			return;
		}
		else{
			username = message.substring(0, message.indexOf(" "));
			restOfMassage = message.substring(message.indexOf(" ")+1);
			
			if(!restOfMassage.contains(" "))
				password = restOfMassage;
	
			else{
				password = restOfMassage.substring(0, restOfMassage.indexOf(" "));
				dataBlock = restOfMassage.substring(restOfMassage.indexOf(" ")+1);
			}
	
			// check if the username requested is already registered in the system
			for (int i = 0 ; i < users.size() ; i++) 
				if(username.toLowerCase().equals(users.get(i).username.toLowerCase())){
					sharedData.unlockUsers(LockType.WRITE);
					sendERROR("registration failed");
					return;
				}
			
			//  create a new user and add him to the users list
			User newUser = new User(username, "normal", password, "", new LinkedList<MovieForUser>(), "0");
			users.add(newUser);
			
			// update "Users.json"
			sharedData.unlockUsers(LockType.WRITE);
			handleRegister(message);
		}
	}
	
	public void handleRegister(String message) {}
	
	public void login(String message){
		
		LinkedList<User> users = sharedData.getUsers(LockType.READ);
		HashMap<Integer, String> loggedInMap = sharedData.getLoggedIn(LockType.WRITE); 

		String username;
		String password;
	
		// check if if the message does not consists of two words(username and password)
		// or if the client already performed successful LOGIN command
		if(!message.contains(" ") || isLoggedIn()){
			sharedData.unlockLoggedIn(LockType.WRITE);
			sharedData.unlockUsers(LockType.READ);
			sendERROR("login failed");
			return;
		}
		
		username = message.substring(0, message.indexOf(" "));
		password = message.substring(message.indexOf(" ")+1);
	
		// check if the given username is already logged in
		for (Map.Entry<Integer, String> entry : loggedInMap.entrySet())
			if(entry.getValue().toLowerCase().equals(username.toLowerCase())){
				sharedData.unlockLoggedIn(LockType.WRITE);
				sharedData.unlockUsers(LockType.READ);
				sendERROR("login failed");
				return;
			}
			
		// check if username and Password combination fit to a user in the system
		for (int i = 0 ; i < users.size() ; i++) 
			if(username.toLowerCase().equals(users.get(i).username.toLowerCase()) && password.equals(users.get(i).password)){
				loggedInMap.put(connectionId, username);
					
				this.username = username;
				updateClientStatus(this.username, users);
				
				sharedData.unlockLoggedIn(LockType.WRITE);
				sharedData.unlockUsers(LockType.READ);
				sendACK("login succeeded");
				return;
			}
		
		// the username and the password combination does not fit any user in the system
		sharedData.unlockLoggedIn(LockType.WRITE);
		sharedData.unlockUsers(LockType.READ);
		sendERROR("login failed");
	}

	public void signOut(){
		
		if(isLoggedIn()){
			
			HashMap<Integer, String> loggedInMap = sharedData.getLoggedIn(LockType.WRITE); 
			
			// remove the user from loggedInMap
			String username = null;
			for (Map.Entry<Integer, String> entry : loggedInMap.entrySet())
				if(entry.getKey().equals(connectionId)){
					username = entry.getValue();
				}
			loggedInMap.remove(connectionId);
			
			sharedData.unlockLoggedIn(LockType.WRITE);
			sendACK("signout succeeded");
			shouldTerminate = true; // After a successful ACK for sign out the client should terminate
		}
		sendERROR("signout failed");
	}
	
	public void request(String message){
		
		if(!isLoggedIn()){
		
			String requestName;
			if(!message.contains(" "))
				requestName = message;
			else
				requestName = message.substring(0, message.indexOf(" "));
			
			final String errorStr = "request " + requestName + " failed";
			sendERROR(errorStr);
		}
		else
			handleRequest(message);	
	}
	
	public void handleRequest(String message) {}

	public void sendERROR(String msg){
		connections.send(connectionId, "ERROR " + msg);
	}
	
	public void sendACK(String msg){
		connections.send(connectionId, "ACK " + msg);
	}
	
	public void sendACKandBROADCAST(String ackMsg, String broadcastMsg){
		connections.send(connectionId, "ACK " + ackMsg);
		
		HashMap<Integer, String> loggedInMap = sharedData.getLoggedIn(LockType.READ);
		
		for(Map.Entry<Integer, String> entry : sharedData.loggedInMap.entrySet())
			connections.send(entry.getKey(), "BROADCAST " + broadcastMsg);
		
		sharedData.unlockLoggedIn(LockType.READ);
	}

	// return the user named userName if he exist, else return null
	private User getUserByName(String userName){
		
		LinkedList<User> listOfUsers = sharedData.users.users;
		
		for (int i=0 ; i<listOfUsers.size() ; i++) {
			if(listOfUsers.get(i).username.toLowerCase().equals(userName.toLowerCase()))
				return listOfUsers.get(i);
		}
		return null;
	}

	private void updateClientStatus(String username, LinkedList<User> users) {
		
		User myUser = getUserByName(username);
			
		if(myUser.type.equals("admin"))
			clientStatus = ClientStatus.ADMIN;
		
		else
			clientStatus = ClientStatus.NORMAL;
	}

}



