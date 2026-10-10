#!/usr/bin/env python3
"""
Regex-based script to rewrite pattern matching switch expressions to if-else chains.
Handles the specific patterns in the mcJava codebase.
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
    
    # Split by case/default - use a more careful approach
    # Find all case/default positions
    positions = []
    i = 0
    while i < len(cases_content):
        if cases_content[i:].startswith('case'):
            # Check word boundary
            if i == 0 or cases_content[i-1].isspace():
                positions.append(('case', i))
                i += 4
                continue
        elif cases_content[i:].startswith('default'):
            if i == 0 or cases_content[i-1].isspace():
                positions.append(('default', i))
                i += 7
                continue
        i += 1
    
    # Also add end position
    positions.append(('end', len(cases_content)))
    
    for idx, (kind, pos) in enumerate(positions[:-1]):
        next_pos = positions[idx + 1][1]
        case_text = cases_content[pos:next_pos]
        
        if kind == 'case':
            # Parse case
            after_case = case_text[4:].lstrip()
            # Find -> or :
            arrow_pos = after_case.find('->')
            colon_pos = after_case.find(':')
            
            end_pos = -1
            arrow_type = None
            if arrow_pos != -1 and (colon_pos == -1 or arrow_pos < colon_pos):
                end_pos = arrow_pos
                arrow_type = '->'
            elif colon_pos != -1:
                end_pos = colon_pos
                arrow_type = ':'
            
            if end_pos == -1:
                continue
            
            pattern = after_case[:end_pos].strip()
            body = after_case[end_pos + (2 if arrow_type == '->' else 1):].strip()
            
            # Check for pattern matching
            pattern_match = re.match(r'^(\S+)\s+(\w+)$', pattern)
            if pattern_match:
                cases.append({
                    'type': 'pattern',
                    'type_name': pattern_match.group(1),
                    'var_name': pattern_match.group(2),
                    'arrow_type': arrow_type,
                    'body': body
                })
            else:
                cases.append({
                    'type': 'constant',
                    'const_value': pattern,
                    'arrow_type': arrow_type,
                    'body': body
                })
        
        elif kind == 'default':
            after_default = case_text[7:].lstrip()
            arrow_type = ':'
            if after_default.startswith('->'):
                arrow_type = '->'
                body = after_default[2:].strip()
            elif after_default.startswith(':'):
                body = after_default[1:].strip()
            else:
                body = after_default.strip()
            
            cases.append({
                'type': 'default',
                'arrow_type': arrow_type,
                'body': body
            })
    
    return cases

def clean_body(body, arrow_type):
    """Clean case body."""
    body = body.strip()
    if not body:
        return []
    
    if arrow_type == '->':
        body = body.rstrip(';,')
        return [body] if body else []
    else:
        lines = []
        for line in body.split('\n'):
            line = line.strip()
            if line and line != 'break;':
                lines.append(line)
        return lines

def detect_context(text, switch_pos):
    """Detect the context of a switch expression."""
    # Look backwards from switch
    i = switch_pos - 1
    while i >= 0 and text[i].isspace():
        i -= 1
    
    # Check for cast like (U)
    if i >= 0 and text[i] == ')':
        # Find matching (
        paren = 1
        j = i - 1
        while j >= 0 and paren > 0:
            if text[j] == ')':
                paren += 1
            elif text[j] == '(':
                paren -= 1
            j -= 1
        cast_text = text[j+1:i+1]
        if re.match(r'^\(\s*[\w\?\<\>\[\],\s]+\s*\)$', cast_text):
            # Continue looking before cast
            i = j
            while i >= 0 and text[i].isspace():
                i -= 1
    
    # Check for return
    if i >= 5 and text[i-5:i+1] == 'return':
        if i == 5 or not (text[i-6].isalnum() or text[i-6] == '_'):
            # Check if there's a cast between return and switch
            between = text[i+1:switch_pos]
            if re.search(r'\(\s*[\w\?\<\>\[\],\s]+\s*\)', between):
                return 'cast_return', None
            return 'return', None
    
    # Check for assignment
    j = i
    paren_count = 0
    while j >= 0:
        ch = text[j]
        if ch == ')':
            paren_count += 1
        elif ch == '(':
            paren_count -= 1
        elif ch == '=' and paren_count == 0:
            k = j - 1
            while k >= 0 and text[k].isspace():
                k -= 1
            end = k + 1
            while k >= 0 and (text[k].isalnum() or text[k] in '._'):
                k -= 1
            target = text[k+1:end]
            return 'assign', target
        elif ch == ';' and paren_count == 0:
            break
        elif ch == '{' and paren_count == 0:
            break
        j -= 1
    
    return 'statement', None

def generate_if_else(switch_info, context_type, assign_target, indent, subject):
    """Generate if-else chain."""
    lines = []
    first = True
    
    for case in switch_info['cases']:
        if case['type'] == 'pattern':
            type_name = case['type_name']
            var_name = case['var_name']
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                lines.append(f"{indent}if ({subject} instanceof {type_name}) {{")
            else:
                lines.append(f"{indent}else if ({subject} instanceof {type_name}) {{")
            
            lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {subject};")
            
            for bl in body_lines:
                if context_type in ('return', 'cast_return'):
                    lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                else:
                    lines.append(f"{indent}    {bl}")
            
            lines.append(f"{indent}}}")
            first = False
        
        elif case['type'] == 'constant':
            const_val = case['const_value']
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                lines.append(f"{indent}if ({subject} == {const_val}) {{")
            else:
                lines.append(f"{indent}else if ({subject} == {const_val}) {{")
            
            for bl in body_lines:
                if context_type in ('return', 'cast_return'):
                    lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                else:
                    lines.append(f"{indent}    {bl}")
            
            lines.append(f"{indent}}}")
            first = False
        
        elif case['type'] == 'default':
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                lines.append(f"{indent}{{")
            else:
                lines.append(f"{indent}else {{")
            
            for bl in body_lines:
                if context_type in ('return', 'cast_return'):
                    lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                else:
                    lines.append(f"{indent}    {bl}")
            
            lines.append(f"{indent}}}")
    
    return '\n'.join(lines)

def rewrite_file_content(content):
    """Rewrite all pattern matching switches in content."""
    result = content
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
        context_type, assign_target = detect_context(result, switch_pos)
        
        # Check if there's a cast wrapping that we need to preserve
        # Look for (Type) before switch
        cast_prefix = ''
        i = switch_pos - 1
        while i >= 0 and result[i].isspace():
            i -= 1
        if i >= 0 and result[i] == ')':
            paren = 1
            j = i - 1
            while j >= 0 and paren > 0:
                if result[j] == ')':
                    paren += 1
                elif result[j] == '(':
                    paren -= 1
                j -= 1
            cast_text = result[j+1:i+1]
            if re.match(r'^\(\s*[\w\?\<\>\[\],\s]+\s*\)$', cast_text):
                cast_prefix = cast_text
                # We'll include the cast in each return
                context_type = 'cast_return'
        
        cases = parse_switch_cases(result, brace_pos, brace_end)
        new_code = generate_if_else({'cases': cases}, context_type, assign_target, indent, subject)
        
        # Determine what to replace
        # Find the start of the expression (might include return, assignment, cast)
        replace_start = switch_pos
        if context_type in ('return', 'cast_return'):
            # Find 'return'
            rpos = result.rfind('return', 0, switch_pos)
            if rpos != -1:
                # Check word boundary
                if rpos == 0 or not (result[rpos-1].isalnum() or result[rpos-1] == '_'):
                    replace_start = rpos
        elif context_type == 'assign':
            # Find the assignment target
            eq_pos = result.rfind('=', 0, switch_pos)
            if eq_pos != -1:
                k = eq_pos - 1
                while k >= 0 and result[k].isspace():
                    k -= 1
                while k >= 0 and (result[k].isalnum() or result[k] in '._'):
                    k -= 1
                replace_start = k + 1
        
        # Find end (after brace, possibly semicolon)
        replace_end = brace_end + 1
        while replace_end < len(result) and result[replace_end].isspace():
            replace_end += 1
        if replace_end < len(result) and result[replace_end] == ';':
            replace_end += 1
        
        # Replace
        result = result[:replace_start] + new_code + result[replace_end:]
        offset = replace_start + len(new_code)
    
    return result

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    original = content
    content = rewrite_file_content(content)
    
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