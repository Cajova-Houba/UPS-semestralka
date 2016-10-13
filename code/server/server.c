#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "common.h"
#include "seneterror.h"
#include "slog.h"

int main(int argc, char **argv)
{
	int sock, incoming_sock;
	int optval;
	struct sockaddr_in addr, incoming_addr;
	unsigned int incoming_addr_len;
	
	char *logMsg = (char*)malloc(sizeof(char)*255);

	/* 
	  Number of connected players. 
	  This number will be increased each time a new nick passes validation until
	  it reaches its maximum (MAX_PLAYERS constant in common.h).	
	*/
	int players = 0;

	printf("\n\n");
	printf("===================\n");
	printf("I'm a Senet server.\n");
	printf("===================\n\n\n");

	/* create socket */
	sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sock < 0) err("socket()");

	/* set reusable flag */
	optval = 1;
	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));

	/* prepare inet address */
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(SRV_PORT);
	addr.sin_addr.s_addr = htonl(INADDR_ANY); /* listen on all interfaces */
	if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) err("bind");

	if (listen(sock, 10) < 0) err("listen");
	
	/* listen loop - wait for players*/
	while (players < MAX_PLAYERS) 
	{	
		/* new connection */
		incoming_addr_len = sizeof(incoming_addr);
    		incoming_sock = accept(sock, (struct sockaddr*)&incoming_addr, 
                      &incoming_addr_len);

		/* error during accepting a new connection*/
    		if (incoming_sock < 0)
    		{
    	  		close(sock);
		        err("accept");
    		}

		/* connection info */
		sprintf(logMsg, "Connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		sinfo("server",logMsg);
	}

	for(;;)
	{
		incoming_addr_len = sizeof(incoming_addr);
    		incoming_sock = accept(sock, (struct sockaddr*)&incoming_addr, 
                      &incoming_addr_len);
    		if (incoming_sock < 0)
    		{
    	  		close(sock);
		        err("accept");
    		}
    		printf("connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		
	}
	
	free(logMsg);	
	return 0;
}
