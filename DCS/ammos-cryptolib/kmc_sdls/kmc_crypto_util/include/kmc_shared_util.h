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

#ifndef AMMOS_CRYPTOLIB_KMC_SHARED_UTIL_H
#define AMMOS_CRYPTOLIB_KMC_SHARED_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h>
#include <stdarg.h>
#include <stddef.h>
#include <string.h>


char * c_read_file(const char * f_name, long * f_size);

void debug_printf(const char* format, ...);
void debug_hexprintf(const char* bin_data,int size_bin_data);


#ifdef __cplusplus
}  /* Close scope of 'extern "C"' declaration which encloses file. */
#endif

#endif //AMMOS_CRYPTOLIB_KMC_SHARED_UTIL_H
