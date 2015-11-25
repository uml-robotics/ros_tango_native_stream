#ifndef __DEBUGGING_TOOLS_H_DEF__
#define __DEBUGGING_TOOLS_H_DEF__ 

// Conditions for the TRY_* macros to try and pass #define  EQ_ZERO(code)((code == 0))
#define EQ_ZERO(code)((code == 0))
#define NEQ_ZERO(code)((code != 0))
#define LESS_ZERO(code)((code < 0))
#define GREATER_ZERO(code)((code > 0))

#ifndef DEBUGGING_OFF
    #include <sys/time.h>
    #include <time.h>
    #define TRY_OR_PRINT(instr, SUCCESS)({\
        int error = instr; \
        if(!SUCCESS(error))  \
            LOGV("%s:%d\t"#instr" failed with error code %d\n", __FILE__, __LINE__, error); \
        })
    #define TRY_OR_DIE(instr, SUCCESS)({ \
          int error = instr; \
          if(!SUCCESS(error)) { \
            LOGV("%s:%d\t"#instr" failed with error code %d\n", __FILE__, __LINE__, error); \
            exit(error); \
          }\
        })


    // get the elapsed wall time of an instruction to the microsecond
    #define TIME_INSTR_WALL_NAMED(name, instr)({\
                                               struct timeval tv_start, tv_end; \
                                               gettimeofday(&tv_start, 0); \
                                               instr; \
                                               gettimeofday(&tv_end, 0); \
                                               LOGV(name" took %lu microseconds\n", (tv_end.tv_sec-tv_start.tv_sec)*1000000+(tv_end.tv_usec-tv_start.tv_usec)); \
                                              })
    #define TIME_INSTR_WALL(instr)({\
                                    struct timeval tv_start, tv_end; \
                                    gettimeofday(&tv_start, 0); \
                                    instr; \
                                    gettimeofday(&tv_end, 0); \
                                    LOGV(#instr" took %lu microseconds\n", (tv_end.tv_sec-tv_start.tv_sec)*1000000+(tv_end.tv_usec-tv_start.tv_usec)); \
                                   })
    // get the elapsed cpu time of an instruction (no time resolution garruntee)
    #define TIME_INSTR_CPU(instr)({\
                                    clock_t end, start = clock(); \
                                    instr; \
                                    end = clock(); \
                                    LOGV(#instr" took %lf seconds\n", (end-start)/(double)CLOCKS_PER_SEC);\
                                  })
#else
    #define TRY_OR_PRINT(intr, SUCCESS)(instr;)
    #define TRY_OR_DIE(intr, SUCCESS)(instr;)
    #define TIME_INSTR_WALL(instr)(instr;)
    #define TIME_INSTR_CPU(instr)(instr;)
    #define TIME_INSTR_WALL_NAMED(name, instr)(instr;)
#endif
#endif
