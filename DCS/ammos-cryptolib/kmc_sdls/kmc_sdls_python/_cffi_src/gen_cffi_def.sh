#!/bin/bash

filtered_dir="./filtered_headers"

# Filter headers
echo "Generating filtered headers in directory $filtered_dir"
mkdir -p "$filtered_dir"
for file in ../../../CryptoLib/include/*.h; do
  basename=$(basename "$file")
  grep -v '#include *<[a-z].*\.h>' "$file" > "$filtered_dir/$basename"
done

gcc_cmd="gcc -E -UMAC_SIZE -DMAC_SIZE=64 -I$filtered_dir ../../public_inc/kmc_sdls.h | egrep -v \"^#\" | sed -e '/^$/d' -e '/^}.*;$/G' -e '/);$/G' -e 's/ __attribute__((packed))//g' > $filtered_dir/gen_cffi_definitions.i"

echo "Generating CFFI definition file: $gcc_cmd"
eval "$gcc_cmd"