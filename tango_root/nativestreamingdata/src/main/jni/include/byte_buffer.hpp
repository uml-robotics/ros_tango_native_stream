#ifndef BYTE_BUFFER_H_INCLUDE_
#define BYTE_BUFFER_H_INCLUDE_ 

#include <stdbool.h>
#include <stdlib.h>
#include <jni.h>

// note must be created and destroyed in the same thread
template<typename T>
class ByteBuffer
{
public:
    ByteBuffer(JNIEnv* env, size_t numElements);
    ByteBuffer(JavaVM *vm, JNIEnv* env, size_t numElements);
    ~ByteBuffer();
    inline jobject getObject() const { return object; }
    inline T*      getBuffer() const { return buffer; }
    inline size_t  getBufferLength() const { return bufferLength; }
    inline size_t  getBufferLengthBytes() const { return bufferSize; }
private:
    jobject object;
    T*      buffer;
    JNIEnv* env;
    JavaVM* vm;
    size_t bufferLength;
    size_t bufferSize;
};



template <typename T>
ByteBuffer<T>::ByteBuffer(JNIEnv *env, size_t numElements)
{
    buffer = new T[numElements];
    this->bufferLength = numElements;
    this->bufferSize = numElements*sizeof(T);
    object = env->NewGlobalRef(env->NewDirectByteBuffer(buffer, this->bufferSize));
    this->env = env;
}

template <typename T>
ByteBuffer<T>::ByteBuffer(JavaVM *vm, JNIEnv *env, size_t numElements)
{
    buffer = new T[numElements];
    this->bufferLength = numElements;
    this->bufferSize = numElements*sizeof(T);
    object = env->NewGlobalRef(env->NewDirectByteBuffer(buffer, this->bufferSize));
    this->vm = vm;
}

template <typename T>
ByteBuffer<T>::~ByteBuffer()
{
    if(vm) {
        vm->AttachCurrentThread(&env, NULL);
    }
    env->DeleteGlobalRef(object);
    delete[] buffer;
}



#endif