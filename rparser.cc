#include "rparser.h"
#include "list.h"
#include "common.h"
#include "token.h"
#include "set.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>


// class RParser

RParser::RParser(Reader *r) 
{
  reader=r;
}


// returns the name of the relation whose range is defined
char* RParser::parserelation()
{
  Token token=reader->peakahead();
  while(token.token_type==TOKEN_EOL) 
    {
      skiptoken();
      token=reader->peakahead();
    }
  
  if (token.token_type==TOKEN_EOF)
    return NULL;
  
  needtoken(0); // we need a normal string
  needtoken(TOKEN_COLON); // we need a colon

  return token.str;
}



WorkSet* RParser::parseworkset()
{
#ifdef DEBUGMESSAGES
  printf("Parsing a new workset... \n");
#endif

  WorkSet *wset = new WorkSet();
  needtoken(TOKEN_OPENBRACE);  // need an open brace
  
  Token token = reader->peakahead();
  while (token.token_type != TOKEN_CLOSEBRACE)
    {
#ifdef DEBUGMESSAGES
      printf("%s ", token.str);
#endif
      wset->addobject(token.str);
      token = reader->peakahead();
    }

  return wset;
}



void RParser::error() 
{
  printf("ERROR\n");
  reader->error();
  exit(-1);
}


void RParser::skiptoken() 
{
  reader->readnext();
}


void RParser::needtoken(int token) 
{
  Token t=reader->readnext();
  if (!(t.token_type==token)) 
    {
      printf("Needed token: ");
      tokenname(token);
      printf("\n Got token: %s ",t.str);
      tokenname(t.token_type);
      error();
    }
}