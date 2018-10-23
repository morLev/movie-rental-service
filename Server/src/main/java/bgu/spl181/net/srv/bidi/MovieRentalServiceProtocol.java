package bgu.spl181.net.srv.bidi;

import java.util.LinkedList;

import bgu.spl181.net.impl.bidi.UserServiceTextbasedprotocol;

public class MovieRentalServiceProtocol extends UserServiceTextbasedprotocol{

	
	public MovieRentalServiceProtocol(SharedData sharedData) {
		super(sharedData);
	}
	
	public void handleRequest(String message){
		
		if(!message.contains(" ") && message.equals("info"))
			handleRequestInfo("");
		else{
			String messagePrefix = message.substring(0, message.indexOf(" "));
			String messageSuffix = message.substring(message.indexOf(" ")+1);
		
			if(messagePrefix.equals("balance"))
				handleRequestBalance(messageSuffix);
			else if(messagePrefix.equals("info"))
				handleRequestInfo(messageSuffix);
			else if(messagePrefix.equals("rent"))
				handleRequestRent(messageSuffix);
			else if(messagePrefix.equals("return"))
				handleRequestReturn(messageSuffix);
			else if(messagePrefix.equals("addmovie"))
				handleRequestAddMovie(messageSuffix);
			else if(messagePrefix.equals("remmovie"))
				handleRequestRemMovie(messageSuffix);
			else if(messagePrefix.equals("changeprice"))
				handleRequestChangePrice(messageSuffix);
			else
				sendERROR("request " + messagePrefix + " failed");
		}
	}
	
	public void handleRegister(String message) {
	
		if(!message.contains("\""))
			sendACK("registration succeeded");
		
		else{
			String country = message.substring(message.indexOf('"')+1, message.lastIndexOf('"'));
		
			LinkedList<User> users = sharedData.getUsers(LockType.WRITE);
		
			String myUsername = message.substring(0, message.indexOf(" "));
			User user = getUserByName(myUsername);
			user.country = country;
		
			sharedData.unlockUsers(LockType.WRITE);
			sendACK("registration succeeded");
		}
	}
	
	
//************************************* Normal Service REQUEST commands ************************************* 
	
	public void handleRequestBalance(String message){

		// check if the specific request is: REQUEST balance info
		if(!message.contains(" ") && message.equals("info"))
			handleRequestBalanceInfo(); 
		
		// check if the specific request is: REQUEST balance add <amount> 
		else{
			String messagePrefix = message.substring(0, message.indexOf(" "));
			String messageSuffix = message.substring(message.indexOf(" ")+1);
			
			if(messagePrefix.equals("add"))
				handleRequestBalanceAdd(messageSuffix);
		}
	}
	
	/**
	 * This function handeles the client command 'REQUEST balance info'.
	 *  
	 * Server returns the user’s current balance
	 */
	public void handleRequestBalanceInfo(){
		
		LinkedList<User> users = sharedData.getUsers(LockType.READ);
		
		final String balance = getUserByName(username).balance;

		sharedData.unlockUsers(LockType.READ);
		sendACK("balance " + balance);
	}	
	
	/**
	 * This function handeles the client command 'REQUEST balance add <amount>'.
	 *    
	 * Server adds the amount given to the user’s balance. 
	 *  
	 * @param message <amount>
	 */
	public void handleRequestBalanceAdd(String message){

		final String amount = message;
		LinkedList<User> users = sharedData.getUsers(LockType.WRITE);

		// add the amount given to the user’s balance
		String oldBalance = getUserByName(username).balance;
		Integer newBalanceInInt = Integer.parseInt(oldBalance) + Integer.parseInt(amount);
		final String newBalance = newBalanceInInt.toString();
		getUserByName(username).balance = newBalance; // change the user's balance in the users list 
		
		sharedData.unlockUsers(LockType.WRITE);
		sendACK("balance " + newBalance + " added " + amount);
	}
	
	/**
	 *  This function handeles the client command 'REQUEST info “[movie name]”'. 
	 *  
	 *  Server returns information about the movies in the system. 
	 * 
	 * @param message “[movie name]” or "" (if no movie name was given)
	 */
	public void handleRequestInfo(String message){
		
		if(message.equals("")){ // no movie name was given 
			
			LinkedList<Movie> movies = sharedData.getMovies(LockType.READ);
			
			String listOfMoviesNames = "";
			for (Movie movie : movies) 
				listOfMoviesNames = listOfMoviesNames + " \"" + movie.name + "\"";
			
			final String ackmsg = listOfMoviesNames;
			
			sharedData.unlockMovies(LockType.READ);
			// returns an ACKCommand with a list of all movies’ names 
			// (even if some of them are not available for rental) 
			sendACK("info" + ackmsg);
		}
		
		else{
			LinkedList<Movie> movies = sharedData.getMovies(LockType.READ);
			
			String movieName = message.substring(1, message.length()-1);
			Movie movieToReturn = getMovieByName(movieName);
			
			if(!movies.contains(movieToReturn)){ // the movie does not exist
				sharedData.unlockMovies(LockType.READ);
				sendERROR("request info failed");
			}
			else{
				
				final String movieInfo =  " \"" + movieName + "\" " + movieToReturn.availableAmount + " " 
				+ movieToReturn.price + getBannedCountriesAsStr(movieToReturn);
				
				sharedData.unlockMovies(LockType.READ);
				sendACK("info" + movieInfo);
			}
		}
	}
	
	/**
	 * This function handeles the client command 'REQUEST rent <”movie name”>'.
	 *  
	 *  Server tries to add the movie to the user rented movie list, 
	 *  remove the cost from the user’s balance and reduce the amount available for rent by 1. 
	 *  
	 * @param message <”movie name”>
	 */
	public void handleRequestRent(String message){
		
		//<"movie name">	 ->		<movie name>
		String MovieNameToRent = message.substring(1, message.length() - 1);
		
		LinkedList<User> users = sharedData.getUsers(LockType.WRITE);
		LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);

		User myUser = getUserByName(username);
		
		Movie movieToRent = getMovieByName(MovieNameToRent);
		
		if(isRentPossible(movieToRent,myUser)) {
			
			int userBalance = Integer.parseInt(myUser.balance);
			int moviePrice =  Integer.parseInt(movieToRent.price);
			int movieAvailableAmount = Integer.parseInt(movieToRent.availableAmount);
		
			myUser.movies.add(new MovieForUser(movieToRent.id, movieToRent.name));
			myUser.balance = Integer.toString(userBalance - moviePrice);
			movieToRent.availableAmount = Integer.toString(movieAvailableAmount - 1);
			
			
			//movieName = <"name"> , MovieNameToRent = <movie>
			final String msgAck =  "rent " + message + " success";
			final String msgBROADCAST =  "movie " + message + " " + movieToRent.availableAmount
										+ " " + movieToRent.price;
			
			sharedData.unlockMovies(LockType.WRITE);
			sharedData.unlockUsers(LockType.WRITE);
			sendACKandBROADCAST(msgAck , msgBROADCAST);
		}
		else {
			sharedData.unlockMovies(LockType.WRITE);
			sharedData.unlockUsers(LockType.WRITE);
			sendERROR("request rent failed");
		}
	}

	/**
	 * This function handeles the client command 'REQUEST return <”movie name”>'. 
	 * 
	 * Server tries to remove the movie from the user rented movie list 
	 * and increase the amount of available copies of the movies by 1.  
	 * 
	 * @param message <”movie name”>
	 */
	public void handleRequestReturn(String message){
		
		//<"movie name">	 ->		<movie name>
		final String movieName = message.substring(1, message.length()-1);
		
		LinkedList<User> users = sharedData.getUsers(LockType.WRITE);
		LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);
		
		// check if the movie exist
		if(getMovieByName(movieName) == null){
			sharedData.unlockMovies(LockType.WRITE);
			sharedData.unlockUsers(LockType.WRITE);
			sendERROR("request return failed");
			return; 
		} 

		// check if the user is currently renting the movie
		User myUser = getUserByName(username);
		if(!getListOfMoviesForUser(myUser).contains(movieName)){
			sharedData.unlockMovies(LockType.WRITE);
			sharedData.unlockUsers(LockType.WRITE);
			sendERROR("request return failed");
			return; 
		}
		
		// remove the movie from the user rented movie list
		removeMovieFromUserByName(movieName, myUser);
		
		Movie movie = getMovieByName(movieName);
		// increase the amount of available copies of the movies by 1
		Integer newAmount = Integer.parseInt(movie.availableAmount)+1;
		movie.availableAmount = newAmount.toString();
		
		sharedData.unlockMovies(LockType.WRITE);
		sharedData.unlockUsers(LockType.WRITE);
		
		String ACKmsg = "return " + movieName + " success";
		String BROADCASTmsg = "movie \"" + movieName + "\" " + movie.availableAmount + " " + movie.price;
		sendACKandBROADCAST(ACKmsg, BROADCASTmsg);
	}
	
	
//************************************* Admin Service REQUEST commands *************************************
	
	/**
	 * This function handeles the Admin command 'REQUEST addmovie <”movie name”> <amount> <price> [“banned country”,…]'. 
	 * 
	 * The server adds a new movie to the system with the given information. 
	 * The new movie ID will be the highest ID in the system + 1. 
	 * 
	 * @param message <”movie name”> <amount> <price> [“banned country”,…] 
	 */
	public void handleRequestAddMovie(String message){
		
		// if user is not an administrator, send ERROR message
		if(clientStatus != ClientStatus.ADMIN){
			sendERROR("request addmovie failed");
			return;
		}
		
		LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);
		
		String temp = message;
		String name = null;
		String id;
		String price;
		String availableAmount;
		LinkedList<String> bannedCountries = new LinkedList<String>();
		
		for(int i = 1 ; i < temp.length() ; i++) {
			if (temp.charAt(i) == '"') {
				name = temp.substring(1, i);
				temp = temp.substring(i + 2);
				break;
			}
		}
		
		availableAmount = temp.substring(0, temp.indexOf(" "));
		temp = temp.substring(temp.indexOf(" ") + 1);
		
		if(!temp.contains(" ")) {
			price = temp;
		}
		else {
	
			price = temp.substring(0,temp.indexOf(" "));
			temp = temp.substring(temp.indexOf("\""));

			int i = 1;
			int j = 1;
		
			while(i < temp.length()) {
				if(temp.charAt(i) == '"'){
					bannedCountries.add(temp.substring(j , i));
					i = i + 3;
					j = i;
				}
				else
					i++;
			}
		}
		
		if(getMovieByName(name) != null || Integer.parseInt(price) < 1 || Integer.parseInt(availableAmount)< 1){
			sharedData.unlockMovies(LockType.WRITE);
			sendERROR("request addMovie failed");
		}
			
		else {	
			movies.add(new Movie(getNewId(movies), name , price , bannedCountries, availableAmount));
			
			sharedData.unlockMovies(LockType.WRITE);
			String msgACK = "addmovie" + " \""+ name +"\" success";
			String msgBROADCAST = "movie" + " \""+ name +"\" " + availableAmount + " " + price;
			sendACKandBROADCAST(msgACK , msgBROADCAST);
		}
	} 

	
	
	/*
	LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);

	String movieName = message.substring(1, message.indexOf(" ")-1);
	String temp = message.substring(message.indexOf(" ")+1); // temp=<amount> <price> [“banned country”,…]
	String amount = message.substring(0, temp.indexOf(" "));
	temp = temp.substring(temp.indexOf(" ")+1); // temp=<price> [“banned country”,…]
	String price;
	String bannedCountries;
	if(temp.contains(" ")){
		price = temp.substring(0, temp.indexOf(" "));
		bannedCountries = temp.substring(temp.indexOf(" ")+1);
	}
	else{
		price = temp;
	}
	
	// if movie name already exists in the system, send ERROR message   
	if(getMovieByName(movieName) != null){
		sharedData.unlockMovies(LockType.WRITE);
		sendERROR("request addmovie failed");
	}
	
	// if price or amount are smaller than or equal to 0, send ERROR message
	else if(Integer.parseInt(price) <= 0 || Integer.parseInt(amount) <= 0){
		sharedData.unlockMovies(LockType.WRITE);
		sendERROR("request addmovie failed");
	}
	
	else{
		movies.add(new Movie(getNewId(movies), movieName, price, bannedCountries, amount));
		
		String msgACK = "movie" + " \""+ movieName +"\" " + amount + " " + price;
		String msgBROADCAST = "movie" + " \""+ movieName +"\" " + amount + " " + price;
		sendACKandBROADCAST(msgACK , msgBROADCAST);
	}
	*/

	/**
	 * This function handeles the Admin command 'REQUEST remmovie <”movie name”>'.
	 *  
	 * Server removes a movie by the given name from the system. 
	 * 
	 * @param message <”movie name”>
	 */
	public void handleRequestRemMovie(String message){
		
		// if user is not an administrator, send ERROR message
		if(clientStatus != ClientStatus.ADMIN){
			sendERROR("request remmovie failed");
			return;
		}
		
		LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);
		
		String movieName = message.substring(1, message.length()-1);
		Movie movieToRemove = getMovieByName(movieName);
		
		// if the movie does not exist in the system, send ERROR message
		if(movieToRemove == null){
			sharedData.unlockMovies(LockType.WRITE);
			sendERROR("request remmovie failed");
		}
		
		// if there is (at least one) a copy of the movie that is currently rented by a user, send ERROR message  
		else if(!movieToRemove.availableAmount.equals(movieToRemove.totalAmount)){
			sharedData.unlockMovies(LockType.WRITE);
			sendERROR("request remmovie failed");
		}
		
		else{
			movies.remove(movieToRemove);
			sharedData.unlockMovies(LockType.WRITE); 
			
			String ACKmsg = "remmovie \"" + movieName + "\" success";
			String BROADCASTmsg = "movie \"" + movieName + "\" removed";
			sendACKandBROADCAST(ACKmsg, BROADCASTmsg);
		}
	}

	/**
	 * This function handeles the Admin command 'REQUEST changeprice <”movie name”> <price>'.
	 *  
	 * Server changes the price of a movie by the given name. 
	 *
	 * @param message <”movie name”> <price>
	 */
	public void handleRequestChangePrice(String message){
		
		// if user is not an administrator, send ERROR message
		if(clientStatus != ClientStatus.ADMIN){
			sendERROR("request changeprice failed");
			return;
		}
		
		LinkedList<Movie> movies = sharedData.getMovies(LockType.WRITE);
		
		String movieName = message.substring(1, message.indexOf(" ")-1);
		String newPrice = message.substring(message.indexOf(" ")+1);
		Movie movieToChangePriceTo = getMovieByName(movieName);
		
		// if the movie does not exist in the system, send ERROR message
		if(movieToChangePriceTo == null){
			sharedData.unlockMovies(LockType.WRITE);
			sendERROR("request changeprice failed");
		}
		
		// if the movie price is smaller than or equal to 0, send ERROR message
		else if(Integer.parseInt(newPrice) <= 0){
			sharedData.unlockMovies(LockType.WRITE);
			sendERROR("request changeprice failed");
		}
		
		else{
			movieToChangePriceTo.price = newPrice;
			sharedData.unlockMovies(LockType.WRITE); 
	
			String ACKmsg = "changeprice \"" + movieName + "\" success";
			String BROADCASTmsg = "movie \"" + movieName + "\" " + movieToChangePriceTo.availableAmount + " " + newPrice;
			sendACKandBROADCAST(ACKmsg, BROADCASTmsg);
		}
	}

	  
//*********************************************** help methods ***********************************************	
	
	// return the user named userName if he exist, else return null
	private User getUserByName(String userName){
		
		LinkedList<User> listOfUsers = sharedData.users.users;
		
		for (int i=0 ; i<listOfUsers.size() ; i++) {
			if(listOfUsers.get(i).username.equals(userName))
				return listOfUsers.get(i);
		}
		return null;
	}
	
	// return the movie named movieName if he exist, else return null
	private Movie getMovieByName(String movieName){
		
		LinkedList<Movie> listOfMovies = sharedData.movies.movies;
		
		for (int i=0 ; i<listOfMovies.size() ; i++) {
			if(listOfMovies.get(i).name.toLowerCase().equals(movieName.toLowerCase()))
				return listOfMovies.get(i);
		}
		return null;
	}
	
	private LinkedList<String> getListOfMoviesForUser(User user){
		
		LinkedList<String> listOfMoviesForUser = new LinkedList<String>();
		
		for(int i=0 ; i<user.movies.size() ; i++)
			listOfMoviesForUser.add(user.movies.get(i).name);
		
		return listOfMoviesForUser;
	}
	
	private boolean isRentPossible(Movie movie, User user) {
		return (movie != null) 
				&& (!movie.availableAmount.equals("0"))
				&& (!(Integer.parseInt(movie.price) > Integer.parseInt(user.balance)))
				&& (!movie.bannedCountries.contains(user.country))
				&& (!getListOfMoviesForUser(user).contains(movie.name));
	}

	private void removeMovieFromUserByName(String movieName, User user){
		
		MovieForUser movieToRemove = null;
		for (MovieForUser movieForUser : user.movies) {
			if(movieForUser.name.toLowerCase().equals(movieName.toLowerCase()))
				movieToRemove = movieForUser;
		}
		user.movies.remove(movieToRemove);
	}
	
	private String getBannedCountriesAsStr(Movie movie){
		
		String toReturn = "";
		for (String countery : movie.bannedCountries) {
			toReturn = toReturn + " \"" + countery + "\"";
		}
		return toReturn;
	}
	
	private String getNewId(LinkedList<Movie> movies) {
		
		int max = 0;
		for (Movie movie : movies) 
			if(Integer.parseInt(movie.id)>max) 
				max = Integer.parseInt(movie.id);	
		
		return Integer.toString(max+1);
	}
	
	
}
