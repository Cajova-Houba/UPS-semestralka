#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "common/common.h"

/****************************************************************************/

int main(int argc, char **argv)
{
  int sock;
  char *hostname;
  struct hostent *hostinfo;
  struct sockaddr_in addr;
  
  char *line;

  if (argc < 2)
  {
    printf("usage: cli hostaddr\n");
    exit(1);
  }

  hostname = argv[1];

  /* create socket */
  sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
  if (sock < 0) printf("socket()");

  /* get IP */
  hostinfo = gethostbyname(hostname);
  if (hostinfo == NULL) printf("gethostbyname");

  /* prepare inet address */
  memset(&addr, 0, sizeof(addr));
  addr.sin_family = AF_INET;
  addr.sin_port = htons(SRV_PORT);
  addr.sin_addr = *(struct in_addr *) hostinfo->h_addr;

  /* make TCP connection */
  printf("connecting to %s:%i\n", inet_ntoa(addr.sin_addr), SRV_PORT);
  if (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) printf("connect");

  printf("Type string to send...\n");
  while(1) {
	  line = (char*)malloc(sizeof(char)*255);
	  scanf("%s",line);
	  line[254] = '\0';
	  
	  if (!send_txt(sock, line))
	  {
		close(sock);
		printf("Error sending %s.\n",line);
		free(line);
		return 1;
	  }
	  
	  free(line);
  }

  return 0;
}

