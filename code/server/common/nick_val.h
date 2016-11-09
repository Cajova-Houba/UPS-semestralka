#ifndef __NICK_VAL_H
#define __NICK_VAL_H

#include "common.h"

#define MIN_NICK_LENGTH			3
#define MAX_NICK_LENGTH			8

/* check nick results */
#define NICK_OK					0
#define NICK_TOO_SHORT			1
#define NICK_TOO_LONG			2
#define FIRST_CHAR_INV			3
#define CONTAINS_INV_CHAR		4
#define NICK_ALREADY_EXISTS		5

/*
 * Checks, if the length is > 0 and <= MAX_NICK_LENGTH.
 * If the length < MIN_NICK_LENGTH, 1 will be returned.
 * If the length is > MAX_LENGTH, 2 will be returned.
 * If the length is ok, 0 will be returned.
 * 
 * It is expected that the nickname is '\0' terminated.
 */
int check_lenght(char *nickname);

/*
 * Checks, if the nickanme consist only of allowe characters
 * Those are: A-Za-z and 0-9 on position > 0.
 * 
 * It is expected that the length has been already checked.
 * 
 * If the nickname is ok, 0 will be returned.
 * If the first character is invalid, 1 will be returned.
 * If any onther character is invalid, 2 will be returned.
 */
int check_characters(char* nickname);

/*
 * Checks if the nickname already exist.
 * If the nickname doesn't exist yet, 
 * 0 will be returned.
 * 
 * It is expected that length and character has already been checked.
 * It is expected that the length of players is MAX_PLAYERS.
 */
int check_duplicity(char* nickname, Player *players);

/*
 * Checks the nickname for length, allowed chars and duplicity.
 * It is possible to specify buffer for error message.
 * It is expected that the nick is '\0' terminated.
 * Return values:
 * 		NICK_OK - nick ok
 * 		NICK_TOO_SHORT - nick length is < MIN_NICK_LENGTH
 * 		NICK_TOO_LONG - nick length is > MAX_NICK_LENGTH
 * 		FIRT_CHAR_INV - nick starts with invalid char
 * 		CONTAINS_INV_CHAR - nick contains invalid chars
 * 		NICK_ALREADY_EXISTS - nick already exists
 */ 
int check_nickname(char* nickname, char *errmsg);

#endif
