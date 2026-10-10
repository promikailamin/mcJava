#!/usr/bin/env python3
"""
Better script to rewrite pattern matching switch expressions to if-else chains.
Handles the specific patterns found in this codebase.
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

def extract_switch_subject(text, switch_pos):
    """Extract the expression being switched on."""
    paren_start = text.find('(', switch_pos)
    if paren_start == -1:
        return None
    
    count = 0
    for i, ch in enumerate(text[paren_start:], paren_start):
        if ch == '(':
            count += 1
        elif ch == ')':
            count -= 1
            if count == 0:
                return text[paren_start + 1:i].strip()
    return None

def get_indent(text, pos):
    """Get indentation at position."""
    line_start = text.rfind('\n', 0, pos) + 1
    return text[line_start:pos]

def parse_switch_cases(text, brace_start, brace_end):
    """Parse the cases inside a switch block."""
    cases_content = text[brace_start + 1:brace_end]
    cases = []
    
    # Use a state machine to parse cases
    i = 0
    while i < len(cases_content):
        # Skip whitespace
        while i < len(cases_content) and cases_content[i].isspace():
            i += 1
        if i >= len(cases_content):
            break
        
        # Check for case or default
        if cases_content[i:].startswith('case'):
            i += 4
            # Skip whitespace
            while i < len(cases_content) and cases_content[i].isspace():
                i += 1
            
            # Parse the case pattern
            # Look for -> or :
            arrow_pos = cases_content.find('->', i)
            colon_pos = cases_content.find(':', i)
            
            # Find the first of -> or :
            end_pos = -1
            arrow_type = None
            if arrow_pos != -1 and (colon_pos == -1 or arrow_pos < colon_pos):
                end_pos = arrow_pos
                arrow_type = '->'
            elif colon_pos != -1:
                end_pos = colon_pos
                arrow_type = ':'
            
            if end_pos == -1:
                break
            
            case_text = cases_content[i:end_pos].strip()
            
            # Check for pattern matching: Type var
            pattern_match = re.match(r'^(\S+)\s+(\w+)$', case_text)
            if pattern_match:
                case_type = 'pattern'
                type_name = pattern_match.group(1)
                var_name = pattern_match.group(2)
            else:
                case_type = 'constant'
                type_name = None
                var_name = None
                const_value = case_text
            
            # Now find the body until next case/default or end
            body_start = end_pos + (2 if arrow_type == '->' else 1)
            body_end = find_case_end(cases_content, body_start)
            
            body = cases_content[body_start:body_end].strip()
            
            if case_type == 'pattern':
                cases.append(('pattern', type_name, var_name, arrow_type, body))
            else:
                cases.append(('constant', const_value, arrow_type, body))
            
            i = body_end
        
        elif cases_content[i:].startswith('default'):
            i += 7
            # Skip whitespace
            while i < len(cases_content) and cases_content[i].isspace():
                i += 1
            
            # Expect : or ->
            if i < len(cases_content) and cases_content[i] in ':->':
                if cases_content[i:i+2] == '->':
                    arrow_type = '->'
                    i += 2
                else:
                    arrow_type = ':'
                    i += 1
            else:
                arrow_type = ':'
            
            # Skip whitespace
            while i < len(cases_content) and cases_content[i].isspace():
                i += 1
            
            body_start = i
            body_end = find_case_end(cases_content, body_start)
            body = cases_content[body_start:body_end].strip()
            
            cases.append(('default', arrow_type, body))
            i = body_end
        else:
            i += 1
    
    return cases

def find_case_end(text, start):
    """Find the end of a case body (next case, default, or end of switch)."""
    i = start
    brace_count = 0
    in_string = False
    string_char = None
    
    while i < len(text):
        ch = text[i]
        
        if not in_string:
            if ch in '"\'':
                in_string = True
                string_char = ch
            elif ch == '{':
                brace_count += 1
            elif ch == '}':
                if brace_count == 0:
                    # End of switch block
                    return i
                brace_count -= 1
            elif ch == 'c' and text[i:i+4] == 'case' and brace_count == 0:
                # Check if it's a keyword (preceded by whitespace or start)
                if i == start or text[i-1].isspace():
                    return i
            elif ch == 'd' and text[i:i+7] == 'default' and brace_count == 0:
                if i == start or text[i-1].isspace():
                    return i
        else:
            if ch == '\\':
                i += 2
                continue
            elif ch == string_char:
                in_string = False
                string_char = None
        
        i += 1
    
    return i

def clean_body(body, arrow_type):
    """Clean up case body."""
    body = body.strip()
    if not body:
        return []
    
    if arrow_type == '->':
        # Expression body - remove trailing ; or ,
        body = body.rstrip(';,')
        return [body] if body else []
    else:
        # Statement body - split by lines, remove break
        lines = []
        for line in body.split('\n'):
            line = line.strip()
            if line and line != 'break;':
                lines.append(line)
        return lines

def rewrite_switch_expression(text):
    """Rewrite all pattern matching switch expressions in text."""
    result = text
    offset = 0
    
    while True:
        switch_pos = result.find('switch (', offset)
        if switch_pos == -1:
            break
        
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
        
        subject = extract_switch_subject(result, switch_pos)
        if subject is None:
            offset = brace_end + 1
            continue
        
        indent = get_indent(result, switch_pos)
        
        # Check context: is this a return, assignment, or standalone?
        before_switch = result[:switch_pos].rstrip()
        is_return = before_switch.endswith('return')
        is_assignment = False
        assign_target = None
        
        if not is_return:
            assign_match = re.search(r'(\w+(?:\.\w+)?)\s*=\s*$', before_switch)
            if assign_match:
                is_assignment = True
                assign_target = assign_match.group(1)
        
        # Also check for: Type var = switch (...) {
        # or just: switch (...) { (statement)
        is_statement = not (is_return or is_assignment)
        
        cases = parse_switch_cases(result, brace_pos, brace_end)
        
        # Generate if-else chain
        new_lines = []
        first = True
        
        for case_info in cases:
            if case_info[0] == 'pattern':
                _, type_name, var_name, arrow_type, body = case_info
                body_lines = clean_body(body, arrow_type)
                
                if first:
                    new_lines.append(f"{indent}if ({subject} instanceof {type_name}) {{")
                else:
                    new_lines.append(f"{indent}else if ({subject} instanceof {type_name}) {{")
                
                new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {subject};")
                
                for bl in body_lines:
                    if is_return:
                        new_lines.append(f"{indent}    return {bl};")
                    elif is_assignment:
                        new_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_lines.append(f"{indent}    {bl}")
                
                new_lines.append(f"{indent}}}")
                first = False
            
            elif case_info[0] == 'constant':
                _, const_val, arrow_type, body = case_info
                body_lines = clean_body(body, arrow_type)
                
                # For constants, use == for primitives/enums
                if first:
                    new_lines.append(f"{indent}if ({subject} == {const_val}) {{")
                else:
                    new_lines.append(f"{indent}else if ({subject} == {const_val}) {{")
                
                for bl in body_lines:
                    if is_return:
                        new_lines.append(f"{indent}    return {bl};")
                    elif is_assignment:
                        new_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_lines.append(f"{indent}    {bl}")
                
                new_lines.append(f"{indent}}}")
                first = False
            
            elif case_info[0] == 'default':
                _, arrow_type, body = case_info
                body_lines = clean_body(body, arrow_type)
                
                if first:
                    new_lines.append(f"{indent}{{")
                else:
                    new_lines.append(f"{indent}else {{")
                
                for bl in body_lines:
                    if is_return:
                        new_lines.append(f"{indent}    return {bl};")
                    elif is_assignment:
                        new_lines.append(f"{indent}    {assign_target} = {bl};")
                    else:
                        new_lines.append(f"{indent}    {bl}")
                
                new_lines.append(f"{indent}}}")
        
        # For statement switches, we don't need a trailing semicolon
        # For expressions, the last line might need one if it's an assignment
        if is_assignment:
            # The assignment is handled inside each branch
            pass
        
        new_code = '\n'.join(new_lines)
        
        # Replace
        result = result[:switch_pos] + new_code + result[brace_end + 1:]
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
    content = rewrite_switch_expression(content)
    
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