/*
 * Copyright (c) 2014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Massachusetts Lowell nor the names
 * from of its contributors may be used to endorse or promote products
 * derived this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * Author: Eric Marcoux <emarcoux@cs.uml.edu>
*/
#include "reader_writer.h"

struct _reader_writer_mutex_t {
  int rdcnt;
  int wrcnt;
  pthread_mutex_t rdcnt_lock;
  pthread_mutex_t wrcnt_lock;
  pthread_mutex_t rd_lock;
  pthread_mutex_t wr_lock;
  pthread_mutex_t singlefile_rd_lock;
};


reader_writer_mutex_t reader_writer_mutex_create()
{
  reader_writer_mutex_t mutex = (reader_writer_mutex_t)malloc(sizeof(struct _reader_writer_mutex_t));

  mutex->rdcnt = 0;
  mutex->wrcnt = 0;

  pthread_mutex_init(&mutex->rdcnt_lock, NULL);
  pthread_mutex_init(&mutex->wrcnt_lock, NULL);
  pthread_mutex_init(&mutex->rd_lock, NULL);
  pthread_mutex_init(&mutex->wr_lock, NULL);
  pthread_mutex_init(&mutex->singlefile_rd_lock, NULL);

  return mutex;
}

int reader_writer_mutex_destroy(reader_writer_mutex_t *mutex)
{
  pthread_mutex_destroy(&(*mutex)->rdcnt_lock);
  pthread_mutex_destroy(&(*mutex)->wrcnt_lock);
  pthread_mutex_destroy(&(*mutex)->rd_lock);
  pthread_mutex_destroy(&(*mutex)->wr_lock);
  pthread_mutex_destroy(&(*mutex)->singlefile_rd_lock);

  free(*mutex);
  *mutex = NULL;

  return 0;
}


int grab_rd_lock(reader_writer_mutex_t mutex)
{
  pthread_mutex_lock(&mutex->singlefile_rd_lock);
    pthread_mutex_lock(&mutex->rd_lock);
      pthread_mutex_lock(&mutex->rdcnt_lock);
        ++mutex->rdcnt;
        if(mutex->rdcnt == 1) pthread_mutex_lock(&mutex->wr_lock);
      pthread_mutex_unlock(&mutex->rdcnt_lock);
    pthread_mutex_unlock(&mutex->rd_lock);
  pthread_mutex_unlock(&mutex->singlefile_rd_lock);

  return 0;
}

int release_rd_lock(reader_writer_mutex_t mutex)
{
  pthread_mutex_lock(&mutex->rdcnt_lock);
    --mutex->rdcnt;
    if(mutex->rdcnt == 0) pthread_mutex_unlock(&mutex->wr_lock);
  pthread_mutex_unlock(&mutex->rdcnt_lock);

  return 0;
}

int grab_wr_lock(reader_writer_mutex_t mutex)
{
  pthread_mutex_lock(&mutex->wrcnt_lock);
    ++mutex->wrcnt;
    if(mutex->wrcnt == 1) pthread_mutex_lock(&mutex->rd_lock);
  pthread_mutex_unlock(&mutex->wrcnt_lock);
  pthread_mutex_lock(&mutex->wr_lock);

  return 0;
}

int release_wr_lock(reader_writer_mutex_t mutex)
{
  pthread_mutex_unlock(&mutex->wr_lock);
  pthread_mutex_lock(&mutex->wrcnt_lock);
    --mutex->wrcnt;
    if(mutex->wrcnt == 0) pthread_mutex_unlock(&mutex->rd_lock);
  pthread_mutex_unlock(&mutex->wrcnt_lock);

  return 0;
}
