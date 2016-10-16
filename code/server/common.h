#ifndef __COMMON_H
#define __COMMON_H

#define SRV_PORT		65000
#define MAX_TXT_LENGTH		50
#define MAX_PLAYERS		2    

/*void err(char *msg);*/
int send_txt(int sock, char *txt);
int recv_txt(int sock);
void send_greetings(int sock);
int recv_txt_buffer(int sock, char *buffer);

#endif
