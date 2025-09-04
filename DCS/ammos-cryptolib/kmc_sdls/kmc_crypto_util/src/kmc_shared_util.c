/*
 * Copyright 2021, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */

//temp debug, remove later.
#include <string.h>
#include <stdlib.h>

#include "kmc_shared_util.h"


/*
* Function:  c_read_file
* --------------------
* Reads a file from disk into a char * buffer.
*
*
*  const char* f_name: file name & path to be read
*  long* f_size:
*
*  returns: a malloc'd char* containing the contents of the buffer.
*      Note that this buffer is NOT null terminated and must be free()'d.
*/
char * c_read_file(const char * f_name, long * f_size) {
    char* buffer=0;
    long length;
    FILE* f = fopen(f_name,"rb");
    if (f){
        fseek (f, 0, SEEK_END);
        length = ftell (f);
        fseek (f, 0, SEEK_SET);
        buffer = malloc (length);
        if (buffer) {
            fread (buffer, 1, length, f);
        }
        fclose (f);
    }
    if (buffer){
        *f_size = length;
        debug_printf("Buffer Length:%lu\n",length);
        return buffer;
    } else{
        return NULL;
    }

}

#ifdef DEBUG
void debug_printf(const char *format, ...)
{
    va_list args;
    fprintf(stderr, "DEBUG - ");
    va_start(args, format);
    vfprintf(stderr,format, args);
    va_end(args);
}
#else
void debug_printf(const char* format, ...) {
    //Do nothing, DEBUG preprocessor disabled.
}
#endif

#ifdef DEBUG
void debug_hexprintf(const char *bin_data, int size_bin_data)
{
    //https://stackoverflow.com/questions/6357031/how-do-you-convert-a-byte-array-to-a-hexadecimal-string-in-c
    //https://stackoverflow.com/questions/5040920/converting-from-signed-char-to-unsigned-char-and-back-again
    unsigned char* u_bin_data = (unsigned char*)bin_data;
    unsigned char output[(size_bin_data*2)+1];
    char *ptr = &output[0];
    int i;
    for(i=0; i < size_bin_data; i++){
        ptr += sprintf(ptr,"%02X",u_bin_data[i]);
    }
    debug_printf("%s\n",output);
}
#else
void debug_hexprintf(const char* bin_data, int size_bin_data) {
    //Do nothing, DEBUG preprocessor disabled.
}
#endif
