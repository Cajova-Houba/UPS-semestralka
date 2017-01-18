#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include <pthread.h>
#include <semaphore.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

#include "../common/common.h"
#include "../common/seneterror.h"
#include "../common/nick_val.h"

#include "../common/message.h"
#include "parallel.h"
#include "game.h"


/* 
 * Max number of connections sending their nicknames.
 */
#define MAX_CONNECTIONS			10

/*
 * Max number of timer threads.
 */
#define MAX_TIMER_THREADS       2

/*
 * Max time for a player to reconnect. In seconds.
 */
#define DEF_TIMEOUT             10


/*
 * Possible parameter names.
 */
#define HELP_PARAM_NAME         "help"
#define PORT_PARM_NAME          "port"
#define IP_PARAM_NAME           "ip"
#define TIMEOUT_PARAM_NAME      "timeout"

#define MIN_TIMEOUT             1
#define MAX_TIMEOUT             60


/*
 * Structure which will be passed as an argument to a new thread.
 */
typedef struct {
	int thread_number;
	int socket;
    uint32_t addr;
    int port;
} thread_arg;

/* 
  Number of connected players. 
  This number will be increased each time a new nick passes validation until
  it reaches its maximum (MAX_PLAYERS constant in common.h).	
*/
int players_count = 0;

/*
 * Incoming connections. Two players will be choose.
 */ 
pthread_t connections[MAX_CONNECTIONS];

/*
 * Timer threads for disconnected players.
 */
pthread_t timer_threads[MAX_TIMER_THREADS];


/*
 * Index in the connections/timer_threads array. Thread which is to be joined
 * will set this index and wake the cleaning thread.
 * 
 * If the cleaner_index is set to -100, cleaning loop will stop and the
 * cleaning thread will end.
 */ 
int cleaner_index;

/*
 * Game instance.
 */
struct Game_struct game;

/*
 * Initialized on the start of server.
 */
int timeout;

/*
 * =====================================================================
 * FUNCTIONS FOR CRITICAL SECTION
 * =====================================================================
 */ 
 
/*
 * Stores the current value of game_started flag in variable.
 * Critical section handled.
 */ 
void get_game_started(int *variable) {
	pthread_mutex_lock(&mutex_game_started);
	
	*variable = get_game_flag(&(game.flags), GAME_STARTED_FLAG);
	
	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls a start_game()
 * function from the game.h.
 */
void server_start_game() {
	pthread_mutex_lock(&mutex_game_started);

    start_game(&game);
	
	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls an end_game()
 * function from the game.h.
 * Also clears the nicks in game struct.
 */ 
void server_end_game() {
    int i;
	pthread_mutex_lock(&mutex_game_started);
	
	end_game(&game);
    for (i = 0; i < MAX_NICK_LENGTH; i++) {
        game.players[0].nick[i] = '\0';
        game.players[1].nick[i] = '\0';
    }

	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls set_winner() and
 * end_game() functions from the game.h.
 */
void server_set_winner(int winner){
    pthread_mutex_lock(&mutex_winner);

    set_winner(&game, winner);

    pthread_mutex_unlock(&mutex_winner);
}

/*
 * Handles the critical section and stores the winner of the game
 * to variable. If noone has won the game, stores -1 to variable.
 */
void server_get_winner(int* variable) {
    pthread_mutex_lock(&mutex_winner);

    *variable = get_winner(&game);

    pthread_mutex_unlock(&mutex_winner);
}
 
/*
 * Stores the actual value of players_count in variable.
 * Critical section handled.
 */ 
void get_players(int *variable) {
	pthread_mutex_lock(&mutex_players);
	
	*variable = players_count;
	
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function increments the players_count variable.
 * Critical section handled.
 */ 
void increment_players() {
    char log_msg[50];
	pthread_mutex_lock(&mutex_players);
	 
	players_count++;
    sprintf((char*)&log_msg,"Current player count: %d.\n", players_count);
    sdebug(SERVER_NAME, log_msg);
	 
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function decrements the curr_conn variable.
 * Critical section handled.
 */ 
void decrement_players() {
	char log_msg[50];
    pthread_mutex_lock(&mutex_players);
	 
	players_count--;
    sprintf((char*)&log_msg,"Current player count: %d.\n", players_count);
    sdebug(SERVER_NAME, log_msg);
	 
	pthread_mutex_unlock(&mutex_players);
}

/*
 * This function stores the first free connection slot in the variable.
 * Critical section handled.
 */ 
void get_curr_conn(int *variable) {
	int i;
	pthread_mutex_lock(&mutex_curr_conn);
	
	for(i = 0; i < MAX_CONNECTIONS; i++) {
		if(connections[i] == NULL) {
			*variable = i;
			break;
		}
	}
	 
	pthread_mutex_unlock(&mutex_curr_conn);
}

/*
 * Stores actual value of cleaner_index to the variable.
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
	/* add me to the cleaning queue */
	sem_wait(&cleaning_queue_sem);	
		
	cleaner_index = thread_index;
	
	/* wake up, Mr. Cleaner */
	sem_post(&cleaner_sem);	
}

/*
 * Sets the cleaner_index to -100 which means that the cleaning loop
 * will be stopped. No need to call sem_post() after this function.
 */ 
void shutdown_cleaner() {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	cleaner_index = -100;
	sem_post(&cleaner_sem);
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}

/*
 * Stores nick of the player to buffer.
 */
void get_player_nick(int p, char* buffer) {
    if (p < 0 || p > 1) {
        return;
    }
    pthread_mutex_lock(&mutex_get_player);

    strcpy(buffer, game.players[p].nick);

    pthread_mutex_unlock(&mutex_get_player);
}

/*
 * If at least one of the waiting flags is set to 1,
 * sets the res to 1. Otherwise sets res to 0.
 */
void server_is_game_waiting(int *res) {
    pthread_mutex_lock(&mutex_is_waiting);

    *res = is_game_waiting(&game);

    pthread_mutex_unlock(&mutex_is_waiting);
}

/*
 * If the waiting_for_p<player> is 1, stores 1 into the res.
 */
void is_game_waiting_for_player(int *res, int player) {
    pthread_mutex_lock(&mutex_is_waiting);

    if(player == 0) {
        *res = get_game_flag(&(game.flags), WAITING_P1_FLAG);
    } else if (player == 1) {
        *res = get_game_flag(&(game.flags), WAITING_P2_FLAG);
    } else {
        *res = 0;
    }

    pthread_mutex_unlock(&mutex_is_waiting);
}

/*
 * Sets the waiting_for_p<player+1> flag to 1.
 */
void set_waiting_for(int player) {
    pthread_mutex_lock(&mutex_is_waiting);

    if(player == 0) {
        set_game_flag(&(game.flags), WAITING_P1_FLAG);
    } else if (player == 1) {
        set_game_flag(&(game.flags), WAITING_P2_FLAG);
    }

    pthread_mutex_unlock(&mutex_is_waiting);
}

void unset_waiting_for(int player) {
    pthread_mutex_lock(&mutex_is_waiting);

    if(player == 0) {
        unset_game_flag(&(game.flags), WAITING_P1_FLAG);
    } else if (player == 1) {
        unset_game_flag(&(game.flags), WAITING_P2_FLAG);
    }

    pthread_mutex_unlock(&mutex_is_waiting);
}

/*
 * Stores the index to timer_threads array for new thread
 * to variable.
 */
void get_timer_thread_num(int *variable) {
    int cnt = 0;

    while(cnt < MAX_TIMER_THREADS && timer_threads[cnt] != NULL) {
        cnt ++;
    }

    *variable = cnt;
}





/*
 * =====================================================================
 * THREAD FUNCTIONS
 * =====================================================================
 */

/*
 * The action to be performed after thread finishes the waiting.
 * It will check if the game is still waiting for a player and if it is, it will
 * mark the other player as the winner and ends the game.
 * If it isn't waiting anymore, this function won't do anything.
 */
void waiting_thread_after(int waiting_for) {
    int tmp;
    char log_msg[100];
    is_game_waiting_for_player(&tmp, waiting_for);
    if(tmp == 1) {
        // the game is still waiting for a player
        sprintf(log_msg, "The player %d still isn't connected. Other player wins the game.\n", waiting_for);
        sdebug(TIMER_THREAD_NAME, log_msg);


        // check the end of the game
        if (is_end_of_game(&game) == 1) {
            sdebug(TIMER_THREAD_NAME, "The game has already ended.\n");
            return;
        }

        // set the winner and end game
        if(waiting_for == 0) {
            server_set_winner(1);
        } else {
            server_set_winner(0);
        }
        decrement_players();
        server_end_game();

        // wake waiting player threads
        switch_turn(&game, waiting_for);
        pthread_mutex_unlock(&mutex_turn);
        pthread_cond_signal(&cond_turn);
    } else {
        sprintf(log_msg, "The game is not waiting for player %d anymore.\n", waiting_for);
        sdebug(TIMER_THREAD_NAME, log_msg);
    }
}

/*
 * Starts a waiting thread.
 * Returns -1 if an error occurs.
 */
int start_waiting_thread(int waiting_for) {
    int tmp, thread_err;
    char err[250];
    struct Timer_thread_struct* tt_args = malloc(sizeof(struct Timer_thread_struct));

    get_timer_thread_num(&tmp);

    // create the argument structure - the timer thread will free it
    tt_args->waiting_for = waiting_for;
    tt_args->timeout = (unsigned )timeout;
    tt_args->cleaning_function = &clean_me;
    tt_args->perform_after = &waiting_thread_after;
    tt_args->thread_number = -tmp - 1;

    // start the timer thread
    thread_err = pthread_create(&timer_threads[tmp], NULL, timer_thread, (void*)tt_args);
    if(thread_err) {
        sprintf(err, "Error while creating a timer thread %d: %s\n",tmp, strerror(thread_err));
        serror(SERVER_NAME,err);
        return -1;
    }

    return 0;
}

/*
 * Thread function which will wait for signal to clean thread slots.
 */
void *cleaner(void *arg) {
	int thread_index;
	char log_msg[50];
	
	sdebug(SERVER_NAME, "Starting cleaning thread.\n");
	
	while(1) {
		/* wait for signal - conditional variable*/
		sem_wait(&cleaner_sem);
		
		/* clean */
		get_cleaner_index(&thread_index);
        if(thread_index >= 0 && thread_index <= MAX_CONNECTIONS) {
            sprintf(log_msg, "Cleaning player thread on index: %d.\n", thread_index);
            sdebug(SERVER_NAME, log_msg);
            pthread_join(connections[thread_index], NULL);
            connections[thread_index] = NULL;
        } else if( thread_index >= -MAX_TIMER_THREADS && thread_index <= -1) {
            sprintf(log_msg, "Cleaning timer thread on index: %d.\n", -thread_index -1);
            sdebug(SERVER_NAME, log_msg);
            pthread_join(timer_threads[-thread_index -1], NULL);
            timer_threads[-thread_index -1] = NULL;
        } else {
            break;
        }
		
		/* call sem_post for the waiting queue */
		sem_post(&cleaning_queue_sem);
	}
	
	sdebug(SERVER_NAME, "Cleaning thread is shuting down.\n");
	
	return NULL;
}

/*
 * Logs a debug message.
 */
void debug_player_message(char* buffer, char * pattern, int player_num) {
    sprintf(buffer, pattern, player_num);
    sdebug(PLAYER_THREAD_NAME, buffer);
}
 
/*
 * Calls function form nick_val to check nick.
 */  
int check_nick(char *nick, char *err_msg)
{
	int check_res;
	
	check_res = check_nickname(nick, NULL, game.players);
	
	switch(check_res) {
//		case ERR_NICK_TOO_SHORT:
//			sprintf(err_msg,"Nick '%s' is too short.\n",nick);
//			return 0;
//		case ERR_NICK_TOO_LONG:
//			sprintf(err_msg,"Nick '%s' is too long.\n",nick);
//			return 0;
//		case ERR_FIRST_CHAR_INV:
//			sprintf(err_msg,"Nick '%s' starts with invlid character.\n",nick);
//			return 0;
//		case ERR_CONTAINS_INV_CHAR:
//			sprintf(err_msg,"Nick '%s' contains invalid characters.\n",nick);
//			return 0;
        case ERR_NICKNAME:
            sprintf(err_msg,"Nick '%s' is too short or contains bad characters.\n", nick);
		case ERR_NICK_EXISTS:
			sprintf(err_msg,"Nick '%s' already exists.\n",nick);
			return 0;
	}
	
	return 1;
}

/*
 * Waits for nick to be received.
 * Returns 1 if the nick is received, or 0 if the sockes closes conneciton.
 */
int wait_for_nick(int socket, char* buffer, char* log_msg) {
    int msg_status = recv_nick(socket, buffer);
    while (msg_status != 1) {
        if (msg_status == 0) {
            return 0;

        } else {
            /* print error from seneterror*/
            sprintf(log_msg, "Error occured while receiving nick: %i\n",msg_status);
            serror(PLAYER_THREAD_NAME, log_msg);
        }
        msg_status = recv_nick(socket, buffer);
    }

    return 1;
}

/*
 * Checks, if the game is waiting for a player. If it's waiting, it will
 * compare the port and addr with the data stored in 'players' variable and
 * returns:
 *
 * -1: Game isn't waiting for anybody or addr/port combination doesn't match stored data.
 * 0: Game is waiting for player 1 and addr/port combination matches the one in players[0]
 * 1: Game is waiting for player 2 and addr/port combination matches the one in players[1];
 */
int check_waiting_player(uint32_t addr, int port, int *res) {
    pthread_mutex_lock(&mutex_players_check);

    int p1w = 0;
    int p2w = 0;

    // the game is already running and is waiting for the player
    is_game_waiting_for_player(&p1w, 1);
    is_game_waiting_for_player(&p2w, 2);
    if (p1w == 1 && game.players[0].addr == addr && game.players[1].port == port) {
        *res = 0;
    } else if (p2w == 1 && game.players[0].addr == addr && game.players[1].port == port) {
        *res = 1;
    } else {
        *res = -1;
    }

    pthread_mutex_unlock(&mutex_players_check);
}

/*
 * Sends the waiting for my_player message to other player (not to my_player).
 */
void send_waiting_message(int my_player) {
    int other_player;
    if(my_player == 0) {
        other_player = 1;
    } else if (my_player == 1) {
        other_player = 0;
    } else {
        return;
    }

    send_waiting_for_player_msg(game.players[other_player].socket, game.players[my_player].nick);
}

/*
 * Sets the waiting flang, sends the waiting message
 * to other player and starts the timer thread.
 */
void handle_disconnect(int disconnected_player) {
    // set waiting flag
    set_waiting_for(disconnected_player);

    // send message to other player
    send_waiting_message(disconnected_player);

    // start timer thread
    start_waiting_thread(disconnected_player);
}

/*
 * Handles the status of received end turn message
 * and returns 1 if the game loop should stop.
 */
int handle_end_turn_message(int msg_status, int my_player, int other_player) {
    char log_msg[255];

    if(msg_status < 0) {
        sprintf(log_msg, "Error while receiving end turn message: %d\n", msg_status);
        serror(PLAYER_THREAD_NAME, (char*)&log_msg);
        //set the other player as winner
        server_set_winner(other_player);
        return 1;
    } else if (msg_status == 0) {
        debug_player_message(log_msg, "Player %d has disconnected, waiting for him to reconnect.\n",
                             my_player);

        handle_disconnect(my_player);

        return 1;
    } else if (msg_status == 2) {
        debug_player_message(log_msg, "Player %d quit. Other player wins the game.\n", my_player);

        server_set_winner(other_player);
        return 1;
    } else {
        debug_player_message(log_msg, "Player %d has ended his turn.\n", my_player);

        return 0;
    }
}

/*
 * A thread for one player. At first, nickname will be checked, then 
 * the thread waits for another player to be registered and the game begins.
 */
void *player_thread(void *arg) {
	
	char log_msg[255];
    char p1name[10];
    char p2name[10];
    char tmp_p1_word[TURN_WORD_LENGTH];
    char tmp_p2_word[TURN_WORD_LENGTH];
	
	char buffer[MAX_TXT_LENGTH + 1];
    thread_arg* args = ((thread_arg *)arg);
	int msg_status = 0;
	int socket = args->socket;
	int thread_num = args->thread_number;
    uint32_t addr = args->addr;
    int port = args->port;
	int nick_valid = 0;
	int tmp;
    int winner;
    int break_game_loop;

    /*
	 * Index in the array of players.
	 */
    int my_player;
    int other_player;

	sprintf(log_msg, "Starting new thread with id=%d.\n",thread_num);
	sinfo(PLAYER_THREAD_NAME, log_msg);

    /*
     * Check if any player has disconnected and the game is waiting for him
     * to reconnect.
     */
    check_waiting_player(addr, port, &my_player);
    if(my_player != -1) {
        // player has returned
        debug_player_message(log_msg, "Player %d has returned to the game!\n", my_player);

        // set other_player
        other_player = (my_player == 0 ? 1 : 0 );

        // unset the waiting flag
        unset_waiting_for(my_player);

        // send the message to other client, the the player has returned - is this even necessary?
        //TODO: implement this

    } else {
        // standard procedure
        /* check if the server isn't already full */
        get_players(&my_player);
        get_game_started(&tmp);
        if(my_player == 2) {
            serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
            send_err_msg(socket, ERR_SERVER_FULL);
            clean_me(thread_num);
            return NULL;
        } else if (tmp) {
            serror(PLAYER_THREAD_NAME, "Game is already running, sorry.\n");
            send_err_msg(socket, ERR_SERVER_FULL);
            clean_me(thread_num);
            return NULL;
        }
        sprintf(log_msg,"Waiting for nick from socket: %d.\n",socket);
        sinfo(PLAYER_THREAD_NAME,log_msg);

        /* wait for correct nick */
        msg_status = wait_for_nick(socket, buffer, log_msg);
        if(msg_status == 0) {
            sprintf(log_msg,"Socket %d closed connection.\n",socket);
            sinfo(PLAYER_THREAD_NAME, log_msg);
            clean_me(thread_num);
            return NULL;
        }


        sprintf(log_msg,"Nick received: %s\n",buffer);
        sinfo(PLAYER_THREAD_NAME,log_msg);
        /* check nick */
        nick_valid = check_nick(buffer, log_msg);
        /* nick validation failed */
        if(!nick_valid) {
            serror(PLAYER_THREAD_NAME, log_msg);

            /* send error */
            send_err_msg(socket, nick_valid);

            clean_me(thread_num);
            return NULL;
        }

        /*
         * Nickname passed validation,
         * register a new player.
         *
         * Wait for the second thread.
         */
        sdebug(PLAYER_THREAD_NAME, "Nick validation ok.\n");

        /* send message to player that the nick is ok */
        msg_status = send_ok_msg(socket);
        if(msg_status == 2) {
            sprintf(log_msg,"Socket %d closed connection.\n",socket);
            sinfo(PLAYER_THREAD_NAME, log_msg);

            clean_me(thread_num);
            return NULL;
        } else if(msg_status == 0) {
            serror(PLAYER_THREAD_NAME, "Some unexpected error occurred while sending the OK message.");
        }
        /* check if the server isn't already full */
        get_players(&my_player);
        if(my_player >= 2) {
            serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
            clean_me(thread_num);
            return NULL;
        }

        initialize_player(&(game.players[my_player]), my_player, buffer, socket, my_player, addr, port);
        print_player(&(game.players[my_player]), log_msg, 1);
        sinfo(PLAYER_THREAD_NAME, log_msg);
        increment_players();
        if(my_player == 1) {
            /* one thread is already in queue - wake it up and lets play */
            other_player = 0;
            sem_post(&player_sem);
            sinfo(PLAYER_THREAD_NAME, "THE GAME HAS STARTED!\n");
            server_start_game();
        } else {
            /* no thread has finished validation yet, wait for another thread */
            other_player = 1;
            sinfo(PLAYER_THREAD_NAME, "Waiting for other player.\n");
            init_new_game(&game, my_player);
            sem_wait(&player_sem);
        }
        // send start game message to client
        get_player_nick(0, p1name);
        get_player_nick(1, p2name);
        send_start_game_msg(socket, p1name, p2name);
    }


    // game loop
    while(1) {
        pthread_mutex_lock(&mutex_turn);

        // wait for my turn
        if(!is_my_turn(&game, my_player)) {
            debug_player_message(log_msg, "Player %d is waiting for his turn.\n", my_player);
            while(!is_my_turn(&game, my_player)) {
                pthread_cond_wait(&cond_turn, &mutex_turn);
            }
            // send start turn message or end game message
            // other thread is sleeping
            if(is_end_of_game(&game) == 1) {
                break;
            } else {
                msg_status = send_start_turn_msg(socket, game.players[0].turn_word, game.players[1].turn_word);
                debug_player_message(log_msg, "Start turn status: %d\n", msg_status);
                if(msg_status < 2) {
                    serror(PLAYER_THREAD_NAME, "Error while sending start turn message.\n");
                    //set the other player as winner
                    server_set_winner(other_player);
                    server_end_game();
                    break;
                }
            }
        }
        // if end game => break through the second while
        if(is_end_of_game(&game) == 1) {
            break;
        }

        debug_player_message(log_msg, "Player %d has started his turn.\n", my_player);

        // wait for end turn message
        msg_status = recv_end_turn(socket, tmp_p1_word, tmp_p2_word);
        break_game_loop = handle_end_turn_message(msg_status, my_player, other_player);
        if(break_game_loop == 1) {
            break;
        }

        // validate the turn
        debug_player_message((char*)&log_msg, "Validating player %d turn.\n", my_player);

        // update the player's stones
        update_players_stones(&(game.players[0]), tmp_p1_word);
        update_players_stones(&(game.players[1]), tmp_p2_word);

        // check the winning conditions
        winner = check_winning_conditions(game.players[0].turn_word, game.players[1].turn_word);
        if(winner != -1) {
            server_set_winner(winner);
            debug_player_message((char*)&log_msg, "Player %d wins the game!\n", winner);
            break;
        }


        // switch the turn
        switch_turn(&game, my_player);
        pthread_mutex_unlock(&mutex_turn);
        pthread_cond_signal(&cond_turn);
    } // end of the game loop

    server_is_game_waiting(&tmp);
    if(tmp == 1) {
        // game loop has exited, because the game is waiting for someone
        // don't switch the turn, don't end the game

        sprintf((char*)&log_msg, "Player %d disconnected. Ending his thread.\n", my_player);
        sinfo(PLAYER_THREAD_NAME, log_msg);
        clean_me(thread_num);
        return NULL;
    }

    /* end game */
    strcpy(buffer, "\0");
    server_get_winner(&winner);
    get_player_nick(winner, buffer);
    send_end_game_msg(socket, buffer);
    server_end_game();

    /* wake the other thread */
    switch_turn(&game, my_player);
    pthread_mutex_unlock(&mutex_turn);
    pthread_cond_broadcast(&cond_turn);

	sinfo(PLAYER_THREAD_NAME,"End of thread.\n");
	
	decrement_players();
	clean_me(thread_num);
	return NULL;
} 






/*
 * =====================================================================
 * MAIN FUNCTION AND HELPER FUNCTIONS
 * =====================================================================
 */

void print_help() {
    printf("+================+\n");
    printf("|  SENET SERVER  |\n");
    printf("+================+\n\n");
    printf("Possibe paramaters:\n");
    printf("===================\n");
    printf("help: Display this help and quit.\n");
    printf("port <port>: Specify the port server will be listening on. Must be 49152 <= port <= 65535. 65000 is used by default.\n");
    printf("ip   <ip>: String which specifies the ip address server will be listening on. If not valid, INADDR_ANY is used.\n");
}

/*
 * Converts argv to number and stores it to port.
 * If the conversion goes wrong or the port isn't in range, default value
 * is used.
 */
void load_port(char* argv, int* port) {
    char log_msg[100];
    *port = (int)strtol(argv, NULL , 10);
    if (*port < 49152 || *port > 65535) {
        sprintf(log_msg, "Provided port value %s is out of possible range, using default %d.\n", argv, SRV_PORT);
        sdebug(SERVER_NAME, log_msg);
        *port = SRV_PORT;
    }
}

/*
 * Converts argv to ip addres.
 * If the conversion fails, INADDR_ANY is used.
 */
void load_ip(char* argv, struct in_addr* addr) {
    char log_msg[100];
    if(inet_aton(argv, addr) == 0) {
        sprintf(log_msg, "Address %s is not valid. Using INADDR_ANY instead.\n", argv);
        serror(SERVER_NAME, log_msg);

        addr->s_addr = htonl(INADDR_ANY);
    }
}

void load_timeout(char* argv, int* timeout) {
    char log_msg[100];
    *timeout = (int)strtol(argv, NULL , 10);
    if (*timeout < MIN_TIMEOUT || *timeout > MAX_TIMEOUT) {
        sprintf(log_msg, "Provided timeout value %s is out of possible range, using default %d.\n", argv, DEF_TIMEOUT);
        sdebug(SERVER_NAME, log_msg);
        *timeout = DEF_TIMEOUT;
    }
}

/*
 * Loads data from arguments and return 1 if the program should exist afterwards.
 */
int load_arguments(int argc, char* argv[], int* port, struct in_addr* addr, int* timeout) {
    int cntr = 1;
    char log_msg[100];

    while (cntr < argc) {
        if(strcmp(argv[cntr], HELP_PARAM_NAME) == 0) {
            // print help
            print_help();
            return 1;
        } else if (strcmp(argv[cntr], PORT_PARM_NAME) == 0) {
            // load port
            cntr++;
            load_port(argv[cntr], port);
            cntr++;
        } else if (strcmp(argv[cntr], IP_PARAM_NAME) == 0) {
            // load ip
            cntr++;
            load_ip(argv[cntr], addr);
            cntr++;
        } else if (strcmp(argv[cntr], TIMEOUT_PARAM_NAME) == 0){
            //load timeout
            cntr++;
            load_timeout(argv[cntr], timeout);
            cntr++;
        } else {
            // unknown parameter
            sprintf(log_msg, "Unknown parameter %s.\n", argv[cntr]);
            sdebug(SERVER_NAME, log_msg);
            print_help();
            return 1;
        }
    }

    return 0;
}

/*
 * Main function.
 */ 
int main(int argc, char *argv[])
{
	int sock, incoming_sock;
    int port = SRV_PORT;
	int optval;
	struct sockaddr_in addr, incoming_addr;
    struct in_addr ip_addr;
	unsigned int incoming_addr_len;
    pthread_t cleaner_thread;
    thread_arg thread_args[MAX_CONNECTIONS];
    int thread_err;
    char log_msg[255];
    int tmp_curr_conn;
    int tmp;

    /* load arguments */
    ip_addr.s_addr = htonl(INADDR_ANY);
    if(load_arguments(argc, argv, &port, &ip_addr, &timeout) == 1) {
        return 0;
    }

    /* init timer threads */
    for (tmp = 0; tmp < MAX_TIMER_THREADS; tmp++) {
        timer_threads[tmp] = NULL;
    }

	printf("\n\n");
	printf("==========================\n");
	printf("   I am a Senet server.\n");
	printf("  Connect on port %d\n",port);
	printf("==========================\n\n\n");

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
	addr.sin_port = htons((uint16_t )port);
	addr.sin_addr = ip_addr; /* listen on all interfaces */
	if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        sprintf(log_msg, "Error while binding a new listener: %s.\n", strerror(errno));
		serror(SERVER_NAME, log_msg);
		return 1;
	}

	if (listen(sock, 10) < 0) {
        sprintf(log_msg, "Error while listening: %s.\n", strerror(errno));
        serror(SERVER_NAME, log_msg);
		return 1;
	}
	
	if (!init_ms()) {
		return 1;
	}
	
	/*
	 * Start the cleaning thread
	 */ 
	if(pthread_create(&cleaner_thread, NULL, cleaner, NULL)) {
		serror(SERVER_NAME, "Error while initializing the cleaner thread.\n");
		return 1;
	}

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
			serror("server","Error while accepting a new connection.");
		}
		
		/* connection info */
		sprintf(log_msg, "Connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		sinfo("server",log_msg);

		/* 
		 * new thread which will validate the nickname 
		 * and accept a new player.
		 */
		
		
		/* !!! critical section !!! */
		get_curr_conn(&tmp_curr_conn);
		
		if(tmp_curr_conn == MAX_CONNECTIONS - 1) {
			sinfo(SERVER_NAME,"Server can't accept any new connections.");
			continue;
		}
		
		thread_args[tmp_curr_conn].thread_number = tmp_curr_conn;
		thread_args[tmp_curr_conn].socket = incoming_sock;
        thread_args[tmp_curr_conn].addr = incoming_addr.sin_addr.s_addr;
        thread_args[tmp_curr_conn].port = incoming_addr.sin_port;
		thread_err = pthread_create(&connections[tmp_curr_conn], NULL, player_thread,(void *)&thread_args[tmp_curr_conn]);
		if(thread_err) {
			sprintf(log_msg, "Error: %s\n",strerror(thread_err));
			serror(SERVER_NAME,log_msg);
            break;
		}
	}
	
	/* signal cleaner*/
	
	/* wait for cleaner */
	shutdown_cleaner();
	pthread_join(cleaner_thread, NULL);
	return 0;
}
