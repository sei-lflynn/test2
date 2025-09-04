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

find_package(Java REQUIRED)
include(UseJava)

enable_testing()

#project (KmcSdlsJavaTest NONE)

add_jar(KmcSdlsJavaTest
        SOURCES KmcSdlsJavaTest.java
        INCLUDE_JARS junit-4.13.2.jar hamcrest-2.2.jar KmcSdlsJNI)

get_target_property(_jarFile KmcSdlsJavaTest JAR_FILE)
get_target_property(_classDir KmcSdlsJavaTest CLASSDIR)

message(STATUS "Jar file ${_jarFile}")
message(STATUS "Class compiled to ${_classDir}")
message(STATUS "PROJECT BINARY DIR: ${PROJECT_BINARY_DIR}")

add_test(NAME TestKmcSdlsJavaTest COMMAND ${Java_JAVA_EXECUTABLE} -Djava.library.path=${PROJECT_BINARY_DIR}/lib -cp ${_jarFile}:${CMAKE_CURRENT_SOURCE_DIR}/junit-4.13.2.jar:${CMAKE_CURRENT_SOURCE_DIR}/hamcrest-2.2.jar:${PROJECT_BINARY_DIR}/lib/KmcSdlsJNI.jar org.junit.runner.JUnitCore KmcSdlsJavaTest)
