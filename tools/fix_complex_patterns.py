#!/usr/bin/env python3
"""
Fix remaining complex pattern matching cases.
"""

import re
from pathlib import Path

def fix_ternary_with_method_call(content):
    """Fix ternary where true expression uses the pattern variable: expr instanceof Type var ? var.method() : false_expr"""
    # Pattern: expr instanceof Type var ? var.method() : false_expr
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        method_call = match.group(4)
        false_expr = match.group(5)
        # Replace var.method() with ((Type) expr).method()
        new_true = method_call.replace(var_name + '.', f'(({type_name}) {expr}).', 1)
        return f"{expr} instanceof {type_name} ? {new_true} : {false_expr}"
    
    pattern = re.compile(
        r'(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\3\.([a-zA-Z_$][a-zA-Z0-9_$]*\([^)]*\))\s*:\s*([^,;\)\n]+)'
    )
    return pattern.sub(replace, content)

def fix_variable_assignment_pattern(content):
    """Fix variable assignment with pattern: Type var = expr instanceof Type var ? ..."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Type var = expr instanceof Type var ? ...
        match = re.match(r'^(\s*)([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(\S+)\s+instanceof\s+\2\s+\3\s*\?', line)
        if match:
            indent = match.group(1)
            type_name = match.group(2)
            var_name = match.group(3)
            expr = match.group(4)
            # Find the rest of the ternary
            # This is complex, handle simple case where it's on one line
            rest = line[match.end():]
            if ':' in rest:
                true_part, false_part = rest.split(':', 1)
                true_part = true_part.strip()
                false_part = false_part.strip().rstrip(';')
                if true_part == var_name:
                    new_line = f"{indent}{type_name} {var_name} = {expr} instanceof {type_name} ? ({type_name}) {expr} : {false_part};"
                    new_lines.append(new_line)
                    i += 1
                    continue
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_negated_pattern(content):
    """Fix negated pattern matching: !(expr instanceof Type var)"""
    # Pattern: !(expr instanceof Type var)
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        return f"!({expr} instanceof {type_name})"
    
    pattern = re.compile(
        r'!\((\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\)'
    )
    return pattern.sub(replace, content)

def fix_return_negated_pattern(content):
    """Fix return with negated pattern: return !(expr instanceof Type var)"""
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        # return !(expr instanceof Type var)
        match = re.match(r'^(\s*)return\s+!\((\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\)\s*;?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            new_line = f"{indent}return !({expr} instanceof {type_name});"
            new_lines.append(new_line)
            continue
        new_lines.append(line)
    return '\n'.join(new_lines)

def fix_lambda_pattern(content):
    """Fix pattern matching in lambda expressions."""
    # Pattern: x -> x instanceof Type var ? true_expr : false_expr
    def replace(match):
        param = match.group(1)
        expr = match.group(2)
        type_name = match.group(3)
        var_name = match.group(4)
        true_expr = match.group(5)
        false_expr = match.group(6)
        if true_expr.strip() == var_name:
            return f"{param} -> {expr} instanceof {type_name} ? ({type_name}) {expr} : {false_expr}"
        return match.group(0)
    
    pattern = re.compile(
        r'([a-zA-Z_$][a-zA-Z0-9_$]*)\s*->\s*(\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\4\s*:\s*([^,;\)\n]+)'
    )
    return pattern.sub(replace, content)

def fix_complex_condition_pattern(content):
    """Fix pattern matching in complex conditions with && or ||."""
    # Pattern: expr instanceof Type var && ...
    # This requires adding variable declaration before the if statement
    # For now, just fix the instanceof part
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        operator = match.group(4)
        return f"{expr} instanceof {type_name} {operator}"
    
    pattern = re.compile(
        r'(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*(&&|\|\|)'
    )
    return pattern.sub(replace, content)

def fix_if_with_complex_condition(content):
    """Fix if statements where pattern variable is used in condition."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # if (expr instanceof Type var && ...)
        match = re.match(r'^(\s*)if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s+(&&|\|\|)\s*(.+)\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            operator = match.group(5)
            rest = match.group(6).strip()
            has_brace = line.rstrip().endswith('{')
            
            # Need to add variable declaration before if
            new_lines.append(f"{indent}{type_name} {var_name} = ({type_name}) {expr};")
            new_condition = f"{indent}if ({expr} instanceof {type_name} {operator} {rest})"
            if has_brace:
                new_condition += " {"
            new_lines.append(new_condition)
            
            if not has_brace:
                i += 1
                if i < len(lines):
                    next_line = lines[i]
                    if next_line.strip() == '{':
                        new_lines.append(next_line)
                    else:
                        new_lines.append(next_line)
                        i += 1
                        continue
            i += 1
            continue
        
        # else if with complex condition
        match = re.match(r'^(\s*)else\s+if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s+(&&|\|\|)\s*(.+)\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            operator = match.group(5)
            rest = match.group(6).strip()
            has_brace = line.rstrip().endswith('{')
            
            new_lines.append(f"{indent}{type_name} {var_name} = ({type_name}) {expr};")
            new_condition = f"{indent}else if ({expr} instanceof {type_name} {operator} {rest})"
            if has_brace:
                new_condition += " {"
            new_lines.append(new_condition)
            
            if not has_brace:
                i += 1
                if i < len(lines):
                    next_line = lines[i]
                    if next_line.strip() == '{':
                        new_lines.append(next_line)
                    else:
                        new_lines.append(next_line)
                        i += 1
                        continue
            i += 1
            continue
        
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_else_if_simple_pattern(content):
    """Fix else if with simple pattern matching."""
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        match = re.match(r'^(\s*)else\s+if\s*\((\S+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
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

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    content = fix_ternary_with_method_call(content)
    content = fix_variable_assignment_pattern(content)
    content = fix_negated_pattern(content)
    content = fix_return_negated_pattern(content)
    content = fix_lambda_pattern(content)
    content = fix_complex_condition_pattern(content)
    content = fix_if_with_complex_condition(content)
    content = fix_else_if_simple_pattern(content)
    
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