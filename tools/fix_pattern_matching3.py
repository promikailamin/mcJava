#!/usr/bin/env python3
"""
Better script to rewrite Java pattern matching for instanceof to traditional syntax.
Uses a more careful approach to handle nested expressions.
"""

import re
from pathlib import Path

def find_matching_paren(text, start):
    """Find the matching closing parenthesis for the opening paren at start."""
    count = 0
    for i, ch in enumerate(text[start:], start):
        if ch == '(':
            count += 1
        elif ch == ')':
            count -= 1
            if count == 0:
                return i
    return -1

def find_matching_brace(text, start):
    """Find the matching closing brace for the opening brace at start."""
    count = 0
    for i, ch in enumerate(text[start:], start):
        if ch == '{':
            count += 1
        elif ch == '}':
            count -= 1
            if count == 0:
                return i
    return -1

def extract_expression(text, end_pos):
    """
    Extract the expression before 'instanceof' at end_pos.
    Handles nested parentheses, brackets, method calls, etc.
    """
    i = end_pos - 1
    # Skip whitespace
    while i >= 0 and text[i].isspace():
        i -= 1
    
    if i < 0:
        return None, -1
    
    # Handle different expression endings
    paren_count = 0
    bracket_count = 0
    brace_count = 0
    in_string = False
    string_char = None
    escaped = False
    
    start = i
    while i >= 0:
        ch = text[i]
        
        if escaped:
            escaped = False
            i -= 1
            continue
        
        if ch == '\\' and in_string:
            escaped = True
            i -= 1
            continue
        
        if ch in ('"', "'") and not in_string:
            in_string = True
            string_char = ch
            i -= 1
            continue
        
        if ch == string_char and in_string:
            in_string = False
            string_char = None
            i -= 1
            continue
        
        if in_string:
            i -= 1
            continue
        
        if ch == ')':
            paren_count += 1
        elif ch == '(':
            paren_count -= 1
            if paren_count < 0:
                break
        elif ch == ']':
            bracket_count += 1
        elif ch == '[':
            bracket_count -= 1
            if bracket_count < 0:
                break
        elif ch == '}':
            brace_count += 1
        elif ch == '{':
            brace_count -= 1
            if brace_count < 0:
                break
        
        # Stop at certain operators if we're at top level
        if paren_count == 0 and bracket_count == 0 and brace_count == 0:
            if ch in ';,{}=<>!&|^~' and i < end_pos - 1:
                # Check for ?:
                if ch == '?' and i + 1 < len(text) and text[i+1] == ':':
                    pass  # This is part of ternary, continue
                else:
                    break
        
        i -= 1
    
    return text[i+1:end_pos].strip(), i+1

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # First, handle ternary pattern matching: expr instanceof Type var ? var : falseExpr
    # We need to find all occurrences and replace them
    # This is complex, so let's use a while loop with find
    
    # Pattern: look for "instanceof Type var ? var :"
    # But we need to find the expression before instanceof
    
    # Let's use a simpler approach: process line by line for simple cases
    # and use a more sophisticated approach for complex cases
    
    lines = content.split('\n')
    new_lines = []
    i = 0
    
    while i < len(lines):
        line = lines[i]
        
        # Check for ternary pattern matching on this line
        # Pattern: expr instanceof Type var ? var :
        ternary_match = re.search(r'\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\2\s*:', line)
        if ternary_match:
            type_name = ternary_match.group(1)
            var_name = ternary_match.group(2)
            # Find the expression before instanceof
            instanceof_pos = line.find('instanceof', ternary_match.start())
            if instanceof_pos > 0:
                expr = line[:instanceof_pos].strip()
                # Check if expr is simple (no nested ?: at top level)
                if '?:' not in expr and expr.count('?') == expr.count(':'):
                    # Replace the pattern
                    before = line[:instanceof_pos]
                    after_match = ternary_match.end()
                    after = line[after_match:]
                    new_line = f"{before}{expr} instanceof {type_name} ? ({type_name}) {expr} :{after}"
                    new_lines.append(new_line)
                    i += 1
                    continue
        
        # Check for if statement with pattern matching
        if_match = re.match(r'^(\s*)if\s*\((.*)\)\s*\{?\s*$', line)
        if if_match:
            indent = if_match.group(1)
            condition = if_match.group(2)
            # Check for pattern matching in condition
            pm_match = re.search(r'\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*$', condition)
            if pm_match:
                type_name = pm_match.group(1)
                var_name = pm_match.group(2)
                expr = condition[:pm_match.start()].strip()
                has_brace = line.rstrip().endswith('{')
                
                new_condition = f"{indent}if ({expr} instanceof {type_name})"
                if has_brace:
                    new_condition += " {"
                new_lines.append(new_condition)
                
                if has_brace:
                    new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                else:
                    i += 1
                    if i < len(lines):
                        next_line = lines[i]
                        if next_line.strip() == '{':
                            new_lines.append(next_line)
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        else:
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                            new_lines.append(next_line)
                            i += 1
                            continue
                i += 1
                continue
        
        # Check for else if with pattern matching
        else_if_match = re.match(r'^(\s*)else\s+if\s*\((.*)\)\s*\{?\s*$', line)
        if else_if_match:
            indent = else_if_match.group(1)
            condition = else_if_match.group(2)
            pm_match = re.search(r'\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*$', condition)
            if pm_match:
                type_name = pm_match.group(1)
                var_name = pm_match.group(2)
                expr = condition[:pm_match.start()].strip()
                has_brace = line.rstrip().endswith('{')
                
                new_condition = f"{indent}else if ({expr} instanceof {type_name})"
                if has_brace:
                    new_condition += " {"
                new_lines.append(new_condition)
                
                if has_brace:
                    new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                else:
                    i += 1
                    if i < len(lines):
                        next_line = lines[i]
                        if next_line.strip() == '{':
                            new_lines.append(next_line)
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        else:
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                            new_lines.append(next_line)
                            i += 1
                            continue
                i += 1
                continue
        
        # Check for while with pattern matching
        while_match = re.match(r'^(\s*)while\s*\((.*)\)\s*\{?\s*$', line)
        if while_match:
            indent = while_match.group(1)
            condition = while_match.group(2)
            pm_match = re.search(r'\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*$', condition)
            if pm_match:
                type_name = pm_match.group(1)
                var_name = pm_match.group(2)
                expr = condition[:pm_match.start()].strip()
                has_brace = line.rstrip().endswith('{')
                
                new_condition = f"{indent}while ({expr} instanceof {type_name})"
                if has_brace:
                    new_condition += " {"
                new_lines.append(new_condition)
                
                if has_brace:
                    new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                else:
                    i += 1
                    if i < len(lines):
                        next_line = lines[i]
                        if next_line.strip() == '{':
                            new_lines.append(next_line)
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        else:
                            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                            new_lines.append(next_line)
                            i += 1
                            continue
                i += 1
                continue
        
        new_lines.append(line)
        i += 1
    
    content = '\n'.join(new_lines)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()