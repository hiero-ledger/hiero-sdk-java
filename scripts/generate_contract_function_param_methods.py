
#!/usr/bin/env python3

int_versions = []
uint_versions = []
int_array_versions = []
uint_array_versions = []

# Does not generate 8 bit versions because those require some special treatment.
def add_with_param_type(bit_width, param_type, map_method_name, exception_comment=""):
    int_versions.append(f"""/**
* Add a {bit_width}-bit integer.
*
* @param value The integer to be added
* @return {{@code this}}
*/
public ContractFunctionParameters addInt{bit_width}({param_type} value) {{
    args.add(new Argument(\"int{bit_width}\", int256(value, {bit_width}), false));
    return this;
}}
""")
    uint_versions.append(f"""/**
* Add a {bit_width}-bit unsigned integer.
*
* The value will be treated as unsigned during encoding (it will be zero-padded instead of
* sign-extended to 32 bytes).
*
* @param value The integer to be added
* @return {{@code this}}
{exception_comment}*/
public ContractFunctionParameters addUint{bit_width}({param_type} value) {{
    args.add(new Argument(\"uint{bit_width}\", uint256(value, {bit_width}), false));
    return this;
}}
""")
    int_array_versions.append(f"""/**
* Add a dynamic array of {bit_width}-bit integers.
*
* @param intArray The array of integers to be added
* @return {{@code this}}
*/
public ContractFunctionParameters addInt{bit_width}Array({param_type}[] intArray) {{
    ByteString arrayBytes = ByteString.copyFrom(
        J8Arrays.stream(intArray).{map_method_name}(i -> int256(i, {bit_width}))
        .collect(Collectors.toList()));
    arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);
    args.add(new Argument(\"int{bit_width}[]\", arrayBytes, true));
    return this;
}}
""")
    uint_array_versions.append(f"""/**
* Add a dynamic array of {bit_width}-bit unsigned integers.
*
* The value will be treated as unsigned during encoding (it will be zero-padded instead of
* sign-extended to 32 bytes).
*
* @param intArray The array of integers to be added
* @return {{@code this}}
{exception_comment}*/
public ContractFunctionParameters addUint{bit_width}Array({param_type}[] intArray) {{
    ByteString arrayBytes = ByteString.copyFrom(
        J8Arrays.stream(intArray).{map_method_name}(i -> uint256(i, {bit_width}))
        .collect(Collectors.toList()));
    arrayBytes = uint256(intArray.length, 32).concat(arrayBytes);
    args.add(new Argument(\"uint{bit_width}[]\", arrayBytes, true));
    return this;
}}
""")

for bit_width in range(16, 257, 8):
    if bit_width <= 32:
        add_with_param_type(bit_width, "int", "mapToObj")
    elif bit_width <= 64:
        add_with_param_type(bit_width, "long", "mapToObj")
    else:
        add_with_param_type(bit_width, "BigInteger", "map", "* @throws IllegalArgumentException if {@code bigInt.signum() < 0}.\n")

with open("output.txt", "w") as f:
    f.write("// XXXXXXXXXXXXXXXXXXXX INT VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")
    for v in int_versions:
        f.write(v + "\n")
    f.write("// XXXXXXXXXXXXXXXXXXXX UINT VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")
    for v in uint_versions:
        f.write(v + "\n")
    f.write("// XXXXXXXXXXXXXXXXXXXX INT ARRAY VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")
    for v in int_array_versions:
        f.write(v + "\n")
    f.write("// XXXXXXXXXXXXXXXXXXXX UINT ARRAY VERSIONS XXXXXXXXXXXXXXXXXXXX\n\n")
    for v in uint_array_versions:
        f.write(v + "\n")
