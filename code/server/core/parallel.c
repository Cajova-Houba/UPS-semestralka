
#include "parallel.h"

/*
 * Initializes mutexes and semaphores and returns 0 if error occurs.
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

    if (pthread_mutex_init(&mutex_is_waiting, NULL) != 0) {
        serror(SERVER_NAME, "Waiting mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_players_check, NULL) != 0) {
        serror(SERVER_NAME, "Players checking mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_is_my_turn, NULL) != 0) {
        serror(SERVER_NAME, "Players checking mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_winner, NULL) != 0) {
        serror(SERVER_NAME, "Winner mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_timer_threads, NULL) != 0) {
        serror(SERVER_NAME, "Timer threads mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_switch_turn, NULL) != 0) {
        serror(SERVER_NAME, "Switch turn mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_queue, NULL) != 0) {
        serror(SERVER_NAME, "Queue mutex initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&mutex_games, NULL) != 0) {
        serror(SERVER_NAME, "Games mutex initialization failed.\n");
        return 0;
    }

    if (sem_init(&player_sem, 0, 0) < 0) {
        serror("server","Players semaphore initialization failed.\n");
        return 0;
    }

    if (sem_init(&cleaner_sem, 0, 0) < 0) {
        serror(SERVER_NAME, "Cleaner semaphore initialization failed.\n");
        return 0;
    }

    if(sem_init(&cleaning_queue_sem, 0, 1) < 0) {
        serror(SERVER_NAME, "Cleaning queue semaphore initialization failed.\n");
        return 0;
    }

    if(sem_init(&turn_sem, 0, 0) < 0) {
        serror(SERVER_NAME, "Turn semaphore initialization failed.\n");
        return 0;
    }


    return 1;
}

/*
 * A timer thread which will wait for some time and then perform an action.
 * It will also free the passed argument structure.
 */
void* timer_thread(void* args) {

    struct Timer_thread_struct* tt_args = ((struct Timer_thread_struct *)args);
    void (*cleaning_function)(int,int) = tt_args->cleaning_function;
    int t_number = tt_args->thread_number;
    char log_msg[255];

    sprintf(log_msg, "Starting timer thread %d with timeout %d seconds.\n", t_number, tt_args->timeout);
    sdebug(TIMER_THREAD_NAME, log_msg);

    // wait
    sleep(tt_args->timeout);

    // call the action after waiting
    (*tt_args->perform_after)(tt_args->waiting_for);

    // clean itself
    free(tt_args);
    cleaning_function(t_number, TIMER_THREAD_TYPE);
}

