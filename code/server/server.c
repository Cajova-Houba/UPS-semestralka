#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <err.h>

#include <pthread.h>
#include <semaphore.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "common.h"
#include "seneterror.h"
#include "slog.h"

/* 
 * Max number of connections sending their nicknames.
 */
#define MAX_CONNECTIONS			10


/*
 * Names for logger.
 */ 
#define SERVER_NAME				"server"
#define PLAYER_THREAD_NAME	 	"server - player thread"

/*
 * Structure which will be passed as an argument to a new thread.
 */
typedef struct {
	int thread_number;
	int socket;
} thread_arg;

/*
 * Mutexes for critical sections.
 */ 
pthread_mutex_t mutex_curr_conn;
pthread_mutex_t mutex_players;
pthread_mutex_t mutex_game_started;
pthread_mutex_t mutex_cleaner_index;

/*
 * A pointer to the arrays of nick validating threads.
 */
int curr_conn = 0;

/* 
  Number of connected players. 
  This number will be increased each time a new nick passes validation until
  it reaches its maximum (MAX_PLAYERS constant in common.h).	
*/
int players_count = 0;

/*
 * A semaphore for threads with players.
 * Threds with passed nick validation will wait for another thread to pass
 * nick validation. If a thread passes validation and there are already two players,
 * error message will be sent to client and connection closed.
 * 
 * The semaphore will be initialized to 0 so that the first thread with successfull 
 * player registration will wait for another thread to finnish. 
 */
sem_t player_sem;

/*
 * A semphore for cleaning.
 * 
 * The semaphore will be initialized to 0 so that the cleaning thread will go sleep immediately 
 * after start. When some other thread finnishes, it will set the cleaner index and wake the cleaner
 * up.
 */ 
sem_t cleaner_sem;

/*
 * Flag indicating that the game has started.
 */
int game_started = 0;

/*
 * Two players.
 */
Player players[2];

/*
 * Incoming connections. Two players will be choosed.
 */ 
pthread_t connections[MAX_CONNECTIONS];

/*
 * Index in the connections array. Thread which is to be joined
 * will set this index and wake the cleaning thread.
 * 
 * If the cleaner_index is set to -1, cleaning loop will stop and the
 * cleaning thread will end.
 */ 
int cleaner_index;


/*
 * =====================================================================
 * FUNCTIONS FOR CRITICAL SECTION
 * =====================================================================
 */ 
 
/*
 * Stores the current value of game_started in variable.
 * Critical section handled.
 */ 
void get_game_started(int *variable) {
	pthread_mutex_lock(&mutex_game_started);
	
	*variable = game_started;
	
	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Sets the game_started flag to 1.
 * Critical section handled.
 */
void start_game() {
	pthread_mutex_lock(&mutex_game_started);
	
	game_started = 1;
	
	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Sets the game_started flag to 0.
 * Critical section handled.
 */ 
void end_game() {
	pthread_mutex_lock(&mutex_game_started);
	
	game_started = 0;
	
	pthread_mutex_unlock(&mutex_game_started);
}
 
/*
 * Stores the actual value of players_count in variable.
 * Critical section handled.
 */ 
void getPlayers(int *variable) {
	pthread_mutex_lock(&mutex_players);
	
	*variable = players_count;
	
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function increments the players_count variable.
 * Critical section handled.
 */ 
void increment_players() {
	pthread_mutex_lock(&mutex_players);
	 
	players_count++;
	 
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function decrements the curr_conn variable.
 * Critical section handled.
 */ 
void decrement_players() {
	pthread_mutex_lock(&mutex_players);
	 
	players_count--;
	 
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function stores the actual value of curr_conn in variable.
 * Critical section handled.
 */ 
void get_curr_conn(int *variable) {
	pthread_mutex_lock(&mutex_curr_conn);
	
	*variable = curr_conn;
	 
	pthread_mutex_unlock(&mutex_curr_conn);
}

/*
 * This function increments the curr_conn variable.
 * Critical section handled.
 */ 
void increment_curr_conn() {
	pthread_mutex_lock(&mutex_curr_conn);
	 
	curr_conn++;
	 
	pthread_mutex_unlock(&mutex_curr_conn);
}

/*
 * This function decrements the curr_conn variable.
 * Critical section handled.
 */ 
void decrement_curr_conn() {
	pthread_mutex_lock(&mutex_curr_conn);
	 
	curr_conn--;
	 
	pthread_mutex_unlock(&mutex_curr_conn);
}

/*
 * Stores actuall value of cleaner_index to the variable.
 */ 
void get_cleaner_index(int *variable) {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	*variable = cleaner_index;
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}

/*
 * Sets the cleaner_index value and wakes the cleaner thread.
 */ 
void clean_me(int thread_index) {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	cleaner_index = thread_index;
	sem_post(&cleaner_sem);
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}

/*
 * Sets the cleaner_index to -1 which means that the cleaning loop
 * will be stopped. No need to call sem_post() after this function.
 */ 
void shutdown_cleaner() {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	cleaner_index = -1;
	sem_post(&cleaner_sem);
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}





/*
 * =====================================================================
 * THREAD FUNCTIONS
 * =====================================================================
 */ 
/*
 * Thread function which will wait for signal to clean thread slots.
 */
void *cleaner() {
	int thread_index;
	
	while(1) {
		/* wait for signal - conditional variable*/
		sem_wait(&cleaner_sem);
		
		/* clean */
		get_cleaner_index(&thread_index);
		if(thread_index == -1) {
			break;
		}
		
		pthread_join(connections[thread_index], NULL);
	}
	
	return NULL;
}
 
/*
 * Prints the player to the buffer.
 */ 
void print_player(Player *p, char *buffer) {
	sprintf(buffer,"Player: {id=%d, nick=%s, socket=%d}",p->id,p->nick,p->socket);
}

/*
 * Prints the player to the buffer and adds a new line symbol.
 */ 
void println_player(Player *p, char *buffer) {
	sprintf(buffer,"Player: {id=%d, nick=%s, socket=%d}\n",p->id,p->nick,p->socket);
}
 
/*
 * This function checks the nick and if it's invalid return 0.
 * It is expected that the nick is terminated with '\0'.
 * Possible error message can be stored.
 */  
int check_nick(char *nick, char *err_msg)
{
	int nicklen, i;

	
	/* check length */
	nicklen = strlen(nick);
	if(nicklen > MAX_NICK_LENGTH) {
		sprintf(err_msg,"Nick '%s' too long.\n",nick);
		return 0;
	}
	
	/* check characters
	 * 
	 * only A-Z (65-90), a-z (97-122)
	 * 
	 */
	for(i = 0; i < nicklen; i++) {
		if(nick[i] < 65 || 
		(nick[i] > 90 && nick[i] < 97) || 
		(nick[i] > 122)) {
			sprintf(err_msg,"Invalid character '%c' at position %i.\n",nick[i],i+1);
			return 0;
		}
	}
	
	/*
	 * Check nick duplicities 
	 */
	
	/* nick valid */
	return 1;
}

/*
 * A thread for one player. At first, nickname will be checked, then 
 * the thread waits for another player to be registered and the game begins.
 */
void *player_thread(void *arg) {
	
	char *log_msg = (char*)malloc(sizeof(char)*255);
	
	char buffer[MAX_TXT_LENGTH + 1];
	int recv_status = 0;
	int socket = (int)arg;
	int nick_valid = 0;
	int tmp_players;
	
	/*
	 * Index in the array of players.
	 */
	int my_player;
	
	/* check if the server isn't already full */
	getPlayers(&my_player);
	if(my_player == 2) {
		serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
		free(log_msg);
		decrement_curr_conn();
		return NULL;
	}
	
	sprintf(log_msg,"Waiting for text from socket: %d.\n",socket);
	sinfo(PLAYER_THREAD_NAME,log_msg);
	while(1) {
		recv_status = recv_txt_buffer(socket, buffer);
		if(recv_status == 2) {
			sprintf(log_msg,"Socket %d closed connection.\n",socket);
			sinfo(PLAYER_THREAD_NAME, log_msg);
			
			free(log_msg);
			
			/*
			 * !!! critical section !!!
			 */
			decrement_curr_conn();  
			return NULL;
		} else if(recv_status == 1) {
			break;
		}
	}
	
	sprintf(log_msg,"Text received: %s\n",buffer);
	sinfo(PLAYER_THREAD_NAME,log_msg);
	
	/* check nick */
	nick_valid = check_nick(buffer, log_msg);
	
	/* nick validation failed */
	if(!nick_valid) {
		serror(PLAYER_THREAD_NAME, log_msg);
		free(log_msg);
		decrement_curr_conn();
		return NULL;
	}
	
	
	
	/*
	 * Nickaname passed validation,
	 * register a new player.
	 * 
	 * Wait for the second thread.
	 */
	sdebug(PLAYER_THREAD_NAME, "Nick validation ok.\n");

	/* check if the server isn't already full */
	getPlayers(&my_player);
	if(my_player == 2) {
		serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
		free(log_msg);
		decrement_curr_conn();
		return NULL;
	}
	increment_players();
	if(sem_trywait(&player_sem) == -1) {
		/* one thread is already in queue - wake it up and lets play */
		sem_post(&player_sem);
		sinfo(PLAYER_THREAD_NAME, "THE GAME HAS STARTED!\n");
		start_game();
	} else {
		/* no thread has finished validation yet, wait for another thread */
		sinfo(PLAYER_THREAD_NAME, "Waiting for other player.\n");
		sem_wait(&player_sem);
	}
	
	players[my_player].id = my_player;
	players[my_player].nick = buffer;
	players[my_player].socket = socket;
	println_player(&players[my_player], log_msg);
	sinfo(PLAYER_THREAD_NAME, log_msg);
	
	/* 
	 * GAME HERE 
	 */
			
	sinfo(PLAYER_THREAD_NAME,"End of thread.\n");
	
	free(log_msg);

	decrement_players();
	decrement_curr_conn();
	return NULL;
} 






/*
 * =====================================================================
 * MAIN FUNCTION AND HELPER FUNCTIONS
 * =====================================================================
 */ 
/*
 * Initializes mutexes and semapthore and returns 0 if error occurs.
 */ 
int init_ms() {
	if (pthread_mutex_init(&mutex_curr_conn, NULL) != 0) {
		serror("server","Current connection mutex initialization failed.\n");
		return 0;
	}
	
	if (pthread_mutex_init(&mutex_players, NULL) != 0) {
		serror("server","Players mutex initialization failed.\n");
		return 0;
	}
	
	if (pthread_mutex_init(&mutex_players, NULL) != 0) {
		serror(SERVER_NAME, "Game_started mutex initialization failed.\n");
		return 0;
	}
	
	if (pthread_mutex_init(&mutex_cleaner_index, NULL) != 0) {
		serror(SERVER_NAME, "Cleaner_index mutex initialization failed.\n");
		return 0;
	}
	
	if (sem_init(&player_sem, 0, 1) < 0) {
		serror("server","Players semaphore initialization failed.\n");
		return 0;
	}
	
	if (sem_init(&cleaner_sem, 0, 1) < 0) {
		serror(SERVER_NAME, "Cleaner semaphore initialization failed.\n");
		return 0;
	}
	
	return 1;
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
	pthread_t cleaner_thread;
	thread_arg thread_args[MAX_CONNECTIONS];
	int thread_err;
	char *logMsg;
	int tmp_players, tmp_curr_conn;

	printf("\n\n");
	printf("===================\n");
	printf("I'm a Senet server.\n");
	printf("===================\n\n\n");

	/* create socket */
	sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sock < 0) {
		serror("server","Error while creating a new socket.");
		return 1;
	}

	/* set reusable flag */
	optval = 1;
	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));

	/* prepare inet address */
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(SRV_PORT);
	addr.sin_addr.s_addr = htonl(INADDR_ANY); /* listen on all interfaces */
	if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
		serror("server","Error while binding a new listener.");
		return 1;
	}

	if (listen(sock, 10) < 0) {
		serror("server","Error while listening.");
		return 1;
	}
	
	if (!init_ms()) {
		return 1;
	}
	
	logMsg = (char*)malloc(sizeof(char)*255);
	
	/* listening loop - wait for players*/
	while (1) 
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
		
		
		/*
		 * TODO:
		 * - get_curr_conn returns first free thread slot,
		 * 	 inc_curr_conn
		 * - new thread which will clean connection threads 
		 * - cleaner will call dec_curr_conn
		 */
		/* !!! critical section !!! */
		get_curr_conn(&tmp_curr_conn);
		
		if(tmp_curr_conn == MAX_CONNECTIONS - 1) {
			sinfo("server","Server can't accept any new connections.");
			continue;
		}
		
		thread_args[tmp_curr_conn].thread_number = tmp_curr_conn;
		thread_args[tmp_curr_conn].socket = incoming_sock;
		thread_err = pthread_create(&connections[tmp_curr_conn], NULL, player_thread,(void *)&thread_args[tmp_curr_conn]);
		if(thread_err) {
			sprintf(logMsg, "Error: %s\n",strerror(thread_err));
			serror("server",logMsg);
			continue;
		}
		increment_curr_conn();
	}
	
	/* signal cleaner*/
	
	/* wait for cleaner */
	shutdown_cleaner();
	pthread_join(cleaner_thread, NULL);
	free(logMsg);	
	return 0;
}
