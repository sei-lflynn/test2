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

list(APPEND PYTHON_VERSIONS 3.6 3.7 3.8 3.9)

foreach(PY_VER IN LISTS PYTHON_VERSIONS)
    find_program(PYTHON_${PY_VER} python${PY_VER})
    if(PYTHON_${PY_VER})
        execute_process (
                COMMAND bash "-c" "${PYTHON_${PY_VER}} -c 'import sys;print(\"%d.%d\"%(sys.version_info[0],sys.version_info[1]))'"
                OUTPUT_STRIP_TRAILING_WHITESPACE
                OUTPUT_VARIABLE PythonVersion_${PY_VER}
        )

        add_custom_command(OUTPUT ${PROJECT_BINARY_DIR}/lib/python${PythonVersion_${PY_VER}}/site-packages/gov/nasa/jpl/ammos/kmc/sdlsclient/KmcSdlsClient.py
                              COMMAND PYTHONPATH=${PROJECT_BINARY_DIR}/lib/python${PythonVersion_${PY_VER}}/site-packages ${PYTHON_${PY_VER}} setup.py install --single-version-externally-managed --root=/ --prefix=${PROJECT_BINARY_DIR}
                              WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})

        #install(SCRIPT ${PYTHON_${PY_VER}} setup.py install --single-version-externally-managed --root=/ --prefix=${CMAKE_INSTALL_PREFIX})
        install(CODE "execute_process(COMMAND ${PYTHON_${PY_VER}} setup.py install --single-version-externally-managed --root=/ --prefix=${CMAKE_INSTALL_PREFIX}
                                      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})")

        add_custom_target(KmcSdlsClient_${PY_VER} ALL
                DEPENDS ${PROJECT_BINARY_DIR}/lib/python${PythonVersion_${PY_VER}}/site-packages/gov/nasa/jpl/ammos/kmc/sdlsclient/KmcSdlsClient.py)
    endif()
endforeach()
