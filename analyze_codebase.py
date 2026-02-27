import os
import re
import collections

# Path to explore
src_dir = r"e:\New folder (4)\Rev-PasswordManager\src\main\java"

# Storage
java_files = []
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

# 1. READ ALL FILES
file_contents = {}
for file in java_files:
    with open(file, 'r', encoding='utf-8', errors='ignore') as f:
        file_contents[file] = f.read()

# 2. EXTRACT METHODS & FIND DUPLICATES
# Rough regex to find Java methods (simplified, catches most)
method_pattern = re.compile(
    r'(?:(?:public|private|protected|static|final|native|synchronized|abstract|transient)\s+)+[\w\<\>\[\]]+\s+(\w+)\s*\([^\)]*\)\s*\{', 
    re.MULTILINE
)

# We will hash the method body (very roughly, by extracting everything inside { ... })
def extract_body(text, start_index):
    open_braces = 0
    in_method = False
    body = []
    
    for i in range(start_index, len(text)):
        char = text[i]
        if char == '{':
            open_braces += 1
            in_method = True
        elif char == '}':
            open_braces -= 1
        
        if in_method:
            body.append(char)
            if open_braces == 0:
                break
    return "".join(body)

methods_defined = set()
blocks_by_hash = collections.defaultdict(list)

for file, content in file_contents.items():
    filename = os.path.basename(file)
    for match in method_pattern.finditer(content):
        method_name = match.group(1)
        
        # Skip constructors (name matches class), main, getters/setters, build, equals, hashCode, toString
        if method_name in ('main', 'equals', 'hashCode', 'toString', 'build', 'builder') or method_name.startswith('get') or method_name.startswith('set'):
            continue
            
        methods_defined.add(method_name)
        
        # Get body
        start_idx = match.end() - 1 
        full_body = extract_body(content, start_idx)
        
        # Normalize body (remove whitespaces and standard logger lines to find true duplicates)
        normalized_body = re.sub(r'\s+', '', full_body)
        normalized_body = re.sub(r'logger\.info\(.*?\);', '', normalized_body)
        
        # Only care about non-trivial methods (e.g. at least 50 chars of logic)
        if len(normalized_body) > 50:
            blocks_by_hash[normalized_body].append((filename, method_name))

# 3. FIND UNUSED METHODS
# Count global occurrences of the method name
method_calls = collections.defaultdict(int)

for method in methods_defined:
    # Look for calling pattern: .methodName( or methodName(
    # Avoid declarations by ensuring it's not preceded by public/private etc.
    call_pattern = re.compile(r'(?<!public\s)(?<!private\s)(?<!protected\s)' + re.escape(method) + r'\s*\(')
    for content in file_contents.values():
        if call_pattern.search(content):
            method_calls[method] += len(call_pattern.findall(content))

unused_methods = [m for m in methods_defined if method_calls[m] <= 1] # <= 1 because 1 is its own definition

# 4. FIND UNUSED FILES (Classes)
# A class is unused if its name (excluding Repository/Controller/Service as they are spring bean managed) 
# is not found in any other file
unused_files = []
for file in java_files:
    classname = os.path.basename(file).replace('.java', '')
    
    # Spring components are auto-wired, skip them as they appear "unused"
    if 'Controller' in classname or 'Service' in classname or 'Repository' in classname or 'Config' in classname or 'Application' in classname or 'Filter' in classname or 'Scheduler' in classname or classname.endswith('DTO') or classname.endswith('Request') or classname.endswith('Response'):
        continue
        
    # Count references in other files
    ref_count = 0
    for other_file, content in file_contents.items():
        if other_file != file and classname in content:
            ref_count += 1
            break
            
    if ref_count == 0:
        unused_files.append(classname)

# 5. GENERATE REPORT
with open("e:/New folder (4)/Rev-PasswordManager/analysis_report.txt", "w") as f:
    f.write("=== DUPLICATE CODE BLOCKS ===\n")
    found_dup = False
    for hash_val, occurrences in blocks_by_hash.items():
        if len(occurrences) > 1:
            # Deduplicate same file/method pairs
            unique_occurrences = list(set(occurrences))
            if len(unique_occurrences) > 1:
                found_dup = True
                f.write(f"Duplicate block found in:\n")
                for filename, method_name in unique_occurrences:
                    f.write(f"  - {filename} :: {method_name}()\n")
                f.write("\n")
    if not found_dup:
        f.write("No significant duplicate code blocks found.\n\n")
        
    f.write("\n=== POTENTIALLY UNUSED METHODS ===\n")
    f.write("(Note: Methods called via Reflection, external APIs, or frameworks might be falsely flagged)\n")
    for m in sorted(unused_methods):
        f.write(f"  - {m}()\n")
        
    f.write("\n\n=== POTENTIALLY UNUSED FILES / CLASSES ===\n")
    f.write("(Excluding Spring Beans like Controllers/Services/Repositories)\n")
    for cls in sorted(unused_files):
        f.write(f"  - {cls}.java\n")

print("Analysis complete. Saved to analysis_report.txt")
