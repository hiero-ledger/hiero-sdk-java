#!/usr/bin/env python3


import os
import subprocess
import sys
import re


if len(sys.argv) > 2:
    print(">>> Incorrect number of arguments.  Exiting.")
    exit()

if len(sys.argv) == 1:
    print(">>> Usage: `" + sys.argv[0] + " ref`")
    print(">>> Where \"ref\" is a valid branch or tag in the Services git repo")

print("\n\n")


# make sure this is the working directory
def go_to_script_dir():
    os.chdir(os.path.dirname(__file__))


go_to_script_dir()






PROTO_GIT_REMOTE = "https://github.com/hiero-ledger/hiero-consensus-node.git"
PROTO_GIT_PATH = os.path.join("hedera-protos-git")
PROTO_GIT_REF = sys.argv[1] if len(sys.argv)>1   else ""


PROTO_IN_PATH = os.path.join(PROTO_GIT_PATH, "hapi/hedera-protobuf-java-api/src/main/proto/services")
BASIC_TYPES_PATH = os.path.join(PROTO_IN_PATH, "basic_types.proto")
RESPONSE_CODE_PATH = os.path.join(PROTO_IN_PATH, "response_code.proto")
FREEZE_TYPE_PATH = os.path.join(PROTO_IN_PATH, "freeze_type.proto")


MAIN_PATH = os.path.join("..", "sdk", "src", "main")
PROTO_OUT_PATH = os.path.join(MAIN_PATH, "proto")
PROTO_MIRROR_OUT_PATH = os.path.join(PROTO_OUT_PATH, "mirror")
JAVA_OUT_PATH = os.path.join(MAIN_PATH, "java", "com", "hedera", "hashgraph", "sdk")
REQUEST_TYPE_OUT_PATH = os.path.join(JAVA_OUT_PATH, "RequestType.java")
STATUS_OUT_PATH = os.path.join(JAVA_OUT_PATH, "Status.java")
FEE_DATA_TYPE_OUT_PATH = os.path.join(JAVA_OUT_PATH, "FeeDataType.java")
FREEZE_TYPE_OUT_PATH = os.path.join(JAVA_OUT_PATH, "FreezeType.java")


PROTO_DO_NOT_REMOVE = (
)


ENUM_OVERRIDE = {
    "UTIL_PRNG": "PRNG"
}






COMMENT_REPLACEMENTS = (
    ("&", "and"),
    ("<tt>", ""),
    ("</tt>", "")
)


PROTO_REPLACEMENTS = (
    ("option java_package = \"com.hederahashgraph.api.proto.java\";",
     "option java_package = \"com.hedera.hashgraph.sdk.proto\";"),

    ("option java_package = \"com.hederahashgraph.service.proto.java\";",
     "option java_package = \"com.hedera.hashgraph.sdk.proto\";"),

    ("option java_package = \"com.hedera.mirror.api.proto\";",
     "option java_package = \"com.hedera.hashgraph.sdk.proto.mirror\";")
)

PROTO_REPLACEMENTS_IMPORTS = (
    # Match any import statement and captures just the part after the last /
    # for example, `import "state/common.proto"` -> `import "common.proto"`
    (r'import ".*\/(.*\.proto)"',
     r'import "\1"'),
)

def do_replacements(s, replacements):
    for r in replacements:
        s = s.replace(r[0], r[1])
    return s

def do_replacements_proto_imports(s, replacements):
    for r in replacements:
        # Check if the replacement should be skipped
        # Skip statements like `import "google/protobuf/wrappers.proto"`
        # to update imports ONLY referred to hedera protobufs
        if 'google' in s:
            continue
        s = re.sub(r[0], r[1], s)
    return s






def main():
    ensure_protobufs()
    print(">>> Generating RequestType.java")
    generate_RequestType()
    print(">>> Generating Status.java")
    generate_Status()
    print(">>> Generating FeeDataType.java")
    generate_FeeDataType()
    print(">>> Generating FreezeType.java")
    generate_FreezeType()
    print(">>> Clearing proto output directory")
    clear_proto_dir()
    print(">>> Generating modified proto files")
    generate_modified_protos()
    print(">>> Done")


def ensure_protobufs():
    if os.path.isdir(PROTO_GIT_PATH):
        print(">>> Detected existing protobufs")
    else:
        print(">>> No protobufs detected")
        run_command("git", "clone", PROTO_GIT_REMOTE, PROTO_GIT_PATH)
    os.chdir(PROTO_GIT_PATH)
    run_command("git", "fetch")
    checkout_ref = "v0.62.2"
    print(f">>> Checking out {checkout_ref}")
    run_command("git", "checkout", checkout_ref)
    if is_branch(checkout_ref):
        run_command("git", "pull", "--rebase")
    go_to_script_dir()

def run_command(*command):
    print(">>> Executing command `" + cmd_to_str(command) + "`")
    if subprocess.run(command).returncode != 0:
        print(">>> Return code was not 0.  Exiting.")
        exit()

def cmd_to_str(command):
    s = ""
    for c in command:
        s += (c + " ")
    return s[0:-1]

def is_branch(ref):
    result = subprocess.run(['git', 'branch', '--list', ref], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        print(f">>> Error checking for branch {ref}: {result.stderr.decode().strip()}")
        return False
    return len(result.stdout.strip()) > 0

def get_latest_tag():
    result = subprocess.run(['git', '-c', 'versionsort.suffix=-alpha', '-c', 'versionsort.suffix=-beta', '-c', 'versionsort.suffix=-rc', 'tag', '-l', '--sort=version:refname'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        print(f">>> Error getting latest tag: {result.stderr.decode().strip()}")
        return ""
    return result.stdout.decode().strip().split('\n')[-1]


def generate_RequestType():
    parse_enum_from_file(
        BASIC_TYPES_PATH,
        "HederaFunctionality",
        add_to_RequestType,
        finalize_RequestType)
    output_java_file(REQUEST_TYPE_OUT_PATH, RequestType_sections)


def generate_Status():
    parse_enum_from_file(
        RESPONSE_CODE_PATH,
        "ResponseCodeEnum",
        add_to_Status,
        finalize_Status)
    output_java_file(STATUS_OUT_PATH, Status_sections)


def generate_FeeDataType():
    parse_enum_from_file(
        BASIC_TYPES_PATH,
        "SubType",
        add_to_FeeDataType,
        finalize_FeeDataType)
    output_java_file(FEE_DATA_TYPE_OUT_PATH, FeeDataType_sections)



def generate_FreezeType():
    parse_enum_from_file(
        FREEZE_TYPE_PATH,
        "FreezeType",
        add_to_FreezeType,
        finalize_FreezeType)
    output_java_file(FREEZE_TYPE_OUT_PATH, FreezeType_sections)


def generate_TokenPauseStatus():
    parse_enum_from_file(
        BASIC_TYPES_PATH,
        "TokenPauseStatus",
        add_to_TokenPauseStatus,
        finalize_TokenPauseStatus)
    output_java_file(TOKEN_PAUSE_STATUS_OUT_PATH, TokenPauseStatus_sections)


def clear_proto_dir():
    clear_dir(PROTO_OUT_PATH)


def clear_dir(dir_path):
    for name in os.listdir(dir_path):
        if name in PROTO_DO_NOT_REMOVE:
            continue
        path = os.path.join(dir_path, name)
        if os.path.isfile(path):
            os.unlink(path)
        elif os.path.isdir(path):
            clear_dir(path)


def generate_modified_protos():
    do_generate_modified_protos(PROTO_IN_PATH, PROTO_OUT_PATH)

def do_generate_modified_protos(in_path, out_path):
    for root, dirs, files in os.walk(in_path):
        for name in files:
            # for name in os.listdir(in_path):
            in_file = open(os.path.join(root, name), "r")
            out_file = open(os.path.join(out_path, name), "w")
            file_contents_after_proto_replacements = do_replacements(in_file.read(), PROTO_REPLACEMENTS)
            file_contents_after_proto_import_replacements = do_replacements_proto_imports(file_contents_after_proto_replacements, PROTO_REPLACEMENTS_IMPORTS)
            out_file.write(file_contents_after_proto_import_replacements)
            in_file.close()
            out_file.close()



def premade(name, n):
    return open(os.path.join("premade", name + "-" + str(n) + ".txt"), "r").read()


RequestType_sections = [
    premade("RequestType", 0),
    "",
    premade("RequestType", 2),
    "",
    premade("RequestType", 4),
    "",
    premade("RequestType", 6)
]


Status_sections = [
    premade("Status", 0),
    "",
    premade("Status", 2),
    "",
    premade("Status", 4),
]


FeeDataType_sections = [
    premade("FeeDataType", 0),
    "",
    premade("FeeDataType", 2),
    "",
    premade("FeeDataType", 4),
    "",
    premade("FeeDataType", 6)
]


FreezeType_sections = [
    premade("FreezeType", 0),
    "",
    premade("FreezeType", 2),
    "",
    premade("FreezeType", 4)
]


def output_java_file(out_path, section_list):
    out_file = open(out_path, "w")
    for section in section_list:
        out_file.write(section)
    out_file.close()

def add_to_RequestType(original_name, cap_snake_name, comment_lines):
    RequestType_sections[1] += \
        generate_enum(original_name, cap_snake_name, comment_lines, "HederaFunctionality", 1)
    RequestType_sections[3] += generate_valueOf(original_name, cap_snake_name, 3)
    RequestType_sections[5] += generate_toString(original_name, cap_snake_name, 3)


def add_to_Status(original_name, cap_snake_name, comment_lines):
    Status_sections[1] += \
        generate_enum(original_name, cap_snake_name, comment_lines, "ResponseCodeEnum", 1)
    Status_sections[3] += generate_valueOf(original_name, cap_snake_name, 3)


def add_to_FeeDataType(original_name, cap_snake_name, comment_lines):
    FeeDataType_sections[1] += \
        generate_enum(original_name, cap_snake_name, comment_lines, "SubType", 1)
    FeeDataType_sections[3] += generate_valueOf(original_name, cap_snake_name, 3)
    FeeDataType_sections[5] += generate_toString(original_name, cap_snake_name, 3)


def add_to_FreezeType(original_name, cap_snake_name, comment_lines):
    FreezeType_sections[1] += \
        generate_enum(original_name, cap_snake_name, comment_lines, "com.hedera.hashgraph.sdk.proto.FreezeType", 1)
    FreezeType_sections[3] += generate_valueOf(original_name, cap_snake_name, 3)


def replace_last_enum_comma(s):
    return s[0:-3] + ";\n\n"


def finalize_RequestType():
    RequestType_sections[1] = replace_last_enum_comma(RequestType_sections[1])


def finalize_Status():
    Status_sections[1] = replace_last_enum_comma(Status_sections[1])


def finalize_FeeDataType():
    FeeDataType_sections[1] = replace_last_enum_comma(FeeDataType_sections[1])


def finalize_FreezeType():
    FreezeType_sections[1] = replace_last_enum_comma(FreezeType_sections[1])






def tabs(n):
    return " "*(4*n)


def generate_comment(comment_lines, tab_count):
    if(len(comment_lines) > 0):
        retval = tabs(tab_count) + "/**\n"
        for line in comment_lines:
            retval += tabs(tab_count) + " * " + \
                do_replacements(line, COMMENT_REPLACEMENTS) + "\n"
        return retval + tabs(tab_count) + " */\n"
    else:
        return ""


def generate_enum_line(original_name, cap_snake_name, enum_name, tab_count):
    return tabs(tab_count) + cap_snake_name + \
        "(" + enum_name + "." + original_name + "),\n\n"


def generate_enum(original_name, cap_snake_name, comment_lines, enum_name, tabs_count):
    return generate_comment(comment_lines, tabs_count) + \
        generate_enum_line(original_name, cap_snake_name, enum_name, tabs_count)


def generate_valueOf(original_name, cap_snake_name, tabs_count):
    return tabs(tabs_count) + "case " + original_name + " -> " + cap_snake_name + ";\n"


def generate_toString(original_name, cap_snake_name, tabs_count):
    return tabs(tabs_count) + "case " + cap_snake_name + " -> " + "\"" + cap_snake_name + "\";\n"



def parse_enum_from_file(in_path, enum_name, add_to_output, finalize_output):
    in_file = open(in_path, "r")
    s = in_file.read()
    in_file.close()
    enum_i = s.find("enum " + enum_name, 0)
    i = s.find("{", enum_i) + 1
    comment_lines = []
    while i >= 0:
        i = parse_enum_code(s, i, comment_lines, add_to_output)
    finalize_output()


# returns -1 when end of enum reached,
# otherwise returns the index after the code that has been parsed
def parse_enum_code(s, i, comment_lines, add_to_output):
    #print("parsing code", i, comment_lines)
    #time.sleep(1)
    equal_i = s.find("=", i)
    sl_comment_i = s.find("//", i)
    ml_comment_i = s.find("/*", i)
    line_end_i = s.find("\n", i)
    enum_end_i = s.find("}", i)
    indices = [equal_i, sl_comment_i, ml_comment_i, line_end_i, enum_end_i]
    indices = [index for index in indices if index >= i]
    next_i = min(indices)
    if next_i == equal_i:
        parse_enum_line(s, i, equal_i, sl_comment_i, line_end_i, comment_lines, add_to_output)
    elif next_i == sl_comment_i:
        parse_sl_comment(s, sl_comment_i, line_end_i, comment_lines)
    elif next_i == ml_comment_i:
        return parse_ml_comment(s, ml_comment_i, comment_lines)
    elif next_i == enum_end_i:
        return -1
    return line_end_i + 1


def parse_enum_line(s, i, equal_i, sl_comment_i, line_end_i, comment_lines, add_to_output):
    if sl_comment_i > equal_i and sl_comment_i < line_end_i:
        parse_sl_comment(s, sl_comment_i, line_end_i, comment_lines)
    original_name = s[i:equal_i].strip()
    cap_snake_name = ensure_cap_snake_name(original_name)
    add_to_output(original_name, ENUM_OVERRIDE.get(cap_snake_name, cap_snake_name), comment_lines)
    comment_lines.clear()


def parse_sl_comment(s, sl_comment_i, line_end_i, comment_lines):
    comment_lines.append(s[sl_comment_i + 2:line_end_i].strip())


def parse_ml_comment(s, ml_comment_i, comment_lines):
    i = ml_comment_i + 2
    ml_comment_end_i = s.find("*/", i)
    while i < ml_comment_end_i:
        i = parse_ml_comment_line(s, i, ml_comment_end_i, comment_lines)
    return ml_comment_end_i + 2


def parse_ml_comment_line(s, i, ml_comment_end_i, comment_lines):
    line_end_i = s.find("\n", i)
    end_i = min(line_end_i, ml_comment_end_i)
    stripped_line = s[i:end_i].strip()
    if len(stripped_line) > 0:
        if stripped_line[0] == "*":
            stripped_line = stripped_line[1:].strip()
    if len(stripped_line) > 0:
        comment_lines.append(stripped_line)
    return line_end_i + 1


def id_is_next(name, i):
    if (i + 1) < len(name):
        return name[i:i+2] == "ID"
    return False


def ensure_cap_snake_name(name):
    # assume that name is snake-case if it contains a _ or is not mixed-case
    has_underscore = "_" in name
    is_not_mixed = name.isupper() or name.islower()
    if has_underscore or (not has_underscore and is_not_mixed):
        return name.upper()
    else:
        out = name[0].upper()
        i = 1
        while i < len(name):
            if id_is_next(name, i):
                out += "_ID"
                i += 2
            else:
                c = name[i]
                if c.isupper():
                    out += "_"
                out += c.upper()
                i += 1
        return out




main()


