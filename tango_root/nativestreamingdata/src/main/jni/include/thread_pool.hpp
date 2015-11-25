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
#ifndef THREAD_POOL_H_INCLUDE_
#define THREAD_POOL_H_INCLUDE_ 
#include <thread>
#include <functional>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
extern "C"{ 
#include "liblfds611.h"
}
#include "debugging_tools.h"
typedef struct lfds611_queue_state* tp_queue_ptr;

template<class T>
struct ThreadPoolContext {
    tp_queue_ptr free_list;
    tp_queue_ptr work_items;

    uint32_t num_threads, num_buffer_elements;

    std::function<void(void**)>   on_start;               // pointer to a pointer to any start data
    std::function<void(void*,T*)> on_notify;
    std::function<void(void*)>    on_shutdown;

    bool threads_should_die;
    pthread_mutex_t *mutexes;
    pthread_cond_t  *conds;
};


template <class T>
struct th_func_internal_param{
  uint32_t i;
  ThreadPoolContext<T> *ctxt;
};


template<class T>
class ThreadPool
{
public:
    ThreadPool(uint32_t num_buffer_elements, uint32_t num_threads, std::function<void(void*,T*)> on_notify)
        : ThreadPool(num_buffer_elements, num_threads, std::function<void(void**)>(), on_notify, std::function<void(void*)>()) {}
    ThreadPool(uint32_t num_buffer_elements, uint32_t num_threads, 
                std::function<void(void**)>            on_start,
                std::function<void(void*,T*)>          on_notify,
                std::function<void(void*)>             on_shutdown);
    virtual ~ThreadPool();

    // for work items you would like to enqueue but are not absolutely necessary
    bool       grabObjectNonBlocking(T** object);
    void       returnObjectToPool(T* buffer);


    void       notifyWithData(T* object);
private:
    static void *th_func_internal(void *ctxt);
    static void queue_element_destructor(void* userdata, void* userstate);
    inline void signal_all() { 
      for(uint32_t i = 0; i < context.num_threads; ++i) 
        pthread_cond_signal(&context.conds[i]);
    }

    ThreadPoolContext<T> context;
    pthread_t  *worker_threads;
};  


template<class T>
ThreadPool<T>::ThreadPool(uint32_t num_buffer_elements, uint32_t num_threads, 
                        std::function<void(void**)>   on_start,               // pointer to a pointer to any start data
                        std::function<void(void*,T*)> on_notify,
                        std::function<void(void*)>    on_shutdown)
{
    context.threads_should_die = false;
    lfds611_queue_new(&context.free_list, num_buffer_elements);
    lfds611_queue_new(&context.work_items, num_buffer_elements);

    context.on_start = on_start;
    context.on_notify = on_notify;
    context.on_shutdown = on_shutdown;

    context.num_threads = num_threads;

    context.conds   = new pthread_cond_t[num_threads];
    for(uint32_t i = 0; i < context.num_threads; ++i) pthread_cond_init(&context.conds[i], NULL);
    context.mutexes = new pthread_mutex_t[num_threads];
    for(uint32_t i = 0; i < context.num_threads; ++i) pthread_mutex_init(&context.mutexes[i], NULL);


    worker_threads = new pthread_t[num_threads];
    for(uint32_t i = 0; i < context.num_threads; ++i) {
        th_func_internal_param<T>* param = new th_func_internal_param<T>;
        param->ctxt = &context;
        param->i = i;

        pthread_create(worker_threads+i, NULL, th_func_internal, (void*)param);
    }
}


template<class T>
ThreadPool<T>::~ThreadPool()
{
    uint32_t i;
    context.threads_should_die = true;
    signal_all();
    for(i = 0; i < context.num_threads; ++i) {
        pthread_join(worker_threads[i], NULL);
        pthread_mutex_destroy(&context.mutexes[i]);
        pthread_cond_destroy(&context.conds[i]);
    }

    delete[] worker_threads;
    lfds611_queue_delete(context.free_list, queue_element_destructor, NULL);
    lfds611_queue_delete(context.work_items, queue_element_destructor, NULL);
}

template<class T>
void *ThreadPool<T>::th_func_internal(void *param)
{
    
    ThreadPoolContext<T>* ctxt;
    uint32_t my_idx;
    pthread_mutex_t mutex;
    pthread_mutex_init(&mutex, NULL);
    {
      struct th_func_internal_param<T>* p = (struct th_func_internal_param<T>*)param;
      ctxt = p->ctxt;
      my_idx = p->i;
      delete p;
    }

    T* work_item = 0;
    void* start_data = NULL;

    if(ctxt->on_start) ctxt->on_start(&start_data);
    while(true) {
      while(!ctxt->threads_should_die && lfds611_queue_dequeue(ctxt->work_items, (void**)&work_item) && work_item != NULL) {
        ctxt->on_notify(start_data, work_item);
        lfds611_queue_enqueue(ctxt->free_list, work_item);
        work_item = NULL;
      }
      if(ctxt->threads_should_die) break;
      pthread_cond_wait(&ctxt->conds[my_idx], &ctxt->mutexes[my_idx]);
    }
    if(ctxt->on_shutdown) ctxt->on_shutdown(start_data);

    pthread_mutex_destroy(&mutex);
    return NULL;
}

template <class T>
void ThreadPool<T>::queue_element_destructor(void* userdata, void*){
  T* data = (T*)userdata; // cast necessary so delete can call destructor
  delete data;
}

template<class T>
bool ThreadPool<T>::grabObjectNonBlocking(T** object)
{
    return lfds611_queue_dequeue(context.free_list, (void**)object);
}


template<class T>
void ThreadPool<T>::notifyWithData(T* data)
{
    lfds611_queue_enqueue(context.work_items, data);
    signal_all();
}


template<class T>
void ThreadPool<T>::returnObjectToPool(T* object)
{
    lfds611_queue_enqueue(context.free_list, object);
}

#endif
