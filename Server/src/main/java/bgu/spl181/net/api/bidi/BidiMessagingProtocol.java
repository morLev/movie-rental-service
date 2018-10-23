/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl181.net.api.bidi;

/**
 * This interface replaces the MessagingProtocol interface. 
 * It exists to support peer to peer messaging via the Connections interface.
 *
 * @param <T>
 */
public interface BidiMessagingProtocol<T>  {

	/**
	 * initiate the protocol with the active connections structure of the server 
	 * and saves the owner client’s connection id.
	 * 
	 * @param connectionId
	 * @param connections
	 */
    void start(int connectionId, Connections<T> connections);
    
    /**
     * As in MessagingProtocol, processes a given message. 
     * Unlike MessagingProtocol, responses are sent via the connections object send function. 
     * 
     * @param message
     */
    void process(T message);
    
	/**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}