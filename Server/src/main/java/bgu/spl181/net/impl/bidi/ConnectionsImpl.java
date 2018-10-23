package bgu.spl181.net.impl.bidi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.srv.bidi.ConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T>{
	
	// mapOfActiveClients
	private ConcurrentHashMap<Integer, ConnectionHandler<T>> mapConnectionHandlersByID = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
	private AtomicInteger IDcounter = new AtomicInteger(0);

	@Override
	public boolean send(int connectionId, T msg) {
		
		if(!mapConnectionHandlersByID.containsKey(connectionId))
			return false;
		
		mapConnectionHandlersByID.get(connectionId).send(msg);
		return true;
	}

	@Override
	public void broadcast(T msg) {
		
		// send a message T to all active clients
		for (Map.Entry<Integer, ConnectionHandler<T>> entery : mapConnectionHandlersByID.entrySet()) 
			entery.getValue().send(msg);
	}

	@Override
	public void disconnect(int connectionId) {
		mapConnectionHandlersByID.remove(connectionId);
	}

	public int addConnection(ConnectionHandler<T> handler) {
		
		int temp = IDcounter.incrementAndGet();
		mapConnectionHandlersByID.put(temp, handler);
		return temp;
	}

}
