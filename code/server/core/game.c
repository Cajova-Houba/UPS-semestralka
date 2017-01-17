

#include "game.h"

/*
 * Resets all flags.
 * Initializes turn_semaphore.
 * And sets turn flag accordingly to first_player.
 *
 * Returns -1 if something fails.
 */
int init_new_game(struct Game_struct* game, int first_player) {
    game->winner = -1;
    unset_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), GAME_ENDED_FLAG);
    unset_game_flag(&(game->flags), WAITING_P1_FLAG);
    unset_game_flag(&(game->flags), WAITING_P2_FLAG);
    unset_game_flag(&(game->flags), WINNER_FLAG);
    if(first_player == 0) {
        unset_game_flag(&(game->flags), TURN_FLAG);
    } else {
        set_game_flag(&(game->flags), TURN_FLAG);
    }

    if(pthread_cond_init(&(game->cond_turn), NULL) != 0) {
        serror(GAME_NAME, "Turn semaphore initialization failed.\n");
        return 0;
    }

    if (pthread_mutex_init(&(game->mutex_turn), NULL) != 0) {
        serror(GAME_NAME, "Turn mutex initialization failed.\n");
        return 0;
    }
}

/*
 * Sets a flag to 1.
 */
void set_game_flag(int* flags, int flag_index) {
    *flags = *flags | (1 << flag_index);
}

/*
 * Sets a flag to 0.
 */
void unset_game_flag(int* flags, int flag_index) {
    *flags = *flags & (~(1 << flag_index));
}

/*
 * Returns the value of the flag.
 */
int get_game_flag(int* flags, int flag_index) {
    // mask flags to get the value and then shift it so it's either 1 or 0
    return (*flags & (1 << flag_index)) >> flag_index;
}

/*
 * Sets the game_started flag to 1.
 */
void start_game(struct Game_struct* game) {
    set_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * Sets the game_started flag to 0, game_ended to 1 and clears waiting flags.
 */
void end_game(struct Game_struct* game) {
    unset_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), WAITING_P1_FLAG);
    unset_game_flag(&(game->flags), WAITING_P2_FLAG);
    set_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * If at least one of the waiting flags is set to 1,
 * returns 1, otherwise returns 0.
 */
int is_game_waiting(struct Game_struct* game) {
    if (get_game_flag(&(game->flags), WAITING_P1_FLAG) == 1 ) {
        return 1;
    }

    if (get_game_flag(&(game->flags), WAITING_P2_FLAG) == 1 ) {
        return 1;
    }

    return 0;
}

/*
 * Compares the my_player value with turn value and either
 * returns 1 if it's my turn or 0 if not.
 */
int is_my_turn(struct Game_struct* game, int my_player) {
    int turn = get_game_flag(&(game->flags), TURN_FLAG);

    return (turn == my_player) ? 1 : 0;
}

/*
 * Switches turn = sets the turn flag to opposite value.
 */
void switch_turn(struct Game_struct* game, int current_turn) {
    if(current_turn == 0) {
        set_game_flag(&(game->flags), TURN_FLAG);
    } else {
        unset_game_flag(&(game->flags), TURN_FLAG);
    }
}

/*
 * If the game ended flag is set, returns 1, otherwise 0.
 */
int is_end_of_game(struct Game_struct* game) {
    return get_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * if the game_started flag is set, returns 1, otherwise 0.
 */
int is_game_running(struct Game_struct* game) {
    return get_game_flag(&(game->flags), GAME_STARTED_FLAG);
}

/*
 * If the player is 0 or 1, sets the winner variable and
 * the winner flag.
 */
void set_winner(struct Game_struct* game, int player) {
    if(player == 0 || player == 1) {
        set_game_flag(&(game->flags), WINNER_FLAG);
        game->winner = player;
    } else {
        unset_game_flag(&(game->flags), WINNER_FLAG);
    }
}

/*
 * If the winner flag is set, returns the value of winner variable.
 * Otherwise returns -1.
 */
int get_winner(struct Game_struct* game) {
    int is_winner = get_game_flag(&(game->flags), WINNER_FLAG);
    if(is_winner == 1) {
        return game->winner;
    } else {
        return -1;
    }
}

/*
 * Checks the new turn words against the old turn words, and returns 1 if the turn was valid.
 *
 * turn indicates which player has moved his pawns. 0 = 1st player, 1 = 2nd player.
 */
int validate_turn(char* p1_old_tw, char* p2_old_tw, char* p1_new_tw, char* p2_new_tw, int turn) {
    int i;

    // check the field numbers
    for (i = 0; i < TURN_WORD_LENGTH; i++) {
        if(!(p1_new_tw[i] >=1 && p1_new_tw[i] <= 31) ) {
            return 0;
        }

        if(!(p2_new_tw[i] >=1 && p2_new_tw[i] <= 31) ) {
            return 0;
        }
    }
    return 1;
}

/*
 * Checks the winning conditions and returns:
 * -1: no winner
 *  0: player 1 wins
 *  1: player 2 wins
 */
int check_winning_conditions(char* p1_turn_word, char* p2_turn_word) {
    int win = 0;
    int i = 0;
    int j = 0;

    // check the first player
    for (i = 0; i < TURN_WORD_LENGTH; ++i) {
        if(p1_turn_word[i] != 31) {
            win = 1;
            break;
        }
    }

    // check the second player
    if (win == 1) {
        for (j = 0; j < TURN_WORD_LENGTH; ++j) {
            if(p2_turn_word[j] != 31) {
                win = -1;
                break;
            }
        }
    }

    return win;
}

/*
 * Prints game flags to buffer.
 */
void print_flags(char *buffer, struct Game_struct *game) {
    int gef = get_game_flag(&(game->flags), GAME_ENDED_FLAG);
    int gsf = get_game_flag(&(game->flags), GAME_STARTED_FLAG);
    int w1f = get_game_flag(&(game->flags), WAITING_P1_FLAG);
    int w2f = get_game_flag(&(game->flags), WAITING_P2_FLAG);
    int tf = get_game_flag(&(game->flags), TURN_FLAG);

    sprintf(buffer, "Flags: game_ended: %d, game_started: %d, waiting_for_p1: %d, waiting_for_p2: %d, turn: %d\n",
                    gef,gsf,w1f,w2f,tf);
}

/*
 * Returns the id of th player.
 * Players is obtained by comparing ports and addresses.
 * If the player is not found, -1 is returned.
 */
int get_pid_player(struct Game_struct* game, struct Player player) {
    struct  Player p1, p2;

    p1 = game->players[0];
    p2 = game->players[1];

    if(p1.addr == player.addr && p1.port == player.port) {
        return p1.id;
    } else if (p1.addr == player.addr && p1.port == player.port) {
        return p2.id;
    } else {
        return -1;
    }
}

/*
 * Goes through the array of games and if a game is waiting for player with ip/port
 * returns a pointer to that game. Otherwise returns NULL.
 */
struct Game_struct* is_game_waiting_for(struct Game_struct* games, int game_num, int port, uint32_t addr) {
    int i;
    struct Player p1;
    struct Player p2;
    int waiting;

    for(i = 0; i < game_num; i++) {
        waiting = is_game_waiting(&games[i]);
        p1 = games[i].players[0];
        p2 = games[i].players[1];
        if(waiting == 1 && p1.addr == addr && p1.port == port) {
            return &(games[i]);
        } else if (waiting == 1 && p2.addr == addr && p2.port == port) {
            return &(games[i]);
        }
    }

    return NULL;
}

