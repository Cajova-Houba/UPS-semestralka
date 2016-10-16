#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <err.h>

#include <pthread.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "common.h"
#include "seneterror.h"
#include "slog.h"

/* 
 * Max number of connections sending their nicknames.
 */
#define MAX_CONNECTIONS		10

/*
 * A pointer to the arrays of nick validating threads.
 */
int curr_conn = 0;

/*
 * Sockets for two players.
 */ 
int p1_sock = -1, p2_sock = -1;

/*
 * This function will be called in a seperate thread and will wait for a nickname to be received.
 * After the nickanem is received, this function will validate it.
 * 
 */
void *check_nick(void *arg)
{
	char buffer[MAX_TXT_LENGTH + 1];
	int recv_status = 0;
	int socket = *(int *)arg;
	
	printf("Waiting for text.");
	recv_status = recv_txt_buffer(socket, buffer);
	if(recv_status == 1) {
		printf("String received: %s\n",buffer);
	}
	
	return NULL;
}
	
/*
 * Main function.
 */ 
int main(int argc, char **argv)
{
	int sock, incoming_sock;
	int optval;
	struct sockaddr_in addr, incoming_addr;
	unsigned int incoming_addr_len;
	pthread_t connections[MAX_CONNECTIONS];
	int thread_err;
	
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
	if (sock < 0) serror("server","Error while creating a new socket.");

	/* set reusable flag */
	optval = 1;
	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));

	/* prepare inet address */
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(SRV_PORT);
	addr.sin_addr.s_addr = htonl(INADDR_ANY); /* listen on all interfaces */
	if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) serror("server","Error while binding a new listener.");

	if (listen(sock, 10) < 0) serror("server","Error while listening.");
	
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
			serror("server","Error while acceptig a new connection.");
		}
		
		/* connection info */
		sprintf(logMsg, "Connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		sinfo("server",logMsg);



		/* 
		 * new thread which will validate the nickname 
		 * and accept a new player.
		 */
		
		/* !!! critical section !!! */
		if(curr_conn == MAX_CONNECTIONS - 1) {
			sinfo("server","Server can't accept any new players.");
			continue;
		}
			
		thread_err = pthread_create(&connections[curr_conn], NULL, check_nick,(void *)socket);
		if(thread_err) {
			printf("Error: %s\n",strerror(thread_err));
		}
		pthread_detach(connections[curr_conn]);
		curr_conn++;
		
		/* !!! end of critical section !!! */
	}
	
	/* game loop */
	for(;;)
	{

		
	}
	
	free(logMsg);	
	return 0;
}
