import os
import re
import collections

# Paths
main_dir = r"e:\New folder (4)\Rev-PasswordManager\src\main\java\com\revature\passwordmanager\controller"
test_dir = r"e:\New folder (4)\Rev-PasswordManager\src\test\java\com\revature\passwordmanager\controller"

# 1. Extract all endpoints from controllers
mapping_pattern = re.compile(r'@(?:Get|Post|Put|Delete|Patch|Request)Mapping\([^)]*\)')
method_pattern = re.compile(r'public\s+(?:ResponseEntity|[^ ]+)\s+(\w+)\s*\(')

endpoints = collections.defaultdict(list)

for root, _, files in os.walk(main_dir):
    for f in files:
        if f.endswith("Controller.java"):
            with open(os.path.join(root, f), 'r', encoding='utf-8') as file:
                content = file.read()
                
                # Find all methods with mapping annotations
                # A robust way is to find the mapping annotation, then the next public method
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if mapping_pattern.search(line):
                        # Look ahead for the method name
                        for j in range(i+1, min(i+10, len(lines))):
                            match = method_pattern.search(lines[j])
                            if match:
                                method_name = match.group(1)
                                endpoints[f.replace('.java', '')].append(method_name)
                                break

# 2. Extract all tests from test controllers
test_methods = collections.defaultdict(list)

test_method_pattern = re.compile(r'@Test\s*(?:\r?\n.*?)*?(?:void|public\s+void)\s+(\w+)\s*\(')

for root, _, files in os.walk(test_dir):
    for f in files:
        if f.endswith("ControllerTest.java"):
            with open(os.path.join(root, f), 'r', encoding='utf-8') as file:
                content = file.read()
                controller_name = f.replace('Test.java', '')
                
                # Simple extraction, just get all method names with @Test
                for match in test_method_pattern.finditer(content):
                    test_methods[controller_name].append(match.group(1).lower())
                
                # Sometimes tests use DisplayName or nested classes, also just grab all method names that look like tests
                # Many tests are named endpointName_ShouldX
                alt_pattern = re.compile(r'(?:void|public\s+void)\s+(\w+(?:_Should|_Returns|_When)[a-zA-Z0-9_]*)\s*\(')
                for match in alt_pattern.finditer(content):
                    test_methods[controller_name].append(match.group(1).lower())

# 3. Compare
missing_tests = []
total_endpoints = 0
covered_endpoints = 0

report = []

for controller, methods in endpoints.items():
    tests_for_controller = test_methods.get(controller, [])
    
    missing_for_controller = []
    
    for method in methods:
        total_endpoints += 1
        # Check if the method name is present in any test method name
        method_lower = method.lower()
        is_covered = any(method_lower in tm for tm in tests_for_controller)
        
        if is_covered:
            covered_endpoints += 1
        else:
            missing_for_controller.append(method)
            
    if missing_for_controller:
        missing_tests.append((controller, missing_for_controller))

with open("e:/New folder (4)/Rev-PasswordManager/endpoint_test_report.txt", "w") as f:
    f.write(f"=== ENDPOINT TEST COVERAGE REPORT ===\n")
    f.write(f"Total Endpoints Found: {total_endpoints}\n")
    f.write(f"Endpoints with Detected Tests: {covered_endpoints}\n")
    f.write(f"Estimated Endpoint Coverage: {int((covered_endpoints/total_endpoints)*100)}%\n\n")
    
    if missing_tests:
        f.write("=== POTENTIALLY UNSTESTED ENDPOINTS ===\n")
        f.write("Note: This script looks for test methods formatted like `methodName_ShouldDoSomething`.\n")
        f.write("If the test is named differently, it might show up here as a false positive.\n\n")
        for controller, methods in missing_tests:
            f.write(f"{controller}:\n")
            for m in methods:
                f.write(f"  - {m}()\n")
            f.write("\n")
    else:
        f.write("All endpoints appear to have corresponding test methods!\n")

print("Analysis complete. Saved to endpoint_test_report.txt")
