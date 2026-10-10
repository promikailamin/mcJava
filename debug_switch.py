#!/usr/bin/env python3
"""
Debug version to understand the replacement bounds.
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
    return text[line_start:pos]

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
    
    print(f"  switch_pos={switch_pos}, char at i={i}: '{text[i]}'")
    print(f"  text before switch: ...{text[max(0,i-30):switch_pos]}...")
    
    cast_text = None
    if i >= 0 and text[i] == ')':
        paren = 1
        j = i - 1
        while j >= 0 and paren > 0:
            if text[j] == ')':
                paren += 1
            elif text[j] == '(':
                paren -= 1
            j -= 1
        cast_text = text[j+1:i+1]
        print(f"  found cast: '{cast_text}'")
        if not re.match(r'^\(\s*[\w\?\<\>\[\],\s]+\s*\)$', cast_text):
            cast_text = None
            print(f"  cast rejected")
        else:
            i = j
            while i >= 0 and text[i].isspace():
                i -= 1
            print(f"  after cast, i={i}, char='{text[i]}'")
    
    context_type = 'statement'
    assign_target = None
    replace_start = switch_pos
    
    if i >= 5 and text[i-5:i+1] == 'return':
        if i == 5 or not (text[i-6].isalnum() or text[i-6] == '_'):
            context_type = 'return'
            replace_start = i - 5
            print(f"  found return at {replace_start}")
    
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
                print(f"  found assignment target='{assign_target}' at {replace_start}")
                break
            elif ch == ';' and paren_count == 0:
                break
            elif ch == '{' and paren_count == 0:
                break
            j -= 1
    
    if cast_text and context_type == 'return':
        context_type = 'cast_return'
        print(f"  cast_return context")
    elif cast_text and context_type == 'statement':
        context_type = 'cast_statement'
        replace_start = j + 1
        print(f"  cast_statement context, replace_start={replace_start}")
    
    print(f"  final: context={context_type}, replace_start={replace_start}, cast_text={cast_text}")
    return context_type, assign_target, replace_start, cast_text

# Test on NbtOps.java
with open('android/app/src/main/java/net/minecraft/nbt/NbtOps.java', 'r') as f:
    content = f.read()

switch_pos = content.find('switch (')
print(f"First switch at {switch_pos}")
print(f"Context: ...{content[max(0,switch_pos-50):switch_pos+50]}...")

context_type, assign_target, replace_start, cast_text = detect_context_and_bounds(content, switch_pos)

print(f"\nreplace_start={replace_start}")
print(f"text to replace: '{content[replace_start:switch_pos]}'")
print(f"switch text: '{content[switch_pos:switch_pos+20]}'")