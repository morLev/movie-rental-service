#include <stdlib.h>
#include <connectionHandler.h>

#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include <boost/thread.hpp>



/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    boost::thread threadKeyBoard(&ConnectionHandler::readFromClient, &connectionHandler);

    while (!connectionHandler.getShouldTerminate()) {

        std::string answer;
        if (!connectionHandler.getLine(answer)) {
            break;
        }

        answer.resize(answer.length() - 1);
        std::cout << answer << std::endl;
        if (answer == "ACK signout succeeded") {
            connectionHandler.shouldTerminateSet(true);
            break;
        }
    }

    return 0;
}

