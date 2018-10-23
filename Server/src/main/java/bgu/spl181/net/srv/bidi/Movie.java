package bgu.spl181.net.srv.bidi;

import java.util.LinkedList;

public class Movie {
	
	public String id;
	public String name;
	public String price;
	public LinkedList<String> bannedCountries;
	public String availableAmount;
	public String totalAmount;
 
	public Movie(String id, String name, String price, LinkedList<String> bannedCountries, String availableAmount) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.bannedCountries = bannedCountries;
		this.availableAmount = availableAmount;
		totalAmount = availableAmount;
	}

}
