#include "nick_val.h"

/*
 * Checks, if the length is > 0 and <= MAX_NICK_LENGTH.
 * If the length is 0, 1 will be returned.
 * If the length is > MAX_LENGTH, 2 will be returned.
 * If the length is ok, 0 will be returned.
 */
int check_lenght(char *nickname){
	int length = 0;
	
	length = strlen(nickname);
	
	if (length == 0) {
		return 1;
	}
	
	if (length > MAX_NICK_LENGTH) {
		return 2;
	}
	
	return 0;
}

/*
 * Checks, if the nickname consist only of allowed characters
 * Those are: A-Za-z and 0-9 on position > 0.
 * 
 * If the nickname is ok, 0 will be returned.
 * If the first character is invalid, 1 will be returned.
 * If any onther character is invalid, 2 will be returned.
 */
int check_characters(char* nickname) {
	/*
	 * 0 - 9 = 48 - 57
	 * A - Z = 65 - 90
	 * a - z = 97 - 122
	 */ 
	
	int length = 0,
	    i = 0; 
	
	length = strlen(nickname);
	
	/* check the first char */
	if (nickname[0] < 65 || 
	    (nickname[0] > 90 && nickname[0] < 97) || 
	    nickname[0] > 122) {
			
		/* first char is invalid */
		return 1;
	}
	
	for (i = 1; i < length; i++) {
		if (nickname[i] < 48 ||
			(nickname[i] > 57 && nickname[i] < 65) || 
			(nickname[i] > 90 && nickname[i] < 97) || 
			nickname[i] > 122) {
			
			return 2;
		}
	}
	
	return 0;
}

/*
 * Checks if the nickname already exist.
 * If the nickname doesn't exist yet, 
 * 0 will be returned.
 * 
 * It is expected that length and character has already been checked.
 */
int check_duplicity(char* nickname) {
	
	/*
	 * TODO
	 */
	
	return 0;
}

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
int check_nickname(char* nickname, char *errmsg) {
	
	int check_res = 0;
	
	check_res = check_lenght(nickname);
	switch(check_res) {
		case 1:
		case 2: return ERR_NICKNAME;
	}
	
	check_res = check_characters(nickname);
	switch(check_res) {
		case 1:
		case 2: return ERR_NICKNAME;
	}
	
	check_res = check_duplicity(nickname);
	switch(check_res) {
		case 1: return ERR_NICK_EXISTS;
	}
	
	return NICK_OK;
}


