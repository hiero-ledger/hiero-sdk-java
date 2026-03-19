#!/usr/bin/env python3

import os
import subprocess
import sys
import re
import shutil
from pathlib import Path
from typing import List, Tuple, Callable, Optional

class ProtoGenerator:
    def __init__(self):
        # Configuration
        self.PROTO_GIT_REMOTE = "https://github.com/hiero-ledger/hiero-consensus-node.git"
        self.PROTO_GIT_PATH = "hedera-protos-git"
        self.DEFAULT_CHECKOUT_REF = "v0.64.0"
        
        # Path configurations
        self.setup_paths()
        
        # Processing configurations
        self.PROTO_DO_NOT_REMOVE = ()
        self.ENUM_OVERRIDE = {"UTIL_PRNG": "PRNG"}
        
        self.COMMENT_REPLACEMENTS = (
            ("&", "and"),
            ("<tt>", ""),
            ("</tt>", "")
        )
        
        self.PROTO_REPLACEMENTS = (
            ("option java_package = \"com.hederahashgraph.api.proto.java\";",
             "option java_package = \"com.hedera.hashgraph.sdk.proto\";"),
            ("option java_package = \"com.hederahashgraph.service.proto.java\";",
             "option java_package = \"com.hedera.hashgraph.sdk.proto\";"),
            ("option java_package = \"com.hedera.mirror.api.proto\";",
             "option java_package = \"com.hedera.hashgraph.sdk.proto.mirror\";")
        )
        
        self.PROTO_REPLACEMENTS_IMPORTS = (
            (r'import ".*\/(.*\.proto)"', r'import "\1"'),
        )
        
        # Java file sections
        self.setup_java_sections()
    
    def setup_paths(self):
        """Initialize all path configurations"""
        self.PROTO_IN_PATH = Path(self.PROTO_GIT_PATH) / "hapi" / "hedera-protobuf-java-api" / "src" / "main" / "proto" / "services"
        self.BASIC_TYPES_PATH = self.PROTO_IN_PATH / "basic_types.proto"
        self.RESPONSE_CODE_PATH = self.PROTO_IN_PATH / "response_code.proto"
        self.FREEZE_TYPE_PATH = self.PROTO_IN_PATH / "freeze_type.proto"
        
        self.MAIN_PATH = Path("..") / "sdk" / "src" / "main"
        self.PROTO_OUT_PATH = self.MAIN_PATH / "proto"
        self.PROTO_MIRROR_OUT_PATH = self.PROTO_OUT_PATH / "mirror"
        self.JAVA_OUT_PATH = self.MAIN_PATH / "java" / "com" / "hedera" / "hashgraph" / "sdk"
        
        self.REQUEST_TYPE_OUT_PATH = self.JAVA_OUT_PATH / "RequestType.java"
        self.STATUS_OUT_PATH = self.JAVA_OUT_PATH / "Status.java"
        self.FEE_DATA_TYPE_OUT_PATH = self.JAVA_OUT_PATH / "FeeDataType.java"
        self.FREEZE_TYPE_OUT_PATH = self.JAVA_OUT_PATH / "FreezeType.java"
    
    def setup_java_sections(self):
        """Initialize Java file section templates"""
        self.RequestType_sections = [
            self.load_premade("RequestType", 0),
            "",
            self.load_premade("RequestType", 2),
            "",
            self.load_premade("RequestType", 4),
            "",
            self.load_premade("RequestType", 6)
        ]
        
        self.Status_sections = [
            self.load_premade("Status", 0),
            "",
            self.load_premade("Status", 2),
            "",
            self.load_premade("Status", 4),
        ]
        
        self.FeeDataType_sections = [
            self.load_premade("FeeDataType", 0),
            "",
            self.load_premade("FeeDataType", 2),
            "",
            self.load_premade("FeeDataType", 4),
            "",
            self.load_premade("FeeDataType", 6)
        ]
        
        self.FreezeType_sections = [
            self.load_premade("FreezeType", 0),
            "",
            self.load_premade("FreezeType", 2),
            "",
            self.load_premade("FreezeType", 4)
        ]
    
    def load_premade(self, name: str, n: int) -> str:
        """Load premade template file with error handling"""
        try:
            premade_path = Path("premade") / f"{name}-{n}.txt"
            return premade_path.read_text(encoding='utf-8')
        except FileNotFoundError:
            print(f">>> Warning: Premade file {premade_path} not found")
            return ""
        except Exception as e:
            print(f">>> Error loading premade file {premade_path}: {e}")
            return ""
    
    def go_to_script_dir(self):
        """Change to the script directory"""
        script_dir = Path(__file__).parent
        os.chdir(script_dir)
    
    def run_command(self, *command) -> bool:
        """Run a command with error handling"""
        print(f">>> Executing command `{' '.join(command)}`")
        try:
            result = subprocess.run(command, check=True)
            return True
        except subprocess.CalledProcessError as e:
            print(f">>> Command failed with return code {e.returncode}")
            return False
    
    def is_branch(self, ref: str) -> bool:
        """Check if a reference is a branch"""
        try:
            result = subprocess.run(
                ['git', 'branch', '--list', ref], 
                stdout=subprocess.PIPE, 
                stderr=subprocess.PIPE,
                text=True
            )
            return result.returncode == 0 and len(result.stdout.strip()) > 0
        except Exception as e:
            print(f">>> Error checking for branch {ref}: {e}")
            return False
    
    def get_latest_tag(self) -> str:
        """Get the latest git tag"""
        try:
            result = subprocess.run([
                'git', '-c', 'versionsort.suffix=-alpha', 
                '-c', 'versionsort.suffix=-beta', 
                '-c', 'versionsort.suffix=-rc', 
                'tag', '-l', '--sort=version:refname'
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            
            if result.returncode == 0:
                tags = result.stdout.strip().split('\n')
                return tags[-1] if tags and tags[0] else ""
            return ""
        except Exception as e:
            print(f">>> Error getting latest tag: {e}")
            return ""
    
    def ensure_protobufs(self, checkout_ref: Optional[str] = None):
        """Ensure protobuf files are available and up to date"""
        if not checkout_ref:
            checkout_ref = self.DEFAULT_CHECKOUT_REF
        
        proto_path = Path(self.PROTO_GIT_PATH)
        
        if proto_path.exists():
            print(">>> Detected existing protobufs")
        else:
            print(">>> No protobufs detected")
            if not self.run_command("git", "clone", self.PROTO_GIT_REMOTE, self.PROTO_GIT_PATH):
                print(">>> Failed to clone repository. Exiting.")
                sys.exit(1)
        
        os.chdir(proto_path)
        
        if not self.run_command("git", "fetch"):
            print(">>> Failed to fetch updates. Continuing with existing state.")
        
        print(f">>> Checking out {checkout_ref}")
        if not self.run_command("git", "checkout", checkout_ref):
            print(f">>> Failed to checkout {checkout_ref}. Exiting.")
            sys.exit(1)
        
        if self.is_branch(checkout_ref):
            if not self.run_command("git", "pull", "--rebase"):
                print(">>> Failed to pull latest changes. Continuing with current state.")
        
        self.go_to_script_dir()
    
    def do_replacements(self, s: str, replacements: Tuple[Tuple[str, str], ...]) -> str:
        """Apply string replacements"""
        for old, new in replacements:
            s = s.replace(old, new)
        return s
    
    def do_replacements_proto_imports(self, s: str, replacements: Tuple[Tuple[str, str], ...]) -> str:
        """Apply regex replacements for proto imports"""
        for pattern, replacement in replacements:
            if 'google' in s:
                continue
            s = re.sub(pattern, replacement, s)
        return s
    
    def clear_dir(self, dir_path: Path):
        """Recursively clear directory contents"""
        if not dir_path.exists():
            return
        
        for item in dir_path.iterdir():
            if item.name in self.PROTO_DO_NOT_REMOVE:
                continue
            
            try:
                if item.is_file():
                    item.unlink()
                elif item.is_dir():
                    shutil.rmtree(item)
            except Exception as e:
                print(f">>> Warning: Failed to remove {item}: {e}")
    
    def generate_modified_protos(self):
        """Generate modified proto files"""
        self.do_generate_modified_protos(self.PROTO_IN_PATH, self.PROTO_OUT_PATH)
    
    def do_generate_modified_protos(self, in_path: Path, out_path: Path):
        """Process proto files and apply modifications"""
        if not in_path.exists():
            print(f">>> Error: Input path {in_path} does not exist")
            return
        
        out_path.mkdir(parents=True, exist_ok=True)
        
        for root, dirs, files in os.walk(in_path):
            for file_name in files:
                if not file_name.endswith('.proto'):
                    continue
                
                try:
                    input_file = Path(root) / file_name
                    output_file = out_path / file_name
                    
                    content = input_file.read_text(encoding='utf-8')
                    content = self.do_replacements(content, self.PROTO_REPLACEMENTS)
                    content = self.do_replacements_proto_imports(content, self.PROTO_REPLACEMENTS_IMPORTS)
                    
                    output_file.write_text(content, encoding='utf-8')
                    
                except Exception as e:
                    print(f">>> Error processing {file_name}: {e}")
    
    def output_java_file(self, out_path: Path, section_list: List[str]):
        """Output Java file from sections"""
        try:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            with open(out_path, "w", encoding='utf-8') as out_file:
                for section in section_list:
                    out_file.write(section)
        except Exception as e:
            print(f">>> Error writing Java file {out_path}: {e}")
    
    def tabs(self, n: int) -> str:
        """Generate indentation"""
        return " " * (4 * n)
    
    def generate_comment(self, comment_lines: List[str], tab_count: int) -> str:
        """Generate Javadoc comment"""
        if not comment_lines:
            return ""
        
        lines = [self.tabs(tab_count) + "/**"]
        for line in comment_lines:
            processed_line = self.do_replacements(line, self.COMMENT_REPLACEMENTS)
            lines.append(f"{self.tabs(tab_count)} * {processed_line}")
        lines.append(self.tabs(tab_count) + " */")
        return "\n".join(lines) + "\n"
    
    def generate_enum_line(self, original_name: str, cap_snake_name: str, enum_name: str, tab_count: int) -> str:
        """Generate enum line"""
        return f"{self.tabs(tab_count)}{cap_snake_name}({enum_name}.{original_name}),\n\n"
    
    def generate_enum(self, original_name: str, cap_snake_name: str, comment_lines: List[str], enum_name: str, tabs_count: int) -> str:
        """Generate complete enum entry"""
        return (self.generate_comment(comment_lines, tabs_count) + 
                self.generate_enum_line(original_name, cap_snake_name, enum_name, tabs_count))
    
    def generate_valueOf(self, original_name: str, cap_snake_name: str, tabs_count: int) -> str:
        """Generate valueOf case"""
        return f"{self.tabs(tabs_count)}case {original_name} -> {cap_snake_name};\n"
    
    def generate_toString(self, original_name: str, cap_snake_name: str, tabs_count: int) -> str:
        """Generate toString case"""
        return f"{self.tabs(tabs_count)}case {cap_snake_name} -> \"{cap_snake_name}\";\n"
    
    def replace_last_enum_comma(self, s: str) -> str:
        """Replace last comma with semicolon"""
        return s[:-3] + ";\n\n" if s.endswith(",\n\n") else s
    
    def add_to_RequestType(self, original_name: str, cap_snake_name: str, comment_lines: List[str]):
        """Add entry to RequestType enum"""
        self.RequestType_sections[1] += self.generate_enum(original_name, cap_snake_name, comment_lines, "HederaFunctionality", 1)
        self.RequestType_sections[3] += self.generate_valueOf(original_name, cap_snake_name, 3)
        self.RequestType_sections[5] += self.generate_toString(original_name, cap_snake_name, 3)
    
    def add_to_Status(self, original_name: str, cap_snake_name: str, comment_lines: List[str]):
        """Add entry to Status enum"""
        self.Status_sections[1] += self.generate_enum(original_name, cap_snake_name, comment_lines, "ResponseCodeEnum", 1)
        self.Status_sections[3] += self.generate_valueOf(original_name, cap_snake_name, 3)
    
    def add_to_FeeDataType(self, original_name: str, cap_snake_name: str, comment_lines: List[str]):
        """Add entry to FeeDataType enum"""
        self.FeeDataType_sections[1] += self.generate_enum(original_name, cap_snake_name, comment_lines, "SubType", 1)
        self.FeeDataType_sections[3] += self.generate_valueOf(original_name, cap_snake_name, 3)
        self.FeeDataType_sections[5] += self.generate_toString(original_name, cap_snake_name, 3)
    
    def add_to_FreezeType(self, original_name: str, cap_snake_name: str, comment_lines: List[str]):
        """Add entry to FreezeType enum"""
        self.FreezeType_sections[1] += self.generate_enum(original_name, cap_snake_name, comment_lines, "com.hedera.hashgraph.sdk.proto.FreezeType", 1)
        self.FreezeType_sections[3] += self.generate_valueOf(original_name, cap_snake_name, 3)
    
    def finalize_RequestType(self):
        """Finalize RequestType enum generation"""
        self.RequestType_sections[1] = self.replace_last_enum_comma(self.RequestType_sections[1])
    
    def finalize_Status(self):
        """Finalize Status enum generation"""
        self.Status_sections[1] = self.replace_last_enum_comma(self.Status_sections[1])
    
    def finalize_FeeDataType(self):
        """Finalize FeeDataType enum generation"""
        self.FeeDataType_sections[1] = self.replace_last_enum_comma(self.FeeDataType_sections[1])
    
    def finalize_FreezeType(self):
        """Finalize FreezeType enum generation"""
        self.FreezeType_sections[1] = self.replace_last_enum_comma(self.FreezeType_sections[1])
    
    def generate_RequestType(self):
        """Generate RequestType.java"""
        self.parse_enum_from_file(
            self.BASIC_TYPES_PATH,
            "HederaFunctionality",
            self.add_to_RequestType,
            self.finalize_RequestType
        )
        self.output_java_file(self.REQUEST_TYPE_OUT_PATH, self.RequestType_sections)
    
    def generate_Status(self):
        """Generate Status.java"""
        self.parse_enum_from_file(
            self.RESPONSE_CODE_PATH,
            "ResponseCodeEnum",
            self.add_to_Status,
            self.finalize_Status
        )
        self.output_java_file(self.STATUS_OUT_PATH, self.Status_sections)
    
    def generate_FeeDataType(self):
        """Generate FeeDataType.java"""
        self.parse_enum_from_file(
            self.BASIC_TYPES_PATH,
            "SubType",
            self.add_to_FeeDataType,
            self.finalize_FeeDataType
        )
        self.output_java_file(self.FEE_DATA_TYPE_OUT_PATH, self.FeeDataType_sections)
    
    def generate_FreezeType(self):
        """Generate FreezeType.java"""
        self.parse_enum_from_file(
            self.FREEZE_TYPE_PATH,
            "FreezeType",
            self.add_to_FreezeType,
            self.finalize_FreezeType
        )
        self.output_java_file(self.FREEZE_TYPE_OUT_PATH, self.FreezeType_sections)
    
    def parse_enum_from_file(self, in_path: Path, enum_name: str, add_to_output: Callable, finalize_output: Callable):
        """Parse enum from protobuf file"""
        try:
            if not in_path.exists():
                print(f">>> Error: File {in_path} does not exist")
                return
            
            content = in_path.read_text(encoding='utf-8')
            enum_start = content.find(f"enum {enum_name}")
            if enum_start == -1:
                print(f">>> Error: Enum {enum_name} not found in {in_path}")
                return
            
            i = content.find("{", enum_start) + 1
            comment_lines = []
            
            while i >= 0:
                i = self.parse_enum_code(content, i, comment_lines, add_to_output)
            
            finalize_output()
            
        except Exception as e:
            print(f">>> Error parsing enum from {in_path}: {e}")
    
    def parse_enum_code(self, s: str, i: int, comment_lines: List[str], add_to_output: Callable) -> int:
        """Parse enum code section"""
        equal_i = s.find("=", i)
        sl_comment_i = s.find("//", i)
        ml_comment_i = s.find("/*", i)
        line_end_i = s.find("\n", i)
        enum_end_i = s.find("}", i)
        
        indices = [idx for idx in [equal_i, sl_comment_i, ml_comment_i, line_end_i, enum_end_i] if idx >= i]
        if not indices:
            return -1
        
        next_i = min(indices)
        
        if next_i == equal_i:
            self.parse_enum_line(s, i, equal_i, sl_comment_i, line_end_i, comment_lines, add_to_output)
        elif next_i == sl_comment_i:
            self.parse_sl_comment(s, sl_comment_i, line_end_i, comment_lines)
        elif next_i == ml_comment_i:
            return self.parse_ml_comment(s, ml_comment_i, comment_lines)
        elif next_i == enum_end_i:
            return -1
        
        return line_end_i + 1
    
    def parse_enum_line(self, s: str, i: int, equal_i: int, sl_comment_i: int, line_end_i: int, comment_lines: List[str], add_to_output: Callable):
        """Parse individual enum line"""
        if equal_i < sl_comment_i < line_end_i:
            self.parse_sl_comment(s, sl_comment_i, line_end_i, comment_lines)
        
        original_name = s[i:equal_i].strip()
        cap_snake_name = self.ensure_cap_snake_name(original_name)
        final_name = self.ENUM_OVERRIDE.get(cap_snake_name, cap_snake_name)
        
        add_to_output(original_name, final_name, comment_lines)
        comment_lines.clear()
    
    def parse_sl_comment(self, s: str, sl_comment_i: int, line_end_i: int, comment_lines: List[str]):
        """Parse single-line comment"""
        comment_text = s[sl_comment_i + 2:line_end_i].strip()
        if comment_text:
            comment_lines.append(comment_text)
    
    def parse_ml_comment(self, s: str, ml_comment_i: int, comment_lines: List[str]) -> int:
        """Parse multi-line comment"""
        i = ml_comment_i + 2
        ml_comment_end_i = s.find("*/", i)
        
        if ml_comment_end_i == -1:
            return -1
        
        while i < ml_comment_end_i:
            i = self.parse_ml_comment_line(s, i, ml_comment_end_i, comment_lines)
        
        return ml_comment_end_i + 2
    
    def parse_ml_comment_line(self, s: str, i: int, ml_comment_end_i: int, comment_lines: List[str]) -> int:
        """Parse single line within multi-line comment"""
        line_end_i = s.find("\n", i)
        if line_end_i == -1:
            line_end_i = ml_comment_end_i
        
        end_i = min(line_end_i, ml_comment_end_i)
        stripped_line = s[i:end_i].strip()
        
        if stripped_line and stripped_line.startswith("*"):
            stripped_line = stripped_line[1:].strip()
        
        if stripped_line:
            comment_lines.append(stripped_line)
        
        return line_end_i + 1
    
    def id_is_next(self, name: str, i: int) -> bool:
        """Check if ID follows at position i"""
        return i + 1 < len(name) and name[i:i+2] == "ID"
    
    def ensure_cap_snake_name(self, name: str) -> str:
        """Convert name to CAPITAL_SNAKE_CASE"""
        has_underscore = "_" in name
        is_not_mixed = name.isupper() or name.islower()
        
        if has_underscore or (not has_underscore and is_not_mixed):
            return name.upper()
        
        result = [name[0].upper()]
        i = 1
        
        while i < len(name):
            if self.id_is_next(name, i):
                result.append("_ID")
                i += 2
            else:
                c = name[i]
                if c.isupper():
                    result.append("_")
                result.append(c.upper())
                i += 1
        
        return "".join(result)
    
    def run(self, checkout_ref: Optional[str] = None):
        """Main execution method"""
        print(">>> Starting Hedera Proto Generator")
        
        self.go_to_script_dir()
        self.ensure_protobufs(checkout_ref)
        
        print(">>> Generating RequestType.java")
        self.generate_RequestType()
        
        print(">>> Generating Status.java")
        self.generate_Status()
        
        print(">>> Generating FeeDataType.java")
        self.generate_FeeDataType()
        
        print(">>> Generating FreezeType.java")
        self.generate_FreezeType()
        
        print(">>> Clearing proto output directory")
        self.clear_dir(self.PROTO_OUT_PATH)
        
        print(">>> Generating modified proto files")
        self.generate_modified_protos()
        
        print(">>> Done")

def main():
    """Main entry point"""
    if len(sys.argv) > 2:
        print(">>> Incorrect number of arguments. Exiting.")
        sys.exit(1)
    
    if len(sys.argv) == 1:
        print(f">>> Usage: `{sys.argv[0]} ref`")
        print(">>> Where \"ref\" is a valid branch or tag in the Services git repo")
        print(">>> If no ref is provided, will use default checkout reference")
        print("\n\n")
        
        generator = ProtoGenerator()
        generator.run()
    else:
        checkout_ref = sys.argv[1]
        print(f">>> Using checkout reference: {checkout_ref}")
        print("\n\n")
        
        generator = ProtoGenerator()
        generator.run(checkout_ref)

if __name__ == "__main__":
    main()