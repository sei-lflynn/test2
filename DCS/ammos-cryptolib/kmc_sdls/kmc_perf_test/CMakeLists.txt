include_directories(include)
include_directories(../crypto/public_inc)
include_directories(../crypto/util/include)
include_directories(../kmc_sdls/public_inc)
include_directories(../kmc_sdls/include/)

aux_source_directory(src UTIL_SRC_FILES)
aux_source_directory(app APP_SRC_FILES)

file( GLOB SOURCE_FILES src/*.c )

SET(CMAKE_SKIP_BUILD_RPATH FALSE)
SET(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
SET(CMAKE_INSTALL_RPATH $ORIGIN/../../lib:$ENV{LIBPATH})

add_executable(performance_test src/performance_test.c)

target_sources(performance_test PRIVATE src/performance_test.c src/shared_util.c)
target_link_libraries(performance_test LINK_PUBLIC kmc_sdls)

add_custom_command(TARGET performance_test POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:performance_test> ${PROJECT_BINARY_DIR}/bin/performance_test
    COMMENT "Created ${PROJECT_BINARY_DIR}/bin/performance_test"
    )

target_include_directories (crypto PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

install(TARGETS performance_test
    DESTINATION ${CMAKE_INSTALL_PREFIX}/test/bin)

