package bgu.spl181.net.srv.bidi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;


public class SharedData {

	public Users users;
	public Movies movies;
	public HashMap<Integer, String> loggedInMap; // username by connaction ID
	public String path;
	public ReadWriteLock readWriteLockUsers = new ReentrantReadWriteLock();
	public ReadWriteLock readWriteLockMovies = new ReentrantReadWriteLock();
	public ReadWriteLock readWriteLockloggedIn = new ReentrantReadWriteLock();
	
	public SharedData(){
		loggedInMap = new HashMap<Integer, String>();

		path = null;
		try{
		 	path = new java.io.File( "." ).getCanonicalPath() + "/Database/" ;
		}
		catch(IOException e){}
		initialize();
	}
	
	public LinkedList<Movie> getMovies(LockType lockType){
		
		if(lockType == LockType.WRITE)
			readWriteLockMovies.writeLock().lock();
		else
			readWriteLockMovies.readLock().lock();
		
		return movies.movies;
	}
	
	public void unlockMovies(LockType lockType){
		
		if(lockType == LockType.WRITE){
			writeToMoviesJson();
			readWriteLockMovies.writeLock().unlock();
		}
		else
			readWriteLockMovies.readLock().unlock();

	}
	
	public LinkedList<User> getUsers(LockType lockType){
		
		if(lockType == LockType.WRITE)
			readWriteLockUsers.writeLock().lock();
		else
			readWriteLockUsers.readLock().lock();
		
		return users.users;
	}
	
	public void unlockUsers(LockType lockType){
		
		if(lockType == LockType.WRITE){
			writeToUsersJson();
			readWriteLockUsers.writeLock().unlock();	
		}
		else
			readWriteLockUsers.readLock().unlock();	
	}
	
	public HashMap<Integer, String> getLoggedIn(LockType lockType){
		
		if(lockType == LockType.WRITE)
			readWriteLockloggedIn.writeLock().lock();
		else
			readWriteLockloggedIn.readLock().lock();
		
		return loggedInMap;
	}
	
	public void unlockLoggedIn(LockType lockType){
		
		if(lockType == LockType.WRITE)
			readWriteLockloggedIn.writeLock().unlock();
		else
			readWriteLockloggedIn.readLock().unlock();
	}
	
  
	public void initialize(){
		
		Gson gson = new Gson();
		JsonReader reader;

		try {
			reader = new JsonReader(new FileReader(path + "Movies.json"));
			movies = gson.fromJson(reader, Movies.class);
			reader = new JsonReader(new FileReader(path + "Users.json"));
			users = gson.fromJson(reader, Users.class);
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeToMoviesJson(){
		
		Gson gson = new Gson();
		FileWriter writer;
		 
		try {
			writer = new FileWriter(path + "Movies.json");
			String jsonString = gson.toJson(movies);
			writer.write(jsonString);
			writer.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeToUsersJson(){
		
		Gson gson = new Gson();
		FileWriter writer;
		 
		try {
			writer = new FileWriter(path + "Users.json");
			String jsonString = gson.toJson(users);
			writer.write(jsonString);
			writer.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	

}
