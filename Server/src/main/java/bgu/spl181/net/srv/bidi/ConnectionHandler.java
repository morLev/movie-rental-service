/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl181.net.srv.bidi;

import java.io.Closeable;

/**
 * The ConnectionHandler interface for Message of type T
 */
public interface ConnectionHandler<T> extends Closeable{

	/**
	 * sends msg T to the client. 
	 * Should be used by send and broadcast in the Connections implementation.
	 * 
	 * @param msg
	 */
    void send(T msg) ;

}