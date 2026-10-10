#!/usr/bin/env python3
"""
Convert pattern matching switches on sealed types to if-else chains
to avoid MatchException on Android.
"""

import re
import sys
from pathlib import Path


# Known record types and their component accessors
RECORD_PATTERNS = {
    # NBT Tags
    'ByteTag': ('value', 'byte'),
    'ShortTag': ('value', 'short'),
    'IntTag': ('value', 'int'),
    'LongTag': ('value', 'long'),
    'FloatTag': ('value', 'float'),
    'DoubleTag': ('value', 'double'),
    'StringTag': ('value', 'String'),
    # Add more as needed
}

# Singleton types that don't need variable binding
SINGLETON_TYPES = {'EndTag'}


def parse_case(case_str):
    """Parse a case string and return (type_name, var_name, component_info, body, is_expression)."""
    case_str = case_str.strip()
    
    # Match patterns:
    # TypeName(Type varName) -> expr  (record pattern)
    # TypeName varName -> expr        (type pattern)
    # TypeName(Type varName): body    (record pattern)
    # TypeName varName: body          (type pattern)
    
    # Arrow syntax (expression) - with parentheses: TypeName(Type varName) -> expr
    arrow_paren_match = re.match(
        r'([A-Z][a-zA-Z_]*)\s*\(\s*([a-zA-Z_][a-zA-Z0-9_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\)\s*->\s*(.+)', 
        case_str, re.DOTALL
    )
    if arrow_paren_match:
        type_name = arrow_paren_match.group(1)
        comp_type = arrow_paren_match.group(2)
        var_name = arrow_paren_match.group(3)
        expr = arrow_paren_match.group(4).strip().rstrip(',').rstrip(';')
        return type_name, var_name, (comp_type, 'record'), expr, True
    
    # Arrow syntax (expression) - without parentheses: TypeName varName -> expr
    arrow_match = re.match(
        r'([A-Z][a-zA-Z_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*->\s*(.+)', 
        case_str, re.DOTALL
    )
    if arrow_match:
        type_name = arrow_match.group(1)
        var_name = arrow_match.group(2)
        expr = arrow_match.group(3).strip().rstrip(',').rstrip(';')
        return type_name, var_name, None, expr, True
    
    # Colon syntax (statement) - with parentheses: TypeName(Type varName): body
    colon_paren_match = re.match(
        r'([A-Z][a-zA-Z_]*)\s*\(\s*([a-zA-Z_][a-zA-Z0-9_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\)\s*:\s*(.+)', 
        case_str, re.DOTALL
    )
    if colon_paren_match:
        type_name = colon_paren_match.group(1)
        comp_type = colon_paren_match.group(2)
        var_name = colon_paren_match.group(3)
        body = colon_paren_match.group(4).strip()
        return type_name, var_name, (comp_type, 'record'), body, False
    
    # Colon syntax (statement) - without parentheses: TypeName varName: body
    colon_match = re.match(
        r'([A-Z][a-zA-Z_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*(.+)', 
        case_str, re.DOTALL
    )
    if colon_match:
        type_name = colon_match.group(1)
        var_name = colon_match.group(2)
        body = colon_match.group(3).strip()
        return type_name, var_name, None, body, False
    
    return None, None, None, None, False


def convert_switch_expression_to_statements(prefix, switch_var, cases, default_case, base_indent, is_return_statement):
    """
    Convert a switch expression to if-else statements.
    If is_return_statement is True, generate returns in each branch.
    Otherwise, generate a variable assignment.
    """
    result_parts = []
    first = True
    
    for case in cases:
        type_name, var_name, comp_info, body, is_expr = parse_case(case)
        if type_name is None:
            print(f"Warning: Could not parse case: {case[:100]}")
            continue
        
        is_singleton = type_name in SINGLETON_TYPES
        
        if comp_info:  # Record pattern
            comp_type, _ = comp_info
            accessor = RECORD_PATTERNS.get(type_name, (var_name, None))[0]
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {comp_type} {var_name} = (({type_name}) {switch_var}).{accessor}();')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {comp_type} {var_name} = (({type_name}) {switch_var}).{accessor}();')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
        elif is_singleton:  # Singleton type pattern
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name}) {{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name}) {{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
        else:  # Type pattern - use instanceof pattern matching
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name} {var_name}) {{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name} {var_name}) {{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {body};')
                else:
                    result_parts.append(f'{base_indent}    result = {body};')
                result_parts.append(f'{base_indent}}}')
    
    if default_case:
        default_body = default_case.strip()
        # Remove only the switch arrow (-> or :) at the beginning, not lambda arrows
        # Find the first -> or : that's not inside parentheses
        default_body = re.sub(r'^(?:->|:)\s*', '', default_body)
        default_body = default_body.rstrip(',').rstrip(';')
        if default_body and default_body != 'throw new IllegalStateException("Unexpected value")':
            if first:
                result_parts.append(f'{base_indent}{{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {default_body};')
                else:
                    result_parts.append(f'{base_indent}    result = {default_body};')
                result_parts.append(f'{base_indent}}}')
            else:
                result_parts.append(f'{base_indent}else {{')
                if is_return_statement:
                    result_parts.append(f'{base_indent}    return {default_body};')
                else:
                    result_parts.append(f'{base_indent}    result = {default_body};')
                result_parts.append(f'{base_indent}}}')
        elif default_body == 'throw new IllegalStateException("Unexpected value")':
            if not first:
                result_parts.append(f'{base_indent}else {{')
                result_parts.append(f'{base_indent}    throw new IllegalStateException("Unexpected value");')
                result_parts.append(f'{base_indent}}}')
    
    if not first and not default_case:
        result_parts.append(f'{base_indent}else {{')
        result_parts.append(f'{base_indent}    throw new IllegalStateException("Unexpected value");')
        result_parts.append(f'{base_indent}}}')
    
    return '\n'.join(result_parts)


def convert_pattern_switch_to_ifelse(content: str) -> str:
    """
    Convert switch expressions/statements with pattern matching to if-else chains.
    """
    result = content
    offset = 0
    
    while True:
        # Find switch (var) {
        match = re.search(r'switch\s*\(([^)]+)\)\s*\{', result[offset:])
        if not match:
            break
        
        switch_var = match.group(1).strip()
        start_pos = offset + match.start()
        brace_start = offset + match.end()  # Position after {
        
        # Find matching closing brace
        brace_count = 1
        pos = brace_start
        while pos < len(result) and brace_count > 0:
            if result[pos] == '{':
                brace_count += 1
            elif result[pos] == '}':
                brace_count -= 1
            pos += 1
        
        if brace_count != 0:
            offset = offset + match.end()
            continue
        
        switch_body = result[brace_start:pos-1]  # Exclude closing }
        full_switch = result[start_pos:pos]
        
        # Check if pattern matching switch (case TypeName varName or case TypeName(varName))
        if not re.search(r'case\s+[A-Z][a-zA-Z_]*\s*[\(\s][a-z]', switch_body):
            offset = offset + match.end()
            continue
        
        # Check if it's an expression (->) or statement (:)
        # Look for -> at case level, not inside nested code
        # A switch expression has cases like "case Type var -> expr" at the top level
        # A switch statement has cases like "case Type var: { ... }" or "case Type var -> expr" but with statements
        # We'll check if the first case uses -> at the top level
        first_case_match = re.search(r'case\s+[A-Z][a-zA-Z_]*\s*[\(\s][a-z]', switch_body)
        is_expression = False
        if first_case_match:
            # Look at the first case to see if it uses -> or :
            first_case_start = first_case_match.start()
            # Find the -> or : for this case
            after_case = switch_body[first_case_start:]
            # Look for -> before any nested { or ;
            arrow_pos = after_case.find('->')
            colon_pos = after_case.find(':')
            if arrow_pos != -1 and (colon_pos == -1 or arrow_pos < colon_pos):
                # Check if -> is at top level (not inside nested parens/braces)
                # Simple heuristic: if -> appears before first { in the case
                brace_pos = after_case.find('{')
                if brace_pos == -1 or arrow_pos < brace_pos:
                    is_expression = True
        
        # Determine indentation from the line containing 'switch'
        line_start = result.rfind('\n', 0, start_pos) + 1
        indent_line = result[line_start:start_pos]
        indent_match = re.match(r'^(\s*)', indent_line)
        base_indent = indent_match.group(1) if indent_match else ''
        
        # Check if this switch is a return statement: "return switch (...) { ... };"
        before_switch = result[:start_pos].rstrip()
        # Check if it ends with return, possibly followed by cast like (U)( or just return
        is_return_statement = (before_switch.endswith('return') or 
                               before_switch.endswith('return ') or 
                               re.search(r'return\s*\(\s*[A-Z][a-zA-Z_]*\s*\)\s*\(\s*$', before_switch) or
                               re.search(r'return\s*\(\s*[A-Z][a-zA-Z_]*\s*\)\s*$', before_switch))
        
        # Check if wrapped in parentheses/cast like (U)(switch ...)
        wrap_start = start_pos
        found_wrap = False
        before_switch_full = result[:start_pos]
        cast_match = re.search(r'\(\s*([A-Z][a-zA-Z_]*)\s*\)\s*\(\s*$', before_switch_full)
        if cast_match:
            wrap_start = cast_match.start()
            found_wrap = True
        
        if found_wrap:
            prefix = result[:wrap_start]
            suffix = result[pos:]
            # Remove the trailing ); or ) from the suffix if present
            suffix = re.sub(r'^\s*\)\s*;?', '', suffix, count=1)
        else:
            prefix = result[:start_pos]
            suffix = result[pos:]
        
        # Parse cases
        cases = []
        default_case = None
        
        # Split by case/default - handle the fact that default might be at the end without trailing content
        # First, find all case/default positions
        case_matches = list(re.finditer(r'\n\s*(case|default)\b', switch_body))
        
        for i, match in enumerate(case_matches):
            keyword = match.group(1)
            start = match.end()
            end = case_matches[i+1].start() if i+1 < len(case_matches) else len(switch_body)
            case_content = switch_body[start:end].strip()
            
            if keyword == 'case':
                cases.append(case_content)
            elif keyword == 'default':
                default_case = 'default ' + case_content
        
        if is_expression and is_return_statement:
            # Convert switch expression in return statement to if-else statements
            # We need to replace the "return switch ... ;" with if-else chain
            new_code = convert_switch_expression_to_statements(
                prefix, switch_var, cases, default_case, base_indent, True
            )
            # Remove the "return " and any cast from prefix
            prefix = prefix.rstrip()
            # Remove trailing return and cast
            prefix = re.sub(r'return\s*\(\s*[A-Z][a-zA-Z_]*\s*\)\s*\(\s*$', '', prefix)
            prefix = re.sub(r'return\s*\(\s*[A-Z][a-zA-Z_]*\s*\)\s*$', '', prefix)
            prefix = re.sub(r'return\s*$', '', prefix)
            prefix = prefix.rstrip()
            result = prefix + '\n' + new_code + suffix
        elif is_expression:
            # Switch expression not in return - convert to if-else with variable
            # This is more complex, for now just use if-else with a result variable
            new_code = convert_switch_expression_to_statements(
                prefix, switch_var, cases, default_case, base_indent, False
            )
            # Need to declare result variable - this is tricky
            # For now, just use the expression form but with proper formatting
            new_code = convert_switch_to_ifelse_expression(switch_var, cases, default_case, base_indent)
            result = prefix + new_code + suffix
        else:
            # Switch statement - convert to if-else statements
            new_code = convert_switch_statement_to_ifelse(switch_var, cases, default_case, base_indent)
            result = prefix + new_code + suffix
        
        offset = len(prefix) + len(new_code) if 'new_code' in locals() else offset + match.end()
    
    return result


def convert_switch_to_ifelse_expression(switch_var, cases, default_case, base_indent):
    """Convert switch expression to if-else expression (not valid Java, but for reference)."""
    # This is a placeholder - Java doesn't have if-else expressions
    # For now, return a comment indicating manual fix needed
    return f'{base_indent}// TODO: Convert switch expression manually\n{base_indent}throw new UnsupportedOperationException("Switch expression conversion needed");'


def convert_switch_statement_to_ifelse(switch_var, cases, default_case, base_indent):
    """Convert switch statement to if-else statements."""
    result_parts = []
    first = True
    
    for case in cases:
        type_name, var_name, comp_info, body, is_expr = parse_case(case)
        if type_name is None:
            print(f"Warning: Could not parse case: {case[:100]}")
            continue
        
        is_singleton = type_name in SINGLETON_TYPES
        
        # Remove break statements from body
        body = re.sub(r'\n\s*break\s*;', '', body)
        body = re.sub(r'\s*break\s*;\s*$', '', body)
        body = body.strip()
        
        if comp_info:  # Record pattern
            comp_type, _ = comp_info
            accessor = RECORD_PATTERNS.get(type_name, (var_name, None))[0]
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {comp_type} {var_name} = (({type_name}) {switch_var}).{accessor}();')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {comp_type} {var_name} = (({type_name}) {switch_var}).{accessor}();')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
        elif is_singleton:
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name}) {{')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
        else:  # Type pattern
            if first:
                result_parts.append(f'{base_indent}if ({switch_var} instanceof {type_name} {var_name}) {{')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
                first = False
            else:
                result_parts.append(f'{base_indent}else if ({switch_var} instanceof {type_name} {var_name}) {{')
                result_parts.append(f'{base_indent}    {body}')
                result_parts.append(f'{base_indent}}}')
    
    if default_case:
        default_body = default_case.strip()
        # Remove default: or default ->
        default_body = re.sub(r'^(?:default\s*:\s*|->\s*)', '', default_body)
        default_body = default_body.strip()
        # Remove break statements
        default_body = re.sub(r'\n\s*break\s*;', '', default_body)
        default_body = re.sub(r'\s*break\s*;\s*$', '', default_body)
        
        # Only add default/else block if there's actual content
        if default_body and default_body != 'throw new IllegalStateException("Unexpected value")':
            if first:
                result_parts.append(f'{base_indent}{{')
                result_parts.append(f'{base_indent}    {default_body}')
                result_parts.append(f'{base_indent}}}')
            else:
                result_parts.append(f'{base_indent}else {{')
                result_parts.append(f'{base_indent}    {default_body}')
                result_parts.append(f'{base_indent}}}')
        elif default_body == 'throw new IllegalStateException("Unexpected value")':
            if not first:
                result_parts.append(f'{base_indent}else {{')
                result_parts.append(f'{base_indent}    throw new IllegalStateException("Unexpected value");')
                result_parts.append(f'{base_indent}}}')
        # If default_body is empty, don't add anything
    
    return '\n'.join(result_parts)


def process_file(filepath: Path):
    """Process a single Java file."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    content = convert_pattern_switch_to_ifelse(content)
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        print(f"Fixed: {filepath}")
        return True
    return False


def main():
    if len(sys.argv) < 2:
        print("Usage: python fix_pattern_switches.py <file1.java> [file2.java ...]")
        sys.exit(1)
    
    for arg in sys.argv[1:]:
        filepath = Path(arg)
        if filepath.exists():
            process_file(filepath)
        else:
            print(f"File not found: {filepath}")


if __name__ == '__main__':
    main()