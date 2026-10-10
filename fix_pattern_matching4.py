#!/usr/bin/env python3
"""
Targeted fix for ternary pattern matching in method arguments.
"""

import re
from pathlib import Path

def fix_ternary_in_method_args(content):
    """
    Fix ternary pattern matching when used as method arguments.
    Pattern: expr instanceof Type var ? var : falseExpr,
    """
    # This regex matches: expr instanceof Type var ? var : falseExpr
    # where expr is a simple expression (method call, variable, etc.)
    # and the whole thing is followed by comma or closing paren
    pattern = re.compile(
        r'(\b(?:[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?))\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\3\s*:\s*([^,\n]+)',
        re.MULTILINE
    )
    
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        false_expr = match.group(4)
        return f"{expr} instanceof {type_name} ? ({type_name}) {expr} : {false_expr}"
    
    return pattern.sub(replace, content)

def fix_return_ternary(content):
    """Fix return statements with ternary pattern matching."""
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        # Match: return expr instanceof Type var ? var : falseExpr;
        match = re.match(r'^(\s*)return\s+(.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\4\s*:\s*(.+);\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            false_expr = match.group(5).strip()
            # Check expr is simple
            if '?:' not in expr and '?' not in expr.split('instanceof')[0]:
                new_line = f"{indent}return {expr} instanceof {type_name} ? ({type_name}) {expr} : {false_expr};"
                new_lines.append(new_line)
                continue
        new_lines.append(line)
    return '\n'.join(new_lines)

def fix_if_pattern(content):
    """Fix if statements with pattern matching."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        match = re.search(r'^(\s*)if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
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
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_else_if_pattern(content):
    """Fix else if statements with pattern matching."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        match = re.search(r'^(\s*)else\s+if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
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
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_while_pattern(content):
    """Fix while statements with pattern matching."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        match = re.search(r'^(\s*)while\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
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
    return '\n'.join(new_lines)

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # Apply fixes in order
    content = fix_ternary_in_method_args(content)
    content = fix_return_ternary(content)
    content = fix_if_pattern(content)
    content = fix_else_if_pattern(content)
    content = fix_while_pattern(content)
    
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