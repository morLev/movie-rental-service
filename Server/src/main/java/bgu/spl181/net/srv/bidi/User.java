package bgu.spl181.net.srv.bidi;

import java.util.LinkedList;

public class User {
	
	public String username;
	public String type;
	public String password;
	public String country;
	public LinkedList<MovieForUser> movies;
	public String balance;
	
	
	public User(String username, String type, String password, String country, LinkedList<MovieForUser> movies, String balance) {
		this.username = username;
		this.type = type;
		this.password = password;
		this.country = country;
		this.movies = movies;
		this.balance = balance;
	}

}
