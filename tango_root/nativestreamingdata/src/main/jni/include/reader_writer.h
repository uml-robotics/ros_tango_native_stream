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
#ifndef _READER_WRITER_H_INCLUDE
#define _READER_WRITER_H_INCLUDE 
#include <pthread.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  The following implements a problem 2 reader-writer mutex
 *  as seen here
 *    http://cs.nyu.edu/~lerner/spring10/MCP-S10-Read04-ReadersWriters.pdf
 *
 *  The purpose of a problem 2 reader-writer mutex is to
 *  allow any number of readers through as long as there is
 *  not a writer actively trying to write.  Furthermore
 *  the writer will only have to wait a minimal amount of
 *  time before the readers yield the lock for it to write
 *  as to not starve the writer.  Note in this implementation
 *  it is still possible for the readers to experience
 *  indefinite postponement if there is a consistent stream
 *  of writers trying to write.
 *
 *  @author Eric Marcoux <emarcoux@cs.uml.edu>
 *  @todo add error handling
 */

// opaque handle to the mutex
typedef struct _reader_writer_mutex_t* reader_writer_mutex_t;

// all functions return -1 on failure like a system call (returns similar to pthread)

// Grabs a lock on the mutex for reading.  There can be more than 1 reader at a time
// but only ever one writer.  Also a reader can't be reading while a writer is writing.
// Though it is possible DO NOT WRITE WITH THE READ LOCK or the sun will implode
// and it will be your fault.
int grab_rd_lock(reader_writer_mutex_t mutex);
// Marks the end of the protected segment for the reader
int release_rd_lock(reader_writer_mutex_t mutex);


// Grabs the lock on the mutex for writing.  There can only ever be one writer at a time.
int grab_wr_lock(reader_writer_mutex_t mutex);
// Marks the end of the protected segment for the writer
int release_wr_lock(reader_writer_mutex_t mutex);


reader_writer_mutex_t reader_writer_mutex_create();
int reader_writer_mutex_destroy(reader_writer_mutex_t *mutex);

#ifdef __cplusplus
}
#endif

#endif
