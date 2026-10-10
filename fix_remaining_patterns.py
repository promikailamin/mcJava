#!/usr/bin/env python3
"""
Fix remaining specific pattern matching cases.
"""

import re
from pathlib import Path

def fix_variable_assignment_pattern(content):
    """Fix: Type var = expr instanceof Type var (no ternary)"""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Type var = expr instanceof Type var
        match = re.match(r'^(\s*)([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(\S+)\s+instanceof\s+\2\s+\3\s*[;,)]', line)
        if match:
            indent = match.group(1)
            type_name = match.group(2)
            var_name = match.group(3)
            expr = match.group(4)
            new_line = f"{indent}{type_name} {var_name} = {expr} instanceof {type_name} ? ({type_name}) {expr} : null;"
            new_lines.append(new_line)
            i += 1
            continue
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_if_pattern_immediate_return(content):
    """Fix: if (expr instanceof Type var) return expr;"""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # if (expr instanceof Type var) return ...
        match = re.match(r'^(\s*)if\s*\((\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\)\s+return\s+(.+);\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            return_expr = match.group(5)
            # Replace var_name with cast expr in return expression
            new_return = return_expr.replace(var_name, f'({type_name}) {expr}')
            new_lines.append(f"{indent}if ({expr} instanceof {type_name}) {{")
            new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
            new_lines.append(f"{indent}    return {new_return};")
            new_lines.append(f"{indent}}}")
            i += 1
            continue
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_complex_ternary_pattern(content):
    """Fix ternary where true expression uses pattern var with method calls."""
    # Pattern: expr instanceof Type var ? var.method(...) : false_expr
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        true_expr = match.group(4)
        false_expr = match.group(5)
        # Replace all occurrences of var_name. with ((Type) expr).
        new_true = true_expr.replace(var_name + '.', f'(({type_name}) {expr}).')
        return f"{expr} instanceof {type_name} ? {new_true} : {false_expr}"
    
    pattern = re.compile(
        r'(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*([^:]+)\s*:\s*([^,;\)\n]+)'
    )
    return pattern.sub(replace, content)

def fix_return_simple_pattern(content):
    """Fix: return expr instanceof Type var"""
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        match = re.match(r'^(\s*)return\s+(\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*;?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            new_line = f"{indent}return {expr} instanceof {type_name};"
            new_lines.append(new_line)
            continue
        new_lines.append(line)
    return '\n'.join(new_lines)

def fix_lambda_pattern(content):
    """Fix lambda with pattern: x -> x instanceof Type var ? ... : ..."""
    def replace(match):
        param = match.group(1)
        expr = match.group(2)
        type_name = match.group(3)
        var_name = match.group(4)
        true_expr = match.group(5)
        false_expr = match.group(6)
        # Replace var_name. with ((Type) expr).
        new_true = true_expr.replace(var_name + '.', f'(({type_name}) {expr}).')
        # Also replace bare var_name
        new_true = re.sub(r'\b' + re.escape(var_name) + r'\b', f'({type_name}) {expr}', new_true)
        return f"{param} -> {expr} instanceof {type_name} ? {new_true} : {false_expr}"
    
    pattern = re.compile(
        r'([a-zA-Z_$][a-zA-Z0-9_$]*)\s*->\s*(\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*([^:]+)\s*:\s*([^,;\)\n]+)'
    )
    return pattern.sub(replace, content)

def fix_else_if_pattern(content):
    """Fix else if with pattern."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        match = re.match(r'^(\s*)\}?\s*else\s+if\s*\((\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
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

def fix_ternary_constructor_pattern(content):
    """Fix ternary where true expression creates new object with pattern var."""
    # Pattern: expr instanceof Type var ? new Type2(var) : false_expr
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        true_expr = match.group(4)
        false_expr = match.group(5)
        new_true = true_expr.replace(var_name, f'({type_name}) {expr}')
        return f"{expr} instanceof {type_name} ? {new_true} : {false_expr}"
    
    pattern = re.compile(
        r'(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*([^:]+)\s*:\s*([^,;\)\n]+)'
    )
    return pattern.sub(replace, content)

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    content = fix_variable_assignment_pattern(content)
    content = fix_if_pattern_immediate_return(content)
    content = fix_complex_ternary_pattern(content)
    content = fix_return_simple_pattern(content)
    content = fix_lambda_pattern(content)
    content = fix_else_if_pattern(content)
    content = fix_ternary_constructor_pattern(content)
    
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