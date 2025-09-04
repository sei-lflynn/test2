#Copyright 2021, by the California Institute of Technology.
#ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
#Any commercial use must be negotiated with the Office of Technology
#Transfer at the California Institute of Technology.
#
#This software may be subject to U.S. export control laws. By accepting
#this software, the user agrees to comply with all applicable U.S.
#export laws and regulations. User has the responsibility to obtain
#export licenses, or other export authority as may be required before
#exporting such information to foreign countries or providing access to
#foreign persons.

install(PROGRAMS ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls_test_app.py
        DESTINATION ${CMAKE_INSTALL_PREFIX}/test/)

install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls_test_app.properties
        DESTINATION ${CMAKE_INSTALL_PREFIX}/test/)