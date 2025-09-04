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


/*
 *  Simple apply security program that reads a file into memory and calls the Crypto_TC_ApplySecurity function on the data.
 */
#include <stdlib.h>

#include <crypto_error.h>
#include "kmc_crypto_sequence.h"

int main(int argc, char *argv[]) {
    char *buffer;
    char const *filename;
    long buffer_size;
    char *security_type;

    if (argc < 3 || argc % 2 == 0) {
        fprintf(stderr,"Command line usage: \n"\
               "\t%s [<tc|tm|aos> <filename>]+\n"\
               "specify as many [<tc_a|tm_a|aos_a|tc_p|tm_p|aos_p> <filename>] pairs as necessary to complete your crypto sequence test. Each file will be loaded and processed in sequence. \n"\
               "<tc_a|tm_a|aos_a> : Apply TeleCommand (tc_a) | Telemetry (tm_a) | Advanced Orbiting Systems (aos_a) Security T\n"\
               "<tc_p|tm_p|aos_p> : Process TeleCommand (tc_p) | Telemetry (tm_p) | Advanced Orbiting Systems (aos_p) Security T\n"\
               "<filename> : binary file with telecommand transfer frame bits\n",argv[0]);

        return CRYPTO_LIB_ERROR;
    }
    //Setup & Initialize CryptoLib
    sdls_init_unit_test();

    int arg_index = 0;
    uint8_t * ptr_enc_frame = NULL;
    uint16_t enc_frame_len;

    while(arg_index != argc-1){
        security_type = argv[++arg_index];
        debug_printf("Security Type: %s\n",security_type);
        filename = argv[++arg_index];
        debug_printf("Filename: %s\n",filename);
        buffer = c_read_file(filename,&buffer_size);
        debug_printf("File buffer size:%lu\n",buffer_size);
        int buffer_size_i = (int) buffer_size;
        debug_printf("File buffer size int:%d\n",buffer_size_i);
        debug_printf("File content: \n");
        debug_hexprintf(buffer,buffer_size_i);

        //Call Apply/ProcessSecurity on buffer contents depending on type.
        if (strcmp(security_type,"tc_a")==0){
            apply_security_tc(buffer, buffer_size_i,&ptr_enc_frame,&enc_frame_len);
        } else if (strcmp(security_type,"tm_a")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"aos_a")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"tc_p")==0){
            TC_t* tc_sdls_processed_frame = malloc(sizeof(TC_t));
            process_security_tc(buffer, &buffer_size_i,tc_sdls_processed_frame);
        } else if (strcmp(security_type,"tm_p")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"aos_p")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        }
        free(buffer);
    }
}