#!/usr/bin/env python3
"""
Comprehensive script to rewrite pattern matching switch expressions to if-else chains.
Uses regex-based approach since javalang doesn't support switch expressions.
"""

import re
from pathlib import Path

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

def find_switch_end(text, start):
    """Find the end of a switch expression/statement."""
    # First find the opening brace
    brace_pos = text.find('{', start)
    if brace_pos == -1:
        return -1
    return find_matching_brace(text, brace_pos)

def extract_switch_subject(text, switch_pos):
    """Extract the expression being switched on."""
    # Find the opening parenthesis after 'switch'
    paren_start = text.find('(', switch_pos)
    if paren_start == -1:
        return None
    
    # Find matching closing parenthesis
    count = 0
    for i, ch in enumerate(text[paren_start:], paren_start):
        if ch == '(':
            count += 1
        elif ch == ')':
            count -= 1
            if count == 0:
                return text[paren_start + 1:i].strip()
    return None

def parse_switch_cases(text, brace_start, brace_end):
    """Parse the cases inside a switch block."""
    cases_content = text[brace_start + 1:brace_end].strip()
    cases = []
    
    # Split by case/default keywords, but be careful with nested structures
    # This is a simplified parser for the common patterns
    lines = cases_content.split('\n')
    current_case = None
    current_body = []
    in_case = False
    
    for line in lines:
        stripped = line.strip()
        
        # Check for case or default
        case_match = re.match(r'^(case|default)\b(.*)', stripped)
        if case_match:
            # Save previous case
            if current_case is not None:
                cases.append((current_case, '\n'.join(current_body)))
            
            keyword = case_match.group(1)
            rest = case_match.group(2).strip()
            
            if keyword == 'case':
                # Check for pattern matching: case Type var ->
                # or case Type var:
                pattern_match = re.match(r'(\S+)\s+(\w+)\s*(->|:)', rest)
                if pattern_match:
                    type_name = pattern_match.group(1)
                    var_name = pattern_match.group(2)
                    arrow = pattern_match.group(3)
                    current_case = ('pattern', type_name, var_name, arrow)
                else:
                    # Regular case (constants, enums)
                    current_case = ('constant', rest.rstrip('->:').strip())
            else:
                current_case = ('default', '')
            
            current_body = []
            in_case = True
        elif in_case:
            current_body.append(line)
    
    # Don't forget the last case
    if current_case is not None:
        cases.append((current_case, '\n'.join(current_body)))
    
    return cases

def convert_case_body(body, var_name, is_expression, arrow_type):
    """Convert case body to if-else body."""
    body = body.strip()
    if not body:
        return []
    
    lines = []
    if is_expression and arrow_type == '->':
        # Expression after ->, might end with ; or ,
        body = body.rstrip(';,')
        if body:
            lines.append(body)
    else:
        # Statement block with :
        # Remove trailing break if present
        body_lines = body.split('\n')
        for bl in body_lines:
            bl = bl.strip()
            if bl and bl != 'break;':
                lines.append(bl)
    
    return lines

def rewrite_switch_in_text(text):
    """Rewrite all pattern matching switches in the text."""
    result = text
    offset = 0
    
    while True:
        # Find next switch
        switch_pos = result.find('switch (', offset)
        if switch_pos == -1:
            break
        
        # Check if it's a switch expression (has ->) or statement
        # Look ahead to find the opening brace
        brace_pos = result.find('{', switch_pos)
        if brace_pos == -1:
            offset = switch_pos + 1
            continue
        
        brace_end = find_matching_brace(result, brace_pos)
        if brace_end == -1:
            offset = switch_pos + 1
            continue
        
        # Check if this switch has pattern matching
        switch_block = result[switch_pos:brace_end + 1]
        if not re.search(r'case\s+\w+\s+\w+\s*(->|:)', switch_block):
            offset = brace_end + 1
            continue
        
        # Extract subject
        subject = extract_switch_subject(result, switch_pos)
        if subject is None:
            offset = brace_end + 1
            continue
        
        # Get indentation
        line_start = result.rfind('\n', 0, switch_pos) + 1
        indent = result[line_start:switch_pos]
        
        # Parse cases
        cases = parse_switch_cases(result, brace_pos, brace_end)
        
        # Check if it's an expression (assigned or returned)
        before_switch = result[:switch_pos].rstrip()
        is_expression = False
        is_return = False
        assign_target = None
        
        if before_switch.endswith('return '):
            is_expression = True
            is_return = True
        else:
            # Check for assignment
            assign_match = re.search(r'(\w+(?:\.\w+)?)\s*=\s*$', before_switch)
            if assign_match:
                is_expression = True
                assign_target = assign_match.group(1)
        
        # Generate if-else chain
        new_code_lines = []
        first = True
        
        for i, (case_info, body) in enumerate(cases):
            case_type = case_info[0]
            
            if case_type == 'pattern':
                _, type_name, var_name, arrow = case_info
                
                if first:
                    new_code_lines.append(f"{indent}if ({subject} instanceof {type_name}) {{")
                else:
                    new_code_lines.append(f"{indent}else if ({subject} instanceof {type_name}) {{")
                
                new_code_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {subject};")
                
                body_lines = convert_case_body(body, var_name, is_expression, arrow)
                for bl in body_lines:
                    if is_expression and is_return:
                        new_code_lines.append(f"{indent}    return {bl};")
                    elif is_expression and assign_target:
                        new_code_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_code_lines.append(f"{indent}    {bl}")
                
                new_code_lines.append(f"{indent}}}")
                first = False
            
            elif case_type == 'constant':
                _, const_val = case_info
                if first:
                    new_code_lines.append(f"{indent}if ({subject} == {const_val}) {{")
                else:
                    new_code_lines.append(f"{indent}else if ({subject} == {const_val}) {{")
                
                body_lines = convert_case_body(body, None, is_expression, '->')
                for bl in body_lines:
                    if is_expression and is_return:
                        new_code_lines.append(f"{indent}    return {bl};")
                    elif is_expression and assign_target:
                        new_code_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_code_lines.append(f"{indent}    {bl}")
                
                new_code_lines.append(f"{indent}}}")
                first = False
            
            elif case_type == 'default':
                if first:
                    new_code_lines.append(f"{indent}{{")
                else:
                    new_code_lines.append(f"{indent}else {{")
                
                body_lines = convert_case_body(body, None, is_expression, '->')
                for bl in body_lines:
                    if is_expression and is_return:
                        new_code_lines.append(f"{indent}    return {bl};")
                    elif is_expression and assign_target:
                        new_code_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_code_lines.append(f"{indent}    {bl}")
                
                new_code_lines.append(f"{indent}}}")
        
        # Add semicolon if it was an expression assignment
        if is_expression and assign_target and not new_code_lines[-1].endswith(';'):
            new_code_lines[-1] = new_code_lines[-1] + ';'
        
        new_code = '\n'.join(new_code_lines)
        
        # Replace the switch block
        result = result[:switch_pos] + new_code + result[brace_end + 1:]
        
        # Continue searching after the replacement
        offset = switch_pos + len(new_code)
    
    return result

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    original = content
    content = rewrite_switch_in_text(content)
    
    if content != original:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Rewrote: {filepath}")
            return True
        except Exception as e:
            print(f"Error writing {filepath}: {e}")
            return False
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
            import traceback
            traceback.print_exc()
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()