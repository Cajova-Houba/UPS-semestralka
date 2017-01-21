#ifndef SERVER_PARALEL_H
#define SERVER_PARALEL_H

#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <stdlib.h>
#include "../common/slog.h"

/*
 * Free slot for thread
 */
#define NO_THREAD       ((pthread_t)-1)

/*
 * Arguments for timer thread.
 */
typedef struct {

    /*
     * An index to the timer_threads array in server.c
     * Note, it begins on -1 and goes to the negative numbers (-2,-3..)
     */
    int thread_number;

    /*
     * A number of second for which the thread will wait.
     */
    unsigned int timeout;

    /*
     * The player thread is waiting for - 0 or 1.
     */
    int waiting_for;

    /*
     * The action to be performed after the thread finishes the waiting.
     */
    void (*perform_after)(int);

    /* A pointer to the clearing function.
     * The thread will call this functions after all the work is done.
     */
    void (*cleaning_function)(int);
} Timer_thread_struct;


/*
 * Mutexes for critical sections.
 */
pthread_mutex_t mutex_curr_conn;
pthread_mutex_t mutex_players;
pthread_mutex_t mutex_game_started;
pthread_mutex_t mutex_cleaner_index;
pthread_mutex_t mutex_get_player;
pthread_mutex_t mutex_is_waiting;
pthread_mutex_t mutex_players_check;
pthread_mutex_t mutex_timer_threads;
pthread_mutex_t mutex_winner;

/*
 * Semaphores for switching turns.
 */
sem_t p1_sem;
sem_t p2_sem;
pthread_mutex_t mutex_get_turn;

/*
 * A semaphore for threads with players.
 * Threads with passed nick validation will wait for another thread to pass
 * nick validation. If a thread passes validation and there are already two players,
 * error message will be sent to client and connection closed.
 *
 * The semaphore will be initialized to 0 so that the first thread with sucessful
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
 * A semaphore for maintaining a queue of threads waiting to be joined.
 *
 * When the thread calls clean_function, it will call sem_wait() and the cleaner thread
 * will call sem_post() when the original thread is joined.
 *
 */
sem_t cleaning_queue_sem;

/*
 * A semaphore used for used for switching turns between threads.
 *
 * The semaphore will be initialized to 0, so that the first thread to call wait (this will always
 * be the second player's thread) will have to wait.
 */
sem_t turn_sem;

/*
 * Initializes mutexes and semaphores and returns 0 if error occurs.
 */
int init_ms();

/*
 * A timer thread which will wait for some time and then perform an action.
 * It will also free the passed argument structure.
 */
void* timer_thread(void* args);

/*
 * Re-initializes turn semaphores of both players.
 *
 * Returns 1 if error occurs.
 */
int reinit_player_sem();

#endif //SERVER_PARALEL_H
