#include "player.h"
#include "../common/seneterror.h"

/*
 * Returns ok if the player slot is free to use.
 */
int is_player_free(Player* player) {
    if(player->free == OK) {
        return OK;
    } else {
        return 0;
    }
}

/*
 * Initializes a new player and stores it in the player variable.
 * Generates a new set of stones for player.
 */
void initialize_player(Player* player, int id, char* nick, int socket, int second_player, __uint32_t addr, int port) {
    player->free = 0;
    player->id = id;
    strcpy(player->nick, nick);
    player->socket = socket;
    player->addr = addr;
    player->port = port;
    generate_new_stones(player, second_player);
}

void clean_player(Player* player) {
    player->nick[0] = '\0';
    player->free = OK;
}

/*
 * Generates a set of stones for new player.
 *
 * If the second_player = 0, stones will be generated from position
 * 2.
 *
 */
void generate_new_stones(Player* player, int second_player) {
    int pos = 1;
    int i = 0;
    char buffer[5];
    if(second_player) {
        pos = 2;
    }

    while(i < TURN_WORD_LENGTH) {
        sprintf(buffer,"%02d",pos);
        player->turn_word[i++] = buffer[0];
        player->turn_word[i++] = buffer[1];

        pos += 2;
    }
    player->turn_word[TURN_WORD_LENGTH] = '\0';
}

/*
 * Prints the player to the buffer.
 */
void print_player(Player *p, char *buffer, int new_line) {
    struct in_addr addr;
    addr.s_addr = p->addr;
    if(new_line) {
        sprintf(buffer,"Player: {id=%d, nick=%s, socket=%d, ip=%s, port=%d}\n",p->id,p->nick,p->socket, inet_ntoa(addr), ntohs(p->port));
    } else {
        sprintf(buffer,"Player: {id=%d, nick=%s, socket=%d, ip=%s, port=%d}",p->id,p->nick,p->socket, inet_ntoa(addr), ntohs(p->port));
    }
}

/*
 * Updates the player's stones.
 */
void update_players_stones(Player *p, char *new_stones) {
    int i;
    for (i = 0; i < TURN_WORD_LENGTH; ++i) {
        p->turn_word[i] = new_stones[i];
    }
}

