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
 * Timeout for messages while the thread is waiting
 */
#define WAITING_TIMEOUT         1000

/*
 * Max number of attempts for player to send a correct nick.
 */
#define MAX_NICK_ATTEMPTS       3


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
Game_struct game;

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
    reinit_player_sem();
	
	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls set_winner() and
 * end_game() functions from the game.h.
 */
void server_set_winner(int winner){
    pthread_mutex_lock(&mutex_winner);

    set_winner(&game, winner);
    end_game(&game);

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
 * Handles the critical section and stores the TURN flag in the variable.
 * 0 = 1st player, 1 = 2nd player.
 */
void server_get_current_turn(int* variable) {
    pthread_mutex_lock(&mutex_get_turn);

    *variable = get_game_flag(&(game.flags), TURN_FLAG);

    pthread_mutex_unlock(&mutex_get_turn);
}

/*
 * Handles the critical section and if the game has already ended,
 * stores OK into variable.
 */
void server_is_game_end(int *variable) {
    int i = 0;
    pthread_mutex_lock(&mutex_winner);

    i = get_game_flag(&(game.flags), GAME_ENDED_FLAG);
    if(i == 1) {
        *variable = OK;
    } else {
        *variable = 0;
    }


    pthread_mutex_unlock(&mutex_winner);
}

/*
 * Handles the critical section and changes the turn.
 */
void server_switch_turn() {
    int i = 0;
    pthread_mutex_lock(&mutex_get_turn);

    i = get_game_flag(&(game.flags), TURN_FLAG);
    switch_turn(&game, i);

    pthread_mutex_unlock(&mutex_get_turn);
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
		if(connections[i] == NO_THREAD) {
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

    while(cnt < MAX_TIMER_THREADS && timer_threads[cnt] != NO_THREAD) {
        cnt ++;
    }

    *variable = cnt;
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
    get_players(&i);
    if(i <= 1) {
        for (i = 0; i < MAX_NICK_LENGTH; i++) {
            game.players[0].nick[i] = '\0';
            game.players[1].nick[i] = '\0';
        }
    }

    pthread_mutex_unlock(&mutex_game_started);
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
//        switch_turn(&game, waiting_for);
        if(waiting_for == 0) {
            sem_post(&p2_sem);
        } else {
            sem_post(&p1_sem);
        }
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
    Timer_thread_struct* tt_args = malloc(sizeof(Timer_thread_struct));

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
            connections[thread_index] = NO_THREAD;
        } else if( thread_index >= -MAX_TIMER_THREADS && thread_index <= -1) {
            sprintf(log_msg, "Cleaning timer thread on index: %d.\n", -thread_index -1);
            sdebug(SERVER_NAME, log_msg);
            pthread_join(timer_threads[-thread_index -1], NULL);
            timer_threads[-thread_index -1] = NO_THREAD;
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
//			sprintf(err_msg,"Nick '%s' starts with invalid character.\n",nick);
//			return 0;
//		case ERR_CONTAINS_INV_CHAR:
//			sprintf(err_msg,"Nick '%s' contains invalid characters.\n",nick);
//			return 0;
        case ERR_NICKNAME:
            sprintf(err_msg,"Nick '%s' is too short or contains bad characters.\n", nick);
            return ERR_NICKNAME;
		case ERR_NICK_EXISTS:
			sprintf(err_msg,"Nick '%s' already exists.\n",nick);
			return ERR_NICK_EXISTS;
	}
	
	return OK;
}

/*
 * Waits for end turn to be received. Timeout should be very small, about 1 second.
 * For every non-end turn message timeout will be added to counter and when the counter
 * reaches MAX_TURN_WAITING_TIMEOUT, makes other player a winner and returns STOP_GAME_LOOP.
 *
 * Returns:
 * OK: Nick is received.
 * STOP_GAME_LOOP: error while receiving nick.
 */
int wait_for_end_turn(int socket, int timeout, int my_player, int other_player, char* p1_tw, char* p2_tw) {
    Message message;
    char log_msg[255];
    int msg_status = 0;
    int end_turn_recv = 0;
    int i = 0;
    int cntr = 0;

    while (end_turn_recv != OK && cntr < MAX_TURN_WAITING_TIMEOUT) {

        // receive message
        msg_status = recv_message(socket, &message, timeout);

        // handle connection errors
        switch(msg_status) {
            case MSG_TIMEOUT:
                cntr += timeout;
                continue;
            case CLOSED_CONNECTION:
                sprintf(log_msg, "Socket %d closed the connection while waiting for the end turn message.\n", socket);
                server_set_winner(other_player);
                return STOP_GAME_LOOP;
        }

        // handle message errors
        if(msg_status != OK) {
            sprintf(log_msg, "Error (%d) while receiving message from socket %d.\n", msg_status, socket);
            serror(PLAYER_THREAD_NAME, log_msg);
            if (msg_status < GENERAL_ERR || msg_status >= ERR_LAST) {
                msg_status = GENERAL_ERR;
            }
            send_err_msg(socket, msg_status);
            cntr += timeout;

        } else {
            // handle possible messages
            if (is_alive(&message) == OK) {
                // response to is alive message
                send_ok_msg(socket);
                cntr += timeout;

            } else if (is_exit(&message) == OK) {
                // handle exit => other player wins
                server_set_winner(other_player);
                return STOP_GAME_LOOP;

            } else if (is_end_turn(&message) == OK) {
                // end turn message
                // copy turn word from message to provided variables
                for(i = 0; i < TURN_WORD_LENGTH; i++) {
                    p1_tw[i] = message.content[i];
                    p2_tw[i] = message.content[i+TURN_WORD_LENGTH];
                }
                sprintf(log_msg, "End turn message received: %s\n", message.content);
                sinfo(PLAYER_THREAD_NAME, log_msg);
                end_turn_recv = OK;

            } else {
                // something else
                send_err_msg(socket, ERR_UNEXPECTED_MSG);
                cntr += timeout;
            }
        }
    }

    if (cntr >= MAX_TURN_WAITING_TIMEOUT) {
        sprintf(log_msg, "Max time for player's %d end turn message reached.\n", my_player);
        server_set_winner(other_player);
        return STOP_GAME_LOOP;
    }

    return OK;
}


/*
 * Waits for nick to be received. Received and checked nick is stored to buffer.
 *
 * Returns:
 * OK: Nick is received and checked.
 * 0: error while receiving nick. The error will be handled in this function.
 */
int wait_for_nick(int socket, char* buffer) {
    Message message;
    char log_msg[255];
    int msg_status = 0;
    int attempt = 0;
    int nick_ok = 0;
    while (nick_ok != OK && attempt < MAX_NICK_ATTEMPTS) {
        msg_status = recv_message(socket, &message, MAX_NICK_TIMEOUT);

        // handle connection errors
        switch (msg_status) {
            case MSG_TIMEOUT:
                sprintf(log_msg, "Socket %d timed out while waiting for nick.\n", socket);
                serror(PLAYER_THREAD_NAME, log_msg);
                send_err_msg(socket, ERR_TIMEOUT);
                return 0;
            case CLOSED_CONNECTION:
                sprintf(log_msg, "Socket %d closed connection.\n", socket);
                return 0;
        }

        // handle message errors
        if(msg_status != OK) {
            sprintf(log_msg, "Error while receiving message from socket %d.\n", socket);
            serror(PLAYER_THREAD_NAME, log_msg);
            if(msg_status < GENERAL_ERR || msg_status >= ERR_LAST) {
                msg_status = GENERAL_ERR;
            }
            send_err_msg(socket, msg_status);
            attempt++;

        // handle messages
        } else {

            if(is_alive(&message) == OK) {
                // response to is alive message
                send_ok_msg(socket);
            } else if (is_exit(&message) == OK) {
                // exit received, disconnect
                sprintf(log_msg, "Socket %d quit.\n", socket);
                sinfo(PLAYER_THREAD_NAME, log_msg);
                return 0;

            } else if (is_nick(&message) != OK) {
                // something other than nick
                send_err_msg(socket, ERR_UNEXPECTED_MSG);
                attempt++;

            } else {
                // message is nick, check it
                sprintf(log_msg,"Nick received: %s\n",message.content);
                sinfo(PLAYER_THREAD_NAME,log_msg);
                // check nick
                nick_ok = check_nick(message.content, log_msg);
                // nick validation failed
                if(nick_ok != OK) {
                    serror(PLAYER_THREAD_NAME, log_msg);

                    /* send error */
                    send_err_msg(socket, nick_ok);

                    attempt++;
                } else {

                    // nick is OK return OK
                    strcpy(buffer, message.content);
                    return OK;
                }

            }
        }
    } // while end

    if(attempt == MAX_NICK_ATTEMPTS) {
        sprintf(log_msg, "Maximum number of nick attempts reached for socket %d.\n", socket);
        send_err_msg(socket, ERR_TOO_MANY_ATTEMPTS);
    }

    return 0;
}

/*
 * Checks, if the game is waiting for a player. If it's waiting, it will
 * compare the port and addr with the data stored in 'players' variable and
 * returns:
 *
 * GAME_NOT_WAITING: Game isn't waiting for anybody or addr/port combination doesn't match stored data.
 * WAITING_P1: Game is waiting for player 1 and addr/port combination matches the one in players[0]
 * WAITING_P2: Game is waiting for player 2 and addr/port combination matches the one in players[1];
 */
//int check_waiting_player(uint32_t addr, int port, int *res) {
//    pthread_mutex_lock(&mutex_players_check);
//
//    int p1w = 0;
//    int p2w = 0;
//
//    // the game is already running and is waiting for the player
//    is_game_waiting_for_player(&p1w, 1);
//    is_game_waiting_for_player(&p2w, 2);
//    if (p1w == 1 && game.players[0].addr == addr && game.players[1].port == port) {
//        *res = WAITING_P1;
//    } else if (p2w == 1 && game.players[0].addr == addr && game.players[1].port == port) {
//        *res = WAITING_P2;
//    } else {
//        *res = GAME_NOT_WAITING;
//    }
//
//    pthread_mutex_unlock(&mutex_players_check);
//}

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
//void handle_disconnect(int disconnected_player) {
//    // set waiting flag
//    set_waiting_for(disconnected_player);
//
//    // send message to other player
//    send_waiting_message(disconnected_player);
//
//    // start timer thread
//    start_waiting_thread(disconnected_player);
//}

/*
 * Handles the status of received end turn message.
 * Returns:
 *  STOP_GAME_LOOP: game loop should stop.
 *  OK
 */
//int handle_end_turn_message(int msg_status, int my_player, int other_player) {
//    char log_msg[255];
//
//    if (msg_status == MSG_TIMEOUT) {
//        sprintf(log_msg, "Player %d timed out.\n");
//        serror(PLAYER_THREAD_NAME, log_msg);
//        server_set_winner(other_player);
//        return STOP_GAME_LOOP;
//    } else if (msg_status == 0) {
//        debug_player_message(log_msg, "Player %d has disconnected, waiting for him to reconnect.\n",
//                             my_player);
//
//        handle_disconnect(my_player);
//
//        return STOP_GAME_LOOP;
//    } if(msg_status < 0) {
//        sprintf(log_msg, "Error while receiving end turn message: %d\n", msg_status);
//        serror(PLAYER_THREAD_NAME, log_msg);
//        //set the other player as winner
//        server_set_winner(other_player);
//        return STOP_GAME_LOOP;
//    }  else if (msg_status == 2) {
//        debug_player_message(log_msg, "Player %d quit. Other player wins the game.\n", my_player);
//
//        server_set_winner(other_player);
//        return STOP_GAME_LOOP;
//    } else {
//        debug_player_message(log_msg, "Player %d has ended his turn.\n", my_player);
//
//        return OK;
//    }
//}

/*
 * Call when the game is to be ended adn the winner is set.
 */
void player_thread_end_game(int socket, int my_player) {
    /* end game */
    char buffer[255];
    int winner;
    strcpy(buffer, "\0");
    server_get_winner(&winner);
    get_player_nick(winner, buffer);
    send_end_game_msg(socket, buffer);
    server_end_game();
    decrement_players();

    /* wake the other thread */
//    switch_turn(&game, my_player);
}

/*
 * Sends a start turn message to player.
 *
 * Returns:
 *  OK: start turn message sent
 *  STOP_GAME_LOOP: error, game loop should be broken.
 */
int send_start_turn_to_player(int socket, int my_player, int other_player) {
    int msg_status;
    char log_msg[255];
    if(is_end_of_game(&game) == 1) {
        return STOP_GAME_LOOP;
    } else {
        debug_player_message(log_msg, "Sending start turn message to player %d.\n", my_player);
        msg_status = send_start_turn_msg(socket, game.players[0].turn_word, game.players[1].turn_word);
//        debug_player_message(log_msg, "Start turn status: %d\n", msg_status);
        if(msg_status < 2) {
            serror(PLAYER_THREAD_NAME, "Error while sending start turn message.\n");
            //set the other player as winner
            server_set_winner(other_player);
            server_end_game();
            return  STOP_GAME_LOOP;
        }
    }

    return  OK;
}

/*
 * Sets clean me and closes the socket.
 */
void set_cleanme_close_sock(int threadnum, int sock) {
    close(sock);
    clean_me(threadnum);
}

/*
 * Handles incoming messages while the thread is waiting for another player to start the game.
 *
 * Returns:
 * OK: message handled.
 * STOP_GAME_LOOP: if error occurs and thread should stop waiting.
 */
int handle_message_waiting(int socket, int timeout, int my_player) {
    int recv_status = 0;
    Message received_message;
    char log_msg[255];

    recv_status = recv_message(socket, &received_message, timeout);
    switch (recv_status) {
        case OK:
            if(is_alive(&received_message) == OK) {
                send_ok_msg(socket);
            } else if(is_exit(&received_message) == OK) {
                sprintf(log_msg, "Player %d quit.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                return STOP_GAME_LOOP;
            } else {
                // send not my turn error
                send_err_msg(socket, ERR_UNEXPECTED_MSG);
            }
            break;

        case MSG_TIMEOUT:
            return OK;

        case CLOSED_CONNECTION:
            sprintf(log_msg, "Player %d closed connection.\n", my_player);
            sdebug(PLAYER_THREAD_NAME, log_msg);
            return STOP_GAME_LOOP;

        default:
            // send error back to client
            if(recv_status < 0) {
                if (recv_status >= GENERAL_ERR && recv_status <= ERR_LAST - 1) {
                    send_err_msg(socket, recv_status);
                    return OK;
                } else {
                    send_err_msg(socket, GENERAL_ERR);
                    return OK;
                }
            }

    }

    return OK;
}

/*
 * Handles incoming messages while thread is currently waiting for
 * its turn. If needed (due to connection error for example), sets the other
 * player as winner and ends the game.
 *
 * The timeout should be around 1s (maybe even smaller).
 *
 * Returns STOP_GAME_LOOP if the game loop should stop. Otherwise returns OK.
 */
int handle_message_waiting_turn(int socket, int timeout, int my_player, int other_player) {
    int recv_status = 0;
    Message received_message;

    recv_status = recv_message(socket, &received_message, timeout);
    switch (recv_status) {
        case OK:
            if(is_alive(&received_message) == OK) {
                send_ok_msg(socket);
            } else if (is_exit(&received_message) == OK) {
                // exit, other player wins
                server_set_winner(other_player);
                return STOP_GAME_LOOP;
            } else {
                // send not my turn error
                send_err_msg(socket, ERR_NOT_MY_TURN);
            }
            break;

        case MSG_TIMEOUT:
            return OK;

        case CLOSED_CONNECTION:
            server_set_winner(other_player);
            return STOP_GAME_LOOP;

        default:
            // send error back to client
            if(recv_status < 0) {
                if (recv_status >= GENERAL_ERR && recv_status <= ERR_LAST - 1) {
                    send_err_msg(socket, recv_status);
                    return OK;
                } else {
                    send_err_msg(socket, GENERAL_ERR);
                    return OK;
                }
            }

    }

    return OK;
}

void game_loop(int socket, int my_player, int other_player) {
    char log_msg[255];
    int break_game_loop = 0;
    char tmp_p1_word[TURN_WORD_LENGTH];
    char tmp_p2_word[TURN_WORD_LENGTH];
    int msg_status = 0;
    int winner = 0;
    int turn_valid = 0;
    int game_loop_state = my_player == 0 ? WAIT_FOR_END_TURN : WAIT_FOR_MY_TURN;
    int tmp = 0;
    int end_game = 0;

    while (game_loop_state != BREAK_LOOP) {
        switch (game_loop_state) {
            case WAIT_FOR_END_TURN:
                break_game_loop = wait_for_end_turn(socket, WAITING_TIMEOUT, my_player, other_player, tmp_p1_word, tmp_p2_word);
                if(break_game_loop == STOP_GAME_LOOP) {
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = VALIDATE_TURN;
                }
                break;

            case VALIDATE_TURN:
                debug_player_message(log_msg, "Validating player %d turn.\n", my_player);
                turn_valid = validate_turn(game.players[0].turn_word, game.players[1].turn_word, tmp_p1_word, tmp_p2_word, my_player);
//                turn_valid = OK;
                if (turn_valid != OK) {
                    debug_player_message(log_msg, "Turn of player %d is not valid, skipping it!\n", my_player);
                    send_err_msg(socket, ERR_TURN);
                } else {
                    // turn valid -> update the player's stones
                    update_players_stones(&(game.players[0]), tmp_p1_word);
                    update_players_stones(&(game.players[1]), tmp_p2_word);
                    send_ok_msg(socket);
                }
                game_loop_state = CHECK_WINNING_COND;
                break;

            case CHECK_WINNING_COND:
                winner = check_winning_conditions(game.players[0].turn_word, game.players[1].turn_word);
                if(winner != OK) {
                    server_set_winner(winner == P1_WINS ? 0 : 1);
                    debug_player_message(log_msg, "Player %d wins the game!\n", winner);
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = SWITCH_TURN;
                }
                break;

            case WAIT_FOR_MY_TURN:
                server_get_current_turn(&tmp);
                end_game = 0;
                if(tmp != my_player) {
                    debug_player_message(log_msg, "Player %d is waiting for his turn.\n", my_player);
                    while (tmp != my_player) {
                        // handle incoming messages
                        handle_message_waiting_turn(socket, WAITING_TIMEOUT, my_player, other_player);

                        // check that the game haven't ended yet
                        server_is_game_end(&end_game);
                        if(end_game == OK) {
                            break;
                        }

                        // check my turn
                        server_get_current_turn(&tmp);
                    }
                    if(end_game == OK) {
                        game_loop_state = BREAK_LOOP;
                    } else {
                        game_loop_state = START_TURN;
                    }
                } else {
                    game_loop_state = START_TURN;
                }
                break;

            case START_TURN:
                debug_player_message(log_msg, "Player %d starts his turn.\n", my_player);
                break_game_loop = send_start_turn_to_player(socket, my_player, other_player);
                if(break_game_loop == STOP_GAME_LOOP) {
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = WAIT_FOR_END_TURN;
                }
                break;

            case SWITCH_TURN:
                server_switch_turn();
                game_loop_state = WAIT_FOR_MY_TURN;

                break;
        }
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
	
	char buffer[MAX_TXT_LENGTH + 1];
    thread_arg* args = ((thread_arg *)arg);
	int msg_status = 0;
	int socket = args->socket;
	int thread_num = args->thread_number;
    uint32_t addr = args->addr;
    int port = args->port;
	int nick_valid = 0;
	int tmp = 0;

    Message message;

    /*
	 * Index in the array of players.
	 */
    int my_player;
    int other_player;

	sprintf(log_msg, "Starting new thread with id=%d.\n",thread_num);
	sinfo(PLAYER_THREAD_NAME, log_msg);

    // check if the server isn't already full
    get_players(&my_player);
    get_game_started(&tmp);
    if(my_player == 2) {
        serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
        send_err_msg(socket, ERR_SERVER_FULL);
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    } else if (tmp) {
        serror(PLAYER_THREAD_NAME, "Game is already running, sorry.\n");
        send_err_msg(socket, ERR_GAME_ALREADY_RUNNING);
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    }

    // wait for nick
    tmp = wait_for_nick(socket, buffer);
    if(tmp != OK) {
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    }

    /*
     * Nickname passed validation,
     * register a new player.
     *
     * Wait for the second thread.
     */
    sdebug(PLAYER_THREAD_NAME, "Nick validation ok.\n");

    // send message to player that the nick is ok
    msg_status = send_ok_msg(socket);
    if (msg_status != OK) {
        sprintf(log_msg, "Error occurred while sending OK message to socket %d.\n", socket);
        serror(PLAYER_THREAD_NAME, log_msg);
        send_err_msg(socket, msg_status);
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    }

    // check again if the server isn't full and initialize new player
    get_players(&my_player);
    if(my_player >= 2) {
        serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
        send_err_msg(socket, ERR_SERVER_FULL);
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    }

    // initialize player
    initialize_player(&(game.players[my_player]), my_player, buffer, socket, my_player, addr, port);
    print_player(&(game.players[my_player]), log_msg, 1);
    sinfo(PLAYER_THREAD_NAME, log_msg);
    increment_players();
    if(my_player == 1) {
        /* one thread is already in queue - wake it up and lets play */
        other_player = 0;
        sinfo(PLAYER_THREAD_NAME, "THE GAME HAS STARTED!\n");
        server_start_game();
    } else {
        // no thread has finished validation yet, wait for another thread
        sinfo(PLAYER_THREAD_NAME, "Waiting for other player.\n");
        other_player = 1;
        init_new_game(&game, my_player);

        while (tmp < 2) {
            // handle incoming messages
            msg_status = handle_message_waiting(socket, WAITING_TIMEOUT, my_player);
            if(msg_status == STOP_GAME_LOOP) {
                decrement_players();
                set_cleanme_close_sock(thread_num, socket);
                return NULL;
            }

            get_players(&tmp);
        }
    }

    // send start game message
    get_player_nick(0, p1name);
    get_player_nick(1, p2name);
    send_start_game_msg(socket, p1name, p2name);

    /*
     * Check if any player has disconnected and the game is waiting for him
     * to reconnect.
     */
//    check_waiting_player(addr, port, &my_player);
//    if(my_player != GAME_NOT_WAITING) {
//        // player has returned
//        debug_player_message(log_msg, "Player %d has returned to the game!\n", my_player);
//
//        // set other_player
//        other_player = (my_player == 0 ? 1 : 0 );
//
//        // unset the waiting flag
//        unset_waiting_for(my_player);
//
//        // send the message to other client, the the player has returned - is this even necessary?
//        //TODO: implement this
//
//    } else {


//    }

    // whole game loop handled inside this function
    game_loop(socket, my_player, other_player);

    server_is_game_waiting(&tmp);
    if(tmp == 1) {
        // game loop has exited, because the game is waiting for someone
        // don't switch the turn, don't end the game

        sprintf((char*)&log_msg, "Player %d disconnected. Ending his thread.\n", my_player);
        sinfo(PLAYER_THREAD_NAME, log_msg);
        set_cleanme_close_sock(thread_num, socket);
        return NULL;
    }

    player_thread_end_game(socket, my_player);

	sinfo(PLAYER_THREAD_NAME,"End of thread.\n");

	set_cleanme_close_sock(thread_num, socket);
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
	int sock = 0;
    int incoming_sock = 0;
	int optval = 0;
    struct sockaddr_in addr, incoming_addr;
    struct in_addr ip_addr;
    unsigned int incoming_addr_len = 0;
    pthread_t cleaner_thread = NO_THREAD;
    thread_arg thread_args[MAX_CONNECTIONS];
    int thread_err = 0;
    char log_msg[255];
    int tmp_curr_conn = 0;
    int tmp = 0;
    int port = SRV_PORT;
    timeout = DEF_TIMEOUT;

    /* init timer threads */
    for (tmp = 0; tmp < MAX_TIMER_THREADS; tmp++) {
        timer_threads[tmp] = NO_THREAD;
    }

    /* load arguments */
    ip_addr.s_addr = htonl(INADDR_ANY);
    if(load_arguments(argc, argv, &port, &ip_addr, &timeout) == 1) {
        return 0;
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
		//new connection
		incoming_addr_len = sizeof(incoming_addr);
    	incoming_sock = accept(sock, (struct sockaddr*)&incoming_addr, 
                      &incoming_addr_len);
		if (incoming_sock < 0)
		{
			close(sock);
			serror("server","Error while accepting a new connection.");
		}
		sprintf(log_msg, "Connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		sinfo("server",log_msg);
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
