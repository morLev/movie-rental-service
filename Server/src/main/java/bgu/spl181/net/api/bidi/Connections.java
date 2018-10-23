package bgu.spl181.net.api.bidi;

/** 
 * This interface should map a unique ID for each active client connected to the server. 
 * The implementation of Connections is part of the server pattern and not part of the protocol. 
 * It has 3 functions that you must implement (You may add more if needed). 
 *
 * @param <T>
 */
public interface Connections<T> {

	/**
	 * sends a message T to client represented by the given connectionId
	 * 
	 * @param connectionId - a client id
	 * @param msg - the message to send to the client
	 * 
	 * @return false if the connection id does not exist
	 */
    boolean send(int connectionId, T msg);

    /**
     * sends a message T to all active clients. 
     * This includes clients that has not yet completed log-in by the User service text based protocol. 
     * Remember, Connections<T> belongs to the server pattern implemenration, not the protocol!.
     * 
     * @param msg
     */
    void broadcast(T msg);

    /**
     * removes active client connId from map.
     * 
     * @param connectionId
     */
    void disconnect(int connectionId);
}