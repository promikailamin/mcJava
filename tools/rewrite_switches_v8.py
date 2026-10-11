#!/usr/bin/env python3
"""
Regex-based script to rewrite pattern matching switch expressions to if-else chains.
Fixed context detection and replacement bounds for casts.
"""

import re
from pathlib import Path

def find_matching_brace(text, start):
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
    line_start = text.rfind('\n', 0, pos) + 1
    line_prefix = text[line_start:pos]
    # Extract only leading whitespace
    match = re.match(r'^(\s*)', line_prefix)
    return match.group(1) if match else ''

def parse_switch_cases(text, brace_start, brace_end):
    cases_content = text[brace_start + 1:brace_end]
    cases = []
    
    positions = []
    i = 0
    while i < len(cases_content):
        if cases_content[i:].startswith('case'):
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
    
    positions.append(('end', len(cases_content)))
    
    for idx, (kind, pos) in enumerate(positions[:-1]):
        next_pos = positions[idx + 1][1]
        case_text = cases_content[pos:next_pos]
        
        if kind == 'case':
            after_case = case_text[4:].lstrip()
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
            
            # Check for pattern matching with optional guard (when clause)
            # Patterns:
            # 1. Simple: TypeName varName
            # 2. Record: TypeName(ComponentType varName) or TypeName(varName)
            # 3. With guard: ... when condition
            
            # First, check for guard clause
            guard_condition = None
            when_pos = pattern.find(' when ')
            if when_pos != -1:
                guard_condition = pattern[when_pos + 6:].strip()
                pattern = pattern[:when_pos].strip()
            
            # Check for pattern matching: Type var OR Type(type var) OR Type(var)
            pattern_match = re.match(r'^(\S+)\s+(\w+)$', pattern)
            record_pattern_match = re.match(r'^(\w+)\((\w+)(?:\s+(\w+))?\)$', pattern)
            
            if pattern_match:
                case_info = {
                    'type': 'pattern',
                    'type_name': pattern_match.group(1),
                    'var_name': pattern_match.group(2),
                    'arrow_type': arrow_type,
                    'body': body,
                    'pattern_kind': 'simple'
                }
                if guard_condition:
                    case_info['guard'] = guard_condition
                cases.append(case_info)
            elif record_pattern_match:
                type_name = record_pattern_match.group(1)
                component_type = record_pattern_match.group(2)
                var_name = record_pattern_match.group(3) if record_pattern_match.group(3) else component_type.lower()
                case_info = {
                    'type': 'pattern',
                    'type_name': type_name,
                    'var_name': var_name,
                    'component_type': component_type,
                    'arrow_type': arrow_type,
                    'body': body,
                    'pattern_kind': 'record'
                }
                if guard_condition:
                    case_info['guard'] = guard_condition
                cases.append(case_info)
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
    body = body.strip()
    if not body:
        return []
    
    # Handle block bodies (starting with {) for both arrow types
    if body.startswith('{'):
        # Block body - extract statements inside braces
        # Find matching closing brace
        brace_count = 0
        in_string = False
        string_char = None
        escaped = False
        end_pos = -1
        for i, ch in enumerate(body):
            if escaped:
                escaped = False
                continue
            if ch == '\\' and in_string:
                escaped = True
                continue
            if not in_string:
                if ch in '"\'':
                    in_string = True
                    string_char = ch
                elif ch == '{':
                    brace_count += 1
                elif ch == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        end_pos = i
                        break
            else:
                if ch == string_char:
                    in_string = False
                    string_char = None
        
        if end_pos > 0:
            # Extract content between braces
            block_content = body[1:end_pos].strip()
            lines = []
            for line in block_content.split('\n'):
                line = line.strip()
                if line:
                    lines.append(('stmt', line))  # All lines in block are statements initially
            # Mark the last yield as expression
            for i in range(len(lines) - 1, -1, -1):
                if lines[i][1].strip().startswith('yield '):
                    lines[i] = ('expr', lines[i][1])
                    break
            return lines
    
    if arrow_type == '->':
        body = body.rstrip(';,')
        return [('expr', body)] if body else []
    else:
        lines = []
        for line in body.split('\n'):
            line = line.strip()
            if line and line != 'break;':
                lines.append(('stmt', line))
        return lines

def detect_context_and_bounds(text, switch_pos):
    """Detect context and find the exact bounds to replace."""
    # Look backwards from the 'switch' keyword
    i = switch_pos - 1
    while i >= 0 and text[i].isspace():
        i -= 1
    
    cast_text = None
    cast_end_pos = None
    # Check for cast like (U) - could be at i (if pattern is )(switch) or at i-1 (if pattern is (U)(switch))
    if i >= 0 and text[i] == ')':
        cast_end_pos = i
    elif i >= 1 and text[i] == '(' and text[i-1] == ')':
        cast_end_pos = i - 1
    
    if cast_end_pos is not None:
        paren = 1
        j = cast_end_pos - 1
        while j >= 0 and paren > 0:
            if text[j] == ')':
                paren += 1
            elif text[j] == '(':
                paren -= 1
            j -= 1
        cast_text = text[j+1:cast_end_pos+1]
        if not re.match(r'^\(\s*[\w\?\<\>\[\],\s]+\s*\)$', cast_text):
            cast_text = None
        else:
            i = j
            while i >= 0 and text[i].isspace():
                i -= 1
    
    context_type = 'statement'
    assign_target = None
    replace_start = switch_pos
    
    # Check for return
    if i >= 5 and text[i-5:i+1] == 'return':
        if i == 5 or not (text[i-6].isalnum() or text[i-6] == '_'):
            context_type = 'return'
            replace_start = i - 5
    
    # Check for assignment
    if context_type == 'statement':
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
                assign_target = text[k+1:end]
                context_type = 'assign'
                replace_start = k + 1
                break
            elif ch == ';' and paren_count == 0:
                break
            elif ch == '{' and paren_count == 0:
                break
            j -= 1
    
    if cast_text and context_type == 'return':
        context_type = 'cast_return'
    elif cast_text and context_type == 'statement':
        context_type = 'cast_statement'
        replace_start = j + 1
    
    # Adjust replace_start to beginning of line (after last newline)
    # This ensures the generated code aligns with the base indentation
    line_start = text.rfind('\n', 0, replace_start) + 1
    replace_start = line_start
    
    return context_type, assign_target, replace_start, cast_text

def find_replace_end(text, brace_end, context_type, cast_text):
    """Find the end of the expression to replace."""
    i = brace_end + 1
    # Skip whitespace
    while i < len(text) and text[i].isspace():
        i += 1
    
    if context_type in ('cast_return', 'cast_statement') and cast_text:
        # For cast contexts, we need to consume the closing ) of the cast
        if i < len(text) and text[i] == ')':
            i += 1
            # Skip whitespace
            while i < len(text) and text[i].isspace():
                i += 1
    
    # Consume semicolon if present
    if i < len(text) and text[i] == ';':
        i += 1
    
    return i

def generate_if_else(cases, context_type, assign_target, base_indent, subject, cast_text):
    lines = []
    first = True
    
    for case in cases:
        if case['type'] == 'pattern':
            type_name = case['type_name']
            var_name = case['var_name']
            pattern_kind = case.get('pattern_kind', 'simple')
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                guard = case.get('guard')
                if guard:
                    lines.append(f"{base_indent}if ({subject} instanceof {type_name} && {guard}) {{")
                else:
                    lines.append(f"{base_indent}if ({subject} instanceof {type_name}) {{")
            else:
                guard = case.get('guard')
                if guard:
                    lines.append(f"{base_indent}else if ({subject} instanceof {type_name} && {guard}) {{")
                else:
                    lines.append(f"{base_indent}else if ({subject} instanceof {type_name}) {{")
            
            if pattern_kind == 'simple':
                lines.append(f"{base_indent}    {type_name} {var_name} = ({type_name}) {subject};")
            elif pattern_kind == 'record':
                # For record patterns like TypeName(ComponentType varName), extract the component
                component_type = case.get('component_type', var_name)
                # Assume the accessor method is the variable name (e.g., value() for byte value)
                lines.append(f"{base_indent}    {component_type} {var_name} = (({type_name}) {subject}).{var_name}();")
            
            for kind, bl in body_lines:
                # Check if body is a throw expression
                is_throw = bl.strip().startswith('throw ')
                # Check if body is a yield expression (from switch expressions)
                is_yield = bl.strip().startswith('yield ')
                if is_throw:
                    # throw is a statement, not an expression
                    lines.append(f"{base_indent}    {bl};")
                elif is_yield:
                    # yield in switch expressions becomes return in if-else
                    yield_expr = bl.strip()[6:].rstrip(';')
                    lines.append(f"{base_indent}    return {yield_expr};")
                elif kind == 'stmt':
                    # Statement (variable declaration, etc.)
                    lines.append(f"{base_indent}    {bl}")
                elif context_type in ('return', 'cast_return'):
                    if cast_text:
                        lines.append(f"{base_indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{base_indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{base_indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    {bl};")
                else:
                    lines.append(f"{base_indent}    {bl}")
            
            lines.append(f"{base_indent}}}")
            first = False
        
        elif case['type'] == 'constant':
            const_val = case['const_value']
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                lines.append(f"{base_indent}if ({subject} == {const_val}) {{")
            else:
                lines.append(f"{base_indent}else if ({subject} == {const_val}) {{")
            
            for kind, bl in body_lines:
                is_throw = bl.strip().startswith('throw ')
                is_yield = bl.strip().startswith('yield ')
                if is_throw:
                    lines.append(f"{base_indent}    {bl};")
                elif is_yield:
                    yield_expr = bl.strip()[6:].rstrip(';')
                    lines.append(f"{base_indent}    return {yield_expr};")
                elif kind == 'stmt':
                    lines.append(f"{base_indent}    {bl}")
                elif context_type in ('return', 'cast_return'):
                    if cast_text:
                        lines.append(f"{base_indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{base_indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{base_indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    {bl};")
                else:
                    lines.append(f"{base_indent}    {bl}")
            
            lines.append(f"{base_indent}}}")
            first = False
        
        elif case['type'] == 'default':
            body_lines = clean_body(case['body'], case['arrow_type'])
            
            if first:
                lines.append(f"{base_indent}{{")
            else:
                lines.append(f"{base_indent}else {{")
            
            for kind, bl in body_lines:
                is_throw = bl.strip().startswith('throw ')
                is_yield = bl.strip().startswith('yield ')
                if is_throw:
                    lines.append(f"{base_indent}    {bl};")
                elif is_yield:
                    yield_expr = bl.strip()[6:].rstrip(';')
                    lines.append(f"{base_indent}    return {yield_expr};")
                elif kind == 'stmt':
                    lines.append(f"{base_indent}    {bl}")
                elif context_type in ('return', 'cast_return'):
                    if cast_text:
                        lines.append(f"{base_indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{base_indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{base_indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{base_indent}    {bl};")
                else:
                    lines.append(f"{base_indent}    {bl}")
            
            lines.append(f"{base_indent}}}")
    
    return '\n'.join(lines)

def rewrite_file_content(content):
    # Quick pre-check: does this file have any pattern matching switch cases?
    # Pattern: case TypeName varName -> or case TypeName<...> varName ->
    if not re.search(r'case\s+(?:[\w\.]+\s*(?:<[^>]*>)?\s*)+\s+\w+\s*(?:->|:)', content):
        return content
    
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
        
        # Check for pattern matching in this specific switch
        switch_block = result[switch_pos:brace_end + 1]
        if not re.search(r'case\s+(?:[\w\.]+\s*(?:<[^>]*>)?\s*)+\s+\w+\s*(->|:)', switch_block):
            offset = brace_end + 1
            continue
        
        subject = extract_switch_subject(result, switch_pos)
        if subject is None:
            offset = brace_end + 1
            continue
        
        indent = get_indent(result, switch_pos)
        context_type, assign_target, replace_start, cast_text = detect_context_and_bounds(result, switch_pos)
        
        cases = parse_switch_cases(result, brace_pos, brace_end)
        new_code = generate_if_else(cases, context_type, assign_target, indent, subject, cast_text)
        
        # Find replace_end
        replace_end = find_replace_end(result, brace_end, context_type, cast_text)
        
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
            import traceback
            traceback.print_exc()
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