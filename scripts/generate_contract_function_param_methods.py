#!/usr/bin/env python3

int_versions = []
uint_versions = []

int_array_versions = []
uint_array_versions = []

# does not generate 8 bit versions because those require some special treatment.

def add_with_param_type(bit_width, param_type, map_method_name, exception_comment = ""):
    int_versions.append(
        "/*\n"
        "* Add a " + str(bit_width) + "-bit integer.\n"
        "*\n"
        "* @param value The integer to be added\n"
        "* @return {@code this}\n"
        "*/\n"
        "public ContractFunctionParameters addInt" + str(bit_width) + "(" + param_type + " value) {\n"
        "    args.add(new Argument(\"int" + str(bit_width) + "\", int256(value, " + str(bit_width) + "), false));\n"
        "\n"
        "    return this;\n"
        "}\n"
    )
    uint_versions.append(
        "/*\n"
        "* Add a " + str(bit_width) + "-bit unsigned integer.\n"
        "\n"
        "* The value will be treated as unsigned during encoding (it will be zero-padded instead of\n"
        "* sign-extended to 32 bytes).\n"
        "*\n"
        "* @param value The integer to be added\n"
        "* @return {@code this}\n" +
        exception_comment +
        "*/\n"
        "public ContractFunctionParameters addUint" + str(bit_width) + "(" + param_type + " value) {\n"
        "    args.add(new Argument(\"uint" + str(bit_width) + "\", uint256(value, " + str(bit_width) + "), false));\n"
        "\n"
        "    return this;\n"
        "}\n"
    )
    int_array_versions.append(
        "/**\n"
        "* Add a dynamic array of " + str(bit_width) + "-bit integers.\n"
        "*\n"
        "* @param intArray The array of integers to be added\n"
        "* @return {@code this}\n"
        "*/\n"
        "public ContractFunctionParameters addInt" + str(bit_width) + "Array(" + param_type + "[] intArray) {\n"
        "    ByteString arrayBytes = ByteString.copyFrom(\n"
        "        J8Arrays.stream(intArray)." + map_method_name + "(i -> int256(i, " + str(bit_width) + "))\n"
        "        .collect(Collectors.toList()));\n"
        "\n"
        "    arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);\n"
        "\n"
        "    args.add(new Argument(\"int" + str(bit_width) + "[]\", arrayBytes, true));\n"
        "\n"
        "    return this;\n"
        "}\n"
    )
    uint_array_versions.append(
        "/**\n"
        "* Add a dynamic array of " + str(bit_width) + "-bit unsigned integers.\n"
        "\n"
        "* The value will be treated as unsigned during encoding (it will be zero-padded instead of\n"
        "* sign-extended to 32 bytes).\n"
        "*\n"
        "* @param intArray The array of integers to be added\n"
        "* @return {@code this}\n" +
        exception_comment +
        "*/\n"
        "public ContractFunctionParameters addUint" + str(bit_width) + "Array(" + param_type + "[] intArray) {\n"
        "    ByteString arrayBytes = ByteString.copyFrom(\n"
        "        J8Arrays.stream(intArray)." + map_method_name + "(i -> uint256(i, " + str(bit_width) + "))\n"
        "        .collect(Collectors.toList()));\n"
        "\n"
        "    arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);\n"
        "\n"
        "    args.add(new Argument(\"uint" + str(bit_width) + "[]\", arrayBytes, true));\n"
        "\n"
        "    return this;\n"
        "}\n"
    )


for bit_width in range(16, 257, 8):
    if bit_width <= 32:
        add_with_param_type(bit_width, "int", "mapToObj")
    elif bit_width <= 64:
        add_with_param_type(bit_width, "long", "mapToObj")
    else:
        add_with_param_type(bit_width, "BigInteger", "map", "* @throws IllegalArgumentException if {@code bigInt.signum() < 0}.\n")

f = open("output.txt", "w")

f.write("// XXXXXXXXXXXXXXXXXXXX INT VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")

for v in int_versions:
    f.write(v + "\n");

f.write("// XXXXXXXXXXXXXXXXXXXX INT ARRAY VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")

for v in int_array_versions:
    f.write(v + "\n");

f.write("// XXXXXXXXXXXXXXXXXXXX UINT VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")

for v in uint_versions:
    f.write(v + "\n");

f.write("// XXXXXXXXXXXXXXXXXXXX UINT ARRAY VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")

for v in uint_array_versions:
    f.write(v + "\n");

f.close()
