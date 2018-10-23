package bgu.spl181.net.impl.BBtpc;

import bgu.spl181.net.impl.bidi.MessageEncoderDecoderImpl;
import bgu.spl181.net.srv.bidi.MovieRentalServiceProtocol;
import bgu.spl181.net.srv.bidi.Server;
import bgu.spl181.net.srv.bidi.SharedData;

public class TPCMain {

	public static void main(String[] args) {

		SharedData sharedData = new SharedData();
		
		Server<String> server = Server.threadPerClient(Integer.parseInt(args[0]),
				()->  new MovieRentalServiceProtocol(sharedData) , 
				()-> new MessageEncoderDecoderImpl() );	
		
		server.serve();
	}

	
}
