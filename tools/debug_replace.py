#!/usr/bin/env python3
"""
Debug version to trace replacement bounds.
"""

import re

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

def detect_context_and_bounds(text, switch_pos):
    i = switch_pos - 1
    while i >= 0 and text[i].isspace():
        i -= 1
    
    cast_text = None
    cast_end_pos = None
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
    
    if i >= 5 and text[i-5:i+1] == 'return':
        if i == 5 or not (text[i-6].isalnum() or text[i-6] == '_'):
            context_type = 'return'
            replace_start = i - 5
    
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
    
    return context_type, assign_target, replace_start, cast_text

def find_replace_end(text, brace_end, context_type, cast_text):
    i = brace_end + 1
    while i < len(text) and text[i].isspace():
        i += 1
    
    print(f"  find_replace_end: brace_end={brace_end}, start_i={i}, char='{text[i] if i<len(text) else 'EOF'}'")
    
    if context_type in ('cast_return', 'cast_statement') and cast_text:
        if i < len(text) and text[i] == ')':
            print(f"  consuming cast closing ) at {i}")
            i += 1
            while i < len(text) and text[i].isspace():
                i += 1
    
    if i < len(text) and text[i] == ';':
        print(f"  consuming semicolon at {i}")
        i += 1
    
    print(f"  final replace_end={i}")
    return i

def generate_if_else(cases, context_type, assign_target, indent, subject, cast_text):
    lines = []
    first = True
    
    for case in cases:
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
                    if cast_text:
                        lines.append(f"{indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    {bl};")
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
                    if cast_text:
                        lines.append(f"{indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    {bl};")
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
                    if cast_text:
                        lines.append(f"{indent}    return {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    return {bl};")
                elif context_type == 'assign':
                    lines.append(f"{indent}    {assign_target} = {bl};")
                elif context_type == 'cast_statement':
                    if cast_text:
                        lines.append(f"{indent}    {cast_text}{bl};")
                    else:
                        lines.append(f"{indent}    {bl};")
                else:
                    lines.append(f"{indent}    {bl}")
            
            lines.append(f"{indent}}}")
    
    return '\n'.join(lines)

# Test on NbtOps.java
with open('android/app/src/main/java/net/minecraft/nbt/NbtOps.java', 'r') as f:
    content = f.read()

switch_pos = content.find('switch (')
print(f"switch_pos={switch_pos}")

brace_pos = content.find('{', switch_pos)
brace_end = find_matching_brace(content, brace_pos)
print(f"brace_pos={brace_pos}, brace_end={brace_end}")

subject = extract_switch_subject(content, switch_pos)
print(f"subject='{subject}'")

indent = get_indent(content, switch_pos)
print(f"indent='{indent}'")

context_type, assign_target, replace_start, cast_text = detect_context_and_bounds(content, switch_pos)
print(f"context_type={context_type}, replace_start={replace_start}, cast_text={cast_text}")

replace_end = find_replace_end(content, brace_end, context_type, cast_text)
print(f"replace_end={replace_end}")

print(f"\nText being replaced (replace_start to replace_end):")
print(repr(content[replace_start:replace_end]))

print(f"\nText before replace_start (last 50 chars):")
print(repr(content[max(0,replace_start-50):replace_start]))

print(f"\nText after replace_end (first 50 chars):")
print(repr(content[replace_end:replace_end+50]))

cases = parse_switch_cases(content, brace_pos, brace_end)
new_code = generate_if_else(cases, context_type, assign_target, indent, subject, cast_text)
print(f"\nNew code:")
print(new_code)